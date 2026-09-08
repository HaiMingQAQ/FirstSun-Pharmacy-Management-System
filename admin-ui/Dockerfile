FROM node:22-alpine

WORKDIR /app
RUN corepack enable && corepack prepare pnpm@11.22.0 --activate

COPY package.json pnpm-lock.yaml pnpm-workspace.yaml .npmrc ./
RUN pnpm install --frozen-lockfile

COPY . .

EXPOSE 80
CMD ["pnpm", "exec", "vite", "--mode", "docker", "--host", "0.0.0.0", "--port", "80"]
