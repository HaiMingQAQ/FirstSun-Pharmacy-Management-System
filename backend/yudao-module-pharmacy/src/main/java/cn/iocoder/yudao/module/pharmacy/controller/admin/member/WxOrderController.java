package cn.iocoder.yudao.module.pharmacy.controller.admin.member;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.order.WxOrderPageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.order.WxOrderRespVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.order.WxOrderSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.orderline.WxOrderLineRespVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.WxOrderDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.WxOrderLineDO;
import cn.iocoder.yudao.module.pharmacy.service.member.WxOrderLineService;
import cn.iocoder.yudao.module.pharmacy.service.member.WxOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 小程序订单")
@RestController
@RequestMapping("/pharmacy/member/order")
@Validated
public class WxOrderController {

    @Resource
    private WxOrderService wxOrderService;

    @Resource
    private WxOrderLineService wxOrderLineService;

    @PostMapping("/create")
    @Operation(summary = "创建小程序订单")
    @PreAuthorize("@ss.hasPermission('pharmacy:member:order:create')")
    public CommonResult<Long> createWxOrder(@Valid @RequestBody WxOrderSaveReqVO createReqVO) {
        Long id = wxOrderService.createWxOrder(createReqVO);
        return success(id);
    }

    @PutMapping("/update")
    @Operation(summary = "更新小程序订单")
    @PreAuthorize("@ss.hasPermission('pharmacy:member:order:update')")
    public CommonResult<Boolean> updateWxOrder(@Valid @RequestBody WxOrderSaveReqVO updateReqVO) {
        wxOrderService.updateWxOrder(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除小程序订单")
    @Parameter(name = "id", description = "订单编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pharmacy:member:order:delete')")
    public CommonResult<Boolean> deleteWxOrder(@RequestParam("id") Long id) {
        wxOrderService.deleteWxOrder(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得小程序订单详情")
    @Parameter(name = "id", description = "订单编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pharmacy:member:order:query')")
    public CommonResult<WxOrderRespVO> getWxOrder(@RequestParam("id") Long id) {
        WxOrderDO wxOrder = wxOrderService.getWxOrder(id);
        return success(BeanUtils.toBean(wxOrder, WxOrderRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得小程序订单分页")
    @PreAuthorize("@ss.hasPermission('pharmacy:member:order:query')")
    public CommonResult<PageResult<WxOrderRespVO>> getWxOrderPage(@Validated WxOrderPageReqVO pageReqVO) {
        PageResult<WxOrderDO> pageResult = wxOrderService.getWxOrderPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, WxOrderRespVO.class));
    }

    @GetMapping("/line-list")
    @Operation(summary = "获得订单明细列表")
    @Parameter(name = "wxOrderId", description = "订单编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pharmacy:member:order:query')")
    public CommonResult<List<WxOrderLineRespVO>> getWxOrderLineList(@RequestParam("wxOrderId") Long wxOrderId) {
        List<WxOrderLineDO> list = wxOrderLineService.getWxOrderLineListByWxOrderId(wxOrderId);
        return success(BeanUtils.toBean(list, WxOrderLineRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出小程序订单 Excel")
    @PreAuthorize("@ss.hasPermission('pharmacy:member:order:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportWxOrder(HttpServletResponse response, @Validated WxOrderPageReqVO reqVO) throws IOException {
        reqVO.setPageSize(cn.iocoder.yudao.framework.common.pojo.PageParam.PAGE_SIZE_NONE);
        List<WxOrderDO> list = wxOrderService.getWxOrderPage(reqVO).getList();
        ExcelUtils.write(response, "小程序订单.xls", "订单列表", WxOrderRespVO.class,
                BeanUtils.toBean(list, WxOrderRespVO.class));
    }

    // ========== 状态流转与核销接口 ==========

    @PutMapping("/pay")
    @Operation(summary = "支付成功回调（幂等）：待支付 → 待拣货")
    @Parameter(name = "id", description = "订单编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pharmacy:member:order:update')")
    public CommonResult<Boolean> payWxOrder(@RequestParam("id") Long id,
                                            @RequestParam(value = "payNo", required = false) String payNo) {
        wxOrderService.payWxOrder(id, payNo);
        return success(true);
    }

    @PutMapping("/cancel")
    @Operation(summary = "取消订单（幂等）：待支付/待拣货 → 取消")
    @Parameter(name = "id", description = "订单编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pharmacy:member:order:cancel')")
    public CommonResult<Boolean> cancelWxOrder(@RequestParam("id") Long id,
                                               @RequestParam(value = "cancelReason", required = false) String cancelReason) {
        wxOrderService.cancelWxOrder(id, cancelReason);
        return success(true);
    }

    @PutMapping("/refund")
    @Operation(summary = "退款订单（幂等）：已支付未完成 → 已退款，并按原批次回补库存")
    @Parameter(name = "id", description = "订单编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pharmacy:member:order:update')")
    public CommonResult<Boolean> refundWxOrder(@RequestParam("id") Long id,
                                               @RequestParam(value = "refundReason", required = false) String refundReason) {
        wxOrderService.refundWxOrder(id, refundReason);
        return success(true);
    }

    @PutMapping("/reserve")
    @Operation(summary = "冻结订单库存（幂等）：按门店可售量校验后按 FEFO 冻结")
    @Parameter(name = "id", description = "订单编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pharmacy:member:order:update')")
    public CommonResult<Boolean> reserveWxOrder(@RequestParam("id") Long id) {
        wxOrderService.reserveWxOrder(id);
        return success(true);
    }

    @PutMapping("/close-expired")
    @Operation(summary = "关闭门店超时未支付订单（门店库存清理节点）")
    @Parameter(name = "storeId", description = "门店编号", required = true, example = "407")
    @PreAuthorize("@ss.hasPermission('pharmacy:member:order:update')")
    public CommonResult<Integer> closeExpiredWxOrders(@RequestParam("storeId") Long storeId,
                                                      @RequestParam(value = "limit", required = false) Integer limit) {
        return success(wxOrderService.closeExpiredWxOrders(storeId, limit));
    }

    @PutMapping("/release-frozen")
    @Operation(summary = "释放已关闭订单仍冻结的库存（门店库存清理节点，幂等）")
    @Parameter(name = "storeId", description = "门店编号", required = true, example = "407")
    @PreAuthorize("@ss.hasPermission('pharmacy:member:order:update')")
    public CommonResult<Integer> releaseFrozenStock(@RequestParam("storeId") Long storeId,
                                                    @RequestParam(value = "limit", required = false) Integer limit) {
        return success(wxOrderService.releaseFrozenStockOfClosedOrders(storeId, limit));
    }

    @PutMapping("/start-picking")
    @Operation(summary = "开始拣货：待拣货 → 拣货中")
    @Parameter(name = "id", description = "订单编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pharmacy:member:order:update')")
    public CommonResult<Boolean> startPicking(@RequestParam("id") Long id) {
        wxOrderService.startPicking(id);
        return success(true);
    }

    @PutMapping("/finish-picking")
    @Operation(summary = "拣货完成：拣货中 → 待自提")
    @Parameter(name = "id", description = "订单编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pharmacy:member:order:update')")
    public CommonResult<Boolean> finishPicking(@RequestParam("id") Long id) {
        wxOrderService.finishPicking(id);
        return success(true);
    }

    @PutMapping("/verify")
    @Operation(summary = "核销订单（幂等）：待自提 → 完成")
    @Parameter(name = "id", description = "订单编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pharmacy:member:order:verify')")
    public CommonResult<Boolean> verifyWxOrder(@RequestParam("id") Long id,
                                               @RequestParam("pickupCode") String pickupCode,
                                               @RequestParam("verifyBy") Long verifyBy) {
        wxOrderService.verifyWxOrder(id, pickupCode, verifyBy);
        return success(true);
    }

}
