# 🌿 物种知识库

一个前后端分离的物种百科系统，支持物种浏览、分类检索、用户收藏、后台管理、数据爬虫。

🖥️ **上线地址**：http://47.119.120.200

---

## 技术栈

| 后端 | 前端 | 部署 |
|------|------|------|
| Spring Boot 3.2 / Java 17 | Vue 3 + Vite | Docker + Docker Compose |
| MyBatis-Plus | Element Plus | Nginx 反向代理 |
| MySQL 8.0 / H2 | Pinia + Vue Router | 2核2G 云服务器 |
| Spring Security + JWT + BCrypt | Axios | — |

---

## 快速开始

### 本地开发（无需 MySQL）

```bash
# 后端（H2 内存数据库，免安装）
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 前端（新开终端）
cd frontend && npm install && npm run dev
```

管理员账号自动创建：**admin / admin123**

### 生产部署

```bash
bash deploy.sh
```

---

## 项目结构

```
species/
├── backend/          # Spring Boot 后端
│   ├── controller/   # 控制器
│   ├── service/      # 业务逻辑
│   ├── mapper/       # MyBatis-Plus 数据访问
│   ├── entity/       # 实体类
│   ├── config/       # 配置（Security/JWT/CORS）
│   └── crawler/      # 数据爬虫
├── frontend/         # Vue 3 前端
│   ├── src/views/    # 页面组件
│   ├── src/api/      # API 接口
│   ├── src/store/    # Pinia 状态管理
│   └── src/router/   # 路由
├── docker-compose.yml
├── init.sql
└── deploy.sh
```

---

## 功能

- 物种浏览 / 分类检索 / 详情查看
- 用户注册登录 / JWT 认证
- 收藏物种 / 评论互动
- 后台管理（物种 / 分类 / 用户 CRUD）
- 数据爬虫自动入库
- 图片上传
- Docker 一键部署
