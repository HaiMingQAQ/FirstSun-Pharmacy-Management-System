package cn.iocoder.yudao.module.pharmacy.controller.app.pharmacy.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "用户 APP - 药品可售库存 Response VO")
@Data
public class AppInventoryAvailableRespVO {

    @Schema(description = "药品编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long drugId;

    @Schema(description = "门店可售数量（已扣冻结，效期/质量由门店拣货时权威校验）", requiredMode = Schema.RequiredMode.REQUIRED, example = "12")
    private Integer qtyAvail;

}
