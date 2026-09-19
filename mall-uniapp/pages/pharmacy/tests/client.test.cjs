// Run: node --experimental-vm-modules pages/pharmacy/tests/client.test.cjs
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const assert = require('node:assert/strict');
(async () => {
  const storage = new Map();
  const context = vm.createContext({
    console,
    setTimeout: (fn) => fn(),
    uni: {
      getStorageSync: (key) => structuredClone(storage.get(key)),
      setStorageSync: (key, value) => storage.set(key, structuredClone(value)),
      removeStorageSync: (key) => storage.delete(key),
    },
  });
  const modules = new Map();
  async function load(file) {
    if (modules.has(file)) return modules.get(file);
    const mod = new vm.SourceTextModule(fs.readFileSync(file, 'utf8'), {
      context,
      identifier: file,
    });
    modules.set(file, mod);
    await mod.link((name, parent) =>
      load(path.resolve(path.dirname(parent.identifier), name + '.js')),
    );
    return mod;
  }
  const mod = await load(path.resolve(__dirname, '../../../sheep/api/pharmacy/client.js'));
  await mod.evaluate();
  const api = mod.namespace.default;
  const mapped = mod.namespace.normalizeDrug({ id: 11, genericName: '测试药品', retailPrice: '19.90', isRx: 1, drugType: 0 });
  assert.equal(mapped.price, 1990);
  assert.equal(mapped.stock, 0);
  assert.equal(mapped.rx, true);
  assert.equal(api.prescriptionDraft().length, 0);
  await assert.rejects(api.cart(), /请先登录/);
  await assert.rejects(api.login('100', '123456'), /手机号/);
  await assert.rejects(api.login('13800138000', '000000'), /验证码/);
  await api.login('13800138000', '123456');
  assert.equal((await api.products({ keyword: 'DEMO000001' })).length, 1);
  await assert.rejects(api.add(3, 4), /库存不足/);
  await assert.rejects(api.add(3, -1), /库存不足/);
  await api.add(3, 3);
  await assert.rejects(api.add(3, 1), /库存不足/);
  await assert.rejects(api.updateCart(3, { qty: 1.5 }), /库存不足/);
  await api.updateCart(3, { checked: false });
  await api.add(1, 2);
  api.beginCheckout();
  assert.equal((await api.checkout()).items.length, 1);
  await assert.rejects(api.createOrder({ mode: 'delivery', remark: '' }), /收货地址/);
  const address = await api.saveAddress({
    name: '测试',
    mobile: '13800138000',
    detail: '测试市测试路 1 号',
    isDefault: true,
  });
  assert.equal((await api.addresses())[0].isDefault, true);
  const options = { mode: 'delivery', address, usePoints: true, remark: '测试订单' };
  const [one, duplicate] = await Promise.all([api.createOrder(options), api.createOrder(options)]);
  assert.equal(one.id, duplicate.id);
  assert.equal(one.total, 3180 + 600 - 360);
  assert.equal((await api.profile()).points, 0);
  assert.equal((await api.orders()).length, 1);
  assert.equal((await api.createOrder(options)).id, one.id);
  assert.equal((await api.product(1)).stock, 26);
  await api.orderAction(one.id, 'cancel');
  assert.equal((await api.profile()).points, 360);
  assert.equal((await api.product(1)).stock, 28);
  await assert.rejects(api.orderAction(one.id, 'cancel'), /订单状态/);
  await assert.rejects(api.orderAction(one.id, 'pay'), /订单状态/);
  api.beginCheckout({ id: 4, qty: 1 });
  await assert.rejects(api.createOrder({ mode: 'pickup', remark: '' }), /处方/);
  assert.throws(() => api.savePrescriptions(['1', '2', '3', '4']), /最多/);
  api.savePrescriptions(['test-local-image']);
  const rx = await api.createOrder({ mode: 'pickup', remark: '' });
  assert.equal(rx.status, 'review');
  await assert.rejects(api.orderAction(rx.id, 'pay'), /订单状态/);
  api.beginCheckout({ id: 2, qty: 1 });
  const otc = await api.createOrder({ mode: 'pickup', remark: '' });
  await api.orderAction(otc.id, 'pay');
  await api.orderAction(otc.id, 'receive');
  assert.equal((await api.order(otc.id)).status, 'completed');
  await api.deleteAddress(address.id);
  assert.equal((await api.addresses()).length, 0);
  api.logout();
  await api.login('13900139000', '123456');
  assert.equal((await api.orders()).length, 0);
  assert.equal((await api.cart()).length, 0);
  storage.set('firstsun-demo-scenario', 'error');
  await assert.rejects(api.products(), /网络/);
  storage.set('firstsun-demo-scenario', 'empty');
  assert.equal((await api.products()).length, 0);
  console.log(
    'PASS: auth, search, stock bounds, selection, integer money, idempotency, points refund, Rx gate, order transitions, address deletion, account isolation, error/empty scenarios',
  );
})().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
