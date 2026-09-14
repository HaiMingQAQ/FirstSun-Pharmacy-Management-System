package cn.iocoder.yudao.module.pharmacy.controller.admin.prescription.vo;

import cn.iocoder.yudao.module.pharmacy.dal.dataobject.prescription.PhPrescRecordDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 处方记录 Response VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class PrescRecordRespVO extends PhPrescRecordDO {

    @Schema(description = "门店名称（冗余展示）", example = "FirstSun 旗舰店")
    private String storeName;

    @Schema(description = "审方药师姓名", example = "王药师")
    private String pharmacistName;

    @Schema(description = "双人复核人姓名", example = "赵药师")
    private String dblCheckByName;
}
