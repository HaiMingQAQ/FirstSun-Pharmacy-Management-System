package cn.iocoder.yudao.module.pharmacy.dal.dataobject.base;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 药品条码 DO
 *
 * 对应数据表 {@code ph_drug_barcode}，字段以 {@code sql/firstsun_pharmacy_init.sql} 真实定义为准。
 *
 * 唯一约束：
 * 1. {@code uk_barcode(barcode)} - 条码全局唯一
 *
 * 关联：
 * - {@link #drugId} → ph_drug.id（通过 DrugService 只读校验）
 *
 * 业务规则：
 * - {@link #isDefault} 同一药品下只能有一个默认条码（=1 时唯一）
 */
@TableName("ph_drug_barcode")
@KeySequence("ph_drug_barcode_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class BarcodeDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 药品编号（ph_drug.id）
     */
    private Long drugId;
    /**
     * 条码（EAN-13/店内码/追溯码）
     */
    private String barcode;
    /**
     * 条码类型 0商品条码/1店内码/2追溯码
     */
    private Integer barcodeType;
    /**
     * 默认扫码码 0否/1是（同药品下唯一）
     */
    private Integer isDefault;

}
