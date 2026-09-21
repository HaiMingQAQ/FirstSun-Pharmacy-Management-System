import request from '@/sheep/request';

/**
 * 药店小程序线上订单 API
 * 对应后端 /app-api/member/wx-order/**（订单仅本人可见）
 * 注意：成功/失败 toast 由药店页面与适配器负责，这里统一关闭系统 toast，避免重复提示。
 */
const OrderApi = {
  // 从购物车已勾选商品下单
  createOrder: (data) => {
    return request({
      url: '/member/wx-order/create',
      method: 'POST',
      data,
      custom: {
        showError: false,
      },
    });
  },
  // 获得本人订单分页
  getOrderPage: (params) => {
    return request({
      url: '/member/wx-order/page',
      method: 'GET',
      params,
      custom: {
        showLoading: false,
        showError: false,
      },
    });
  },
  // 获得本人订单详情（含明细）
  getOrder: (id) => {
    return request({
      url: '/member/wx-order/get',
      method: 'GET',
      params: {
        id,
      },
      custom: {
        showLoading: false,
        showError: false,
      },
    });
  },
  // 取消本人订单
  cancelOrder: (id, cancelReason) => {
    return request({
      url: '/member/wx-order/cancel',
      method: 'PUT',
      params: {
        id,
        cancelReason,
      },
      custom: {
        showError: false,
      },
    });
  },
  // 模拟支付本人订单（幂等，课堂演示用）
  simulatePay: (id) => {
    return request({
      url: '/member/wx-order/simulate-pay',
      method: 'POST',
      params: { id },
      custom: {
        showError: false,
      },
    });
  },
  // 确认收货 / 自提核销本人订单（幂等）
  confirmReceive: (id) => {
    return request({
      url: '/member/wx-order/confirm-receive',
      method: 'POST',
      params: { id },
      custom: {
        showError: false,
      },
    });
  },
};

export default OrderApi;
