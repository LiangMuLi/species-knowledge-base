# 🌿 物种知识库

> 一个前后端分离的物种百科系统，支持物种信息浏览、分类检索、用户收藏、后台管理、数据爬取等功能。

🖥️ **上线地址**：http://47.119.120.200

---

## 目录

- [技术栈](#技术栈)
- [架构图](#架构图)
- [功能特性](#功能特性)
- [快速开始（本地开发）](#快速开始本地开发)
- [Docker 部署](#docker-部署)
- [踩坑记录](#踩坑记录)
- [项目结构](#项目结构)
- [环境要求](#环境要求)

---

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| **后端框架** | Spring Boot | 3.2.0 |
| **语言** | Java | 17 |
| **ORM** | MyBatis-Plus | 3.5.5 |
| **数据库** | MySQL 8.0（生产） / H2（本地开发） | — |
| **安全** | Spring Security + JWT + BCrypt | — |
| **构建** | Maven | — |
| **前端框架** | Vue 3 + Vite | 5.0 / 3.4 |
| **状态管理** | Pinia | 2.1 |
| **路由** | Vue Router | 4.2 |
| **UI 组件库** | Element Plus | 2.5 |
| **HTTP 客户端** | Axios | 1.6 |
| **容器化** | Docker + Docker Compose | — |
| **反向代理** | Nginx | — |

---

## 架构图

```
┌─────────────────────────────────────────────────────────┐
│                      用户浏览器                           │
│              http://47.119.120.200                        │
└────────────────────────┬────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│                   Nginx (端口 80)                         │
│  ┌────────────────────────────────────────────────────┐ │
│  │  location /  → 前端静态文件 (dist/)                 │ │
│  │  location /api/  → 反向代理后端 http://backend:8081 │ │
│  │  location /uploads/  → 静态文件服务 (图片缓存30天)   │ │
│  └────────────────────────────────────────────────────┘ │
└────────────────────────┬────────────────────────────────┘
                         │
                    ┌────┴────┐
                    ▼         ▼
┌──────────────────────┐  ┌──────────────────────────────────┐
│   Vue 3 Frontend     │  │  Spring Boot Backend (8081)      │
│   Element Plus UI    │  │  ┌────────────────────────────┐  │
│   Pinia Store        │◄─┤  │  Controller → Service →    │  │
│   Axios HTTP         │  │  │  Mapper(MyBatis-Plus)      │  │
│   Vue Router         │  │  └────────────────────────────┘  │
└──────────────────────┘  │  JWT Auth Filter + BCrypt       │
                          │  + Crawler                       │
                          └──────────┬───────────────────────┘
                                     │
                                     ▼
                          ┌──────────────────────┐
                          │   MySQL 8.0 (3306)   │
                          │   数据库: species     │
                          │   init.sql 自动建表   │
                          │   内存: 256M (2C2G优化)│
                          └──────────────────────┘
```

---

## 功能特性

- **物种浏览** — 分页查询物种信息，支持分类筛选
- **物种详情** — 查看物种的完整介绍、图片、分类
- **分类管理** — 多级分类树结构
- **用户系统** — 注册、登录、JWT 认证
- **收藏功能** — 用户收藏感兴趣的物种
- **评论互动** — 对物种发表评论
- **后台管理** — 物种 / 分类 / 用户 CRUD
- **数据爬虫** — 自动爬取物种数据入库
- **图片上传** — 支持物种图片上传与管理
- **Docker 部署** — 一键构建、启动、更新

---

## 快速开始（本地开发）

### 前置条件

- JDK 17+
- Node.js 18+
- Maven 3.8+
- （可选）MySQL 8.0 — 默认使用 H2 内存数据库，无需安装

### 1. 启动后端

```bash
cd backend

# 用 H2 内存数据库启动（不需要 MySQL）
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

后端启动后：
- API 服务：http://localhost:8081
- H2 控制台：http://localhost:8081/h2-console
- 管理员账号自动创建：**admin / admin123**

### 2. 启动前端

新开一个终端：

```bash
cd frontend
npm install
npm run dev
```

前端启动后访问：http://localhost:5173

### 3. 本地预览构建产物

```bash
cd frontend
npm run build
npm run preview
```

---

## Docker 部署

### 服务器部署（生产环境）

```bash
# 一键部署（会自动打包、上传、构建、启动）
bash deploy.sh
```

### 手动部署

```bash
# 1. 后端打包
cd backend && mvn clean package -DskipTests && cd ..

# 2. 前端构建
cd frontend && npm install && npm run build && cd ..

# 3. 上传到服务器
scp docker-compose.yml root@你的服务器:/opt/species/
scp init.sql root@你的服务器:/opt/species/
scp backend/target/species.jar backend/Dockerfile root@你的服务器:/opt/species/backend/
scp -r frontend/Dockerfile frontend/nginx.conf frontend/dist root@你的服务器:/opt/species/frontend/

# 4. 服务器上启动
ssh root@你的服务器 "cd /opt/species && docker compose up -d --build"
```

### 访问

部署完成后访问：http://你的服务器IP

---

## 踩坑记录

### 1. Nginx proxy_pass 斜杠问题

**问题：** `/api/species/list` 请求代理到后端时路径丢失或重复。

**原因：** `proxy_pass` 末尾是否加 `/` 行为不同：

| 写法 | 效果 |
|------|------|
| `proxy_pass http://backend:8081;` | 传递完整路径：`/api/species/list` → `/api/species/list` |
| `proxy_pass http://backend:8081/;` | 去掉匹配前缀：`/api/species/list` → `/species/list` |

**解决：** 使用**不加斜杠**的写法，保持路径不变：

```nginx
location /api/ {
    proxy_pass http://backend:8081;     # ✅ 不加斜杠，完整路径透传
}
```

### 2. Nginx 上传目录挂载

**问题：** 前端上传的图片访问 404，`/uploads/` 路由找不到文件。

**原因：** Docker 中 Nginx 容器和前端的 dist 共享一个镜像层，上传的文件不在镜像中。

**解决：** 通过 Docker 卷将上传目录挂载到 Nginx 的静态文件目录，由 `location /` 自动服务：

```yaml
# docker-compose.yml
volumes:
  - uploads-data:/usr/share/nginx/html/uploads   # ✅ 挂载上传卷
```

```nginx
# nginx.conf — 不需要单独配 location，直接由 try_files 匹配
location / {
    try_files $uri $uri/ /index.html;   # ✅ /uploads/xxx.jpg 会被 try_files 命中
}
```

### 3. BCrypt 密码加密

**问题：** 密码如何安全存储？每次加密结果不同怎么验证？

**原因：** Spring Security 的 BCryptPasswordEncoder 自动加盐，每次加密结果不同。

**解决：**

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();   // ✅ 自动加盐，防彩虹表
}
```

| 特点 | 说明 |
|------|------|
| 自动加盐 | 每次加密结果不同，但 matches() 能验证 |
| 可调强度 | 默认 10，越高越安全也越慢 |
| 防彩虹表 | 相同密码加密结果不同，无法预计算密文 |

使用方式：

```java
// 加密（注册时）
String encoded = passwordEncoder.encode(rawPassword);

// 验证（登录时）
boolean matched = passwordEncoder.matches(rawPassword, encodedPassword);
```

### 4. MySQL 2C2G 服务器优化

**问题：** 2核2G云服务器，MySQL 默认配置占用内存过高。

**解决：**

```yaml
command:
  - "--innodb_buffer_pool_size=256M"    # 适量放大
  - "--innodb_log_buffer_size=16M"      # 保持默认
  - "--max_connections=50"              # 连接数限制
  - "--thread_cache_size=8"             # 线程缓存
  - "--skip-name-resolve"               # 跳过DNS反向查询
```

### 5. Docker 健康检查控制启动顺序

**问题：** 后端容器在 MySQL 还没就绪时就启动，导致启动失败。

**解决：** MySQL 容器配置 healthcheck，后端 `depends_on` 配合 `condition: service_healthy`：

```yaml
mysql:
  healthcheck:
    test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-pSpecies@2024"]
    interval: 15s
    timeout: 5s
    retries: 5

backend:
  depends_on:
    mysql:
      condition: service_healthy    # ✅ 等待 MySQL 健康检查通过才启动
```

---

## 项目结构

```
species/
├── backend/                          # Java Spring Boot 后端
│   ├── Dockerfile                    # 后端 Docker 构建
│   ├── pom.xml                       # Maven 依赖配置
│   ├── target/species.jar            # Maven 构建产物
│   └── src/main/
│       ├── java/com/species/
│       │   ├── SpeciesApplication.java      # 启动类
│       │   ├── config/                      # 配置类
│       │   │   ├── SecurityConfig.java      # Spring Security + BCrypt
│       │   │   ├── CorsConfig.java          # 跨域配置
│       │   │   ├── JwtAuthFilter.java       # JWT 认证过滤器
│       │   │   ├── DataInitializer.java     # 初始化数据（管理员+种子）
│       │   │   ├── MyMetaObjectHandler.java # MyBatis-Plus 自动填充
│       │   │   └── WebMvcConfig.java        # Web MVC 配置
│       │   ├── controller/                  # 控制器层
│       │   │   ├── AuthController.java      # 登录/注册
│       │   │   ├── SpeciesController.java   # 物种 CRUD
│       │   │   ├── CategoryController.java  # 分类管理
│       │   │   ├── CommentController.java   # 评论
│       │   │   ├── FavoriteController.java  # 收藏
│       │   │   ├── CrawlerController.java   # 爬虫触发
│       │   │   ├── UploadController.java    # 文件上传
│       │   │   └── AdminController.java     # 后台管理
│       │   ├── service/                     # 服务层
│       │   ├── mapper/                      # MyBatis-Plus Mapper
│       │   ├── entity/                      # 实体类
│       │   ├── dto/                         # 数据传输对象
│       │   ├── crawler/                     # 爬虫
│       │   └── util/                        # 工具类
│       │       ├── JwtUtil.java             # JWT 工具
│       │       └── Result.java              # 统一响应
│       └── resources/
│           ├── application.yml              # 公共配置
│           ├── application-dev.yml          # 本地开发（H2）
│           ├── application-prod.yml         # 生产环境（MySQL）
│           └── schema-dev.sql               # 本地开发建表SQL
├── frontend/                          # Vue 3 前端
│   ├── Dockerfile                     # 前端 Docker 构建（Nginx）
│   ├── nginx.conf                     # Nginx 反向代理配置
│   ├── index.html                     # 入口 HTML
│   ├── package.json                   # NPM 依赖
│   ├── vite.config.js                 # Vite 构建配置
│   ├── dist/                          # 构建产物
│   └── src/
│       ├── api/                       # API 接口封装（Axios）
│       ├── router/                    # Vue Router 路由
│       ├── store/                     # Pinia 状态管理
│       ├── views/                     # 页面组件
│       │   ├── Login.vue / Register.vue         # 登录注册
│       │   ├── SpeciesList.vue / SpeciesDetail.vue  # 物种列表/详情
│       │   ├── Favorites.vue                    # 收藏页
│       │   ├── Layout.vue                       # 布局
│       │   └── Admin*.vue                       # 后台管理
│       ├── App.vue                    # 根组件
│       ├── main.js                    # 入口
│       └── style.css                  # 全局样式
├── docker-compose.yml                 # Docker Compose 编排
├── init.sql                           # 建表 + 种子数据
├── deploy.sh                          # 一键部署脚本
├── .gitignore                         # Git 忽略规则
└── README.md                          # 项目说明
```

---

## 环境要求

| 环境 | 要求 |
|------|------|
| **本地开发** | JDK 17+, Node.js 18+, Maven 3.8+ |
| **服务器** | 2核2G Linux + Docker 24+ |
| **浏览器** | Chrome / Firefox / Edge 最新版 |

### 默认管理员账号

| 账号 | 密码 | 说明 |
|------|------|------|
| admin | admin123 | 管理员（可管理全部数据） |

---

## 本地开发命令速查

```bash
# 后端（H2 模式，无需 MySQL）
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 前端
cd frontend && npm install && npm run dev

# 打包
cd backend && mvn clean package -DskipTests
cd frontend && npm run build

# 部署
bash deploy.sh
```

---

> 项目基于 Spring Boot 3.2 + Vue 3 构建，采用 Docker 容器化部署。
