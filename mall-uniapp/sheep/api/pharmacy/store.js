import request from '@/sheep/request';

/**
 * 门店选择 API（app-api 只读投影，数据来源 A 成员的门店能力）
 */
const StoreApi = {
  // 获得营业中的门店列表
  getStoreList: () => {
    return request({
      url: '/pharmacy/store/list',
      method: 'GET',
      custom: {
        showLoading: false,
        showError: false,
      },
    });
  },
};

export default StoreApi;
