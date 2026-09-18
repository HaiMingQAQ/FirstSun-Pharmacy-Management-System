package cn.iocoder.yudao.module.ai.controller.admin.pharmacy.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PharmacyAiConfirmReqVO {
    @NotBlank
    private String confirmationToken;
}
