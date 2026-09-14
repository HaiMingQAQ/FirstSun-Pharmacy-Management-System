package cn.iocoder.yudao.module.pharmacy.api.payment;

import cn.iocoder.yudao.module.pay.api.order.PayOrderApi;
import cn.iocoder.yudao.module.pay.api.order.dto.PayOrderCreateReqDTO;
import cn.iocoder.yudao.module.pay.api.order.dto.PayOrderRespDTO;
import cn.iocoder.yudao.module.pay.api.refund.PayRefundApi;
import cn.iocoder.yudao.module.pay.api.refund.dto.PayRefundCreateReqDTO;
import cn.iocoder.yudao.module.pay.dal.dataobject.app.PayAppDO;
import cn.iocoder.yudao.module.pay.enums.order.PayOrderStatusEnum;
import cn.iocoder.yudao.module.pay.service.app.PayAppService;
import cn.iocoder.yudao.module.pharmacy.api.payment.dto.PayOrderDTO;
import cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;

/**
 * 统一支付门面适配实现（E 成员提供）。
 *
 * <p>本 Bean 注册后，{@code FacadeFallbackConfiguration} 中的临时降级实现
 * （{@code PaymentFacadeImpl}）会因 {@code @ConditionalOnMissingBean} 自动失效，
 * POS 渠道退款与线上订单支付将走真实支付模块（yudao-module-pay）。
 *
 * <p>职责边界：本类只做「跨模块契约 → 支付模块」的适配：
 * <ul>
 *   <li>createPayOrder：两段式第一段（创建支付单），返回支付单编号；金额单位分。</li>
 *   <li>refund：渠道退款；{@code channelPayOrderId} 为 null 时视为现金销售退货，
 *       无需真实渠道退款，直接放行（状态层面已由调用方处理）。</li>
 *   <li>queryStatus：支付单状态映射为 0 未付 / 1 成功 / 2 失败 / 3 已退款。</li>
 * </ul>
 *
 * <p>幂等约定：业务单号（bizNo/merchantOrderId）与退款号（refundNo/merchantRefundId）
 * 由调用方生成，支付模块侧保证同单号不重复创建。
 */
@Service
@Slf4j
public class PaymentFacadeAdapter implements PaymentFacade {

    /** 默认支付应用 key（对应 SQL 迁移中初始化的 pay_app） */
    private static final String DEFAULT_APP_KEY = "firstsun";

    @Resource
    private PayOrderApi payOrderApi;
    @Resource
    private PayRefundApi payRefundApi;
    @Resource
    private PayAppService payAppService;

    @Override
    public String createPayOrder(PayOrderDTO req) {
        String appKey = resolveAppKey();
        PayOrderCreateReqDTO createReq = new PayOrderCreateReqDTO();
        createReq.setAppKey(appKey);
        createReq.setUserIp("127.0.0.1");
        createReq.setMerchantOrderId(req.getBizNo());
        createReq.setSubject(req.getSubject());
        createReq.setBody(req.getSubject());
        createReq.setPrice(req.getPriceFen());
        createReq.setExpireTime(LocalDateTime.now().plusMinutes(30));
        try {
            Long orderId = payOrderApi.createOrder(createReq);
            log.info("[createPayOrder] 创建支付单成功, bizNo={}, priceFen={}, payOrderId={}, appKey={}",
                    req.getBizNo(), req.getPriceFen(), orderId, appKey);
            return String.valueOf(orderId);
        } catch (Exception ex) {
            log.error("[createPayOrder] 创建支付单失败, bizNo={}, appKey={}", req.getBizNo(), appKey, ex);
            throw exception(ErrorCodeConstants.PAY_ORDER_CREATE_FAIL);
        }
    }

    @Override
    public void refund(Long channelPayOrderId, String refundNo, Integer refundFen, String reason) {
        // 现金销售退货：无渠道支付单，无需真实渠道退款（状态层面已由调用方完成）
        if (channelPayOrderId == null) {
            log.info("[refund] 无渠道支付单（现金单），跳过渠道退款, refundNo={}, refundFen={}", refundNo, refundFen);
            return;
        }
        // 1. 查原支付单，取商户订单号
        PayOrderRespDTO order = payOrderApi.getOrder(channelPayOrderId);
        if (order == null) {
            log.error("[refund] 原支付单不存在, channelPayOrderId={}", channelPayOrderId);
            throw exception(ErrorCodeConstants.PAY_REFUND_CREATE_FAIL);
        }
        // 2. 创建退款单
        PayRefundCreateReqDTO refundReq = new PayRefundCreateReqDTO();
        refundReq.setAppKey(resolveAppKey());
        refundReq.setUserIp("127.0.0.1");
        refundReq.setMerchantOrderId(order.getMerchantOrderId());
        refundReq.setMerchantRefundId(refundNo);
        refundReq.setReason(reason == null ? "药店销售退货" : reason);
        refundReq.setPrice(refundFen);
        try {
            Long refundId = payRefundApi.createRefund(refundReq);
            log.info("[refund] 创建退款单成功, channelPayOrderId={}, refundNo={}, refundId={}",
                    channelPayOrderId, refundNo, refundId);
        } catch (Exception ex) {
            log.error("[refund] 创建退款单失败, channelPayOrderId={}, refundNo={}", channelPayOrderId, refundNo, ex);
            throw exception(ErrorCodeConstants.PAY_REFUND_CREATE_FAIL);
        }
    }

    @Override
    public Integer queryStatus(Long channelPayOrderId) {
        PayOrderRespDTO order = payOrderApi.getOrder(channelPayOrderId);
        if (order == null) {
            throw exception(ErrorCodeConstants.PAY_STATUS_UNKNOWN);
        }
        Integer status = order.getStatus();
        // 0 未付 / 1 成功 / 2 失败(关闭) / 3 已退款
        if (PayOrderStatusEnum.isWaiting(status)) {
            return 0;
        }
        if (Objects.equals(PayOrderStatusEnum.SUCCESS.getStatus(), status)) {
            return 1;
        }
        if (Objects.equals(PayOrderStatusEnum.REFUND.getStatus(), status)) {
            return 3;
        }
        return 2; // CLOSED 及其它视为失败
    }

    /**
     * 解析支付应用 key：优先取已启用的第一个应用，否则使用默认 key（SQL 初始化）
     */
    private String resolveAppKey() {
        List<PayAppDO> apps = payAppService.getAppList();
        if (apps != null && !apps.isEmpty()) {
            return apps.get(0).getAppKey();
        }
        return DEFAULT_APP_KEY;
    }
}
