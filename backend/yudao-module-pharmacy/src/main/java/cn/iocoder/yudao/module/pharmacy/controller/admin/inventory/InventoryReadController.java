package cn.iocoder.yudao.module.pharmacy.controller.admin.inventory;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryReadQuery;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryReadVO;
import cn.iocoder.yudao.module.pharmacy.service.inventory.InventoryReadService;
import cn.iocoder.yudao.module.pharmacy.service.inventory.InventoryPreviewService;
import cn.iocoder.yudao.module.pharmacy.service.inventory.InventoryExpiryService;
import cn.iocoder.yudao.module.pharmacy.service.inventory.InventoryReconciliationService;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryPreviewReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryExpiryHandleReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryExpiryQuery;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryExpiryVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryReconciliationQuery;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryReconciliationVO;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    private final InventoryExpiryService expiryService;
    private final InventoryReconciliationService reconciliationService;

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

    @GetMapping("/expiry/page")
    @Operation(summary = "本门店效期预警分页")
    @PreAuthorize("@ss.hasPermission('pharmacy:inventory-expiry:query')")
    public CommonResult<PageResult<InventoryExpiryVO.Alert>> expiry(@Valid InventoryExpiryQuery query) {
        return success(expiryService.page(query));
    }

    @PostMapping("/expiry/refresh")
    @Operation(summary = "刷新本门店每日效期预警")
    @PreAuthorize("@ss.hasPermission('pharmacy:inventory-expiry:refresh')")
    public CommonResult<InventoryExpiryService.RefreshResult> refreshExpiry() {
        return success(expiryService.refresh());
    }

    @PostMapping("/expiry/handle")
    @Operation(summary = "处理效期预警")
    @PreAuthorize("@ss.hasPermission('pharmacy:inventory-expiry:handle')")
    public CommonResult<Boolean> handleExpiry(@Valid @RequestBody InventoryExpiryHandleReqVO request) {
        expiryService.handle(request);
        return success(true);
    }

    @GetMapping("/reconciliation/page")
    @Operation(summary = "本门店库存三账核对")
    @PreAuthorize("@ss.hasPermission('pharmacy:inventory-reconciliation:query')")
    public CommonResult<PageResult<InventoryReconciliationVO.Row>> reconciliation(
            @Valid InventoryReconciliationQuery query) {
        return success(reconciliationService.page(query));
    }

    @GetMapping("/reconciliation/export-excel")
    @Operation(summary = "导出本门店库存三账核对")
    @PreAuthorize("@ss.hasPermission('pharmacy:inventory-reconciliation:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportReconciliation(HttpServletResponse response,
                                     @Valid InventoryReconciliationQuery query) throws IOException {
        List<InventoryReconciliationVO.Row> rows = reconciliationService.export(query);
        ExcelUtils.write(response, "库存三账核对.xls", "核对结果", InventoryReconciliationVO.Row.class, rows);
    }
}
