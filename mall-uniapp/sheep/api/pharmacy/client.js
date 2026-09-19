import { drugs, categories, store } from './fixtures';
import { TEST_CODE, MAX_PRESCRIPTIONS } from './config';

export const money = (cents = 0) => (Number(cents) / 100).toFixed(2);
export const statusNames = {
  unpaid: '待支付',
  review: '待审核',
  ready: '待自提 / 待配送',
  completed: '已完成',
  cancelled: '已取消',
};
const key = 'firstsun-demo-v1';
const clone = (value) => JSON.parse(JSON.stringify(value));
const fresh = () => ({ user: null, accounts: {}, history: [] });
const read = () => uni.getStorageSync(key) || fresh();
const write = (db) => uni.setStorageSync(key, db);
const account = (db) => {
  if (!db.user) throw new Error('请先登录');
  return db.accounts[db.user.mobile];
};
const newAccount = () => ({
  points: 360,
  cart: [],
  orders: [],
  addresses: [],
  used: {},
  selectedAddress: null,
});
const catalogue = (db) =>
  drugs.map((d) => ({
    ...d,
    stock: Math.max(0, d.stock - (db.user ? account(db).used[d.id] || 0 : 0)),
  }));
const rows = (db) =>
  account(db).cart.map((r) => ({
    ...r,
    product: catalogue(db).find((d) => d.id === r.drugId) || {
      id: r.drugId,
      name: '商品已下架',
      stock: 0,
      active: false,
      price: 0,
    },
  }));
const validQty = (product, qty) => {
  if (!product || !product.active) throw new Error('商品已失效，请移除后重试');
  if (!Number.isInteger(qty) || qty < 1 || qty > product.stock)
    throw new Error(`库存不足，当前最多可购 ${product.stock} 件`);
};
// 测试注入：uni.setStorageSync('firstsun-demo-scenario', 'error' | 'empty')，删除该键恢复。
async function execute(fn) {
  await new Promise((resolve) => setTimeout(resolve, 180));
  if (uni.getStorageSync('firstsun-demo-scenario') === 'error')
    throw new Error('网络暂时不可用，请重试');
  return fn();
}
// AppDrugRespVO 当前不含图片、批准文号、门店库存；由真实适配器补充这些字段。
// 未提供库存时按 0 处理，不能把未知库存当作可售库存。
export function normalizeDrug(raw, availability = {}) {
  return {
    id: raw.id,
    name: raw.tradeName || raw.genericName,
    genericName: raw.genericName,
    specification: raw.specification,
    manufacturer: raw.manufacturer,
    category: raw.categoryId,
    barcode: availability.barcode || '',
    approval: availability.approval || '商品档案暂未提供',
    image: availability.image || '',
    price: Math.round(Number(raw.retailPrice || 0) * 100),
    stock: Math.max(0, Math.floor(Number(availability.stock) || 0)),
    rx: Number(raw.isRx) === 1,
    device: Number(raw.drugType) === 6,
    active: availability.active !== false,
  };
}
const api = {
  session: () => read().user,
  store,
  categories,
  async products({ keyword = '', category = 'all' } = {}) {
    return execute(() =>
      uni.getStorageSync('firstsun-demo-scenario') === 'empty'
        ? []
        : catalogue(read()).filter(
            (d) =>
              (category === 'all' || d.category === category) &&
              `${d.name}${d.genericName}${d.barcode}`
                .toLowerCase()
                .includes(keyword.trim().toLowerCase()),
          ),
    );
  },
  async product(id) {
    return execute(() => {
      const d = catalogue(read()).find((p) => p.id === Number(id));
      if (!d) throw new Error('药品不存在或已下架');
      return d;
    });
  },
  async login(mobile, code) {
    return execute(() => {
      if (!/^1[3-9]\d{9}$/.test(mobile)) throw new Error('请输入正确的 11 位手机号');
      if (code !== TEST_CODE) throw new Error('测试验证码不正确');
      const db = read();
      db.user = { mobile, name: 'FirstSun 会员', level: '普通会员' };
      db.accounts[mobile] ||= newAccount();
      write(db);
      return db.user;
    });
  },
  logout() {
    const db = read();
    db.user = null;
    write(db);
    uni.removeStorageSync('firstsun-checkout');
  },
  async profile() {
    return execute(() => {
      const db = read();
      return { ...db.user, points: account(db).points };
    });
  },
  history() {
    return read().history;
  },
  remember(word) {
    if (!word.trim()) return;
    const db = read();
    db.history = [word.trim(), ...db.history.filter((w) => w !== word.trim())].slice(0, 6);
    write(db);
  },
  clearHistory() {
    const db = read();
    db.history = [];
    write(db);
  },
  async cart() {
    return execute(() => rows(read()));
  },
  async add(drugId, qty = 1) {
    return execute(() => {
      const db = read();
      const a = account(db);
      const row = a.cart.find((r) => r.drugId === drugId);
      const count = (row?.qty || 0) + qty;
      validQty(
        catalogue(db).find((p) => p.id === drugId),
        count,
      );
      if (row) {
        row.qty = count;
        row.checked = true;
      } else a.cart.push({ drugId, qty, checked: true });
      write(db);
    });
  },
  async updateCart(drugId, patch) {
    return execute(() => {
      const db = read();
      const row = account(db).cart.find((r) => r.drugId === drugId);
      if (!row) throw new Error('购物车已更新，请刷新');
      if (patch.qty !== undefined)
        validQty(
          catalogue(db).find((p) => p.id === drugId),
          patch.qty,
        );
      Object.assign(row, patch);
      write(db);
    });
  },
  async selectAll(checked) {
    return execute(() => {
      const db = read();
      account(db).cart.forEach((r) => {
        const p = catalogue(db).find((d) => d.id === r.drugId);
        r.checked = !!(checked && p?.active && p.stock >= r.qty);
      });
      write(db);
    });
  },
  async remove(drugId) {
    return execute(() => {
      const db = read();
      account(db).cart = account(db).cart.filter((r) => r.drugId !== drugId);
      write(db);
    });
  },
  async addresses() {
    return execute(() => clone(account(read()).addresses));
  },
  async saveAddress(address) {
    return execute(() => {
      if (!address.name.trim() || !address.detail.trim()) throw new Error('请填写收货人和完整地址');
      if (!/^1[3-9]\d{9}$/.test(address.mobile)) throw new Error('请输入正确的 11 位手机号');
      const db = read();
      const a = account(db);
      const item = { ...address, id: address.id || Date.now() };
      if (!a.addresses.length) item.isDefault = true;
      if (item.isDefault) a.addresses.forEach((r) => (r.isDefault = false));
      const index = a.addresses.findIndex((r) => r.id === item.id);
      if (index >= 0) a.addresses[index] = item;
      else a.addresses.push(item);
      if (!a.addresses.some((r) => r.isDefault)) a.addresses[0].isDefault = true;
      write(db);
      return item;
    });
  },
  async deleteAddress(id) {
    return execute(() => {
      const db = read();
      const a = account(db);
      a.addresses = a.addresses.filter((r) => r.id !== id);
      if (a.addresses.length && !a.addresses.some((r) => r.isDefault))
        a.addresses[0].isDefault = true;
      if (a.selectedAddress === id) a.selectedAddress = null;
      write(db);
    });
  },
  chooseAddress(id) {
    const db = read();
    account(db).selectedAddress = id;
    write(db);
  },
  async checkout() {
    return execute(() => {
      const db = read();
      const a = account(db);
      const draft = uni.getStorageSync('firstsun-checkout');
      if (!draft || draft.mobile !== db.user.mobile) throw new Error('结算已失效，请重新选择商品');
      const items = draft.buy
        ? [
            {
              drugId: draft.buy.id,
              qty: draft.buy.qty,
              product: catalogue(db).find((p) => p.id === draft.buy.id),
            },
          ]
        : rows(db).filter((r) => r.checked);
      if (!items.length) throw new Error('请先选择要结算的商品');
      items.forEach((r) => validQty(r.product, r.qty));
      return {
        items,
        points: a.points,
        address:
          a.addresses.find((r) => r.id === a.selectedAddress) ||
          a.addresses.find((r) => r.isDefault),
        prescriptions: draft.prescriptions || [],
        draft,
      };
    });
  },
  beginCheckout(buy = null) {
    const user = read().user;
    if (!user) throw new Error('请先登录');
    uni.setStorageSync('firstsun-checkout', {
      mobile: user.mobile,
      buy,
      token: `${Date.now()}-${Math.random()}`,
      prescriptions: [],
    });
  },
  prescriptionDraft() {
    const draft = uni.getStorageSync('firstsun-checkout');
    return draft && draft.mobile === read().user?.mobile ? draft.prescriptions || [] : [];
  },
  savePrescriptions(images) {
    const draft = uni.getStorageSync('firstsun-checkout');
    if (!draft || draft.mobile !== read().user?.mobile) throw new Error('请先从确认订单页上传处方');
    if (images.length > MAX_PRESCRIPTIONS) throw new Error('最多上传 3 张处方');
    uni.setStorageSync('firstsun-checkout', { ...draft, prescriptions: images });
  },
  async createOrder({ mode, address, usePoints, remark }) {
    const dbBefore = read();
    const token = uni.getStorageSync('firstsun-checkout')?.token;
    const existing = account(dbBefore).orders.find((o) => o.token === token);
    if (existing) return clone(existing);
    const checkout = await api.checkout();
    return execute(() => {
      const db = read();
      const a = account(db);
      const prior = a.orders.find((o) => o.token === checkout.draft.token);
      if (prior) return prior;
      if (!['delivery', 'pickup'].includes(mode)) throw new Error('请选择配送或自提');
      if (mode === 'delivery' && !address) throw new Error('请选择收货地址');
      const rx = checkout.items.some((r) => r.product.rx);
      if (rx && !checkout.prescriptions.length) throw new Error('处方药需上传处方并经药师审核');
      checkout.items.forEach((r) =>
        validQty(
          catalogue(db).find((p) => p.id === r.drugId),
          r.qty,
        ),
      );
      const subtotal = checkout.items.reduce((s, r) => s + r.product.price * r.qty, 0);
      const points = usePoints ? Math.min(a.points, subtotal, 500) : 0;
      const shipping = mode === 'delivery' ? 600 : 0;
      const order = {
        id: `FS${Date.now()}`,
        token: checkout.draft.token,
        createdAt: new Date().toLocaleString(),
        status: rx ? 'review' : 'unpaid',
        review: rx ? '待药师审核' : '无需处方审核',
        paid: false,
        mode,
        address: clone(address || null),
        store: clone(store),
        items: clone(checkout.items),
        prescriptions: [...checkout.prescriptions],
        subtotal,
        shipping,
        points,
        total: subtotal + shipping - points,
        remark: remark.trim(),
      };
      checkout.items.forEach((r) => (a.used[r.drugId] = (a.used[r.drugId] || 0) + r.qty));
      a.points -= points;
      a.orders.unshift(order);
      if (!checkout.draft.buy)
        a.cart = a.cart.filter((r) => !checkout.items.some((i) => i.drugId === r.drugId));
      write(db);
      return clone(order);
    });
  },
  async orders() {
    return execute(() => clone(account(read()).orders));
  },
  async order(id) {
    return execute(() => {
      const order = account(read()).orders.find((o) => o.id === id);
      if (!order) throw new Error('订单不存在，请返回订单列表');
      return clone(order);
    });
  },
  async orderAction(id, action) {
    return execute(() => {
      const db = read();
      const a = account(db);
      const o = a.orders.find((r) => r.id === id);
      if (!o) throw new Error('订单不存在');
      if (action === 'cancel' && ['unpaid', 'review'].includes(o.status)) {
        o.status = 'cancelled';
        a.points += o.points;
        o.items.forEach((r) => (a.used[r.drugId] -= r.qty));
      } else if (action === 'pay' && o.status === 'unpaid') {
        o.paid = true;
        o.status = 'ready';
      } else if (action === 'receive' && o.status === 'ready') o.status = 'completed';
      else throw new Error('订单状态已变化，请刷新后重试');
      write(db);
      return clone(o);
    });
  },
};
export default api;
