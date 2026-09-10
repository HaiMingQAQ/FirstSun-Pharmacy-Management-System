package cn.iocoder.yudao.module.pharmacy.api.payment;

import cn.iocoder.yudao.module.pharmacy.api.payment.dto.PayOrderDTO;

/**
 * 统一支付门面（E 实现）。
 * <p>
 * 金额一律为分（Integer）。业务单号由调用方（D）生成，payNo 作为幂等键。
 */
public interface PaymentFacade {

    /**
     * 创建渠道支付单，返回支付单号
     */
    String createPayOrder(PayOrderDTO req);

    /**
     * 渠道退款，refundNo 为幂等键
     */
    void refund(Long channelPayOrderId, String refundNo, Integer refundFen, String reason);

    /**
     * 查询支付状态：0 未支付 / 1 成功 / 2 失败 / 3 已退款
     */
    Integer queryStatus(Long channelPayOrderId);
}
