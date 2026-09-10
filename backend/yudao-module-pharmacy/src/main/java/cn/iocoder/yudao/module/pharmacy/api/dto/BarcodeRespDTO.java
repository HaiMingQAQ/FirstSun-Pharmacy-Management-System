package cn.iocoder.yudao.module.pharmacy.api.dto;

import lombok.Data;

/**
 * 药品条码跨模块 Response DTO
 *
 * 仅供其他模块（销售、库存等）只读使用。
 *
 * @author A 成员
 */
@Data
public class BarcodeRespDTO {

    /**
     * 条码编号
     */
    private Long id;
    /**
     * 药品编号
     */
    private Long drugId;
    /**
     * 条码
     */
    private String barcode;
    /**
     * 条码类型 0商品条码/1店内码/2追溯码
     */
    private Integer barcodeType;
    /**
     * 是否默认扫码码 0否/1是
     */
    private Integer isDefault;

}
