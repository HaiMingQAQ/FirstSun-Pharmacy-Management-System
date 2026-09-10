package cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.store;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 门店分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class StorePageReqVO extends PageParam {

    @Schema(description = "门店编码，模糊匹配", example = "S001")
    private String storeCode;

    @Schema(description = "门店名称，模糊匹配", example = "朝阳")
    private String storeName;

    @Schema(description = "是否医保定点 0否/1是", example = "1")
    private Integer isMedical;

    @Schema(description = "营业状态 1营业/0停业", example = "1")
    private Integer status;

    @Schema(description = "关联部门编号", example = "100")
    private Long deptId;

}
