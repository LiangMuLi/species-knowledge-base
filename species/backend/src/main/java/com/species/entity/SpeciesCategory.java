package com.species.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 物种分类实体
 * 对应 species_category 表，支持多级分类（parentId 指向父分类）
 */
@Data
@TableName("species_category")
public class SpeciesCategory {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /**
     * 父分类ID，顶级分类为 0
     */
    private Long parentId;

    private Integer sortOrder;

    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
