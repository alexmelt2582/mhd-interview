package com.mhd.interview.web.modules.resume.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mhd.interview.common.enums.ErrorCodeEnum;
import com.mhd.interview.common.enums.ResumeAnalysisStatusEnum;
import com.mhd.interview.common.exception.BusinessException;
import com.mhd.interview.common.mybatis.domain.PageParam;
import com.mhd.interview.common.mybatis.domain.PageResponse;
import com.mhd.interview.common.mybatis.domain.PageResultUtils;
import com.mhd.interview.common.mybatis.util.MybatisPlusUtils;
import com.mhd.interview.common.utils.StringUtils;
import com.mhd.interview.web.infrastructure.file.FileParseService;
import com.mhd.interview.web.infrastructure.file.FileStorageService;
import com.mhd.interview.web.infrastructure.file.TextCleaningService;
import com.mhd.interview.web.modules.resume.convert.ResumeStructMapper;
import com.mhd.interview.web.modules.resume.dto.ResumeDetailDTO;
import com.mhd.interview.web.modules.resume.dto.ResumeListItemDTO;
import com.mhd.interview.web.modules.resume.dto.ResumeUploadResultDTO;
import com.mhd.interview.web.modules.resume.entity.ResumeEntity;
import com.mhd.interview.web.modules.resume.mapper.ResumeMapper;
import com.mhd.interview.web.modules.resume.service.IResumeService;
import com.mhd.interview.web.modules.resume.stream.ResumeAnalysisProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 简历管理服务实现类
 *
 * @author zhao-hao-dong
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeServiceImpl implements IResumeService {

    /**
     * 简历 Mapper
     */
    private final ResumeMapper baseMapper;
    /**
     * 文件存储服务
     */
    private final FileStorageService fileStorageService;
    /**
     * 文件解析服务
     */
    private final FileParseService fileParseService;
    /**
     * 文本清洗服务
     */
    private final TextCleaningService textCleaningService;

    /**
     * 简历分析任务生产者
     */
    private final ResumeAnalysisProducer resumeAnalysisProducer;
    private final ResumeStructMapper resumeStructMapper;

    /**
     * 允许上传的简历文件 MIME 类型
     */
    private static final Set<String> ALLOWED_RESUME_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain"
    );

    private static final long RESUME_MAX_FILE_SIZE = 20 * 1024 * 1024;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResumeUploadResultDTO uploadResume(MultipartFile file) {
        // 1. 校验文件合法性（类型 + 大小）
        fileParseService.validateFile(file, RESUME_MAX_FILE_SIZE, ALLOWED_RESUME_TYPES, "简历");

        String fileName = file.getOriginalFilename();
        long fileSize = file.getSize();
        log.info("收到简历上传请求: {}, 大小: {} bytes ({}), 上传开始处理",
                fileName, fileSize, fileParseService.formatFileSize(fileSize));

        // 2. 计算文件哈希，进行去重检查
        String fileHash;
        try (InputStream hashStream = file.getInputStream()) {
            fileHash = fileParseService.calculateHash(hashStream);
        } catch (IOException e) {
            throw new BusinessException(ErrorCodeEnum.RESUME_PARSE_FAILED, "文件读取失败");
        }

        // 检查是否已存在相同哈希的简历
        ResumeEntity existing = baseMapper.findByFileHash(fileHash);
        if (existing != null) {
            log.info("检测到重复简历，fileHash={}, existingId={}", fileHash, existing.getId());
            return resumeStructMapper.toResumeUploadResultDTO(existing, true);
        }

        // 3. 解析文件文本内容
        String parsedText;
        try (InputStream parseStream = file.getInputStream()) {
            // 先使用 Tika 解析文本内容
            parsedText = fileParseService.tikaParseText(parseStream);
            // 对解析结果进行清洗，去除多余空白、特殊字符等
            parsedText = textCleaningService.cleanText(parsedText);
        } catch (IOException e) {
            throw new BusinessException(ErrorCodeEnum.RESUME_PARSE_FAILED, "文件读取失败");
        }
        log.info("简历文本解析完成: {} - 文本长度: {} 字符",
                fileName, parsedText.length());

        // 4. 上传文件到存储设备
        String fileKey = fileStorageService.uploadResume(file);

        // 5. 保存简历记录到数据库
        ResumeEntity entity = new ResumeEntity();
        entity.setOriginalName(file.getOriginalFilename());
        entity.setFileHash(fileHash);
        entity.setFileSize(file.getSize());
        entity.setContentType(file.getContentType());
        entity.setStorageKey(fileKey);
        entity.setParsedText(parsedText);
        entity.setAnalysisStatus(ResumeAnalysisStatusEnum.PENDING.getCode());
        baseMapper.insert(entity);

        // 6. 发送异步分析任务到 Redis Stream
        resumeAnalysisProducer.sendAnalyzeTask(entity.getId(), entity.getParsedText());
        log.info("简历上传完成，resumeId={}, fileHash={}", entity.getId(), fileHash);

        return resumeStructMapper.toResumeUploadResultDTO(entity, false);
    }

    @Override
    public List<ResumeListItemDTO> selectResumeList() {
        List<ResumeEntity> resumes = baseMapper.selectList(null);
        return resumes.stream().map(resume -> {
            // 获取最新分析结果的分数
            Integer latestScore = null;
            LocalDateTime lastAnalysisTime = null;
            // 获取最新的分析结果
            // TODO 添加分析结果
            //ResumeAnalysisEntity resumeAnalysis = res.findFirstByResumeId(resume.getId());
            //if (!Objects.isNull(resumeAnalysis)) {
            //    latestScore = resumeAnalysis.getScore();
            //    lastAnalysisTime = resumeAnalysis.getCreateTime();
            //}
            // 获取面试次数
            // TODO 0
            //int interviewCount = interviewSessionService.findByResumeId(resume.getId()).size();
            // 使用 MapStruct 映射
            return resumeStructMapper.toResumeListItemDTO(resume, latestScore, lastAnalysisTime, 0);
        }).toList();
    }

    @Override
    public PageResponse<ResumeListItemDTO> selectResumePageList(PageParam pageParam) {
        Page<ResumeEntity> page = MybatisPlusUtils.buildPage(pageParam, null);
        LambdaQueryWrapper<ResumeEntity> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.orderByDesc(ResumeEntity::getCreateTime);
        page = baseMapper.selectPage(page,
                queryWrapper);

        List<ResumeListItemDTO> list = page.getRecords().stream()
                .map(resume -> {
                    // 获取最新分析结果的分数
                    Integer latestScore = null;
                    LocalDateTime lastAnalysisTime = null;
                    // 获取最新的分析结果
                    // TODO 添加分析结果
                    //ResumeAnalysisEntity resumeAnalysis = res.findFirstByResumeId(resume.getId());
                    //if (!Objects.isNull(resumeAnalysis)) {
                    //    latestScore = resumeAnalysis.getScore();
                    //    lastAnalysisTime = resumeAnalysis.getCreateTime();
                    //}
                    // 获取面试次数
                    // TODO 0
                    //int interviewCount = interviewSessionService.findByResumeId(resume.getId()).size();
                    // 使用 MapStruct 映射
                    return resumeStructMapper.toResumeListItemDTO(resume, latestScore, lastAnalysisTime, 0);
                })
                .toList();

        return PageResultUtils.build(list, page.getTotal());
    }

    @Override
    public ResumeEntity selectResumeById(Long id) {
        if (Objects.isNull(id)) return null;
        return baseMapper.selectById(id);
    }

    @Override
    public ResumeDetailDTO selectResumeDetailById(Long id) {
        ResumeEntity entity = baseMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.RESUME_NOT_FOUND, "简历不存在: " + id);
        }
        // 生成预签名下载 URL
        String fileUrl = fileStorageService.getFileUrl(entity.getStorageKey());
        // TODO List.of()
        return resumeStructMapper.toResumeDetailDTO(entity, List.of(), fileUrl);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateResumeAnalysisInfo(Long id, ResumeAnalysisStatusEnum resumeAnalysisStatusEnum, String errorMessage) {
        ResumeEntity entity = baseMapper.selectById(id);
        if (Objects.nonNull(entity)) {
            entity.setAnalysisStatus(resumeAnalysisStatusEnum.getCode());
            entity.setAnalysisError(errorMessage);
            baseMapper.updateById(entity);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteResumeByIds(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) return;
        for (Long id : ids) {
            ResumeEntity entity = baseMapper.selectById(id);
            if (entity == null) {
                continue;
            }
            // 1. 删除存储文件，删除失败不影响后续删除
            fileStorageService.deleteFile(entity.getStorageKey());
            // 2. 删除面试会话
            // TODO 删除面试会话
            // 3. 删除分析记录
            // TODO 删除分析记录
            // 4. 删除数据库记录
            baseMapper.deleteById(id);
            log.info("简历删除完成: id={}", id);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reanalyze(Long resumeId) {
        ResumeEntity resume = baseMapper.selectById(resumeId);
        if (resume == null) {
            throw new BusinessException(ErrorCodeEnum.RESUME_NOT_FOUND, "简历不存在: " + id);
        }
        log.info("开始重新分析简历: resumeId={}, filename={}", resumeId, resume.getOriginalFilename());
        String parsedText = resume.getParsedText();
        if (StringUtils.isBlank(parsedText)) {
            throw new BusinessException(ErrorCodeEnum.RESUME_PARSE_FAILED, "简历文本内容为空，无法分析");
        }
        // 更新状态
        resume.setAnalysisStatus(ResumeAnalysisStatusEnum.PENDING.getCode());
        resume.setAnalysisError(null);
        baseMapper.updateById(resume);
        // 发送分析任务到 Stream
        resumeAnalysisProducer.sendAnalyzeTask(resumeId, parsedText);
        log.info("重新分析任务已发送: resumeId={}", resumeId);
    }
}
