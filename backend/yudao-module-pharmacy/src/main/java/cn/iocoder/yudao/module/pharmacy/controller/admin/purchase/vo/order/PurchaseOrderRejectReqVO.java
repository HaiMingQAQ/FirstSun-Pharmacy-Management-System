package cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 采购订单驳回 Request VO（B-2）
 *
 * <p>驳回原因必填：既在 Controller 侧由 {@code @NotBlank} 拦截，服务层也会再校验一次
 * （防止绕过 Controller 的内部调用）。
 *
 * @author B 成员
 */
@Schema(description = "管理后台 - 采购订单驳回 Request VO")
@Data
public class PurchaseOrderRejectReqVO {

    @Schema(description = "订单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "订单编号不能为空")
    private Long id;

    @Schema(description = "驳回原因", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "供应商资质即将到期，请更换供应商后重新提交")
    @NotBlank(message = "驳回原因不能为空")
    @Size(max = 500, message = "驳回原因长度不能超过 500 个字符")
    private String rejectReason;

}
