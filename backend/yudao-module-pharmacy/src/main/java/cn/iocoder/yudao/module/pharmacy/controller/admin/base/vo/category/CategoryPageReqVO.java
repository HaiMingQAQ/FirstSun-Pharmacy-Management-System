package cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.category;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 药品分类分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class CategoryPageReqVO extends PageParam {

    @Schema(description = "分类编码，模糊匹配", example = "YP")
    private String catCode;

    @Schema(description = "分类名，模糊匹配", example = "中成药")
    private String catName;

    @Schema(description = "分类类型 0药品/1保健品/2医疗器械/3中药饮片/4日化/5其他", example = "0")
    private Integer catType;

    @Schema(description = "状态 1启用/0停用", example = "1")
    private Integer status;

    @Schema(description = "上级分类编号", example = "0")
    private Long parentId;

}
