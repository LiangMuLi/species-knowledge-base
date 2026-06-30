package com.species.controller;

import com.species.dto.LoginRequest;
import com.species.dto.RegisterRequest;
import com.species.entity.User;
import com.species.service.UserService;
import com.species.util.JwtUtil;
import com.species.util.Result;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证 Controller
 *
 * 处理登录、注册、获取当前用户信息等认证相关请求
 * 不需要 token 就能访问（在 SecurityConfig 中 permitAll）
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    public AuthController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * 用户登录
     * POST /api/auth/login
     *
     * 请求体: {"username": "admin", "password": "admin123"}
     * 响应: {"code": 200, "data": {"token": "xxx", "userInfo": {...}}}
     */
    @PostMapping("/login")
    public Result<?> login(@Valid @RequestBody LoginRequest request) {
        User user = userService.login(request.getUsername(), request.getPassword());

        if (user == null) {
            return Result.error(401, "用户名或密码错误");
        }

        // 生成 JWT token（有效期 24 小时）
        String token = jwtUtil.generateToken(user.getId(), user.getRole());

        // 用 HashMap 以避免 Map.of() 报 NPE（数据库字段可能为 null）
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("nickname", user.getNickname());
        userInfo.put("avatar", user.getAvatar());
        userInfo.put("role", user.getRole());

        return Result.success(Map.of("token", token, "userInfo", userInfo));
    }

    /**
     * 用户注册
     * POST /api/auth/register
     *
     * 请求体: {"username": "newuser", "password": "123456", "nickname": "昵称"}
     * 注册成功后自动返回 token，用户无需再次登录
     */
    @PostMapping("/register")
    public Result<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            User user = userService.register(
                    request.getUsername(),
                    request.getPassword(),
                    request.getNickname()
            );

            // 注册成功直接返回 token，让用户自动登录
            String token = jwtUtil.generateToken(user.getId(), user.getRole());

            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("username", user.getUsername());
            userInfo.put("nickname", user.getNickname());
            userInfo.put("avatar", user.getAvatar());
            userInfo.put("role", user.getRole());

            return Result.success(Map.of("token", token, "userInfo", userInfo));
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }
    }

    /**
     * 获取当前登录用户信息
     * GET /api/auth/me
     *
     * @AuthenticationPrincipal 从 SecurityContext 中获取当前用户ID
     * （在 JwtAuthFilter 中已写入）
     */
    @GetMapping("/me")
    public Result<?> getCurrentUser(@AuthenticationPrincipal Long userId) {
        User user = userService.getById(userId);
        if (user == null) {
            return Result.unauthorized("用户不存在");
        }

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("nickname", user.getNickname());
        userInfo.put("avatar", user.getAvatar());
        userInfo.put("role", user.getRole());

        return Result.success(userInfo);
    }
}
