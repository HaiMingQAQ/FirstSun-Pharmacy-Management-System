package cn.iocoder.yudao.module.pharmacy.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 采购单号序列的单据类型（对应 {@code ph_po_doc_seq.biz_type}）
 *
 * <p>与 {@code ph_po_doc_seq} 的唯一键 {@code (store_id, biz_date, biz_type)} 配合，
 * 让采购订单与采购收货单各自维护独立的流水序列。
 *
 * @author B 成员
 */
@Getter
@AllArgsConstructor
public enum PurchaseDocSeqTypeEnum {

    /**
     * 采购订单：PO&lt;门店&gt;-&lt;yyyyMMdd&gt;-&lt;流水&gt;
     */
    ORDER("ORDER", "采购订单"),
    /**
     * 采购收货单：GR&lt;门店&gt;-&lt;yyyyMMdd&gt;-&lt;流水&gt;
     */
    RECEIPT("RECEIPT", "采购收货单");

    /**
     * 类型值（落库）
     */
    private final String type;
    /**
     * 类型名
     */
    private final String name;

}
