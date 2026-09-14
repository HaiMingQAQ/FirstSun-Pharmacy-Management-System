package cn.iocoder.yudao.module.pharmacy.controller.app.pharmacy.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "用户 APP - 药品分类 Response VO")
@Data
public class AppCategoryRespVO {

    @Schema(description = "分类编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "分类编码", example = "CAT001")
    private String catCode;

    @Schema(description = "分类名称", example = "抗感染类")
    private String catName;

    @Schema(description = "上级分类编号", example = "0")
    private Long parentId;

    @Schema(description = "分类类型", example = "0")
    private Integer catType;

    @Schema(description = "排序", example = "1")
    private Integer sort;

}
