package cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.barcode;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 药品条码分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class BarcodePageReqVO extends PageParam {

    @Schema(description = "药品编号", example = "1024")
    private Long drugId;

    @Schema(description = "条码，模糊匹配", example = "6901234")
    private String barcode;

    @Schema(description = "条码类型 0商品条码/1店内码/2追溯码", example = "0")
    private Integer barcodeType;

    @Schema(description = "是否默认 0否/1是", example = "1")
    private Integer isDefault;

}
