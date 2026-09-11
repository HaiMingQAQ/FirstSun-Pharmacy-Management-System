package cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.pointrecord;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 会员积分记录 Response VO")
@Data
@ExcelIgnoreUnannotated
public class MemberPointRecordRespVO {

    @Schema(description = "记录编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("记录编号")
    private Long id;

    @Schema(description = "用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("用户编号")
    private Long userId;

    @Schema(description = "业务编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "ORDER_001")
    @ExcelProperty("业务编码")
    private String bizId;

    @Schema(description = "业务类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("业务类型")
    private Integer bizType;

    @Schema(description = "积分标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "签到奖励")
    @ExcelProperty("积分标题")
    private String title;

    @Schema(description = "积分描述", example = "每日签到获得10积分")
    private String description;

    @Schema(description = "积分变动值", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @ExcelProperty("积分变动值")
    private Integer point;

    @Schema(description = "变动后的积分", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @ExcelProperty("变动后的积分")
    private Integer totalPoint;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}
