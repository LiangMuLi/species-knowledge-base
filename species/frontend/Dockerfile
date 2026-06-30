# ============================================
# 物种知识库 - 前端 Dockerfile
# 本地已构建好 dist/，容器只负责用 Nginx 运行
# ============================================
FROM nginx:alpine

# 复制构建产物到 Nginx 静态目录
COPY dist /usr/share/nginx/html

# 复制 Nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 暴露 80 端口
EXPOSE 80

# Nginx 前台启动
CMD ["nginx", "-g", "daemon off;"]
