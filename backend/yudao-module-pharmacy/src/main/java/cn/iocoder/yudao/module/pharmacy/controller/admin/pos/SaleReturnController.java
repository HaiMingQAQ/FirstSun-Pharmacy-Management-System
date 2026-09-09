package cn.iocoder.yudao.module.pharmacy.controller.admin.pos;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.pharmacy.controller.admin.pos.vo.SaleReturnDetailRespVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.pos.vo.SaleReturnPageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.pos.vo.SaleReturnSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhSaleReturnDO;
import cn.iocoder.yudao.module.pharmacy.service.sale.SaleReturnService;
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

@Tag(name = "管理后台 - POS 退货单")
@RestController
@RequestMapping("/pharmacy/pos/sale-return")
@Validated
public class SaleReturnController {

    @Resource
    private SaleReturnService saleReturnService;

    @PostMapping("/create")
    @Operation(summary = "创建退货单（全退/部分退）")
    @PreAuthorize("@ss.hasPermission('pharmacy:pos-sale-return:create')")
    public CommonResult<Long> createSaleReturn(@Valid @RequestBody SaleReturnSaveReqVO createReqVO) {
        return success(saleReturnService.createReturn(createReqVO));
    }

    @GetMapping("/page")
    @Operation(summary = "获得退货单分页")
    @PreAuthorize("@ss.hasPermission('pharmacy:pos-sale-return:query')")
    public CommonResult<PageResult<PhSaleReturnDO>> getSaleReturnPage(@Valid SaleReturnPageReqVO pageReqVO) {
        return success(saleReturnService.getReturnPage(pageReqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得退货单详情")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pharmacy:pos-sale-return:query')")
    public CommonResult<PhSaleReturnDO> getSaleReturn(@RequestParam("id") Long id) {
        return success(saleReturnService.getReturn(id));
    }

    @GetMapping("/detail")
    @Operation(summary = "获得退货单详情（含明细）")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pharmacy:pos-sale-return:query')")
    public CommonResult<SaleReturnDetailRespVO> getSaleReturnDetail(@RequestParam("id") Long id) {
        return success(saleReturnService.getReturnDetail(id));
    }

}
