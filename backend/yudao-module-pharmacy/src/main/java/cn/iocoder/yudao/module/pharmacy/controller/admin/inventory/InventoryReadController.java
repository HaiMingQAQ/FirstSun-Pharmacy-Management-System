package cn.iocoder.yudao.module.pharmacy.controller.admin.inventory;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryReadQuery;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryReadVO;
import cn.iocoder.yudao.module.pharmacy.service.inventory.InventoryReadService;
import cn.iocoder.yudao.module.pharmacy.service.inventory.InventoryPreviewService;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryPreviewReqVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 库存查询")
@RestController
@RequestMapping("/pharmacy/inventory")
@Validated
@RequiredArgsConstructor
public class InventoryReadController {
    private final InventoryReadService service;
    private final InventoryPreviewService previewService;

    @GetMapping("/batch/get")
    @Operation(summary = "本门店批次详情")
    @PreAuthorize("@ss.hasPermission('pharmacy:inventory-batch:query')")
    public CommonResult<InventoryReadVO.Batch> batch(@RequestParam("id") @Positive long id) {
        return success(service.batch(id));
    }

    @GetMapping("/location/get")
    @Operation(summary = "本门店货位详情")
    @PreAuthorize("@ss.hasPermission('pharmacy:inventory-location:query')")
    public CommonResult<InventoryReadVO.Location> location(@RequestParam("id") @Positive long id) {
        return success(service.location(id));
    }

    @GetMapping("/flow/get")
    @Operation(summary = "本门店历史流水详情")
    @PreAuthorize("@ss.hasPermission('pharmacy:inventory-flow:query')")
    public CommonResult<InventoryReadVO.Flow> flow(@RequestParam("id") @Positive long id) {
        return success(service.flow(id));
    }

    @GetMapping("/batch/fefo-preview")
    @Operation(summary = "单仓 FEFO 模拟预览，不占用库存或授权销售")
    @PreAuthorize("@ss.hasPermission('pharmacy:inventory-batch:query')")
    public CommonResult<InventoryPreviewService.Preview> preview(@Valid InventoryPreviewReqVO query) {
        return success(previewService.preview(query));
    }

    @GetMapping("/warehouse/page")
    @Operation(summary = "本门店仓库分页")
    @PreAuthorize("@ss.hasPermission('pharmacy:inventory-warehouse:query')")
    public CommonResult<PageResult<InventoryReadVO.Warehouse>> warehouses(@Valid InventoryReadQuery query) {
        return success(service.warehouses(query));
    }

    @GetMapping("/warehouse/get")
    @Operation(summary = "本门店仓库详情")
    @PreAuthorize("@ss.hasPermission('pharmacy:inventory-warehouse:query')")
    public CommonResult<InventoryReadVO.Warehouse> warehouse(@RequestParam("id") @Positive long id) {
        return success(service.warehouse(id));
    }

    @GetMapping("/location/page")
    @Operation(summary = "本门店货位分页")
    @PreAuthorize("@ss.hasPermission('pharmacy:inventory-location:query')")
    public CommonResult<PageResult<InventoryReadVO.Location>> locations(@Valid InventoryReadQuery query) {
        return success(service.locations(query));
    }

    @GetMapping("/batch/page")
    @Operation(summary = "本门店批次库存分页，返回账面可用量")
    @PreAuthorize("@ss.hasPermission('pharmacy:inventory-batch:query')")
    public CommonResult<PageResult<InventoryReadVO.Batch>> batches(@Valid InventoryReadQuery query) {
        return success(service.batches(query));
    }

    @GetMapping("/batch/location-stock")
    @Operation(summary = "本门店货位库存分页")
    @PreAuthorize("@ss.hasPermission('pharmacy:inventory-batch:query')")
    public CommonResult<PageResult<InventoryReadVO.LocationStock>> locationStock(@Valid InventoryReadQuery query) {
        return success(service.locationStock(query));
    }

    @GetMapping("/flow/page")
    @Operation(summary = "本门店库存流水分页")
    @PreAuthorize("@ss.hasPermission('pharmacy:inventory-flow:query')")
    public CommonResult<PageResult<InventoryReadVO.Flow>> flows(@Valid InventoryReadQuery query) {
        return success(service.flows(query));
    }
}
