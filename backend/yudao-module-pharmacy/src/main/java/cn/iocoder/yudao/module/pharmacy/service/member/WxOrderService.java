package cn.iocoder.yudao.module.pharmacy.service.member;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.order.WxOrderPageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.order.WxOrderSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.WxOrderDO;

import jakarta.validation.Valid;
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
     * 已取消、已完成状态不可再取消；已支付（已出库）的取消会按原批次、原货位回补库存，
     * 重复取消不会重复回补。
     */
    void cancelWxOrder(Long id, String cancelReason);

    /**
     * 退款订单（幂等）：已支付未完成 → 已退款 + 取消
     *
     * 重复退款回调不重复处理；退款时按原批次、原货位回补库存。
     * 已完成订单的出库已转销售，需走 D 的销售退货流程，本接口拒绝。
     */
    void refundWxOrder(Long id, String refundReason);

    /**
     * 冻结订单库存（门店管理端节点，幂等）
     *
     * 下单后由门店节点调用：先按门店可售量粗校验，再按 FEFO 冻结库存并落库实际分配。
     * 同一订单重复调用只冻结一次；可用库存不足抛业务错误且不产生任何冻结。
     *
     * <p>冻结必须由具备「目标门店在职管理员」身份的调用方发起（C 的库存门禁要求），
     * 小程序端没有该身份，因此不在小程序下单时冻结。
     */
    void reserveWxOrder(Long id);

    /**
     * 会员取消订单（不做库存作业）
     *
     * 小程序端不具备库存作业身份，会员取消只关闭订单状态；
     * 已冻结或已出库的库存由门店节点释放（{@link #releaseFrozenStockOfClosedOrders}）或回补（退款节点）。
     */
    void cancelWxOrderByMember(Long id, String cancelReason);

    /**
     * 关闭门店下已超时未支付的订单（门店管理端节点，幂等）
     *
     * @param storeId 门店编号
     * @param limit   单次处理上限（为空取默认值）
     * @return 实际关闭的订单数
     */
    int closeExpiredWxOrders(Long storeId, Integer limit);

    /**
     * 释放已关闭订单仍冻结的库存（门店管理端节点，幂等）
     *
     * 只处理仍处于「已冻结」的分配；已出库的分配需走退款 / 退货回补，避免未退款先回补。
     *
     * @param storeId 门店编号
     * @param limit   单次处理上限（为空取默认值）
     * @return 实际释放的分配条数
     */
    int releaseFrozenStockOfClosedOrders(Long storeId, Integer limit);

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

    // ========== 小程序端（app）下单 ==========

    /**
     * 从购物车已勾选商品创建订单（小程序下单）
     *
     * 业务校验：购物车不能为空、药品必须可销售、处方药必须关联处方、同城配送必须有地址。
     * 下单成功后清空本次结算的购物车记录。
     *
     * @param memberId  会员编号（取自登录令牌）
     * @param storeId   履约门店编号
     * @param orderType 订单类型 0到店自提/1同城配送
     * @param addressId 收货地址编号（同城配送必填）
     * @param prescId   处方案编号（含处方药时必填）
     * @param remark    备注
     * @return 订单编号
     */
    Long createOrderFromCart(Long memberId, Long storeId, Integer orderType,
                             Long addressId, Long prescId, String remark);

}
