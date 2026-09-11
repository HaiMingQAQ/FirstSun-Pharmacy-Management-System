package cn.iocoder.yudao.module.pharmacy.service.member;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.order.WxOrderPageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.order.WxOrderSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.WxOrderDO;

import javax.validation.Valid;
import java.util.Collection;
import java.util.List;

/**
 * 小程序订单 Service
 */
public interface WxOrderService {

    /**
     * 创建小程序订单
     */
    Long createWxOrder(@Valid WxOrderSaveReqVO createReqVO);

    /**
     * 更新小程序订单
     */
    void updateWxOrder(@Valid WxOrderSaveReqVO updateReqVO);

    /**
     * 删除小程序订单
     */
    void deleteWxOrder(Long id);

    /**
     * 获取小程序订单详情
     */
    WxOrderDO getWxOrder(Long id);

    /**
     * 批量获得小程序订单列表
     *
     * @param ids 订单编号集合
     * @return 订单列表（仅返回未删除的）
     */
    List<WxOrderDO> getWxOrderList(Collection<Long> ids);

    /**
     * 获取小程序订单分页
     */
    PageResult<WxOrderDO> getWxOrderPage(WxOrderPageReqVO reqVO);

    /**
     * 根据订单号获取订单
     */
    WxOrderDO getWxOrderByOrderNo(String orderNo);

    /**
     * 根据会员编号获取订单列表
     */
    List<WxOrderDO> getWxOrderListByMemberId(Long memberId);

    /**
     * 根据取货码获取订单
     */
    WxOrderDO getWxOrderByPickupCode(String pickupCode);

    /**
     * 校验订单存在
     */
    WxOrderDO validateWxOrderExists(Long id);

}
