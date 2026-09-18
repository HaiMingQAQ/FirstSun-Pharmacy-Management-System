package cn.iocoder.yudao.module.pharmacy.api.member;

import cn.iocoder.yudao.module.pharmacy.api.member.dto.SalePointCalcDTO;
import cn.iocoder.yudao.module.pharmacy.service.member.MemberPointRecordService;
import cn.iocoder.yudao.module.pharmacy.service.member.MemberPointSettlementService;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.math.BigDecimal;

/**
 * 会员积分门面实现（F 成员提供）。
 *
 * <p>本 Bean 注册后，{@code FacadeFallbackConfiguration} 中的临时降级实现
 * （{@code MemberPointFacadeImpl}）会因 {@code @ConditionalOnMissingBean} 自动失效，
 * 销售奖励、积分抵扣与退货回退将走真实积分逻辑。
 *
 * <p>职责边界：本类只做「跨模块契约 → 会员积分服务」的适配，积分计算、幂等与流水
 * 由 {@link MemberPointSettlementService}（一站式结算）与
 * {@link MemberPointRecordService}（原子变动）实现；不直接操作其他模块的数据。
 */
@Service
public class MemberPointFacadeAdapter implements MemberPointFacade {

    @Resource
    private MemberPointRecordService memberPointRecordService;

    @Resource
    private MemberPointSettlementService memberPointSettlementService;

    @Override
    public SalePointCalcDTO calcSalePoints(Long memberId, BigDecimal orderAmount, Integer wantDeductPoints) {
        return memberPointSettlementService.calcSalePoints(memberId, orderAmount, wantDeductPoints);
    }

    @Override
    public int settleSalePoints(Long memberId, String orderNo, BigDecimal orderAmount, Integer deductPoints) {
        return memberPointSettlementService.settleSalePoints(memberId, orderNo, orderAmount, deductPoints);
    }

    @Override
    public void refundSalePoints(Long memberId, String orderNo, String returnNo, BigDecimal orderAmount,
                                 BigDecimal returnAmount, boolean fullReturn) {
        memberPointSettlementService.refundSalePoints(memberId, orderNo, returnNo, orderAmount, returnAmount,
                fullReturn);
    }

    @Override
    public void releaseSalePoints(Long memberId, String orderNo) {
        memberPointSettlementService.releaseSalePoints(memberId, orderNo);
    }

    @Override
    public void addPoints(Long memberId, String bizNo, Integer point, String title) {
        memberPointRecordService.addPoints(memberId, bizNo, point, title);
    }

    @Override
    public void backPoints(Long memberId, String bizNo, Integer point) {
        memberPointRecordService.backPoints(memberId, bizNo, point);
    }

    @Override
    public void deductPoints(Long memberId, String bizNo, Integer point, String title) {
        memberPointRecordService.deductPoints(memberId, bizNo, point, title);
    }

    @Override
    public void returnPoints(Long memberId, String bizNo, Integer point, String title) {
        memberPointRecordService.returnPoints(memberId, bizNo, point, title);
    }

}
