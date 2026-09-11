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

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
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

}
