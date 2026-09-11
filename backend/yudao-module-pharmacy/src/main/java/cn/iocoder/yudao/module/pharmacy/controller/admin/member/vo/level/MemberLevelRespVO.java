package cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.level;

import cn.iocoder.yudao.framework.excel.core.annotations.DictFormat;
import cn.iocoder.yudao.framework.excel.core.convert.DictConvert;
import cn.iocoder.yudao.module.pharmacy.enums.DictTypeConstants;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 会员等级 Response VO")
@Data
@ExcelIgnoreUnannotated
public class MemberLevelRespVO {

    @Schema(description = "等级编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("等级编号")
    private Long id;

    @Schema(description = "等级名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "黄金会员")
    @ExcelProperty("等级名称")
    private String name;

    @Schema(description = "等级", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @ExcelProperty("等级")
    private Integer level;

    @Schema(description = "升级经验", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000")
    @ExcelProperty("升级经验")
    private Integer experience;

    @Schema(description = "享受折扣(%)", requiredMode = Schema.RequiredMode.REQUIRED, example = "90")
    @ExcelProperty("享受折扣(%)")
    private Integer discountPercent;

    @Schema(description = "等级图标", example = "https://www.iocoder.cn/icon.png")
    private String icon;

    @Schema(description = "等级背景图", example = "https://www.iocoder.cn/bg.png")
    private String backgroundUrl;

    @Schema(description = "状态 0启用/1禁用", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @ExcelProperty(value = "状态", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.PHARMACY_MEMBER_LEVEL_STATUS)
    private Integer status;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}
