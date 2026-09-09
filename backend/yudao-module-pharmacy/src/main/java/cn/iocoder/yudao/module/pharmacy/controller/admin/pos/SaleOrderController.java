package cn.iocoder.yudao.module.pharmacy.controller.admin.pos;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.pharmacy.controller.admin.pos.vo.SaleOrderDetailRespVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.pos.vo.SaleOrderPageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.pos.vo.SaleOrderSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhSaleOrderDO;
import cn.iocoder.yudao.module.pharmacy.service.sale.SaleOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - POS 销售单")
@RestController
@RequestMapping("/pharmacy/pos/sale-order")
@Validated
public class SaleOrderController {

    @Resource
    private SaleOrderService saleOrderService;

    @PostMapping("/create")
    @Operation(summary = "创建销售单（收银）")
    @PreAuthorize("@ss.hasPermission('pharmacy:pos-sale-order:create')")
    public CommonResult<Long> createSaleOrder(@Valid @RequestBody SaleOrderSaveReqVO createReqVO) {
        return success(saleOrderService.createSaleOrder(createReqVO));
    }

    @GetMapping("/page")
    @Operation(summary = "获得销售单分页")
    @PreAuthorize("@ss.hasPermission('pharmacy:pos-sale-order:query')")
    public CommonResult<PageResult<PhSaleOrderDO>> getSaleOrderPage(@Valid SaleOrderPageReqVO pageReqVO) {
        return success(saleOrderService.getSaleOrderPage(pageReqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得销售单详情")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pharmacy:pos-sale-order:query')")
    public CommonResult<PhSaleOrderDO> getSaleOrder(@RequestParam("id") Long id) {
        return success(saleOrderService.getSaleOrder(id));
    }

    @GetMapping("/detail")
    @Operation(summary = "获得销售单详情（含明细与支付）")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pharmacy:pos-sale-order:query')")
    public CommonResult<SaleOrderDetailRespVO> getSaleOrderDetail(@RequestParam("id") Long id) {
        return success(saleOrderService.getSaleOrderDetail(id));
    }

}
