// =====================================================
// 药店小程序 API 门面
// -----------------------------------------------------
// 页面与组件只从这里导入，统一消费同一契约：
//   import api, { money, statusNames } from '@/sheep/api/pharmacy/client';
// Mock / 真实适配器由 config.js 的 PHARMACY_DEMO 决定，页面不感知、不散落判断。
// =====================================================
import { PHARMACY_DEMO } from './config';
import demo from './demo';
import server from './server';
import { money, statusNames, normalizeDrug, formatDate } from './common';

const api = PHARMACY_DEMO ? demo : server;

export default api;
export { money, statusNames, normalizeDrug, formatDate, PHARMACY_DEMO };
