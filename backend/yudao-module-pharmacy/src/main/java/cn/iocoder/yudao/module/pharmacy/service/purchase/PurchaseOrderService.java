package cn.iocoder.yudao.module.pharmacy.service.purchase;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.order.PurchaseOrderPageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.order.PurchaseOrderSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.purchase.PurchaseOrderDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.purchase.PurchaseOrderLineDO;

import jakarta.validation.Valid;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 采购订单 Service
 *
 * 金额一律由服务端按订单明细重算（totalAmount / discountAmount / payableAmount），
 * 不接受前端传入的金额字段。
 *
 * @author B 成员
 */
public interface PurchaseOrderService {

    /**
     * 创建采购订单（草稿）
     *
     * @return 订单编号
     */
    Long createOrder(@Valid PurchaseOrderSaveReqVO createReqVO);

    /**
     * 更新采购订单（仅草稿可改，明细整体重建）
     */
    void updateOrder(@Valid PurchaseOrderSaveReqVO updateReqVO);

    /**
     * 删除采购订单（仅草稿，且无收货记录）
     */
    void deleteOrder(Long id);

    /**
     * 获取采购订单
     */
    PurchaseOrderDO getOrder(Long id);

    /**
     * 批量获取采购订单（用于列表回填订单号）
     */
    List<PurchaseOrderDO> getOrderList(Collection<Long> ids);

    /**
     * 获取采购订单分页
     */
    PageResult<PurchaseOrderDO> getOrderPage(PurchaseOrderPageReqVO reqVO);

    /**
     * 获取订单明细
     */
    List<PurchaseOrderLineDO> getOrderLines(Long orderId);

    /**
     * 批量获取订单明细
     */
    List<PurchaseOrderLineDO> getOrderLinesByOrderIds(Collection<Long> orderIds);

    /**
     * 校验收货单可用的订单：存在且未取消
     */
    PurchaseOrderDO validateOrderExists(Long id);

    /**
     * 提交审批：草稿(0) → 已提交(1)
     */
    void submitOrder(Long id);

    /**
     * 审批通过：已提交(1) → 已审批(2)，记录审批人与时间
     */
    void approveOrder(Long id);

    /**
     * 标记已发出：已审批(2) → 已发出(3)
     */
    void issueOrder(Long id);

    /**
     * 取消订单：草稿/已提交/已审批 → 已取消(-1)，存在未作废收货单时拒绝
     */
    void cancelOrder(Long id);

    /**
     * 收货入账成功后累计订单行已收数量，并推进订单状态（部分到货/完成）
     *
     * 由 {@code PurchaseReceiptService#postReceipt} 在同一事务内调用，保证
     * 「收货单入账」与「订单已收数量」一致。
     *
     * @param orderId          采购订单编号
     * @param qtyByOrderLineId 订单行编号 → 本次入账数量
     */
    void applyReceiptPosted(Long orderId, Map<Long, Integer> qtyByOrderLineId);

}
