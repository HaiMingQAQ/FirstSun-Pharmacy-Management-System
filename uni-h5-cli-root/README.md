# uni-h5-cli-root（小程序 H5 命令行构建与本地预览）

## 用途

`mall-uniapp` 是 **HBuilderX 工程**：`package.json` 未声明 `@dcloudio` 系列平台插件，
因此无法直接用命令行构建。本目录是一个 **CLI 根目录 shim**，作用是在
**不修改 `mall-uniapp/package.json`** 的前提下声明 H5 平台插件并复用同一份依赖，
用于命令行构建与本地 H5 预览。

> 使用 HBuilderX 或微信开发者工具的同学可以忽略本目录；本目录只服务于
> 「命令行 + 浏览器」的接口联调与验收。

## 前置条件

- Node.js 18+
- 仓库内已存在 `mall-uniapp` 目录

## 一次性准备

**1. 在 `mall-uniapp` 目录安装依赖（不写入 package.json、不产生锁文件）**

```powershell
npm install --no-save --legacy-peer-deps `
  @dcloudio/uni-app@3.0.0-4080720251210001 `
  @dcloudio/uni-h5@3.0.0-4080720251210001 `
  @dcloudio/uni-cli-shared@3.0.0-4080720251210001 `
  @dcloudio/vite-plugin-uni@3.0.0-4080720251210001 `
  vite@5.2.8 sass vue@3.4.21
```

> **`vue` 必须为 `3.4.21`**：uni-app 4.87 的运行时按 vue 3.4 构建，
> 安装 vue 3.5.x 会因 `@vue/shared` 缺少 `normalizeCssVarValue` / `isInSSRComponentSetup`
> 导出而构建失败。（HBuilderX 自带该运行时，故 `mall-uniapp` 中声明的 `vue: ^3.5.11`
> 在 HBuilderX 下可正常运行。）

**2. 在本目录建立指向 `mall-uniapp\node_modules` 的目录联接**

```powershell
New-Item -ItemType Junction -Path .\node_modules -Target ..\mall-uniapp\node_modules
```

（Windows 需要管理员权限或已开启「开发者模式」。）

## 构建与预览

以下命令的当前目录必须是 **`uni-h5-cli-root`**：

```powershell
$R = (Resolve-Path ..).Path
$env:UNI_INPUT_DIR = "$R\mall-uniapp"
$env:VITE_ROOT_DIR = "$R\mall-uniapp"

# 预览（开发服务器，默认 http://localhost:5173/）
$env:UNI_OUTPUT_DIR = "$R\mall-uniapp\unpackage\dist\dev\h5"
node ".\node_modules\@dcloudio\vite-plugin-uni\bin\uni.js" -p h5

# 发行构建（产物输出到 mall-uniapp\unpackage\dist\build\h5）
$env:UNI_OUTPUT_DIR = "$R\mall-uniapp\unpackage\dist\build\h5"
node ".\node_modules\@dcloudio\vite-plugin-uni\bin\uni.js" build -p h5
```

访问地址：<http://localhost:5173/>

## 接口与租户配置

后端地址、接口前缀与租户由 `mall-uniapp` 的环境文件决定：

| 配置项 | 文件 | 值 |
| --- | --- | --- |
| 后端地址 | `mall-uniapp/.env.development` | `SHOPRO_DEV_BASE_URL=http://localhost:48080` |
| 接口前缀 | 同上 | `SHOPRO_API_PATH=/app-api` |
| 租户编号 | 同上 | `SHOPRO_TENANT_ID=1` |

即前端请求 `http://localhost:48080/app-api/**`，需先启动后端与数据库
（仓库根目录执行 `docker compose up -d`）。

## 说明与边界

- `node_modules` 为本地目录联接，已由 `.gitignore` 忽略，**不纳入版本库**。
- 本目录**不修改** `mall-uniapp/package.json`，也不产出需要提交的 `package-lock.json`。
- 若只想运行小程序（微信开发者工具 / HBuilderX），无需本目录。
