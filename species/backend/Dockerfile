# ============================================
# 物种知识库 - 后端 Dockerfile
# 基于 OpenJDK 17，体积小
# ============================================
FROM openjdk:17-jdk-slim

# 维护者信息
LABEL maintainer="species-knowledge-base"

# 工作目录
WORKDIR /app

# 复制 JAR 包（上传到与 Dockerfile 同目录）
COPY species.jar app.jar

# 暴露后端端口（需与 server.port / docker-compose 映射一致）
EXPOSE 8081

# 启动命令 - 通过 JAVA_OPTS 环境变量控制 JVM 内存
# 使用方式: docker run -e JAVA_OPTS="-Xmx256m -Xms128m" ...
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
