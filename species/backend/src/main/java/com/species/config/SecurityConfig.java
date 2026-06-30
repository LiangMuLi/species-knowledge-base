package com.species.config;

import com.species.util.Result;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 核心配置
 *
 * 配置要点：
 * 1. 登录接口 permitAll() — 不需要认证
 * 2. 其他接口需要认证 — 请求必须携带有效 JWT
 * 3. 无状态会话 — 不用 Session，只用 JWT
 * 4. 异常处理 — 未认证返回 JSON 而非跳转页面
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 禁用 CSRF（前后端分离，用 JWT 不需要 CSRF 保护）
            .csrf(csrf -> csrf.disable())

            // 无状态会话（不创建 Session，每次请求都通过 JWT 验证）
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // 接口权限配置
            .authorizeHttpRequests(auth -> auth
                // 登录、注册公开
                .requestMatchers("/api/auth/**").permitAll()
                // 浏览类接口公开
                .requestMatchers(HttpMethod.GET, "/api/species/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/comments/**").permitAll()
                // 上传的文件（静态资源）
                .requestMatchers("/uploads/**").permitAll()
                // 写操作需要管理员权限
                .requestMatchers(HttpMethod.POST, "/api/species/**").hasRole("admin")
                .requestMatchers(HttpMethod.PUT, "/api/species/**").hasRole("admin")
                .requestMatchers(HttpMethod.DELETE, "/api/species/**").hasRole("admin")
                .requestMatchers(HttpMethod.POST, "/api/categories/**").hasRole("admin")
                .requestMatchers(HttpMethod.DELETE, "/api/categories/**").hasRole("admin")
                .requestMatchers("/api/upload/**").hasRole("admin")
                .requestMatchers("/api/admin/**").hasRole("admin")
                .requestMatchers("/api/crawler/**").hasRole("admin")
                // 其他接口需要认证（收藏等）
                .anyRequest().authenticated()
            )

            // 异常处理：未认证时返回 JSON 而非重定向
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType("application/json;charset=utf-8");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("{\"code\":401,\"msg\":\"未登录或 token 已失效\",\"data\":null}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setContentType("application/json;charset=utf-8");
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.getWriter().write("{\"code\":403,\"msg\":\"权限不足\",\"data\":null}");
                })
            )

            // 在 UsernamePasswordAuthenticationFilter 之前执行 JWT 过滤器
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 密码编码器 — BCrypt 哈希算法
     * BCrypt 自动加盐，每次加密结果不同，防止彩虹表攻击
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
