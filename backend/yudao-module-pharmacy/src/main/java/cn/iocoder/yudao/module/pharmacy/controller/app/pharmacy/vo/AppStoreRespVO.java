package cn.iocoder.yudao.module.pharmacy.controller.app.pharmacy.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "用户 APP - 门店 Response VO")
@Data
public class AppStoreRespVO {

    @Schema(description = "门店编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "门店编码", example = "ST001")
    private String storeCode;

    @Schema(description = "门店名称", example = "FirstSun 旗舰店")
    private String storeName;

    @Schema(description = "门店地址", example = "某某路 1 号")
    private String address;

    @Schema(description = "联系电话", example = "010-00000000")
    private String phone;

    @Schema(description = "营业时间", example = "08:00-22:00")
    private String businessHours;

}
