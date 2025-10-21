# 第一阶段：构建应用
FROM maven:3.8.5-openjdk-8 AS build

# 设置工作目录
WORKDIR /app

# 复制整个项目
COPY pom.xml .
COPY ruoyi-admin ./ruoyi-admin
COPY ruoyi-common ./ruoyi-common
COPY ruoyi-framework ./ruoyi-framework
COPY ruoyi-generator ./ruoyi-generator
COPY ruoyi-quartz ./ruoyi-quartz
COPY ruoyi-system ./ruoyi-system

# 构建项目
RUN mvn clean package -DskipTests -pl ruoyi-admin -am

# 第二阶段：运行应用
FROM openjdk:8-jre-slim

# 设置工作目录
WORKDIR /app

# 从构建阶段复制jar包
COPY --from=build /app/ruoyi-admin/target/*.jar app.jar

# 创建日志目录
RUN mkdir -p /app/logs

# 暴露端口
EXPOSE 8080

# 设置时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 启动应用（使用docker配置文件）
ENTRYPOINT ["java", "-jar", "/app/app.jar", "--spring.profiles.active=docker"]

