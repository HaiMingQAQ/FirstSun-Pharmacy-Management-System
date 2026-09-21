import request from '@/sheep/request';

/**
 * 药店小程序会员积分 API
 * 对应后端 /app-api/member/point/** 与 /member/point-record/**
 */
const PointApi = {
  // 获得积分总览（当前积分 / 等级 / 抵扣规则）
  getPointSummary: () => {
    return request({
      url: '/member/point/summary',
      method: 'GET',
      custom: {
        showLoading: false,
        showError: false,
      },
    });
  },
  // 抵扣试算（服务端口径：金额单位元；usePoints=0 时返回最大可用抵扣）
  deductPreview: (data) => {
    return request({
      url: '/member/point/deduct-preview',
      method: 'POST',
      data,
      custom: {
        showLoading: false,
        showError: false,
      },
    });
  },
  // 获得积分明细分页
  getPointRecordPage: (params) => {
    return request({
      url: '/member/point-record/page',
      method: 'GET',
      params,
    });
  },
};

export default PointApi;
