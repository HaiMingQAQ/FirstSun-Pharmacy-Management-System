import request from '@/sheep/request';

/**
 * 药店商品浏览 API（app-api 只读投影，数据来源 A 成员的商品能力）
 * 浏览类接口无需登录
 */
const DrugApi = {
  // 获得可线上销售药品分页
  getDrugPage: (params) => {
    return request({
      url: '/pharmacy/drug/page',
      method: 'GET',
      params,
      custom: {
        showLoading: false,
        showError: false,
      },
    });
  },
  // 获得药品详情
  getDrug: (id) => {
    return request({
      url: '/pharmacy/drug/get',
      method: 'GET',
      params: {
        id,
      },
      custom: {
        showError: false,
      },
    });
  },
  // 获得启用的药品分类列表
  getCategoryList: () => {
    return request({
      url: '/pharmacy/drug/category-list',
      method: 'GET',
      custom: {
        showLoading: false,
        showError: false,
      },
    });
  },
};

export default DrugApi;
