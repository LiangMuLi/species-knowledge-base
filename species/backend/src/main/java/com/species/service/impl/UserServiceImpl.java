package com.species.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.species.entity.User;
import com.species.mapper.UserMapper;
import com.species.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户 Service 实现类
 *
 * ServiceImpl 是 MyBatis-Plus 提供的默认实现，包含:
 *   save/remove/update/get/list/page 等 20+ 方法
 * 不需要的 CRUD 方法可以不覆盖
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User login(String username, String password) {
        // 按用户名查找用户
        User user = lambdaQuery()
                .eq(User::getUsername, username)
                .one();

        if (user == null) {
            return null;
        }

        // BCrypt 验证密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return null;
        }

        return user;
    }

    @Override
    public User register(String username, String password, String nickname) {
        // 检查用户名是否已存在
        User existing = lambdaQuery()
                .eq(User::getUsername, username)
                .one();

        if (existing != null) {
            throw new RuntimeException("用户名已存在");
        }

        // 创建新用户
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(nickname != null && !nickname.isEmpty() ? nickname : username);
        user.setRole("user");   // 普通用户角色
        user.setStatus(1);      // 正常状态

        save(user);
        return user;
    }
}
