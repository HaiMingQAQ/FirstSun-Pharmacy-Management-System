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

    // ========== 状态流转与核销 ==========

    /**
     * 支付成功回调（幂等）：待支付 → 待拣货
     *
     * 重复回调不重复处理：已支付、已完成状态直接返回，不抛错。
     */
    void payWxOrder(Long id, String payNo);

    /**
     * 取消订单（幂等）：待支付 / 待拣货 → 取消
     *
     * 已取消、已完成状态不可再取消；已支付（已扣库存）的取消需释放库存，重复取消不重复释放。
     */
    void cancelWxOrder(Long id, String cancelReason);

    /**
     * 开始拣货：待拣货 → 拣货中
     */
    void startPicking(Long id);

    /**
     * 拣货完成：拣货中 → 待自提
     */
    void finishPicking(Long id);

    /**
     * 核销订单（幂等）：待自提 → 完成
     *
     * 校验取货码，记录核销员工与核销时间；已核销订单重复核销直接返回，不抛错。
     */
    void verifyWxOrder(Long id, String pickupCode, Long verifyBy);

    /**
     * 校验订单归属（线上订单只允许本人查看/操作）
     *
     * @return 订单信息
     */
    WxOrderDO validateWxOrderOwner(Long memberId, Long orderId);

}
