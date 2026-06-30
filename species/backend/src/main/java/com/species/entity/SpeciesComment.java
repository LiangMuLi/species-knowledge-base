package com.species.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 物种评论实体
 * 对应 species_comment 表
 */
@Data
@TableName("species_comment")
public class SpeciesComment {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long speciesId;

    private Long userId;

    private String content;

    private Integer rating;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    // 非数据库字段 — 关联查询用
    @TableField(exist = false)
    private String nickname;
}
