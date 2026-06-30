package com.species.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.species.entity.UserFavorite;
import com.species.mapper.FavoriteMapper;
import com.species.service.FavoriteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 收藏 Service 实现
 */
@Service
public class FavoriteServiceImpl extends ServiceImpl<FavoriteMapper, UserFavorite> implements FavoriteService {

    @Override
    @Transactional
    public boolean addFavorite(Long userId, Long speciesId) {
        // 检查是否已收藏
        if (isFavorited(userId, speciesId)) {
            return false;
        }

        UserFavorite favorite = new UserFavorite();
        favorite.setUserId(userId);
        favorite.setSpeciesId(speciesId);
        save(favorite);
        return true;
    }

    @Override
    @Transactional
    public boolean removeFavorite(Long userId, Long speciesId) {
        return lambdaUpdate()
                .eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getSpeciesId, speciesId)
                .remove();
    }

    @Override
    public boolean isFavorited(Long userId, Long speciesId) {
        return lambdaQuery()
                .eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getSpeciesId, speciesId)
                .count() > 0;
    }

    @Override
    public Set<Long> getUserFavoriteIds(Long userId) {
        return lambdaQuery()
                .eq(UserFavorite::getUserId, userId)
                .list()
                .stream()
                .map(UserFavorite::getSpeciesId)
                .collect(Collectors.toSet());
    }

    @Override
    public List<UserFavorite> getUserFavorites(Long userId) {
        return lambdaQuery()
                .eq(UserFavorite::getUserId, userId)
                .orderByDesc(UserFavorite::getCreatedAt)
                .list();
    }
}
