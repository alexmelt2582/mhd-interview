package com.mhd.interview.web.modules.resume.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mhd.interview.web.modules.resume.entity.ResumeEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 简历 Mapper 接口
 *
 * @author zhao-hao-dong
 */
@Mapper
public interface ResumeMapper extends BaseMapper<ResumeEntity> {

    /**
     * 根据文件哈希查询简历（用于去重判断）
     *
     * @param fileHash SHA-256 文件哈希
     * @return 已存在的简历实体，不存在返回 null
     */
    @Select("SELECT * FROM t_resume WHERE file_hash = #{fileHash} AND is_deleted = false LIMIT 1")
    ResumeEntity findByFileHash(String fileHash);
}
