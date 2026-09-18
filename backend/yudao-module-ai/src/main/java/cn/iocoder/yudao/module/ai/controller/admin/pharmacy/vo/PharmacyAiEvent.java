package cn.iocoder.yudao.module.ai.controller.admin.pharmacy.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PharmacyAiEvent {
    private String type;
    private Object data;
}
