package cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.category;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 药品分类精简信息 VO
 *
 * 主要用于前端的下拉选择、树形选择等场景，仅返回启用状态。
 */
@Schema(description = "管理后台 - 药品分类精简信息 Response VO")
@Data
public class CategorySimpleRespVO {

    @Schema(description = "分类编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "分类名", requiredMode = Schema.RequiredMode.REQUIRED, example = "中成药")
    private String catName;

    @Schema(description = "分类编码", example = "YP")
    private String catCode;

    @Schema(description = "上级分类编号", example = "0")
    private Long parentId;

}
