package com.species.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.species.entity.SpeciesInfo;
import com.species.service.SpeciesInfoService;
import com.species.util.Result;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 物种信息 Controller
 *
 * 处理物种的增删改查
 * GET 接口公开，POST/PUT/DELETE 需管理员权限
 */
@RestController
@RequestMapping("/api/species")
public class SpeciesController {

    private final SpeciesInfoService speciesInfoService;

    public SpeciesController(SpeciesInfoService speciesInfoService) {
        this.speciesInfoService = speciesInfoService;
    }

    /**
     * 分页查询物种列表
     * GET /api/species?page=1&size=10&keyword=熊猫&categoryId=1&conservationStatus=EN&habitat=森林
     */
    @GetMapping
    public Result<?> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String conservationStatus,
            @RequestParam(required = false) String habitat,
            @RequestParam(defaultValue = "false") boolean showAll,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {

        Page<SpeciesInfo> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<SpeciesInfo> wrapper = new LambdaQueryWrapper<>();

        // 关键词搜索（中文名/英文名/学名）
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                .like(SpeciesInfo::getNameZh, keyword)
                .or()
                .like(SpeciesInfo::getNameEn, keyword)
                .or()
                .like(SpeciesInfo::getNameScientific, keyword));
        }

        // 分类筛选
        if (categoryId != null) {
            wrapper.eq(SpeciesInfo::getCategoryId, categoryId);
        }

        // 保护级别筛选
        if (StringUtils.hasText(conservationStatus)) {
            wrapper.eq(SpeciesInfo::getConservationStatus, conservationStatus);
        }

        // 栖息地筛选（模糊匹配）
        if (StringUtils.hasText(habitat)) {
            wrapper.like(SpeciesInfo::getHabitat, habitat);
        }

        // showAll=true 时不限制状态（管理员用）
        if (!showAll) {
            wrapper.eq(SpeciesInfo::getStatus, 1);
        }

        // 排序
        boolean asc = "asc".equalsIgnoreCase(sortOrder);
        if ("id".equals(sortBy)) {
            wrapper.orderBy(true, asc, SpeciesInfo::getId);
        } else if ("nameZh".equals(sortBy)) {
            wrapper.orderBy(true, asc, SpeciesInfo::getNameZh);
        } else if ("conservationStatus".equals(sortBy)) {
            wrapper.orderBy(true, asc, SpeciesInfo::getConservationStatus);
        } else {
            wrapper.orderBy(true, asc, SpeciesInfo::getUpdatedAt);
        }

        return Result.success(speciesInfoService.page(pageParam, wrapper));
    }

    /**
     * 搜索建议（输入关键词时返回匹配的物种名列表）
     * GET /api/species/suggestions?keyword=熊
     */
    @GetMapping("/suggestions")
    public Result<?> suggestions(@RequestParam String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return Result.success(List.of());
        }

        List<SpeciesInfo> list = speciesInfoService.lambdaQuery()
                .like(SpeciesInfo::getNameZh, keyword)
                .or()
                .like(SpeciesInfo::getNameEn, keyword)
                .or()
                .like(SpeciesInfo::getNameScientific, keyword)
                .eq(SpeciesInfo::getStatus, 1)
                .last("LIMIT 8")
                .list();

        List<Map<String, Object>> result = list.stream().map(s -> Map.<String, Object>of(
                "id", s.getId(),
                "nameZh", s.getNameZh(),
                "nameEn", s.getNameEn() != null ? s.getNameEn() : "",
                "conservationStatus", s.getConservationStatus() != null ? s.getConservationStatus() : ""
        )).collect(Collectors.toList());

        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<?> getById(@PathVariable Long id) {
        SpeciesInfo species = speciesInfoService.getById(id);
        if (species == null) {
            return Result.error(404, "物种不存在");
        }
        return Result.success(species);
    }

    @PostMapping
    public Result<?> create(@RequestBody SpeciesInfo species,
                            @AuthenticationPrincipal Long userId) {
        if (species.getStatus() == null) species.setStatus(1);
        species.setCreatedBy(userId);
        speciesInfoService.save(species);
        return Result.success(species);
    }

    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody SpeciesInfo species) {
        species.setId(id);
        speciesInfoService.updateById(species);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        speciesInfoService.removeById(id);
        return Result.success();
    }

    /**
     * 批量删除物种
     * DELETE /api/species/batch
     * 请求体: { "ids": [1, 2, 3] }
     */
    @DeleteMapping("/batch")
    public Result<?> batchDelete(@RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body.get("ids");
        if (ids == null || ids.isEmpty()) {
            return Result.error(400, "请选择要删除的物种");
        }
        speciesInfoService.removeByIds(ids);
        return Result.success("已删除 " + ids.size() + " 条记录");
    }
}
