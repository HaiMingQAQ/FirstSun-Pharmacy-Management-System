package cn.iocoder.yudao.module.pharmacy.api.member;

import cn.iocoder.yudao.module.pharmacy.service.member.MemberPointRecordService;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

/**
 * 会员积分门面实现（F 成员提供）。
 *
 * <p>本 Bean 注册后，{@code FacadeFallbackConfiguration} 中的临时降级实现
 * （{@code MemberPointFacadeImpl}）会因 {@code @ConditionalOnMissingBean} 自动失效，
 * 销售奖励与退货积分回退将走真实积分逻辑。
 *
 * <p>职责边界：本类只做「跨模块契约 → 会员积分服务」的适配，积分变动、幂等与流水
 * 由 {@link MemberPointRecordService} 实现；不直接操作其他模块的数据。
 *
 * <p>幂等约定：{@code bizNo} 作为业务编码写入积分流水，同一会员 + 同一业务编码
 * 只会处理一次，重复调用（如支付/退货重复触发）不会重复加减积分。
 */
@Service
public class MemberPointFacadeAdapter implements MemberPointFacade {

    @Resource
    private MemberPointRecordService memberPointRecordService;

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
