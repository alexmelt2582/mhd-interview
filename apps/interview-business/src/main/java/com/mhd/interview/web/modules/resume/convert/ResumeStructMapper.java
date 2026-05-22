package com.mhd.interview.web.modules.resume.convert;

import com.mhd.interview.common.enums.EnumUtil;
import com.mhd.interview.common.enums.ResumeAnalysisStatusEnum;
import com.mhd.interview.common.utils.json.JsonUtils;
import com.mhd.interview.web.modules.resume.dto.ResumeAnalysisDTO;
import com.mhd.interview.web.modules.resume.dto.ResumeDetailDTO;
import com.mhd.interview.web.modules.resume.dto.ResumeListItemDTO;
import com.mhd.interview.web.modules.resume.dto.ResumeUploadResultDTO;
import com.mhd.interview.web.modules.resume.entity.ResumeAnalysisEntity;
import com.mhd.interview.web.modules.resume.entity.ResumeEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * @author zhao-hao-dong
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ResumeStructMapper {
    /**
     * 将实体基础字段映射到DTO的ScoreDetail
     */
    //@Mapping(target = "contentScore", source = "contentScore", qualifiedByName = "nullToZero")
    //@Mapping(target = "structScore", source = "structScore", qualifiedByName = "nullToZero")
    //@Mapping(target = "skillScore", source = "skillScore", qualifiedByName = "nullToZero")
    //@Mapping(target = "expressionScore", source = "expressionScore", qualifiedByName = "nullToZero")
    //@Mapping(target = "projectScore", source = "projectScore", qualifiedByName = "nullToZero")
    //ResumeAnalysisResponse.ScoreDetail toScoreDetail(ResumeAnalysis entity);

    /**
     * 将简历实体转换为简历上传结果 DTO
     *
     * @param resume      简历实体
     * @param isDuplicate 是否重复上传
     * @return 简历上传结果 DTO
     */
    default ResumeUploadResultDTO toResumeUploadResultDTO(
            ResumeEntity resume,
            boolean isDuplicate
    ) {
        return new ResumeUploadResultDTO(
                String.valueOf(resume.getId()),
                resume.getOriginalName(),
                EnumUtil.getDescriptionByCode(resume.getAnalysisStatus(), ResumeAnalysisStatusEnum.class),
                isDuplicate,
                resume.getCreateTime()
        );
    }

    /**
     * 将简历实体转换为列表项 DTO
     *
     * @param resume           简历实体
     * @param latestScore      最新分析评分（无分析时为 null）
     * @param lastAnalysisTime 最近分析时间（无分析时为 null）
     * @param interviewCount   关联面试次数
     * @return 简历列表项 DTO
     */
    default ResumeListItemDTO toResumeListItemDTO(
            ResumeEntity resume,
            Integer latestScore,
            LocalDateTime lastAnalysisTime,
            Integer interviewCount
    ) {
        return new ResumeListItemDTO(
                String.valueOf(resume.getId()),
                resume.getOriginalName(),
                resume.getFileSize(),
                resume.getCreateTime(),
                resume.getAccessCount(),
                EnumUtil.getDescriptionByCode(resume.getAnalysisStatus(), ResumeAnalysisStatusEnum.class),
                latestScore,
                lastAnalysisTime,
                interviewCount
        );
    }

    /**
     * 将简历实体和分析历史列表转换为详情 DTO
     *
     * @param resume     简历实体
     * @param analyses   分析历史实体列表
     * @param storageUrl 文件预签名 URL
     * @return 简历详情 DTO
     */
    default ResumeDetailDTO toResumeDetailDTO(
            ResumeEntity resume,
            List<ResumeAnalysisEntity> analyses,
            String storageUrl
    ) {
        List<ResumeAnalysisDTO> analysisDTOs = analyses.stream()
                .map(this::toResumeAnalysisDTO)
                .toList();
        return new ResumeDetailDTO(
                String.valueOf(resume.getId()),
                resume.getOriginalName(),
                resume.getFileSize(),
                resume.getContentType(),
                storageUrl,
                resume.getCreateTime(),
                resume.getAccessCount(),
                resume.getParsedText(),
                EnumUtil.getDescriptionByCode(resume.getAnalysisStatus(), ResumeAnalysisStatusEnum.class),
                resume.getAnalysisError(),
                analysisDTOs,
                Collections.emptyList()
        );
    }

    /**
     * 将分析结果实体转换为 ResumeAnalysisDTO
     *
     * @param resumeAnalysis 分析结果实体
     * @return ResumeAnalysisDTO
     */
    default ResumeAnalysisDTO toResumeAnalysisDTO(
            ResumeAnalysisEntity resumeAnalysis
    ) {
        List<String> strengths = JsonUtils.parseArray(resumeAnalysis.getStrengths(), String.class);
        List<ResumeAnalysisDTO.SuggestionDTO> suggestions = JsonUtils.parseArray(resumeAnalysis.getSuggestions(), ResumeAnalysisDTO.SuggestionDTO.class);
        return new ResumeAnalysisDTO(
                String.valueOf(resumeAnalysis.getId()),
                String.valueOf(resumeAnalysis.getResumeId()),
                resumeAnalysis.getScore(),
                resumeAnalysis.getContentScore(),
                resumeAnalysis.getStructureScore(),
                resumeAnalysis.getSkillMatchScore(),
                resumeAnalysis.getExpressionScore(),
                resumeAnalysis.getProjectScore(),
                resumeAnalysis.getSummary(),
                resumeAnalysis.getCreateTime(),
                strengths,
                suggestions
        );
    }

    /**
     * 从 ResumeAnalysisResponse 创建 ResumeAnalysisEntity
     * 注意：JSON 字段和 Resume 关联需要在 Service 层设置
     */
    //@Mapping(target = "id", ignore = true)
    //@Mapping(target = "resume", ignore = true)
    //@Mapping(target = "strengthsJson", ignore = true)
    //@Mapping(target = "suggestsJson", ignore = true)
    //@Mapping(target = "analysisTime", ignore = true)
    //@Mapping(target = "contentScore", source = "scoreDetail.contentScore")
    //@Mapping(target = "structScore", source = "scoreDetail.structScore")
    //@Mapping(target = "skillScore", source = "scoreDetail.skillScore")
    //@Mapping(target = "expressionScore", source = "scoreDetail.expressionScore")
    //@Mapping(target = "projectScore", source = "scoreDetail.projectScore")
    //ResumeAnalysis toAnalysis(ResumeAnalysisResponse response);
    @Named("nullToZero")
    default int nullToZero(Integer value) {
        return value != null ? value : 0;
    }
}
