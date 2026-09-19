# FirstSun 顾客购药小程序 · 第一版交付说明

工作目录：`D:\github-2\FirstSun-miniapp`  
分支：`feat/miniapp-client`  
基线：`f0068958`。开始时工作区干净；未 commit、push 或创建 PR。只完善现有 mall-uniapp。

## 1. 修改文件

- 重写 9 个已有药店页面：`index.vue`、`category.vue`、`detail.vue`、`cart.vue`、`login.vue`、`user.vue`、`order.vue`、`order-detail.vue`、`address.vue`。
- 新增页面：`checkout.vue`、`prescription-upload.vue`。
- 药店页共享交互：`pages/pharmacy/usePharmacy.js`。
- 集中式数据服务：`sheep/api/pharmacy/client.js`、`fixtures.js`、`config.js`。原 `drug.js`、`store.js`、`cart.js`、`order.js` 真实接口封装保留不改。
- 共用组件：`s-pharmacy-page`、`s-pharmacy-product`、`s-pharmacy-state`、`s-pharmacy-stepper`、`s-pharmacy-order-actions`；更新已有 `s-pharmacy-tabbar`。
- 药店主题：`sheep/scss/pharmacy.scss`。
- `App.vue`：引入限定在 `.fs-page` 内的药店样式；药店演示入口跳过商城租户/装修初始化。非药店入口保留原初始化流程。
- `pages.json`：新增两页；11 个药店页面单独启用原生导航，避免继承商城 custom 导航造成顶部系统区域冲突。
- 静态资源：`static/pharmacy/` 的三个真实包装图、统一占位图和来源说明。
- 测试：`pages/pharmacy/tests/client.test.cjs`；本说明文件。

没有修改 backend、数据库、admin-ui、B–F 业务代码、package.json、manifest.json。

## 2. 设计系统

沿用现有药店绿 `#176b5b`。页面浅灰 `#f5f7f6`，内容白色，正文深灰 `#202d29`，次级文字 `#64716c`；价格红 `#c13c34`，处方提醒为橙色字/浅橙背景。无渐变、装饰球、嵌套卡片。

- 正文 16px，模块标题 18px，页面内容标题 22px；辅助文字 13px。
- 8/12/16/24px 间距，内容左右 16px。
- 卡片圆角 8px，按钮和输入框 6px；按钮最小高度 44px。
- 药名自然换行；价格不换行；数量使用固定宽度数字栏，长药名不会推开按钮。
- uni-icons 图标；图片优先显示真实产品图，加载失败或无图使用统一占位图。
- 固定底部操作区及导航均适配 `env(safe-area-inset-bottom)`；页面预留对应滚动空间。
- 组件显式 `styleIsolation: apply-shared`，兼容小程序公共主题样式。
- 页面共享加载、空数据、失败重试；操作失败用明确 toast，提交和购物车修改有互斥状态。

## 3. 已完成页面

| 页面 | 内容 |
| --- | --- |
| 首页 | FirstSun 品牌文字、门店及营业时间、配送/自提、搜索、五类快捷入口、常备药、下拉刷新 |
| 分类 | 左分类右商品、名称/通用名/演示条码搜索、最近搜索、本地历史清除、缺货及无结果 |
| 详情 | 药品图、商品/通用名、规格、厂家、批准文号展示位、价格/库存、OTC/处方、用药提示、数量、购买操作 |
| 购物车 | 勾选/全选、数量、删除确认、库存不足/失效、合计与结算、未登录及空状态 |
| 确认订单 | 配送/自提、地址、门店、清单和小计、处方入口、积分、模拟支付、备注和金额汇总 |
| 登录 | 手机号校验、获取测试码、测试协议/隐私说明、勾选同意、登录后返回原页面 |
| 会员 | 手机号脱敏、等级/积分、订单状态入口、地址、处方、联系门店、退出确认 |
| 订单列表 | 全部/待支付/待审核/待收取/已完成/已取消、订单号/状态/金额、操作入口、空/错误状态 |
| 订单详情 | 支付和审核状态、履约信息、商品/金额、备注、创建时间、可用操作 |
| 地址 | 列表、默认地址、新增/编辑、格式反馈、删除确认、结算选择回填 |
| 处方 | 最多三张、10MB 单张限制、相册/拍照、预览/删除、本地模拟保存、订单审核记录 |

## 4. 已完成交互

- 浏览 → 登录 → 加购 → 调整数量/选择 → 确认订单 → 新增地址/自提 → 积分抵扣 → 提交 → 模拟支付 → 确认收货。
- 详情立即购买，不必修改购物车。
- 处方药 → 上传测试图片 → 提交待审核订单。待审核订单不能模拟支付，也不会自动审核通过。
- 取消待支付/待审核订单退回测试积分并释放占用库存；重复取消不会重复退还。
- 数量必须为正整数且不能超库存；结算和提交时复核库存。库存变化后可减至可售上限。
- 以整数分计算金额，演示规则为 100 积分抵 1 元，每单最多 5 元，不抵配送费；配送费为 6 元，自提免费。
- 提交按钮防重复、服务层按结算 token 幂等；返回页面后刷新最新订单、地址、积分与购物车。
- 确认订单返回地址/处方选择后保留已挂载表单，刷新期间禁用提交，避免 H5 输入组件恢复时产生 ResizeSensor 异常。
- 测试账户按手机号隔离；不向真实后端写入测试数据。

## 5. Mock 范围

本版药店页面均调用 `client.js` 中的本地演示服务：商品与门店、库存、会员及登录、购物车、积分、地址、订单、支付、处方保存。价格/库存/营业信息均标注为演示，不代表实际销售承诺。

`fixtures.js` 是商品和门店唯一数据源。测试验证码在 `config.js` 统一管理，页面通过“获取测试码”填入；不发送短信，不保存正式密码。新测试会员有 360 积分。使用格式正确的测试手机号即可体验。

`firstsun-demo-v1` 保存测试账户、购物车、地址和订单；`firstsun-checkout` 保存当前结算草稿。图片使用 `uni.chooseImage` 返回的临时本地路径/Blob URL，不发送服务器；新会话可能需要重新选择。不要上传真实处方。

## 6. 下一位工程师接入真实接口的边界

保持页面消费的数据结构和方法名，替换 `client.js` 的服务实现；不要在页面里添加零散 mock 分支。`PHARMACY_DEMO = false` **只会恢复应用初始化，不会自动把服务切到真实后端**，必须先实现并验证真实适配器。

| 能力 | 已有入口 / 待补充 |
| --- | --- |
| 商品/分类 | `DrugApi.getDrugPage/getDrug/getCategoryList`；保留服务端分页，转换为页面使用的数据 |
| 商品转换 | `normalizeDrug(raw, availability)`；已对照 `AppDrugRespVO`，元转分、isRx 转布尔、drugType=6 为器械 |
| 缺失商品字段 | 现有 AppDrugRespVO **没有图片、批准文号、条码及门店库存**；适配器需补充 `availability.image/approval/barcode/stock/active`。未知库存按 0 禁止购买，不假定有货 |
| 门店 | `StoreApi.getStoreList`；storeName/address/phone/businessHours 映射到 name/address/phone/hours；后续接真实可选门店及配送范围 |
| 购物车 | `CartApi` 下 `/member/wx-cart/*`；服务端购物车行 id 与 drugId 分开转换，selectedFlag 映射 checked |
| 登录/会员 | 复用 `sheep/api/member/auth.js` 与现有会员信息接口；替换本地 session，接真实 token 生命周期；本阶段不实现真实短信 |
| 地址 | `sheep/api/member/address.js`；现版完整地址为一个字段，接入时转换后端省市区 ID 和详细地址 |
| 订单 | `OrderApi.createOrder/getOrderPage/getOrder/cancelOrder`；核对真实状态码、积分及配送费试算契约、服务端幂等和库存锁定 |
| 支付 | 第一版只保留模拟支付；真实微信支付另立阶段，不直接改前端 paid 字段作为真实支付依据 |
| 处方 | 接私有上传、订单关联、药师审核状态查询及拒绝/重传；不在客户端生成审核结果 |
| 积分/履约 | 接服务端积分余额及抵扣规则、取消返还、配送进度/自提核销；服务端金额为准 |

## 7. 编译与逻辑验证

验证环境：Windows、本机 HBuilderX 5.07 附带 uni-app Vue3 编译器。H5 和 mp-weixin 均输出 `DONE Build complete.`。

本机原项目是 HBuilderX 目录结构，没有 npm build 脚本。为了不修改 package.json，在 `.tmp/build-root` 声明 H5/微信平台插件，链接本机编译器，设置 `UNI_INPUT_DIR` 和 `VITE_ROOT_DIR` 指向 mall-uniapp。本地 node_modules 与编译器 Vue 3.4.21 对齐，Sass 1.77.8 放在 `.tmp/tooling`；这些工具链文件全部被 Git 忽略。

本机重跑编译（PowerShell，工作目录 `.tmp/build-root`）：

```powershell
$env:UNI_INPUT_DIR = 'D:\github-2\FirstSun-miniapp\mall-uniapp'
$env:VITE_ROOT_DIR = $env:UNI_INPUT_DIR
$env:UNI_OUTPUT_DIR = "$env:UNI_INPUT_DIR\dist\build\h5"
node node_modules/@dcloudio/vite-plugin-uni/bin/uni.js build -p h5 --config "$env:UNI_INPUT_DIR\vite.config.js"
```

微信目标把平台改为 `mp-weixin`、输出改为 `dist/build/mp-weixin`。项目已有 mplive 插件会重写 manifest.json，验证时已保存并恢复其原始字节，未留下该文件修改。

逻辑测试（工作目录 mall-uniapp）：

```powershell
node --experimental-vm-modules pages/pharmacy/tests/client.test.cjs
```

通过：登录校验、条码搜索、负数/非整数/超库存、勾选、金额、并发重复下单、退积分/库存、处方限制、订单状态流转、地址删除、账户隔离、空数据与网络错误；11 条药店路由存在并启用正确导航。Node VM 实验性功能提示只来自测试运行器。

构建提示：Browserslist 数据过期；微信构建提示现有样式含 img 标签选择器。未为消除这些既有提示扩大修改范围。

## 8. 截图与浏览器验证

使用真实 H5 构建产物在 Edge/Playwright 中运行，375/390/430px、844px 高度。核心十页 × 三种宽度和 22px 超长药名购物车共 31 项布局检查通过；页面及可见组件没有横向越界。

走通登录、加购、数量调整、结算、地址新增回填、积分、模拟支付、确认收货、处方选择/预览/删除、待审核、失败重试、搜索空结果。浏览器未捕获页面运行异常、控制台 error 或应用资源加载失败。

另有 8 项边界/导航检查通过：库存变化后数量修正、失效商品删除、四栏真实点击跳转、商品不存在的业务错误、分类页 22px 超长名称、99 件数量上限、未登录购物车及空购物车。结果保存于 `.tmp/edge-results.json`，脚本 `.tmp/edge-qa.cjs`。已逐张查看首页、分类、详情、购物车、确认订单、会员中心、订单详情及处方上传截图。

截图保存在项目 `.tmp/screenshots/`，检测结果 `.tmp/browser-results.json`，浏览器验证脚本 `.tmp/browser-qa.cjs`。截图包含首页、分类、详情、购物车、确认订单、会员、订单详情、地址、处方和空/错误/超长药名状态。完整长截图中的固定导航位于首屏底部，这是浏览器截图对固定定位元素的正常表现；实际滚动区域已预留底部空间。

## 9. 未解决 / 明确保留的限制

- 已完成微信目标编译，**未在微信开发者工具和真机运行验收**。相册权限、系统大字体、刘海屏安全区需真机复核。
- 图片、批准文号、真实门店库存缺少现有 App API 字段，未伪造批准文号；部分商品图仍为占位图。
- 测试门店没有真实联系电话；联系入口展示营业信息和待配置说明，不拨打虚构号码。
- 处方为临时本地图片，不持久上传；审核始终等待真实药师接入。仅用于测试流程，不具备上线购药能力。
- 演示库存按测试账户隔离，不模拟跨设备/多用户库存并发。
- 第一版加载完整演示列表，真实商品量增大后需接服务端分页。

## 10. 建议下一阶段

先落实商品详情/库存/图片/批准文号契约，接真实只读目录与门店；再接会员、购物车、地址和订单试算/幂等提交。之后单独接处方私有上传和审核状态，最后安排微信真机测试、可访问性与上线前合规验收。真实短信和微信支付按独立阶段处理。
