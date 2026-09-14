package cn.iocoder.yudao.module.pharmacy.controller.app.member.vo.point;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "用户 APP - 会员积分记录 Response VO")
@Data
public class AppMemberPointRecordRespVO {

    @Schema(description = "记录编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "业务编码", example = "ORDER_001")
    private String bizId;

    @Schema(description = "业务类型", example = "2")
    private Integer bizType;

    @Schema(description = "积分标题", example = "消费奖励")
    private String title;

    @Schema(description = "积分描述", example = "订单消费获得 10 积分")
    private String description;

    @Schema(description = "积分变动值（正增负减）", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Integer point;

    @Schema(description = "变动后积分", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Integer totalPoint;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
