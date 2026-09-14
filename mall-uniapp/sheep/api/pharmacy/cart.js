import request from '@/sheep/request';

/**
 * 药店小程序购物车 API
 * 对应后端 /app-api/member/wx-cart/**（会员令牌必传，购物车仅本人可见）
 */
const CartApi = {
  // 获得本人购物车列表（含商品信息）
  getCartList: () => {
    return request({
      url: '/member/wx-cart/list',
      method: 'GET',
      custom: {
        showLoading: false,
      },
    });
  },
  // 获得本人购物车数量合计
  getCartCount: () => {
    return request({
      url: '/member/wx-cart/count',
      method: 'GET',
      custom: {
        showLoading: false,
        showError: false,
      },
    });
  },
  // 加购（同一门店同一商品累加数量）
  addCart: (data) => {
    return request({
      url: '/member/wx-cart/add',
      method: 'POST',
      data,
      custom: {
        showSuccess: true,
        successMsg: '已加入购物车',
      },
    });
  },
  // 修改购物车数量
  updateCartQty: (id, qty) => {
    return request({
      url: '/member/wx-cart/update-qty',
      method: 'PUT',
      data: {
        id,
        qty,
      },
      custom: {
        showLoading: false,
      },
    });
  },
  // 勾选 / 取消勾选
  updateCartSelected: (id, selectedFlag) => {
    return request({
      url: '/member/wx-cart/update-selected',
      method: 'PUT',
      params: {
        id,
        selectedFlag,
      },
      custom: {
        showLoading: false,
      },
    });
  },
  // 删除购物车记录
  deleteCart: (id) => {
    return request({
      url: '/member/wx-cart/delete',
      method: 'DELETE',
      params: {
        id,
      },
    });
  },
  // 清空本人购物车
  clearCart: () => {
    return request({
      url: '/member/wx-cart/clear',
      method: 'DELETE',
      custom: {
        showSuccess: true,
        successMsg: '已清空购物车',
      },
    });
  },
};

export default CartApi;
