package cn.iocoder.yudao.module.pharmacy.controller.app.member;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.order.WxOrderPageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.app.member.vo.order.AppWxOrderCreateReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.app.member.vo.order.AppWxOrderLineRespVO;
import cn.iocoder.yudao.module.pharmacy.controller.app.member.vo.order.AppWxOrderPageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.app.member.vo.order.AppWxOrderRespVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.WxOrderDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.WxOrderLineDO;
import cn.iocoder.yudao.module.pharmacy.service.member.WxOrderLineService;
import cn.iocoder.yudao.module.pharmacy.service.member.WxOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/**
 * 用户 APP - 小程序线上订单
 *
 * 线上订单只允许本人查看与操作，全部接口基于登录令牌中的会员编号做归属校验。
 */
@Tag(name = "用户 APP - 小程序线上订单")
@RestController
@RequestMapping("/member/wx-order")
@Validated
@Slf4j
public class AppWxOrderController {

    @Resource
    private WxOrderService wxOrderService;

    @Resource
    private WxOrderLineService wxOrderLineService;

    @PostMapping("/payment-notify")
    @jakarta.annotation.security.PermitAll // Pay module sends no member token; service verifies persisted records.
    @Operation(summary = "接收支付业务通知")
    public CommonResult<Boolean> paymentNotify(@RequestBody @Valid
            cn.iocoder.yudao.module.pay.api.notify.dto.PayOrderNotifyReqDTO request) {
        wxOrderService.notifyWxOrderPaid(request.getMerchantOrderId(), request.getPayOrderId());
        return success(true);
    }

    @PostMapping("/create")
    @Operation(summary = "从购物车已勾选商品下单（到店自提/同城配送）",
            description = "usePoints 只表示希望使用的抵扣积分，实际可用值与赠送积分由后端按会员等级与积分规则计算")
    public CommonResult<Long> createOrder(@RequestBody @Valid AppWxOrderCreateReqVO reqVO) {
        Long id = wxOrderService.createOrderFromCart(getLoginUserId(), reqVO.getStoreId(), reqVO.getOrderType(),
                reqVO.getAddressId(), reqVO.getPrescId(), reqVO.getRemark(), reqVO.getUsePoints());
        return success(id);
    }

    @GetMapping("/page")
    @Operation(summary = "获得本人订单分页")
    public CommonResult<PageResult<AppWxOrderRespVO>> getOrderPage(@Valid AppWxOrderPageReqVO pageReqVO) {
        // 强制使用登录会员编号，防止越权查询他人订单
        WxOrderPageReqVO reqVO = new WxOrderPageReqVO();
        reqVO.setPageNo(pageReqVO.getPageNo());
        reqVO.setPageSize(pageReqVO.getPageSize());
        reqVO.setMemberId(getLoginUserId());
        reqVO.setStatus(pageReqVO.getStatus());
        reqVO.setPayStatus(pageReqVO.getPayStatus());
        PageResult<WxOrderDO> pageResult = wxOrderService.getWxOrderPage(reqVO);
        return success(BeanUtils.toBean(pageResult, AppWxOrderRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "获得本人订单详情（含明细）")
    @Parameter(name = "id", description = "订单编号", required = true, example = "1024")
    public CommonResult<AppWxOrderRespVO> getOrder(@RequestParam("id") Long id) {
        WxOrderDO order = wxOrderService.validateWxOrderOwner(getLoginUserId(), id);
        AppWxOrderRespVO respVO = BeanUtils.toBean(order, AppWxOrderRespVO.class);
        respVO.setLines(getOrderLines(order.getId()));
        return success(respVO);
    }

    @GetMapping("/line-list")
    @Operation(summary = "获得本人订单明细列表")
    @Parameter(name = "wxOrderId", description = "订单编号", required = true, example = "1024")
    public CommonResult<List<AppWxOrderLineRespVO>> getOrderLineList(@RequestParam("wxOrderId") Long wxOrderId) {
        // 校验订单归属
        wxOrderService.validateWxOrderOwner(getLoginUserId(), wxOrderId);
        return success(getOrderLines(wxOrderId));
    }

    @PutMapping("/cancel")
    @Operation(summary = "取消本人订单（幂等）：待支付/待拣货 → 取消")
    @Parameter(name = "id", description = "订单编号", required = true, example = "1024")
    public CommonResult<Boolean> cancelOrder(@RequestParam("id") Long id,
                                             @RequestParam(value = "cancelReason", required = false) String cancelReason) {
        // 校验订单归属后取消，避免越权取消他人订单
        wxOrderService.validateWxOrderOwner(getLoginUserId(), id);
        // 小程序端没有库存作业身份，会员取消只关闭订单，冻结 / 出库的库存由门店节点释放或回补
        wxOrderService.cancelWxOrderByMember(id, cancelReason);
        return success(true);
    }

    @PostMapping("/simulate-pay")
    @Operation(summary = "模拟支付本人订单（幂等）：待支付 → 已支付 + 待拣货",
            description = "默认关闭；仅开发测试环境可启用，使用服务端金额并经支付成功链完成扣库")
    @Parameter(name = "id", description = "订单编号", required = true, example = "1024")
    public CommonResult<Boolean> simulatePay(@RequestParam("id") Long id) {
        // 校验订单归属后模拟支付，避免越权操作他人订单
        wxOrderService.validateWxOrderOwner(getLoginUserId(), id);
        wxOrderService.simulatePayWxOrderByMember(id);
        return success(true);
    }

    @PostMapping("/confirm-receive")
    @Operation(summary = "确认本人配送订单收货（幂等）：配送准备完成 → 完成",
            description = "仅已支付且完成拣货的配送订单；自提由门店员工按取货码核销")
    @Parameter(name = "id", description = "订单编号", required = true, example = "1024")
    public CommonResult<Boolean> confirmReceive(@RequestParam("id") Long id) {
        // 校验订单归属后确认收货，避免越权操作他人订单
        wxOrderService.validateWxOrderOwner(getLoginUserId(), id);
        wxOrderService.confirmReceiveWxOrderByMember(id);
        return success(true);
    }

    // ========== 私有方法 ==========

    private List<AppWxOrderLineRespVO> getOrderLines(Long wxOrderId) {
        List<WxOrderLineDO> lines = wxOrderLineService.getWxOrderLineListByWxOrderId(wxOrderId);
        return BeanUtils.toBean(lines, AppWxOrderLineRespVO.class);
    }

}
