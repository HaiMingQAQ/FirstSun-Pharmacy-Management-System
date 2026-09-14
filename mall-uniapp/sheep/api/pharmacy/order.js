import request from '@/sheep/request';

/**
 * 药店小程序线上订单 API
 * 对应后端 /app-api/member/wx-order/**（订单仅本人可见）
 */
const OrderApi = {
  // 从购物车已勾选商品下单
  createOrder: (data) => {
    return request({
      url: '/member/wx-order/create',
      method: 'POST',
      data,
      custom: {
        showSuccess: true,
        successMsg: '下单成功',
      },
    });
  },
  // 获得本人订单分页
  getOrderPage: (params) => {
    return request({
      url: '/member/wx-order/page',
      method: 'GET',
      params,
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
        showSuccess: true,
        successMsg: '已取消订单',
      },
    });
  },
};

export default OrderApi;
