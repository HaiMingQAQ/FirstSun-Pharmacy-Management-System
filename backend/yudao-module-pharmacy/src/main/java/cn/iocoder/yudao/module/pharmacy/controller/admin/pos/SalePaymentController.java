package cn.iocoder.yudao.module.pharmacy.controller.admin.pos;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.pharmacy.controller.admin.pos.vo.SalePaymentPageReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhSalePaymentDO;
import cn.iocoder.yudao.module.pharmacy.service.sale.SalePaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - POS 销售支付明细")
@RestController
@RequestMapping("/pharmacy/pos/payment")
@Validated
public class SalePaymentController {

    @Resource
    private SalePaymentService salePaymentService;

    @GetMapping("/page")
    @Operation(summary = "获得支付明细分页")
    @PreAuthorize("@ss.hasPermission('pharmacy:pos-payment:query')")
    public CommonResult<PageResult<PhSalePaymentDO>> getPaymentPage(@Valid SalePaymentPageReqVO pageReqVO) {
        return success(salePaymentService.getPaymentPage(pageReqVO));
    }

    @GetMapping("/list-by-order")
    @Operation(summary = "按销售单获得支付明细列表")
    @Parameter(name = "orderId", description = "销售单编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pharmacy:pos-payment:query')")
    public CommonResult<List<PhSalePaymentDO>> getPaymentsByOrderId(@RequestParam("orderId") Long orderId) {
        return success(salePaymentService.getPaymentsByOrderId(orderId));
    }

}
