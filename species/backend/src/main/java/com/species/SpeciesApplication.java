package com.species;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 物种知识库 - 后端启动类
 *
 * 启动入口，@SpringBootApplication 包含三个注解：
 *   @EnableAutoConfiguration   — 根据依赖自动配置
 *   @SpringBootConfiguration   — 标记为配置类
 *   @ComponentScan             — 扫描当前包下的 @Component
 */
@SpringBootApplication
public class SpeciesApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpeciesApplication.class, args);
    }
}
