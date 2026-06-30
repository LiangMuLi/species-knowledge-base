package com.species.controller;

import com.species.entity.User;
import com.species.mapper.UserMapper;
import com.species.util.Result;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理员功能 Controller
 *
 * 所有接口需要 admin 角色（SecurityConfig 已配置）
 * 功能：用户管理、重置密码、启用/禁用
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public AdminController(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 获取所有用户列表
     * GET /api/admin/users
     */
    @GetMapping("/users")
    public Result<?> listUsers() {
        List<User> users = userMapper.selectList(null);
        // 不返回密码
        users.forEach(u -> u.setPassword(null));
        return Result.success(users);
    }

    /**
     * 管理员重置用户密码
     * PUT /api/admin/users/{id}/reset-password
     * 请求体: {"newPassword": "xxx"}
     */
    @PutMapping("/users/{id}/reset-password")
    public Result<?> resetPassword(@PathVariable Long id,
                                   @RequestBody Map<String, String> body) {
        String newPassword = body.get("newPassword");
        if (newPassword == null || newPassword.length() < 6) {
            return Result.error(400, "密码至少 6 位");
        }

        User user = userMapper.selectById(id);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
        return Result.success("密码已重置");
    }

    /**
     * 启用/禁用用户
     * PUT /api/admin/users/{id}/status
     * 请求体: {"status": 1} 或 {"status": 0}
     */
    @PutMapping("/users/{id}/status")
    public Result<?> toggleStatus(@PathVariable Long id,
                                  @RequestBody Map<String, Integer> body) {
        Integer status = body.get("status");
        if (status == null || (status != 0 && status != 1)) {
            return Result.error(400, "状态值无效（0 或 1）");
        }

        User user = userMapper.selectById(id);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }

        user.setStatus(status);
        userMapper.updateById(user);
        return Result.success(status == 1 ? "用户已启用" : "用户已禁用");
    }
}
