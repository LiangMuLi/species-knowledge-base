package com.species.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.species.entity.UserFavorite;

import java.util.List;
import java.util.Set;

/**
 * 收藏 Service 接口
 */
public interface FavoriteService extends IService<UserFavorite> {

    /**
     * 添加收藏
     * @return 是否成功（已收藏返回 false）
     */
    boolean addFavorite(Long userId, Long speciesId);

    /**
     * 取消收藏
     */
    boolean removeFavorite(Long userId, Long speciesId);

    /**
     * 检查是否已收藏
     */
    boolean isFavorited(Long userId, Long speciesId);

    /**
     * 获取用户所有收藏的物种 ID 集合
     */
    Set<Long> getUserFavoriteIds(Long userId);

    /**
     * 获取用户收藏的物种列表（分页）
     */
    List<UserFavorite> getUserFavorites(Long userId);
}
