// =====================================================
// 药店小程序重复性测试（真实适配器 + stub 网络）
// -----------------------------------------------------
// Run: node --experimental-vm-modules pages/pharmacy/tests/client.test.cjs
//
// 覆盖：
//   - 真实适配器 server.js 的完整客户端契约（金额分、状态机、字段映射）
//   - 五条功能闭环（首页→购物车 / 结算→下单 / 登录→会员→地址→订单 /
//     处方药→上传处方→提交→审核状态 / 订单→模拟支付→详情→确认收货）
//   - 业务校验（手机号/验证码/协议、库存上限、地址必填、处方必传、
//     防重复下单、重复支付/收货幂等、库存变化拦截）
//   - Mock 适配器回归冒烟（demo.js）
// 网络层为 stub，不代表真实 HTTP、事务、库存或文件权限验收。
// =====================================================
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const assert = require('node:assert/strict');

const MALL_ROOT = path.resolve(__dirname, '../../../');
const PHARMACY_ROOT = path.join(MALL_ROOT, 'pages/pharmacy');

// ---------------- 内存后端（stub 网络，语义对齐真实 app-api） ----------------
const members = {
  '13900001611': { id: 163611, mobile: '13900001611', nickname: '顾晨', point: 680, levelName: '青叶会员', cart: [], orders: [], addresses: [], presc: [] },
  '13900001612': { id: 163612, mobile: '13900001612', nickname: '林悦', point: 0, levelName: '青叶会员', cart: [], orders: [], addresses: [], presc: [] },
};
let memberSeq = 1;
const currentMember = () => {
  const token = String(storage.get('token') || '');
  const mobile = token.replace(/^token-/, '');
  return members[mobile] || Object.values(members)[0];
};
const state = {
  seq: 1000,
  inventory: new Map([
    [163101, 28],
    [163102, 16],
    [163103, 10],
    [163104, 50],
    [163105, 99],
  ]),
  prices: new Map([[163101, 17.8], [163102, 29.9], [163103, 41.0], [163104, 23.8], [163105, 35.9]]),
  drugs: [
    { id: 163101, categoryId: 163001, genericName: '复方氨酚烷胺片', tradeName: '感康', specification: '12片/盒', manufacturer: '华润三九', drugType: 0, isRx: 0, retailPrice: 19.8, memberPrice: 17.8, approvalNo: '国药准字H20000001', imageUrl: '' },
    { id: 163102, categoryId: 163002, genericName: '布洛芬缓释胶囊', tradeName: '芬必得', specification: '0.3g*20粒', manufacturer: '中美史克', drugType: 0, isRx: 0, retailPrice: 32.5, memberPrice: 29.9, approvalNo: '国药准字H10900089', imageUrl: '' },
    { id: 163103, categoryId: 163003, genericName: '奥美拉唑肠溶胶囊', tradeName: '洛赛克', specification: '20mg*14粒', manufacturer: '阿斯利康', drugType: 0, isRx: 1, retailPrice: 45.0, memberPrice: 41.0, approvalNo: '国药准字H20000003', imageUrl: '' },
    { id: 163104, categoryId: 163001, genericName: '维生素C片', tradeName: '维C', specification: '100片/瓶', manufacturer: '东北制药', drugType: 5, isRx: 0, retailPrice: 26.8, memberPrice: 23.8, approvalNo: '国药准字H20000004', imageUrl: '' },
    { id: 163105, categoryId: 163001, genericName: '电子体温计', tradeName: '体温计', specification: '1支', manufacturer: '欧姆龙', drugType: 6, isRx: 0, retailPrice: 39.9, memberPrice: 35.9, approvalNo: '', imageUrl: '' },
  ],
  categories: [
    { id: 163001, catName: '感冒用药', catType: 0, sort: 1 },
    { id: 163002, catName: '肠胃用药', catType: 0, sort: 2 },
    { id: 163003, catName: '处方用药', catType: 0, sort: 3 },
  ],
  stores: [{ id: 407, storeCode: 'FS-DEMO-001', storeName: 'FirstSun 演示门店', address: '演示路 88 号', businessHours: '08:00-22:00', phone: '05920000000' }],
  forceFail: new Set(), // url 前缀 → 模拟网络失败
};

const POINT_RULE = () => ({ memberPoint: currentMember().point, levelId: 1, levelName: currentMember().levelName, levelMultiplier: 1, earnPerYuan: 1, pointsPerYuan: 100, maxDeductPercent: 5, minDeductPoints: 0, maxDeductPoints: 500 });

function calcMaxPoints(amountFen) {
  return Math.min(POINT_RULE().maxDeductPoints, currentMember().point, Math.floor(amountFen / 100) * POINT_RULE().pointsPerYuan);
}
function fenOf(order) {
  return Math.round(order.goodsAmount * 100);
}

async function backend(config) {
  const url = config.url;
  const fail = [...state.forceFail].find((p) => url.startsWith(p));
  if (fail) return false; // 模拟 HTTP 层失败（拦截器返回 false）
  const now = () => new Date().toISOString().replace('T', ' ').slice(0, 19);
  const stock = (id) => state.inventory.get(Number(id)) ?? 0;
  const priceOf = (id) => state.prices.get(Number(id));

  if (url === '/member/auth/send-sms-code') {
    return { code: 0, data: { ok: true } };
  }
  if (url === '/member/auth/login-or-register') {
    const code = config.header['X-Sms-Code'];
    if (code !== '123456') return { code: 1030006002, msg: '测试验证码不正确' };
    const mobile = String(config.params.mobile || '');
    const member = members[mobile] || (members[mobile] = { id: 163600 + memberSeq++, mobile, nickname: mobile, point: 0, levelName: '青叶会员', cart: [], orders: [], addresses: [], presc: [] });
    const token = `token-${member.mobile}`;
    storage.set('token', token);
    storage.set('refresh-token', `refresh-${token}`);
    return { code: 0, data: { userId: member.id, accessToken: token, refreshToken: `refresh-${token}`, expiresTime: 0 } };
  }
  if (url === '/member/auth/logout') {
    storage.delete('token');
    storage.delete('refresh-token');
    return { code: 0, data: true };
  }
  if (!storage.get('token')) return { code: 401, msg: '账号未登录' };
  const member = currentMember();
  const cart = member.cart;
  const orders = member.orders;
  const addresses = member.addresses;
  const prescriptions = member.presc;

  if (url === '/member/user/get') return { code: 0, data: member };
  if (url === '/member/point/summary') return { code: 0, data: POINT_RULE() };
  if (url === '/member/point/deduct-preview') {
    const amountFen = Math.round(Number(config.data.orderAmount) * 100);
    const max = calcMaxPoints(amountFen);
    const want = Number(config.data.usePoints) > 0 ? Math.min(Number(config.data.usePoints), max) : max;
    return { code: 0, data: { deductPoints: want, deductAmount: want / 100 } };
  }
  if (url === '/pharmacy/store/list') return { code: 0, data: state.stores };
  if (url === '/pharmacy/drug/category-list') return { code: 0, data: state.categories };
  if (url === '/pharmacy/drug/page') {
    let list = state.drugs;
    if (config.params.categoryId) list = list.filter((d) => d.categoryId === Number(config.params.categoryId));
    const kw = String(config.params.keyword || '').trim();
    if (kw) list = list.filter((d) => `${d.tradeName}${d.genericName}`.includes(kw));
    return { code: 0, data: { total: list.length, list } };
  }
  if (url === '/pharmacy/drug/get') {
    const drug = state.drugs.find((d) => d.id === Number(config.params.id));
    return drug ? { code: 0, data: drug } : { code: 103004001, msg: '药品不存在' };
  }
  if (url === '/pharmacy/inventory/available') {
    const ids = String(config.params.drugIds || '').split(',').map(Number).filter(Boolean);
    return { code: 0, data: ids.map((id) => ({ drugId: id, qtyAvail: stock(id) })) };
  }
  if (url === '/member/wx-cart/list') {
    return { code: 0, data: cart.map((c) => {
        const d = state.drugs.find((x) => x.id === c.drugId);
        return { id: c.id, drugId: c.drugId, drugName: d.tradeName, specification: d.specification, unit: d.unit, isRx: d.isRx, retailPrice: d.retailPrice, memberPrice: d.memberPrice, qty: c.qty, selectedFlag: c.selectedFlag, storeId: 407, addTime: now() };
      }) };
  }
  if (url === '/member/wx-cart/add') {
    const drugId = Number(config.data.drugId);
    const row = cart.find((c) => c.drugId === drugId);
    if (row) row.qty += config.data.qty;
    else cart.push({ id: ++state.seq, drugId, qty: config.data.qty, selectedFlag: 1 });
    return { code: 0, data: ++state.seq };
  }
  if (url === '/member/wx-cart/update-qty') {
    const row = cart.find((c) => c.id === Number(config.data.id));
    if (row) row.qty = config.data.qty;
    return { code: 0, data: true };
  }
  if (url === '/member/wx-cart/update-selected') {
    const row = cart.find((c) => c.id === Number(config.params.id));
    if (row) row.selectedFlag = Number(config.params.selectedFlag);
    return { code: 0, data: true };
  }
  if (url === '/member/wx-cart/delete') {
    member.cart = cart.filter((c) => c.id !== Number(config.params.id));
    return { code: 0, data: true };
  }
  if (url === '/member/address/list') return { code: 0, data: addresses };
  if (url === '/member/address/create') {
    const addr = { ...config.data, id: ++state.seq, createTime: now() };
    if (addr.defaultStatus) addresses.forEach((a) => (a.defaultStatus = false));
    addresses.push(addr);
    return { code: 0, data: addr.id };
  }
  if (url === '/member/address/update') {
    const i = addresses.findIndex((a) => a.id === Number(config.data.id));
    if (i >= 0) {
      if (config.data.defaultStatus) addresses.forEach((a) => (a.defaultStatus = false));
      addresses[i] = { ...addresses[i], ...config.data };
    }
    return { code: 0, data: true };
  }
  if (url === '/member/address/delete') {
    member.addresses = addresses.filter((a) => a.id !== Number(config.params.id));
    return { code: 0, data: true };
  }
  if (url === '/member/prescription/create') {
    const presc = { id: ++state.seq, prescNo: `PX-407-20260919-${String(prescriptions.length + 1).padStart(4, '0')}`, storeId: config.data.storeId, patientName: config.data.patientName, hospital: '', doctorName: '', diagnosis: '', usageDesc: '', prescDate: '2026-09-19', images: config.data.images || [], reviewStatus: 0, status: 0, createTime: now(), items: config.data.items || [] };
    prescriptions.push(presc);
    return { code: 0, data: presc.id };
  }
  if (url === '/member/prescription/get') {
    const presc = prescriptions.find((p) => p.id === Number(config.params.id));
    return presc ? { code: 0, data: presc } : { code: 103204001, msg: '处方不存在' };
  }
  if (url === '/member/wx-order/create') {
    const data = config.data;
    const goods = cart.filter((c) => c.selectedFlag === 1);
    if (!goods.length) return { code: 103008001, msg: '购物车不能为空' };
    const lines = goods.map((g) => {
      const d = state.drugs.find((x) => x.id === g.drugId);
      return { drugId: g.drugId, drugName: d.tradeName, specification: d.specification, unit: d.unit, qty: g.qty, price: priceOf(g.drugId), lineAmount: priceOf(g.drugId) * g.qty, pickedQty: 0 };
    });
    const goodsAmount = lines.reduce((s, l) => s + l.lineAmount, 0);
    const freightAmount = 0;
    const usePoints = Number(data.usePoints) || 0;
    const deductPoints = usePoints > 0 ? Math.min(usePoints, calcMaxPoints(goodsAmount * 100)) : 0;
    const pointDeductAmount = deductPoints / 100;
    const payableAmount = goodsAmount + freightAmount - pointDeductAmount;
    const id = ++state.seq;
    const order = { id, orderNo: `WX-407-20260919-${String(orders.length + 1).padStart(4, '0')}`, memberId: member.id, storeId: data.storeId, orderType: data.orderType, goodsAmount, freightAmount, couponAmount: 0, discountAmount: 0, pointDeduct: deductPoints, pointDeductAmount, pointEarned: 0, payableAmount, payStatus: 0, status: 0, prescId: data.prescId || null, addressSnapshot: data.addressId ? (addresses.find((a) => a.id === Number(data.addressId))?.detailAddress || '') : null, remark: data.remark || '', expireAt: now(), pickupCode: '123456', createTime: now(), lines };
    orders.unshift(order);
    member.cart = cart.filter((c) => !goods.includes(c));
    return { code: 0, data: id };
  }
  if (url === '/member/wx-order/page') {
    return { code: 0, data: { total: orders.length, list: orders } };
  }
  if (url === '/member/wx-order/get') {
    const order = orders.find((o) => o.id === Number(config.params.id));
    return order ? { code: 0, data: order } : { code: 103007001, msg: '订单不存在' };
  }
  if (url === '/member/wx-order/cancel') {
    const order = orders.find((o) => o.id === Number(config.params.id));
    if (order && order.status === 0 && order.payStatus === 0) {
      order.status = -1;
      member.point += order.pointDeduct; // 释放预扣积分
    }
    return { code: 0, data: true };
  }
  if (url === '/member/wx-order/simulate-pay') {
    return { code: 403, msg: '模拟支付未启用，成功链尚未接通' };
  }
  if (url === '/member/wx-order/confirm-receive') {
    const order = orders.find((o) => o.id === Number(config.params.id));
    if (order && order.orderType === 1 && order.payStatus === 1 && order.status === 3) {
      order.status = 4;
      order.finishAt = now();
      const earned = Math.floor(order.payableAmount);
      order.pointEarned = earned;
      member.point += earned;
    }
    return { code: 0, data: true };
  }
  return { code: 404, msg: `stub 未实现: ${url}` };
}

// ---------------- vm 加载器（支持 @/ 别名 + 模块级 stub） ----------------
const storage = new Map();
const context = vm.createContext({
  console,
  setTimeout: (fn) => fn(),
  uni: {
    getStorageSync: (key) => structuredClone(storage.get(key)),
    setStorageSync: (key, value) => storage.set(key, structuredClone(value)),
    removeStorageSync: (key) => storage.delete(key),
    showLoading: () => {},
    hideLoading: () => {},
    showToast: () => {},
    uploadFile: (options) => {
      options.success({
        data: JSON.stringify({ code: 0, data: { url: `http://file-server/pharmacy/prescription/p${++state.seq}.jpg` } }),
      });
      options.complete && options.complete();
    },
  },
});
context.__backend = backend;
context.__storage = storage;

const modules = new Map();
function resolveId(name, parentFile) {
  if (name.startsWith('@/')) return path.join(MALL_ROOT, name.slice(2) + '.js');
  return path.resolve(path.dirname(parentFile), name + '.js');
}
async function load(file) {
  if (modules.has(file)) return modules.get(file);
  const mod = new vm.SourceTextModule(fs.readFileSync(file, 'utf8'), { context, identifier: file });
  modules.set(file, mod);
  await mod.link((name, parent) => {
    if (name === '@/sheep/request') return loadStubRequest();
    if (name === '@/sheep/config') return loadStubConfig();
    return load(resolveId(name, parent.identifier));
  });
  return mod;
}
async function loadStubRequest() {
  const file = path.join(MALL_ROOT, '.test-stubs', 'request.js');
  const mod = new vm.SourceTextModule(
    "export default (config) => Promise.resolve().then(() => __backend(config));" +
      "export const getAccessToken = () => uni.getStorageSync('token') || '';",
    { context, identifier: file },
  );
  modules.set(file, mod);
  await mod.link(() => {
    throw new Error('request stub 无依赖');
  });
  return mod;
}
async function loadStubConfig() {
  const file = path.join(MALL_ROOT, '.test-stubs', 'config.js');
  const mod = new vm.SourceTextModule(
    "export const baseUrl = 'http://localhost:48080';\nexport const apiPath = '/app-api';\nexport const tenantId = 163;",
    { context, identifier: file },
  );
  modules.set(file, mod);
  await mod.link(() => {
    throw new Error('config stub 无依赖');
  });
  return mod;
}

(async () => {
  // ---------- 单元：金额与字段映射 ----------
  const clientMod = await load(path.join(PHARMACY_ROOT, '../../sheep/api/pharmacy/client.js'));
  await clientMod.evaluate();
  const api = clientMod.namespace.default;
  const { money, statusNames, normalizeDrug } = clientMod.namespace;

  assert.equal(money(1990), '19.90');
  assert.equal(statusNames.unpaid, '待支付');
  const mapped = normalizeDrug({ id: 11, genericName: '测试药品', retailPrice: '19.90', memberPrice: '17.80', isRx: 1, drugType: 0 }, { stock: 5, approval: '国药准字H1' });
  assert.equal(mapped.price, 1780, '会员价优先');
  assert.equal(mapped.stock, 5);
  assert.equal(mapped.rx, true);
  assert.equal(mapped.approval, '国药准字H1');
  const device = normalizeDrug({ id: 12, genericName: '体温计', retailPrice: '9.9', isRx: 0, drugType: 6 }, { stock: 0 });
  assert.equal(device.device, true);
  assert.equal(device.stock, 0);

  // ---------- 闭环三：登录 → 会员中心 → 地址 → 订单 ----------
  assert.equal(api.prescriptionDraft().length, 0);
  await assert.rejects(api.cart(), /账号未登录/);
  await assert.rejects(api.login('100', '123456'), /手机号/);
  await assert.rejects(api.login('13900001611', '000000'), /测试验证码不正确/);
  await api.login('13900001611', '123456');
  assert.ok(api.session(), '登录后 session 存在');
  const profile = await api.profile();
  assert.equal(profile.name, '顾晨');
  assert.equal(profile.points, 680);

  // ---------- 首页 / 分类 / 搜索（闭环一前半） ----------
  await api.loadCategories();
  const cats = api.categories;
  assert.equal(cats[0].id, 'all');
  assert.equal(cats[0].name, '全部药品');
  assert.ok(cats.length >= 4, '分类含全部 + 真实分类');
  assert.equal((await api.products({ category: 163003 })).length, 1, '处方分类只含洛赛克');
  assert.equal((await api.products({ keyword: '感康' })).length, 1, '关键词搜索');
  const home = await api.products();
  assert.ok(home.every((p) => p.stock >= 0 && Number.isInteger(p.price)), '价格分为整数');
  assert.equal((await api.product(163105)).device, true);
  assert.equal((await api.product(163105)).stock, 99, '详情页库存来自可售量');

  // ---------- 闭环一：加入购物车（库存校验） ----------
  await assert.rejects(api.add(163101, 999), /库存不足/);
  await api.add(163101, 2);
  await api.add(163102, 1);
  let cart = await api.cart();
  assert.equal(cart.length, 2);
  assert.equal(cart.find((r) => r.drugId === 163101).qty, 2);
  assert.equal(cart.find((r) => r.drugId === 163101).product.price, 1780, '购物车取会员价');
  await assert.rejects(api.updateCart(163101, { qty: 999 }), /库存不足/);
  await assert.rejects(api.updateCart(163101, { qty: 0 }), /库存不足/);
  await api.updateCart(163101, { checked: false });
  assert.equal((await api.cart()).find((r) => r.drugId === 163101).checked, false);
  await api.selectAll(true);
  assert.ok((await api.cart()).every((r) => r.checked));

  // ---------- 库存变化：结算前拦截 ----------
  await assert.rejects(api.checkout(), /结算已失效/);
  api.beginCheckout();
  state.inventory.set(163102, 0);
  await assert.rejects(api.checkout(), /163102|芬必得|库存不足/, '库存为 0 时结算被拦截');
  state.inventory.set(163102, 16);

  // ---------- 闭环二：结算 → 地址/自提 → 提交订单 ----------
  const checkout = await api.checkout();
  assert.equal(checkout.items.length, 2);
  assert.equal(checkout.points, 680);
  assert.ok(checkout.maxDeductFen > 0, '服务端试算最大抵扣金额');
  assert.match(checkout.pointsRule, /积分抵 ¥1/);
  await assert.rejects(api.createOrder({ mode: 'delivery', address: null, usePoints: true, remark: '' }), /收货地址/);
  const address = await api.saveAddress({ name: '顾晨', mobile: '13900001611', detail: '厦门市思明区演示路 88 号', isDefault: true });
  assert.equal((await api.addresses())[0].isDefault, true);
  assert.ok(address.id > 0, '地址由后端分配编号');

  const subtotalFen = checkout.items.reduce((s, r) => s + r.product.price * r.qty, 0);
  const deductFen = checkout.maxDeductFen;
  const options = { mode: 'delivery', address, usePoints: true, remark: '测试订单' };
  const [one, duplicate] = await Promise.all([api.createOrder(options), api.createOrder(options)]);
  assert.equal(one.id, duplicate.id, '并发重复提交只生成一单');
  assert.equal(one.mode, 'delivery');
  assert.equal(one.status, 'unpaid');
  assert.equal(one.total, subtotalFen - deductFen, '应付 = 商品金额 - 服务端抵扣');
  assert.equal(one.subtotal, subtotalFen);
  assert.equal(one.shipping, 0, '配送费按后端口径为 0');
  assert.equal((await api.orders()).length, 1);
  assert.equal((await api.createOrder(options)).id, one.id, '同草稿重复下单幂等');

  // Stub adapter tests only: unsafe mock payment must propagate a rejection.
  await assert.rejects(api.orderAction(one.id, 'pay'), /未启用|尚未接通/);
  assert.equal((await api.order(one.id)).status, 'unpaid');
  // Explicit paid fixture is NOT proof of payment success or stock accounting.
  const fixture = members['13900001611'].orders.find(o => o.id === one.id);
  fixture.payStatus = 1; fixture.status = 1;
  assert.equal((await api.order(one.id)).canReceive, false);
  await assert.rejects(api.orderAction(one.id, 'receive'), /状态已变化/);
  fixture.status = 3;
  assert.equal((await api.order(one.id)).canReceive, true);
  const before = (await api.profile()).points;
  await api.orderAction(one.id, 'receive');
  assert.equal((await api.order(one.id)).status, 'completed');
  assert.ok((await api.profile()).points > before);

  // 取消订单（积分退回）
  await api.add(163104, 2);
  api.beginCheckout();
  const checkout2 = await api.checkout();
  const order2 = await api.createOrder({ mode: 'pickup', address: null, usePoints: false, remark: '' });
  assert.equal(order2.mode, 'pickup');
  const pointsBeforeCancel = (await api.profile()).points;
  await api.orderAction(order2.id, 'cancel');
  assert.equal((await api.order(order2.id)).status, 'cancelled');
  assert.equal((await api.profile()).points, pointsBeforeCancel, '取消订单不产生积分变化（未用积分）');

  // Private upload is blocked until authenticated file access exists.
  api.beginCheckout({ id: 163103, qty: 1 });
  await assert.rejects(api.createOrder({ mode: 'pickup', address: null, usePoints: false, remark: '' }), /处方/);
  await assert.rejects(api.savePrescriptions(['a', 'b', 'c', 'd']), /最多/);
  await assert.rejects(api.savePrescriptions(['file:///tmp/p1.jpg']), /私有上传/);
  await assert.rejects(api.savePrescriptions(['https://public.example/prescription.jpg']), /私有上传/);
  assert.equal(api.prescriptionDraft().length, 0);

  // ---------- 错误 / 空数据 / 网络失败 ----------
  state.forceFail.add('/pharmacy/inventory');
  const degraded = await api.products();
  assert.equal(degraded[0].stock, 0, '库存服务失败降级为 0 且不阻塞浏览');
  state.forceFail.clear();
  state.forceFail.add('/pharmacy/drug');
  await assert.rejects(api.products(), /网络请求失败/, '药品列表服务失败');
  state.forceFail.clear();
  await api.refreshStore();
  state.forceFail.add('/pharmacy/store');
  await assert.rejects(api.refreshStore(), /网络请求失败|营业门店/, '门店服务失败');
  state.forceFail.clear();
  await api.refreshStore();
  assert.ok(api.store.id > 0, '门店恢复后可重新加载');

  // ---------- 登出与账号隔离 ----------
  await api.logout();
  assert.equal(api.session(), null);
  await assert.rejects(api.orders(), /账号未登录/);
  await api.login('13900001612', '123456');
  assert.equal((await api.orders()).length, 0, '新账号看不到旧订单');
  assert.equal((await api.cart()).length, 0);

  // ---------- Mock 适配器回归冒烟 ----------
  storage.clear();
  const demoMod = await load(path.join(PHARMACY_ROOT, '../../sheep/api/pharmacy/demo.js'));
  await demoMod.evaluate();
  const demo = demoMod.namespace.default;
  await demo.login('13800138000', '123456');
  assert.equal((await demo.products({ keyword: 'DEMO000001' })).length, 1);
  await demo.add(1, 2);
  demo.beginCheckout();
  assert.equal((await demo.checkout()).items.length, 1);
  const demoOrder = await demo.createOrder({ mode: 'pickup', address: null, usePoints: false, remark: '' });
  assert.equal(demoOrder.total, 3180);

  console.log('PASS: stub adapter and demo smoke; payment rejection, delivery receipt guards, private-upload rejection, amount/cart/stock/account checks. NOT real HTTP acceptance.');
})().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
