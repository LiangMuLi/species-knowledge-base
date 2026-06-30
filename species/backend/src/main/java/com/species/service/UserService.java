package com.species.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.species.entity.User;

/**
 * 用户 Service 接口
 * IService 提供了批量操作、链式查询等高级功能
 */
public interface UserService extends IService<User> {

    /**
     * 用户登录验证
     * @param username 用户名
     * @param password 密码（明文，内部用 BCrypt 比对）
     * @return 登录成功返回 User，失败返回 null
     */
    User login(String username, String password);

    /**
     * 用户注册
     * @param username 用户名
     * @param password 密码（明文，自动 BCrypt 加密）
     * @param nickname 昵称（可选）
     * @return 注册成功返回 User
     * @throws RuntimeException 用户名已存在时抛出
     */
    User register(String username, String password, String nickname);
}
