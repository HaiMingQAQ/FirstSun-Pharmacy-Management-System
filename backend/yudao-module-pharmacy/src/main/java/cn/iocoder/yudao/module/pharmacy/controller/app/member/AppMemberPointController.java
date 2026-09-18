package cn.iocoder.yudao.module.pharmacy.controller.app.member;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.pharmacy.api.member.dto.MemberPointRuleDTO;
import cn.iocoder.yudao.module.pharmacy.api.member.dto.SalePointCalcDTO;
import cn.iocoder.yudao.module.pharmacy.controller.app.member.vo.point.AppMemberPointDeductPreviewReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.app.member.vo.point.AppMemberPointSummaryRespVO;
import cn.iocoder.yudao.module.pharmacy.service.member.MemberPointSettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/**
 * 用户 APP - 会员积分中心（积分余额、规则、抵扣试算）。
 *
 * <p>本控制器是「积分抵扣」的实际可用入口：小程序在结算页调用
 * {@code /member/point/deduct-preview} 获取本单最多可用积分与抵扣金额，
 * 再把希望使用的积分通过 {@code /member/wx-order/create} 的 usePoints 传给后端；
 * 具体可用值、抵扣金额与赠送积分一律由后端积分结算服务计算。
 *
 * <p>会员编号全部取自登录令牌，禁止查询 / 操作他人积分。
 */
@Tag(name = "用户 APP - 会员积分中心")
@RestController
@RequestMapping("/member/point")
@Validated
@Slf4j
public class AppMemberPointController {

    @Resource
    private MemberPointSettlementService memberPointSettlementService;

    @GetMapping("/summary")
    @Operation(summary = "获得本人积分摘要（余额、等级倍率与当前生效的积分规则）")
    public CommonResult<AppMemberPointSummaryRespVO> getSummary() {
        MemberPointRuleDTO summary = memberPointSettlementService.getRuleSummary(getLoginUserId());
        return success(BeanUtils.toBean(summary, AppMemberPointSummaryRespVO.class));
    }

    @PostMapping("/deduct-preview")
    @Operation(summary = "积分抵扣试算",
            description = "未传 usePoints 时返回本单最多可用积分；传了 usePoints 时校验余额、抵扣比例、"
                    + "单笔上限与订单金额，校验不通过返回明确的业务错误")
    public CommonResult<SalePointCalcDTO> deductPreview(
            @RequestBody @Valid AppMemberPointDeductPreviewReqVO reqVO) {
        Long memberId = getLoginUserId();
        Integer usePoints = reqVO.getUsePoints();
        if (usePoints == null || usePoints <= 0) {
            // 未指定使用积分：返回本单最多可用积分，便于结算页展示「最多可抵 X 元」
            SalePointCalcDTO result = memberPointSettlementService.calcSalePoints(memberId, reqVO.getOrderAmount(), 0);
            Integer maxPoints = result.getMaxDeductPoints() == null ? 0 : result.getMaxDeductPoints();
            result.setDeductPoints(maxPoints);
            result.setDeductAmount(memberPointSettlementService.pointsToAmount(maxPoints));
            return success(result);
        }
        return success(memberPointSettlementService.calcSalePoints(memberId, reqVO.getOrderAmount(), usePoints));
    }

}
