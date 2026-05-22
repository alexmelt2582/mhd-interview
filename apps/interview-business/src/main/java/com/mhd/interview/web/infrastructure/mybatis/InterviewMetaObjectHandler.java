package com.mhd.interview.web.infrastructure.mybatis;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 字段自动填充处理器
 * <p>
 * 在 INSERT 时自动填充 {@code createTime} 和 {@code updateTime}，
 * 在 UPDATE 时自动填充 {@code updateTime}。
 * 实体类字段须标注 {@code @TableField(fill = FieldFill.INSERT)} 或
 * {@code @TableField(fill = FieldFill.INSERT_UPDATE)}。
 *
 * @author mhd
 */
@Component
@Slf4j
public class InterviewMetaObjectHandler implements MetaObjectHandler {

    /**
     * INSERT 时自动填充 createTime 和 updateTime
     *
     * @param metaObject MyBatis 反射元对象
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        // 填充创建时间（仅 INSERT 时）
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        // 填充更新时间（INSERT 时也初始化）
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
    }

    /**
     * UPDATE 时自动填充 updateTime
     *
     * @param metaObject MyBatis 反射元对象
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        // 仅更新 updateTime，不修改 createTime
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
