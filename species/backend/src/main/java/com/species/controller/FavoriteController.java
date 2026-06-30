package com.species.controller;

import com.species.entity.SpeciesInfo;
import com.species.entity.UserFavorite;
import com.species.service.FavoriteService;
import com.species.service.SpeciesInfoService;
import com.species.util.Result;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 收藏 Controller
 *
 * 所有接口需要登录（已在 SecurityConfig 里配置）
 */
@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final SpeciesInfoService speciesInfoService;

    public FavoriteController(FavoriteService favoriteService,
                              SpeciesInfoService speciesInfoService) {
        this.favoriteService = favoriteService;
        this.speciesInfoService = speciesInfoService;
    }

    /**
     * 获取当前用户收藏的所有物种（带详情）
     * GET /api/favorites
     */
    @GetMapping
    public Result<?> list(@AuthenticationPrincipal Long userId) {
        List<UserFavorite> favorites = favoriteService.getUserFavorites(userId);
        if (favorites.isEmpty()) {
            return Result.success(List.of());  // 空列表直接返回，否则 MySQL 5.6 的 IN() 会报语法错误
        }
        // 查询物种详情
        List<Long> speciesIds = favorites.stream()
                .map(UserFavorite::getSpeciesId)
                .collect(Collectors.toList());
        List<SpeciesInfo> speciesList = speciesInfoService.listByIds(speciesIds);
        return Result.success(speciesList);
    }

    /**
     * 获取当前用户收藏的物种 ID 集合（前端用来渲染收藏状态）
     * GET /api/favorites/ids
     */
    @GetMapping("/ids")
    public Result<?> getFavoriteIds(@AuthenticationPrincipal Long userId) {
        Set<Long> ids = favoriteService.getUserFavoriteIds(userId);
        return Result.success(ids);
    }

    /**
     * 添加收藏
     * POST /api/favorites/{speciesId}
     */
    @PostMapping("/{speciesId}")
    public Result<?> add(@AuthenticationPrincipal Long userId,
                         @PathVariable Long speciesId) {
        boolean success = favoriteService.addFavorite(userId, speciesId);
        if (success) {
            return Result.success("收藏成功");
        }
        return Result.error(400, "已收藏过了");
    }

    /**
     * 取消收藏
     * DELETE /api/favorites/{speciesId}
     */
    @DeleteMapping("/{speciesId}")
    public Result<?> remove(@AuthenticationPrincipal Long userId,
                            @PathVariable Long speciesId) {
        favoriteService.removeFavorite(userId, speciesId);
        return Result.success("已取消收藏");
    }
}
