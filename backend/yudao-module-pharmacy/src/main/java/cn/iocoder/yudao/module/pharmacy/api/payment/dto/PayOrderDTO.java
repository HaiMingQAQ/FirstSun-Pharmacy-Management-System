package cn.iocoder.yudao.module.pharmacy.api.payment.dto;

import lombok.Data;

/**
 * 创建支付单参数（金额单位：分）
 */
@Data
public class PayOrderDTO {

    /** 业务单号（销售单号/线上单号） */
    private String bizNo;

    /** 金额，单位分 */
    private Integer priceFen;

    private Long memberId;

    private String channelCode;

    private String subject;

    private String notifyUrl;
}
