package cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.store;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 门店精简信息 VO，用于下拉选择
 */
@Schema(description = "管理后台 - 门店精简信息 Response VO")
@Data
public class StoreSimpleRespVO {

    @Schema(description = "门店编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "门店编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "S001")
    private String storeCode;

    @Schema(description = "门店名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "朝阳店")
    private String storeName;

}
