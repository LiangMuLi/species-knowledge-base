package com.species.controller;

import com.species.entity.SpeciesCategory;
import com.species.service.SpeciesCategoryService;
import com.species.util.Result;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分类 Controller
 *
 * 物种分类是固定的，提供查询接口供前端下拉框和分类导航使用
 */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final SpeciesCategoryService categoryService;

    public CategoryController(SpeciesCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * 获取所有分类（树形结构）
     * GET /api/categories
     */
    @GetMapping
    public Result<?> list() {
        List<SpeciesCategory> list = categoryService.lambdaQuery()
                .orderByAsc(SpeciesCategory::getSortOrder)
                .list();
        return Result.success(list);
    }

    /**
     * 获取单个分类
     * GET /api/categories/{id}
     */
    @GetMapping("/{id}")
    public Result<?> getById(@PathVariable Long id) {
        SpeciesCategory category = categoryService.getById(id);
        if (category == null) {
            return Result.error(404, "分类不存在");
        }
        return Result.success(category);
    }

    /**
     * 新增分类
     * POST /api/categories
     */
    @PostMapping
    public Result<?> create(@RequestBody SpeciesCategory category) {
        categoryService.save(category);
        return Result.success(category);
    }

    /**
     * 删除分类
     * DELETE /api/categories/{id}
     */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        categoryService.removeById(id);
        return Result.success();
    }
}
