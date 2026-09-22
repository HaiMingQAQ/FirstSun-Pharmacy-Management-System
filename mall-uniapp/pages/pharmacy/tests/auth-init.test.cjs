const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const MALL_ROOT = path.resolve(__dirname, '../../..');
const requestSource = fs.readFileSync(path.join(MALL_ROOT, 'sheep/request/index.js'), 'utf8');
const authSource = fs.readFileSync(path.join(MALL_ROOT, 'sheep/api/member/auth.js'), 'utf8');
const userSource = fs.readFileSync(path.join(MALL_ROOT, 'sheep/store/user.js'), 'utf8');
const pharmacySource = fs.readFileSync(
  path.join(MALL_ROOT, 'sheep/api/pharmacy/server.js'),
  'utf8',
);

// Pharmacy auth responses keep the shared login state but do not start mall-only initialization.
assert.match(requestSource, /skipLoginAfter: response\.config\.custom\.skipUserInit === true/);
assert.match(requestSource, /AuthUtil\.refreshToken\(\s*refreshToken,\s*config\.custom\.skipUserInit === true/);
assert.match(requestSource, /config\.custom\.refreshAttempted/);
assert.match(requestSource, /response\.data\.code === 401 && !response\.config\.custom\.skipRefresh/);
assert.match(requestSource, /if \(!config\.custom\?\.skipUserInit\) showAuthModal\(\)/);
assert.match(requestSource, /error\.config\.custom\.rejectOnError/);
assert.match(authSource, /refreshToken: \(refreshToken, skipUserInit = false\)/);
assert.match(userSource, /if \(!options\.skipLoginAfter\) this\.loginAfter\(\);/);
assert.match(pharmacySource, /isToken: true,\s*skipUserInit: true/);
assert.match(pharmacySource, /url: '\/member\/auth\/social-login'[\s\S]*?skipUserInit: true/);
assert.match(pharmacySource, /url: '\/member\/auth\/login-or-register'[\s\S]*?skipUserInit: true/);

// Exercise the changed user action: ordinary mall login still starts loginAfter,
// while the pharmacy marker does not. No request, token, or secret is logged.
const executable = userSource
  .replace(/^import .*?;\r?\n/gm, '')
  .replace('export default user;', 'globalThis.__userConfig = user;');
const storage = new Map();
const context = vm.createContext({
  clone: (value) => ({ ...value }),
  cloneDeep: (value) => JSON.parse(JSON.stringify(value)),
  defineStore: (_name, config) => config,
  cart: () => ({ emptyList() {} }),
  app: () => ({ platform: { bind_mobile: 0 } }),
  $share: { getShareInfo() {}, bindBrokerageUser() {} },
  showAuthModal() {},
  uni: {
    getStorageSync: (key) => storage.get(key),
    setStorageSync: (key, value) => storage.set(key, value),
    removeStorageSync: (key) => storage.delete(key),
  },
});
vm.runInContext(executable, context, { filename: 'sheep/store/user.js' });
const instance = { ...vm.runInContext('__userConfig.state()', context) };
const actions = vm.runInContext('__userConfig.actions', context);
Object.assign(instance, actions);
let loginAfterCalls = 0;
instance.loginAfter = () => {
  loginAfterCalls++;
};
instance.setToken('mall-token', 'mall-refresh');
instance.setToken('pharmacy-token', 'pharmacy-refresh', { skipLoginAfter: true });
assert.equal(loginAfterCalls, 1, 'pharmacy login must not start mall initialization');
assert.equal(storage.get('token'), 'pharmacy-token');
assert.equal(storage.get('refresh-token'), 'pharmacy-refresh');

console.log('PASS: pharmacy auth bypasses mall-only user initialization; mall login path remains unchanged.');
