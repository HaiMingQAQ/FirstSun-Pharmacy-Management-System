import request from '@/sheep/request';

/**
 * 药店小程序处方 API（F 新增最小 app-api）
 * 对应后端 /app-api/member/prescription/**（处方仅本人可见）
 */
const PrescriptionApi = {
  // 登记处方（纸质拍照，来源=0，审方状态=待审）
  createPrescription: (data) => {
    return request({
      url: '/member/prescription/create',
      method: 'POST',
      data,
      custom: {
        showError: false,
      },
    });
  },
  // 获得我的处方列表
  getMyPrescriptions: () => {
    return request({
      url: '/member/prescription/page',
      method: 'GET',
      custom: {
        showLoading: false,
        showError: false,
      },
    });
  },
  // 获得我的处方详情（校验归属）
  getPrescription: (id) => {
    return request({
      url: '/member/prescription/get',
      method: 'GET',
      params: { id },
      custom: {
        showLoading: false,
        showError: false,
      },
    });
  },
};

export default PrescriptionApi;
