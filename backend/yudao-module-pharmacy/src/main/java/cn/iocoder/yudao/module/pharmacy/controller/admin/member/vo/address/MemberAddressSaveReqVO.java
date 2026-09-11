package cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.address;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Schema(description = "管理后台 - 会员收件地址创建/修改 Request VO")
@Data
public class MemberAddressSaveReqVO {

    @Schema(description = "收件地址编号", example = "1024")
    private Long id;

    @Schema(description = "用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "用户编号不能为空")
    private Long userId;

    @Schema(description = "收件人名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    @NotBlank(message = "收件人名称不能为空")
    @Size(max = 10, message = "收件人名称长度不能超过 10 个字符")
    private String name;

    @Schema(description = "手机号", requiredMode = Schema.RequiredMode.REQUIRED, example = "13800138000")
    @NotBlank(message = "手机号不能为空")
    @Size(max = 20, message = "手机号长度不能超过 20 个字符")
    private String mobile;

    @Schema(description = "地区编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "110101")
    @NotNull(message = "地区编码不能为空")
    private Long areaId;

    @Schema(description = "收件详细地址", requiredMode = Schema.RequiredMode.REQUIRED, example = "朝阳区某某街道123号")
    @NotBlank(message = "收件详细地址不能为空")
    @Size(max = 250, message = "收件详细地址长度不能超过 250 个字符")
    private String detailAddress;

    @Schema(description = "是否默认", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    @NotNull(message = "是否默认不能为空")
    private Boolean defaultStatus;

}
