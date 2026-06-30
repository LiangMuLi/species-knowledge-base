-- ============================================
-- 物种知识库 - 数据库 DDL 初始化脚本
-- 作用: Docker 首次启动时自动建库建表
-- 数据由后端 DataInitializer 自动插入
-- ============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS species
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE species;

-- ============================================
-- 1. 用户表 (RBAC 权限系统基础)
-- ============================================
CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username    VARCHAR(50)  NOT NULL UNIQUE COMMENT '用户名',
    password    VARCHAR(255) NOT NULL COMMENT '密码(bcrypt加密)',
    nickname    VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    email       VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    avatar      VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    role        VARCHAR(20)  NOT NULL DEFAULT 'user' COMMENT '角色: admin/user',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 1启用 0禁用',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

-- ============================================
-- 2. 物种分类表
-- ============================================
CREATE TABLE IF NOT EXISTS species_category (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '分类ID',
    name        VARCHAR(100) NOT NULL COMMENT '分类名称',
    parent_id   BIGINT       DEFAULT 0 COMMENT '父分类ID (0为顶级)',
    sort_order  INT          DEFAULT 0 COMMENT '排序',
    description VARCHAR(500) DEFAULT NULL COMMENT '分类描述',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_parent (parent_id),
    UNIQUE INDEX uk_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物种分类表';

-- ============================================
-- 3. 物种信息表 (核心表)
-- ============================================
CREATE TABLE IF NOT EXISTS species_info (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '物种ID',
    category_id     BIGINT       NOT NULL COMMENT '所属分类ID',
    name_zh         VARCHAR(200) NOT NULL COMMENT '中文名',
    name_en         VARCHAR(200) DEFAULT NULL COMMENT '英文名',
    name_scientific VARCHAR(200) DEFAULT NULL COMMENT '学名(拉丁名)',
    alias           VARCHAR(500) DEFAULT NULL COMMENT '别名(逗号分隔)',
    description     TEXT         DEFAULT NULL COMMENT '物种描述/简介',
    habitat         VARCHAR(1000) DEFAULT NULL COMMENT '栖息地',
    distribution    VARCHAR(1000) DEFAULT NULL COMMENT '分布区域',
    conservation_status VARCHAR(20) DEFAULT NULL COMMENT '保护级别: CR/EN/VU/NT/LC',
    image_url       VARCHAR(500) DEFAULT NULL COMMENT '图片URL',
    weight          VARCHAR(100) DEFAULT NULL COMMENT '体重范围',
    lifespan        VARCHAR(100) DEFAULT NULL COMMENT '寿命',
    diet            VARCHAR(500) DEFAULT NULL COMMENT '食性',
    reproduction    VARCHAR(500) DEFAULT NULL COMMENT '繁殖方式',
    fun_facts       TEXT         DEFAULT NULL COMMENT '趣味知识',
    is_endemic      TINYINT      DEFAULT 0 COMMENT '是否特有物种: 0否 1是',
    status          TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 1已发布 0草稿',
    created_by      BIGINT       DEFAULT NULL COMMENT '创建者用户ID',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_category (category_id),
    INDEX idx_name_zh (name_zh),
    INDEX idx_conservation (conservation_status),
    INDEX idx_status (status),
    FULLTEXT INDEX ft_search (name_zh, name_en, description)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物种信息表';

-- ============================================
-- 4. 物种图片表 (一个物种多张图片)
-- ============================================
CREATE TABLE IF NOT EXISTS species_image (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '图片ID',
    species_id  BIGINT       NOT NULL COMMENT '物种ID',
    image_url   VARCHAR(500) NOT NULL COMMENT '图片URL',
    is_cover    TINYINT      DEFAULT 0 COMMENT '是否封面: 0否 1是',
    sort_order  INT          DEFAULT 0 COMMENT '排序',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_species (species_id),
    FOREIGN KEY (species_id) REFERENCES species_info(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物种图片表';

-- ============================================
-- 5. 用户收藏表
-- ============================================
CREATE TABLE IF NOT EXISTS user_favorite (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT   NOT NULL COMMENT '用户ID',
    species_id  BIGINT   NOT NULL COMMENT '物种ID',
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_user_species (user_id, species_id),
    INDEX idx_user (user_id),
    FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    FOREIGN KEY (species_id) REFERENCES species_info(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户收藏表';

-- ============================================
-- 6. 物种评论表 (由后端 DataInitializer 兜底创建)
-- ============================================
CREATE TABLE IF NOT EXISTS species_comment (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    species_id  BIGINT   NOT NULL COMMENT '物种ID',
    user_id     BIGINT   NOT NULL COMMENT '用户ID',
    content     TEXT     NOT NULL COMMENT '评论内容',
    rating      TINYINT  DEFAULT NULL COMMENT '评分 1-5',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_species (species_id),
    FOREIGN KEY (species_id) REFERENCES species_info(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物种评论表';

-- ============================================
-- 7. 操作日志表
-- ============================================
CREATE TABLE IF NOT EXISTS sys_log (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       DEFAULT NULL COMMENT '操作用户ID',
    action      VARCHAR(100) NOT NULL COMMENT '操作类型',
    detail      VARCHAR(500) DEFAULT NULL COMMENT '操作详情',
    ip          VARCHAR(50)  DEFAULT NULL COMMENT '请求IP',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_action (action),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';
