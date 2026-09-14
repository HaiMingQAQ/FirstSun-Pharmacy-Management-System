package cn.iocoder.yudao.module.pharmacy.controller.admin.inventory;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryDamageCreateReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryDamageQuery;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryDamageVO;
import cn.iocoder.yudao.module.pharmacy.service.inventory.InventoryDamageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 库存报损")
@RestController
@RequestMapping("/pharmacy/inventory/damage")
@Validated
@RequiredArgsConstructor
public class InventoryDamageController {
    private final InventoryDamageService service;

    @GetMapping("/page")
    @Operation(summary = "本门店报损分页")
    @PreAuthorize("@ss.hasPermission('pharmacy:inventory-damage:query')")
    public CommonResult<PageResult<InventoryDamageVO.Summary>> page(@Valid InventoryDamageQuery query) {
        return success(service.page(query));
    }

    @GetMapping("/get")
    @Operation(summary = "报损详情")
    @PreAuthorize("@ss.hasPermission('pharmacy:inventory-damage:query')")
    public CommonResult<InventoryDamageVO.Detail> get(@RequestParam("id") @Positive long id) {
        return success(service.detail(id));
    }

    @PostMapping("/create")
    @Operation(summary = "创建报损单")
    @PreAuthorize("@ss.hasPermission('pharmacy:inventory-damage:create')")
    public CommonResult<Long> create(@Valid @RequestBody InventoryDamageCreateReqVO request) {
        return success(service.create(request));
    }

    @PostMapping("/submit")
    @Operation(summary = "提交报损")
    @PreAuthorize("@ss.hasPermission('pharmacy:inventory-damage:update')")
    public CommonResult<Boolean> submit(@RequestParam("id") @Positive long id) {
        service.submit(id); return success(true);
    }

    @PostMapping("/approve")
    @Operation(summary = "审批报损")
    @PreAuthorize("@ss.hasPermission('pharmacy:inventory-damage:approve')")
    public CommonResult<Boolean> approve(@RequestParam("id") @Positive long id) {
        service.approve(id); return success(true);
    }

    @PostMapping("/reject")
    @Operation(summary = "驳回报损")
    @PreAuthorize("@ss.hasPermission('pharmacy:inventory-damage:approve')")
    public CommonResult<Boolean> reject(@RequestParam("id") @Positive long id) {
        service.reject(id); return success(true);
    }

    @PostMapping("/review")
    @Operation(summary = "复核报损")
    @PreAuthorize("@ss.hasPermission('pharmacy:inventory-damage:review')")
    public CommonResult<Boolean> review(@RequestParam("id") @Positive long id) {
        service.review(id); return success(true);
    }

    @PostMapping("/execute")
    @Operation(summary = "执行报损出账")
    @PreAuthorize("@ss.hasPermission('pharmacy:inventory-damage:execute')")
    public CommonResult<Boolean> execute(@RequestParam("id") @Positive long id) {
        service.execute(id); return success(true);
    }

    @PostMapping("/cancel")
    @Operation(summary = "取消草稿报损")
    @PreAuthorize("@ss.hasPermission('pharmacy:inventory-damage:cancel')")
    public CommonResult<Boolean> cancel(@RequestParam("id") @Positive long id) {
        service.cancelDraft(id); return success(true);
    }
}
