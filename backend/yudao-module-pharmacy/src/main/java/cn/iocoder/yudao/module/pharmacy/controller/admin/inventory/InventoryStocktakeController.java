package cn.iocoder.yudao.module.pharmacy.controller.admin.inventory;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryStocktakeCreateReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryStocktakeQuery;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryStocktakeRecordReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryStocktakeVO;
import cn.iocoder.yudao.module.pharmacy.service.inventory.InventoryStocktakeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 库存盘点")
@RestController
@RequestMapping("/pharmacy/inventory/stocktake")
@Validated
@RequiredArgsConstructor
public class InventoryStocktakeController {
    private final InventoryStocktakeService service;

    @GetMapping("/page")
    @Operation(summary = "本门店盘点单分页")
    @PreAuthorize("@ss.hasPermission('pharmacy:inventory-stocktake:query')")
    public CommonResult<PageResult<InventoryStocktakeVO.Summary>> page(@Valid InventoryStocktakeQuery query) {
        return success(service.page(query));
    }

    @GetMapping("/get")
    @Operation(summary = "盘点单详情")
    @PreAuthorize("@ss.hasPermission('pharmacy:inventory-stocktake:query')")
    public CommonResult<InventoryStocktakeVO.Detail> get(@RequestParam("id") @Positive long id) {
        return success(service.detail(id));
    }

    @PostMapping("/create")
    @Operation(summary = "创建盘点单")
    @PreAuthorize("@ss.hasPermission('pharmacy:inventory-stocktake:create')")
    public CommonResult<Long> create(@Valid @RequestBody InventoryStocktakeCreateReqVO request) {
        return success(service.create(request));
    }

    @PostMapping("/start")
    @Operation(summary = "开始盘点")
    @PreAuthorize("@ss.hasPermission('pharmacy:inventory-stocktake:update')")
    public CommonResult<Boolean> start(@RequestParam("id") @Positive long id) {
        service.start(id); return success(true);
    }

    @PostMapping("/record")
    @Operation(summary = "录入实盘数量")
    @PreAuthorize("@ss.hasPermission('pharmacy:inventory-stocktake:update')")
    public CommonResult<Boolean> record(@Valid @RequestBody InventoryStocktakeRecordReqVO request) {
        service.record(request); return success(true);
    }

    @PostMapping("/complete")
    @Operation(summary = "完成盘点")
    @PreAuthorize("@ss.hasPermission('pharmacy:inventory-stocktake:update')")
    public CommonResult<Boolean> complete(@RequestParam("id") @Positive long id) {
        service.complete(id); return success(true);
    }

    @PostMapping("/approve")
    @Operation(summary = "审批并执行盘点调整")
    @PreAuthorize("@ss.hasPermission('pharmacy:inventory-stocktake:approve')")
    public CommonResult<Boolean> approve(@RequestParam("id") @Positive long id) {
        service.approveAndAdjust(id); return success(true);
    }

    @PostMapping("/cancel")
    @Operation(summary = "取消草稿盘点")
    @PreAuthorize("@ss.hasPermission('pharmacy:inventory-stocktake:cancel')")
    public CommonResult<Boolean> cancel(@RequestParam("id") @Positive long id) {
        service.cancelDraft(id); return success(true);
    }
}
