package com.species.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户收藏实体
 * 对应 user_favorite 表，记录用户收藏的物种
 */
@Data
@TableName("user_favorite")
public class UserFavorite {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long speciesId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
