package cn.iocoder.yudao.module.pharmacy.api.payment;

import cn.iocoder.yudao.module.pharmacy.api.payment.dto.PayOrderDTO;

/**
 * 支付门面【临时降级实现】。
 * <p>
 * E 成员实现 {@link PaymentFacade} 后，本 Bean 因 @ConditionalOnMissingBean 自动失效；
 * 在此之前，POS 退货渠道退款会收到 UnsupportedOperationException，
 * 由 Service 层转换为 PAY_SERVICE_UNAVAILABLE，事务整体回滚。
 */
public class PaymentFacadeImpl implements PaymentFacade {

    @Override
    public String createPayOrder(PayOrderDTO req) {
        throw new UnsupportedOperationException("E 支付服务未实现，渠道支付不可用");
    }

    @Override
    public void refund(Long channelPayOrderId, String refundNo, Integer refundFen, String reason) {
        throw new UnsupportedOperationException("E 支付服务未实现，渠道退款不可用");
    }

    @Override
    public Integer queryStatus(Long channelPayOrderId) {
        throw new UnsupportedOperationException("E 支付服务未实现，支付查询不可用");
    }

}
