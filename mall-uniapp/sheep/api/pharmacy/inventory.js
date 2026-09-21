import request from '@/sheep/request';

/**
 * 药店小程序可售库存 API（F 新增最小 app-api，只读投影）
 * 对应后端 /app-api/pharmacy/inventory/available
 */
const InventoryApi = {
  // 获得门店下药品的可售数量（逗号分隔 drugIds；只返回有可售数量的药品）
  getAvailable: ({ storeId, drugIds }) => {
    return request({
      url: '/pharmacy/inventory/available',
      method: 'GET',
      params: { storeId, drugIds },
      custom: {
        showLoading: false,
        showError: false,
      },
    });
  },
};

export default InventoryApi;
