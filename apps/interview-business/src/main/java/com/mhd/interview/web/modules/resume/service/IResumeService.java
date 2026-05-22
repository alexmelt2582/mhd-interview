package com.mhd.interview.web.modules.resume.service;

import com.mhd.interview.common.enums.ResumeAnalysisStatusEnum;
import com.mhd.interview.common.mybatis.domain.PageParam;
import com.mhd.interview.common.mybatis.domain.PageResponse;
import com.mhd.interview.web.modules.resume.dto.ResumeDetailDTO;
import com.mhd.interview.web.modules.resume.dto.ResumeListItemDTO;
import com.mhd.interview.web.modules.resume.dto.ResumeUploadResultDTO;
import com.mhd.interview.web.modules.resume.entity.ResumeEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 简历管理服务接口
 *
 * @author zhao-hao-dong
 */
public interface IResumeService {

    /**
     * 上传简历并触发异步分析流程
     * <p>
     * 校验文件类型和大小，计算哈希去重，保存文件至存储平台，
     * 提取文本内容，创建简历记录，并发送异步分析消息到 Redis Stream
     *
     * @param file 上传的简历文件（支持 PDF、DOCX、DOC、TXT、MD等）
     * @return 上传结果（含简历 ID、状态、是否重复）
     */
    ResumeUploadResultDTO uploadResume(MultipartFile file);

    /**
     * 查询所有简历列表
     *
     * @return 简历列表
     */
    List<ResumeListItemDTO> selectResumeList();

    /**
     * 分页查询简历列表
     *
     * @param pageParam 分页参数（当前页、每页数量）
     * @return 简历分页列表
     */
    PageResponse<ResumeListItemDTO> selectResumePageList(PageParam pageParam);

    /**
     * 根据 ID 查询简历
     *
     * @param id 简历 ID
     * @return 简历
     */
    ResumeEntity selectResumeById(Long id);

    /**
     * 根据 ID 查询简历详情（含文件预签名 URL）
     *
     * @param id 简历 ID
     * @return 简历详情，不存在时抛出 BusinessException
     */
    ResumeDetailDTO selectResumeDetailById(Long id);

    /**
     * 更新简历分析信息（分析完成后调用，更新分析状态、结果等字段）
     *
     * @param id 简历 ID
     */
    void updateResumeAnalysisInfo(Long id, ResumeAnalysisStatusEnum resumeAnalysisStatusEnum, String errorMessage);

    /**
     * 批量删除简历（同时删除存储文件、面试会话、分析记录）
     *
     * @param ids 简历 ID 列表
     */
    void deleteResumeByIds(List<Long> ids);

    /**
     * 重新分析简历
     *
     * @param resumeId 简历 ID
     */
    void reanalyze(Long resumeId);
}
