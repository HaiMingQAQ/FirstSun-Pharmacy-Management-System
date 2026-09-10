package cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.category;

import cn.iocoder.yudao.framework.excel.core.annotations.DictFormat;
import cn.iocoder.yudao.framework.excel.core.convert.DictConvert;
import cn.iocoder.yudao.module.pharmacy.enums.DictTypeConstants;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 药品分类 Response VO")
@Data
@ExcelIgnoreUnannotated
public class CategoryRespVO {

    @Schema(description = "分类编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("分类编号")
    private Long id;

    @Schema(description = "分类编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "YP")
    @ExcelProperty("分类编码")
    private String catCode;

    @Schema(description = "分类名", requiredMode = Schema.RequiredMode.REQUIRED, example = "中成药")
    @ExcelProperty("分类名")
    private String catName;

    @Schema(description = "上级分类编号", example = "0")
    @ExcelProperty("上级分类编号")
    private Long parentId;

    @Schema(description = "分类类型 0药品/1保健品/2医疗器械/3中药饮片/4日化/5其他", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @ExcelProperty(value = "分类类型", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.PHARMACY_CATEGORY_TYPE)
    private Integer catType;

    @Schema(description = "排序", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @ExcelProperty("排序")
    private Integer sort;

    @Schema(description = "状态 1启用/0停用", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty(value = "状态", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.PHARMACY_STATUS)
    private Integer status;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}
