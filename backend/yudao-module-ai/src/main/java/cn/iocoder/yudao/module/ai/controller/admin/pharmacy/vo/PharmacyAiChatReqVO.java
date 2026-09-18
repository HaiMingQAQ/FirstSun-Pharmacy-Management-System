package cn.iocoder.yudao.module.ai.controller.admin.pharmacy.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PharmacyAiChatReqVO {
    private Long conversationId;
    @NotBlank @Size(max = 64)
    private String clientMessageId;
    @NotBlank @Size(max = 2000)
    private String content;
}
