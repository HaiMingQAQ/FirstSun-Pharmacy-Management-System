package cn.iocoder.yudao.module.pharmacy.dal.dataobject.purchase;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.module.pharmacy.enums.PurchaseDocSeqTypeEnum;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 采购单号序列 DO（B 模块并发取号）
 *
 * <p>对应表 {@code ph_po_doc_seq}，为 {@link PurchaseDocSeqTypeEnum} 中每种单据、
 * 每个「门店 + 业务日」维护一条独立且唯一的流水序列。
 *
 * <p>引入原因：原先用 {@code SELECT MAX(单号) + 1} 取号，在 REPEATABLE-READ 下
 * 同一事务内重试读到的 MAX 恒定不变，且并发插入会争抢唯一键索引锁产生死锁。
 * 详见迁移脚本 {@code 20260917_b_purchase_doc_seq.sql}。
 *
 * @author B 成员
 */
@TableName("ph_po_doc_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class PurchaseDocSeqDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 门店编号
     */
    private Long storeId;
    /**
     * 业务日期（单号中的 yyyyMMdd）
     */
    private LocalDate bizDate;
    /**
     * 单据类型：ORDER=采购订单、RECEIPT=采购收货单
     */
    private String bizType;
    /**
     * 已分配到的最大流水号
     */
    private Integer nextSeq;

}
