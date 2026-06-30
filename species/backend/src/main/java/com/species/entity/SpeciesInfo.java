package com.species.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 物种信息实体（核心表）
 * 对应 species_info 表，存储物种的详细数据
 */
@Data
@TableName("species_info")
public class SpeciesInfo {
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属分类ID，关联 species_category.id
     */
    private Long categoryId;

    private String nameZh;

    private String nameEn;

    private String nameScientific;

    private String alias;

    private String description;

    private String habitat;

    private String distribution;

    /**
     * 保护级别：CR(极危) EN(濒危) VU(易危) NT(近危) LC(无危)
     */
    private String conservationStatus;

    private String imageUrl;

    private String weight;

    private String lifespan;

    private String diet;

    private String reproduction;

    private String funFacts;

    private Integer isEndemic;

    private Integer status;

    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
