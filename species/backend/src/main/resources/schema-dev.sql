-- ============================================
-- 本地开发数据库初始化 (H2 兼容版)
-- 只在 dev profile 下使用
-- 仅建表，数据由 DataInitializer 自动插入
-- ============================================

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    nickname    VARCHAR(50)  DEFAULT NULL,
    email       VARCHAR(100) DEFAULT NULL,
    avatar      VARCHAR(255) DEFAULT NULL,
    role        VARCHAR(20)  NOT NULL DEFAULT 'user',
    status      TINYINT      NOT NULL DEFAULT 1,
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- 物种分类表
CREATE TABLE IF NOT EXISTS species_category (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    parent_id   BIGINT       DEFAULT 0,
    sort_order  INT          DEFAULT 0,
    description VARCHAR(500) DEFAULT NULL,
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- 物种信息表（核心表）
CREATE TABLE IF NOT EXISTS species_info (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id     BIGINT       NOT NULL,
    name_zh         VARCHAR(200) NOT NULL,
    name_en         VARCHAR(200) DEFAULT NULL,
    name_scientific VARCHAR(200) DEFAULT NULL,
    alias           VARCHAR(500) DEFAULT NULL,
    description     CLOB         DEFAULT NULL,
    habitat         VARCHAR(1000) DEFAULT NULL,
    distribution    VARCHAR(1000) DEFAULT NULL,
    conservation_status VARCHAR(20) DEFAULT NULL,
    image_url       VARCHAR(500) DEFAULT NULL,
    weight          VARCHAR(100) DEFAULT NULL,
    lifespan        VARCHAR(100) DEFAULT NULL,
    diet            VARCHAR(500) DEFAULT NULL,
    reproduction    VARCHAR(500) DEFAULT NULL,
    fun_facts       CLOB         DEFAULT NULL,
    is_endemic      TINYINT      DEFAULT 0,
    status          TINYINT      NOT NULL DEFAULT 1,
    created_by      BIGINT       DEFAULT NULL,
    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- 用户收藏表
CREATE TABLE IF NOT EXISTS user_favorite (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT   NOT NULL,
    species_id  BIGINT   NOT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, species_id)
);

-- 物种评论表
CREATE TABLE IF NOT EXISTS species_comment (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    species_id  BIGINT   NOT NULL,
    user_id     BIGINT   NOT NULL,
    content     CLOB     NOT NULL,
    rating      TINYINT  DEFAULT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
