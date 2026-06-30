package com.species.controller;

import com.species.entity.SpeciesComment;
import com.species.entity.User;
import com.species.service.SpeciesCommentService;
import com.species.service.UserService;
import com.species.util.Result;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 评论 Controller
 *
 * 评论是公开的（查看），但发表需要登录
 */
@RestController
@RequestMapping("/api/comments")
public class CommentController {

    private final SpeciesCommentService commentService;
    private final UserService userService;

    public CommentController(SpeciesCommentService commentService, UserService userService) {
        this.commentService = commentService;
        this.userService = userService;
    }

    /**
     * 获取物种的评论列表
     * GET /api/comments/{speciesId}
     */
    @GetMapping("/{speciesId}")
    public Result<?> list(@PathVariable Long speciesId) {
        List<SpeciesComment> comments = commentService.getCommentsWithUser(speciesId);

        // 关联用户昵称
        List<Map<String, Object>> result = comments.stream().map(c -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", c.getId());
            item.put("content", c.getContent());
            item.put("rating", c.getRating());
            item.put("createdAt", c.getCreatedAt());

            User user = userService.getById(c.getUserId());
            item.put("nickname", user != null ? user.getNickname() : "未知用户");

            return item;
        }).collect(Collectors.toList());

        // 计算平均评分
        Double avgRating = comments.stream()
                .filter(c -> c.getRating() != null)
                .collect(Collectors.averagingInt(SpeciesComment::getRating));

        Map<String, Object> data = new HashMap<>();
        data.put("comments", result);
        data.put("total", result.size());
        data.put("avgRating", avgRating.isNaN() ? 0 : Math.round(avgRating * 10) / 10.0);

        return Result.success(data);
    }

    /**
     * 发表评论
     * POST /api/comments/{speciesId}
     */
    @PostMapping("/{speciesId}")
    public Result<?> add(@AuthenticationPrincipal Long userId,
                         @PathVariable Long speciesId,
                         @RequestBody Map<String, Object> body) {
        String content = (String) body.get("content");
        if (content == null || content.trim().isEmpty()) {
            return Result.error(400, "评论内容不能为空");
        }

        SpeciesComment comment = new SpeciesComment();
        comment.setSpeciesId(speciesId);
        comment.setUserId(userId);
        comment.setContent(content.trim());

        // 可选评分
        Object ratingObj = body.get("rating");
        if (ratingObj instanceof Integer) {
            int rating = (Integer) ratingObj;
            if (rating >= 1 && rating <= 5) {
                comment.setRating(rating);
            }
        }

        commentService.save(comment);
        return Result.success("评论成功");
    }

    /**
     * 删除评论（仅评论作者或管理员可删）
     * DELETE /api/comments/{id}
     */
    @DeleteMapping("/{id}")
    public Result<?> delete(@AuthenticationPrincipal Long userId,
                            @PathVariable Long id) {
        SpeciesComment comment = commentService.getById(id);
        if (comment == null) {
            return Result.error(404, "评论不存在");
        }

        // 只能删除自己的评论（管理员可以在前端隐藏删除按钮）
        if (!comment.getUserId().equals(userId)) {
            return Result.error(403, "只能删除自己的评论");
        }

        commentService.removeById(id);
        return Result.success("删除成功");
    }
}
