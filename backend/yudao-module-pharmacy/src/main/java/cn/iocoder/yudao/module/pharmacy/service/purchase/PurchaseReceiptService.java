package cn.iocoder.yudao.module.pharmacy.service.purchase;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.receipt.PurchaseReceiptPageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.receipt.PurchaseReceiptSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.purchase.PurchaseReceiptDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.purchase.PurchaseReceiptLineDO;

import jakarta.validation.Valid;
import java.util.Collection;
import java.util.List;

/**
 * 采购收货 Service
 *
 * 入账（postReceipt）在同一事务内完成三件事：
 * 1）收货单状态 CAS 置为「已入账」，重复调用直接拒绝；
 * 2）调用 C 的库存服务写入批次/货位库存/流水，失败整体回滚；
 * 3）累计采购订单行已收数量并推进订单状态。
 *
 * @author B 成员
 */
public interface PurchaseReceiptService {

    /**
     * 创建收货单（待提交）
     *
     * @return 收货单编号
     */
    Long createReceipt(@Valid PurchaseReceiptSaveReqVO createReqVO);

    /**
     * 更新收货单（仅待提交可改，明细整体重建）
     */
    void updateReceipt(@Valid PurchaseReceiptSaveReqVO updateReqVO);

    /**
     * 删除收货单（仅待提交或已作废）
     */
    void deleteReceipt(Long id);

    /**
     * 获取收货单
     */
    PurchaseReceiptDO getReceipt(Long id);

    /**
     * 获取收货单分页
     */
    PageResult<PurchaseReceiptDO> getReceiptPage(PurchaseReceiptPageReqVO reqVO);

    /**
     * 获取收货明细
     */
    List<PurchaseReceiptLineDO> getReceiptLines(Long receiptId);

    /**
     * 批量获取收货明细
     */
    List<PurchaseReceiptLineDO> getReceiptLinesByReceiptIds(Collection<Long> receiptIds);

    /**
     * 校验收货单存在
     */
    PurchaseReceiptDO validateReceiptExists(Long id);

    /**
     * 提交收货单：待提交(0) → 已提交(1)
     */
    void submitReceipt(Long id);

    /**
     * 收货入账：已提交(1) → 已入账(2)，调用库存服务并累计订单已收数量
     */
    void postReceipt(Long id);

    /**
     * 作废收货单：待提交(0) → 已作废(3)；已入账不可作废
     */
    void voidReceipt(Long id);

}
