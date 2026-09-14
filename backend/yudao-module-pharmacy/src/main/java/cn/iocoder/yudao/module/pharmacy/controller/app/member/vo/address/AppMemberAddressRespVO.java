package cn.iocoder.yudao.module.pharmacy.controller.app.member.vo.address;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "用户 APP - 会员收件地址 Response VO")
@Data
public class AppMemberAddressRespVO {

    @Schema(description = "收件地址编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "收件人名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    private String name;

    @Schema(description = "手机号", requiredMode = Schema.RequiredMode.REQUIRED, example = "13800138000")
    private String mobile;

    @Schema(description = "地区编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "110101")
    private Long areaId;

    @Schema(description = "收件详细地址", requiredMode = Schema.RequiredMode.REQUIRED, example = "朝阳区某某街道123号")
    private String detailAddress;

    @Schema(description = "是否默认", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean defaultStatus;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
