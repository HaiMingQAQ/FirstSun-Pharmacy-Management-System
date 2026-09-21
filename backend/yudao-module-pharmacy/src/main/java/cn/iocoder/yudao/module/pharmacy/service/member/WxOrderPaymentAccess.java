package cn.iocoder.yudao.module.pharmacy.service.member;

import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.pay.dal.dataobject.order.PayOrderDO;
import cn.iocoder.yudao.module.pay.enums.order.PayOrderStatusEnum;
import cn.iocoder.yudao.module.pay.service.app.PayAppService;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.WxOrderDO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.member.WxOrderPaymentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.*;

/** Internal authorization based on locked, persisted business/payment records, never a client success flag. */
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
public class WxOrderPaymentAccess {
    private final WxOrderPaymentMapper mapper;
    private final PayAppService appService;
    @Value("${firstsun.miniapp.pay-app-key:firstsun}")
    private String appKey;

    public String getAppKey() { return appKey; }

    public static Long tenantId() {
        Long tenant = TenantContextHolder.getTenantId();
        if (tenant == null || tenant < 0 || TenantContextHolder.isIgnore()) {
            throw new AccessDeniedException("支付库存作业需要有效租户上下文");
        }
        return tenant;
    }

    public WxOrderDO lockOrder(Long id) {
        WxOrderDO order = mapper.lockOrder(id, tenantId());
        if (order == null) throw exception(PHARMACY_WX_ORDER_NOT_EXISTS);
        return order;
    }

    public static int amountFen(WxOrderDO order) {
        try {
            int amount = order.getPayableAmount().movePointRight(2).intValueExact();
            if (amount > 0) return amount;
        } catch (ArithmeticException | NullPointerException ignored) { }
        throw exception(PAY_AMOUNT_INVALID);
    }

    public PayOrderDO lockPayment(WxOrderDO order, Long paymentId) {
        PayOrderDO payment = mapper.lockPayment(paymentId, tenantId());
        if (payment == null
                || !Objects.equals(payment.getAppId(), appService.validPayApp(appKey).getId())
                || !Objects.equals(payment.getMerchantOrderId(), order.getOrderNo())
                || !Objects.equals(payment.getUserId(), order.getMemberId())
                || !Objects.equals(payment.getUserType(), UserTypeEnum.MEMBER.getValue())
                || !Objects.equals(payment.getPrice(), amountFen(order))) {
            throw exception(PAY_AMOUNT_INVALID);
        }
        return payment;
    }

    public WxOrderDO requirePaidOrder(Long orderId, Long paymentId) {
        WxOrderDO order = lockOrder(orderId);
        if (paymentId == null || !Objects.equals(order.getPayNo(), paymentId.toString())
                || !Objects.equals(order.getPayStatus(), 1) || !Objects.equals(order.getStatus(), 1)
                || order.getStoreId() == null || order.getStoreId() <= 0) {
            throw exception(PHARMACY_WX_ORDER_STATUS_FLOW_ERROR);
        }
        PayOrderDO payment = lockPayment(order, paymentId);
        if (!Objects.equals(payment.getStatus(), PayOrderStatusEnum.SUCCESS.getStatus())
                || payment.getSuccessTime() == null || payment.getRefundPrice() == null
                || payment.getRefundPrice() != 0) {
            throw exception(PHARMACY_WX_ORDER_STATUS_FLOW_ERROR);
        }
        return order;
    }
}
