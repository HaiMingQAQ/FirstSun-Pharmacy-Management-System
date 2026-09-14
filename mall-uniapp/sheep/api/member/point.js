import request from '@/sheep/request';

/**
 * 会员积分 API
 *
 * 说明：本项目的积分接口由药店模块提供，路径为 /app-api/member/point-record/page
 * （不同于商城模块的 /member/point/record/page），仅返回本人积分流水。
 */
const PointApi = {
  // 获得本人积分记录分页
  getPointRecordPage: (params) => {
    return request({
      url: '/member/point-record/page',
      method: 'GET',
      params,
      custom: {
        showLoading: false,
      },
    });
  },
};

export default PointApi;
