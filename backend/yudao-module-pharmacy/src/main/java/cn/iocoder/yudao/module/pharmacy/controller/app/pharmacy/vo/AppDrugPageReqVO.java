package cn.iocoder.yudao.module.pharmacy.controller.app.pharmacy.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "用户 APP - 药店商品分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class AppDrugPageReqVO extends PageParam {

    @Schema(description = "药品分类编号", example = "1")
    private Long categoryId;

    @Schema(description = "关键字（匹配通用名/商品名/拼音码）", example = "阿莫西林")
    private String keyword;

}
