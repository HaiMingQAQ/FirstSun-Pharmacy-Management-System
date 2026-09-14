package cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.category;

import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.pharmacy.enums.PharmacyStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "管理后台 - 药品分类创建/修改 Request VO")
@Data
public class CategorySaveReqVO {

    @Schema(description = "分类编号", example = "1024")
    private Long id;

    @Schema(description = "分类编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "YP")
    @NotBlank(message = "分类编码不能为空")
    @Size(max = 16, message = "分类编码长度不能超过 16 个字符")
    private String catCode;

    @Schema(description = "分类名", requiredMode = Schema.RequiredMode.REQUIRED, example = "中成药")
    @NotBlank(message = "分类名不能为空")
    @Size(max = 32, message = "分类名长度不能超过 32 个字符")
    private String catName;

    @Schema(description = "上级分类编号，0 或空表示顶级", example = "0")
    private Long parentId;

    @Schema(description = "分类类型 0药品/1保健品/2医疗器械/3中药饮片/4日化/5其他", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "分类类型不能为空")
    private Integer catType;

    @Schema(description = "排序", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "排序不能为空")
    private Integer sort;

    @Schema(description = "状态 1启用/0停用", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @InEnum(PharmacyStatusEnum.class)
    private Integer status;

}
