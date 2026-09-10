# FirstSun 药店管理系统 管理端镜像
# 两阶段构建：vite 构建产物 → nginx 静态服务（避免 dev server 按需编译导致的首次加载慢）
FROM node:22-alpine AS builder

WORKDIR /app
RUN corepack enable && corepack prepare pnpm@11.22.0 --activate

COPY package.json pnpm-lock.yaml pnpm-workspace.yaml .npmrc ./
RUN pnpm install --frozen-lockfile

COPY . .
# mode=docker 读取 .env.docker（VITE_BASE_URL=http://localhost:48080）
RUN pnpm exec vite build --mode docker

FROM nginx:1.27-alpine

COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
