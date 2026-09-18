package cn.iocoder.yudao.module.pharmacy.api.payment;

import cn.iocoder.yudao.module.pharmacy.api.payment.dto.PayOrderDTO;

/**
 * 统一支付门面（E 实现）。
 * <p>
 * 金额一律为分（Integer）。业务单号由调用方（D）生成，payNo 作为幂等键。
 * <p>
 * 幂等与金额约定：
 * <ul>
 *   <li>createPayOrder：同 {@code bizNo}（merchantOrderId）重复调用由支付模块幂等返回原支付单，不重复扣款；金额必须为正。</li>
 *   <li>refund：{@code refundNo} 为幂等键，同号重复请求幂等放行，不重复退款；退款金额必须为正且不超过原支付金额（支持部分退款）；
 *       现金退货传 {@code channelPayOrderId=null}，门面直接放行不发起渠道退款。</li>
 *   <li>queryStatus：状态同步入口，返回 0 未支付 / 1 成功 / 2 失败 / 3 已退款；渠道异步回调由支付模块 notify 接口接收并幂等更新。</li>
 * </ul>
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
