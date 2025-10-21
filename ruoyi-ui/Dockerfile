# 构建阶段
FROM node:18-alpine AS builder

# 设置工作目录
WORKDIR /app

# 配置 npm 使用淘宝镜像源
RUN npm config set registry https://registry.npmmirror.com

# 安装 pnpm 并配置淘宝镜像源
RUN npm install -g pnpm && \
    pnpm config set registry https://registry.npmmirror.com

# 复制依赖文件
COPY package.json pnpm-lock.yaml ./

# 安装依赖
RUN pnpm install --frozen-lockfile

# 复制项目文件
COPY . .

RUN sed -i 's#/prod-api#http://your_real_backend_addr#' .env.production
# 构建项目
RUN pnpm run build:prod

# 生产阶段
FROM nginx:alpine

# 复制自定义 nginx 配置
COPY nginx.conf /etc/nginx/nginx.conf

# 从构建阶段复制构建产物
COPY --from=builder /app/dist /usr/share/nginx/html

# 暴露端口
EXPOSE 80

# 启动 nginx
CMD ["nginx", "-g", "daemon off;"]

