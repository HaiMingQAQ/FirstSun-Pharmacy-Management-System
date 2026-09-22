const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const MALL_ROOT = path.resolve(__dirname, '../../..');
const PHARMACY_ROOT = path.join(MALL_ROOT, 'pages/pharmacy');
const serverSource = fs.readFileSync(path.join(MALL_ROOT, 'sheep/api/pharmacy/server.js'), 'utf8');
const loginSource = fs.readFileSync(path.join(PHARMACY_ROOT, 'login.vue'), 'utf8');
const userSource = fs.readFileSync(path.join(PHARMACY_ROOT, 'user.vue'), 'utf8');
const pageSource = fs.readFileSync(
  path.join(MALL_ROOT, 'sheep/components/s-pharmacy-page/s-pharmacy-page.vue'),
  'utf8',
);

assert.match(serverSource, /url: '\/member\/auth\/social-login'/);
assert.match(serverSource, /data: \{ code: loginResult\.code \}/);
assert.match(serverSource, /isToken: false, skipTenant: true/);
assert.match(serverSource, /skipUserInit: true/);
assert.match(serverSource, /api\.profile\(\{ skipRefresh: true \}\)/);
assert.match(serverSource, /clearAuthSession\(\)/);
assert.doesNotMatch(serverSource, /data:\s*\{[^}]*openid/);
assert.match(loginSource, /<!-- #ifdef MP-WEIXIN -->/);
assert.match(loginSource, /<!-- #ifndef MP-WEIXIN -->/);
assert.match(loginSource, /:disabled="busy \|\| !agreed"/);
assert.match(userSource, /mobile \? mobile\.replace/);
assert.match(userSource, /profile\.value = session/);
assert.match(userSource, /await api\.logout\(\)/);
assert.match(pageSource, /PHARMACY_DEMO[\s\S]*商品价格及库存来自演示环境/);

function createHarness({ login, socialResponse, profileResponse } = {}) {
  const storage = new Map([
    ['tenant-id', 999],
    ['token', 'stale-token'],
    ['refresh-token', 'stale-refresh'],
    ['firstsun-session', { userId: 999 }],
  ]);
  const requests = [];
  const logs = [];
  let loginCount = 0;
  const clearToken = () => {
    storage.delete('token');
    storage.delete('refresh-token');
  };
  const context = vm.createContext({
    console: {
      log: console.log,
      warn: console.warn,
      error: console.error,
      info: (label, detail) => logs.push({ label, detail }),
    },
    Math,
    Date,
    Promise,
    Map,
    uni: {
      getStorageSync: (key) => storage.get(key),
      setStorageSync: (key, value) => storage.set(key, value),
      removeStorageSync: (key) => storage.delete(key),
      login:
        login ||
        (async () => ({ errMsg: 'login:ok', code: `synthetic-code-${++loginCount}` })),
    },
    $store: () => ({
      setToken: (token = '', refreshToken = '') => {
        if (!token) return clearToken();
        storage.set('token', token);
        storage.set('refresh-token', refreshToken);
      },
    }),
    clearLoginState: clearToken,
    request: async (config) => {
      requests.push(config);
      if (config.url === '/member/auth/social-login') {
        const response = socialResponse || {
          code: 0,
          data: { accessToken: 'synthetic-access-token', refreshToken: 'synthetic-refresh-token' },
        };
        if (response.code === 0) {
          storage.set('token', response.data.accessToken);
          storage.set('refresh-token', response.data.refreshToken);
        }
        return response;
      }
      if (config.url === '/member/user/get') {
        return (
          profileResponse || {
            code: 0,
            data: { id: 11001, nickname: '合成会员', mobile: null, point: 0 },
          }
        );
      }
      if (config.url === '/member/auth/logout') return { code: 0, data: true };
      throw new Error(`unexpected request: ${config.url}`);
    },
    DrugApi: {},
    StoreApi: {},
    CartApi: {},
    OrderApi: {},
    PrescriptionApi: {},
    InventoryApi: {},
    TEST_CODE: '123456',
    MAX_PRESCRIPTIONS: 3,
    DEFAULT_AREA_ID: 350203,
    money: () => '',
    statusNames: {},
    normalizeDrug: (value) => value,
    fen: (value) => value,
    yuan: (value) => value,
    formatDate: (value) => value,
    reviewStatusNames: {},
  });
  const executable = serverSource
    .replace(/^import .*?;\r?\n/gm, '')
    .replace('export default api;', 'globalThis.__pharmacyApi = api;');
  vm.runInContext(executable, context, { filename: 'server.js' });
  return { context, storage, requests, logs };
}

(async () => {
  const success = createHarness();
  const first = await vm.runInContext('__pharmacyApi.wechatLogin()', success.context);
  const second = await vm.runInContext('__pharmacyApi.wechatLogin()', success.context);
  assert.equal(first.userId, 11001);
  assert.equal(second.userId, first.userId, '重复登录复用同一会员资料');
  assert.equal(success.storage.has('tenant-id'), false);
  assert.equal(success.requests.length, 4);
  assert.equal(success.requests[0].custom.isToken, false);
  assert.equal(success.requests[0].custom.skipTenant, true);
  assert.equal(success.requests[1].url, '/member/user/get');
  assert.equal(success.requests[1].custom.skipRefresh, true);
  assert.equal(success.requests[1].custom.rejectOnError, true);
  assert.equal(success.requests.some((request) => JSON.stringify(request).includes('openid')), false);
  assert.deepEqual(
    success.logs.map(({ detail }) => [detail.stage, detail.status, detail.code]),
    [
      ['wechat-sdk', 'success', 0],
      ['social-login', 'success', 0],
      ['member-profile', 'success', 0],
      ['wechat-sdk', 'success', 0],
      ['social-login', 'success', 0],
      ['member-profile', 'success', 0],
    ],
  );
  const serializedLogs = JSON.stringify(success.logs);
  assert.equal(serializedLogs.includes('synthetic-code'), false);
  assert.equal(serializedLogs.includes('synthetic-access-token'), false);
  await vm.runInContext('__pharmacyApi.logout()', success.context);
  assert.equal(success.storage.has('token'), false);
  assert.equal(success.storage.has('refresh-token'), false);
  assert.equal(success.storage.has('firstsun-session'), false);

  const sdkFailure = createHarness({
    login: async () => Promise.reject({ errMsg: 'login:fail synthetic', errCode: -80002 }),
  });
  await assert.rejects(
    vm.runInContext('__pharmacyApi.wechatLogin()', sdkFailure.context),
    /微信登录凭证获取失败/,
  );
  assert.equal(sdkFailure.requests.length, 0);
  assert.equal(sdkFailure.storage.has('token'), false);
  assert.equal(sdkFailure.storage.has('firstsun-session'), false);
  assert.deepEqual(
    [sdkFailure.logs[0].detail.stage, sdkFailure.logs[0].detail.status, sdkFailure.logs[0].detail.code],
    ['wechat-sdk', 'failed', -80002],
  );
  assert.equal(JSON.stringify(sdkFailure.logs).includes('login:fail synthetic'), false);

  const profileFailure = createHarness({
    profileResponse: { code: 401, msg: '合成会员资料校验失败' },
  });
  await assert.rejects(
    vm.runInContext('__pharmacyApi.wechatLogin()', profileFailure.context),
    /合成会员资料校验失败/,
  );
  assert.equal(profileFailure.requests.length, 2);
  assert.equal(profileFailure.storage.has('token'), false);
  assert.equal(profileFailure.storage.has('refresh-token'), false);
  assert.equal(profileFailure.storage.has('firstsun-session'), false);
  assert.deepEqual(
    [
      profileFailure.logs[2].detail.stage,
      profileFailure.logs[2].detail.status,
      profileFailure.logs[2].detail.code,
    ],
    ['member-profile', 'failed', 401],
  );

  console.log(
    'PASS: pharmacy WeChat login success/repeat/failure cleanup and safe stage diagnostics; no real WeChat request performed.',
  );
})().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
