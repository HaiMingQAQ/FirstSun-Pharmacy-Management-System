package cn.iocoder.yudao.module.pharmacy.controller.admin.inventory;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryCatalogCreateReqVO;
import cn.iocoder.yudao.module.pharmacy.service.inventory.InventoryCatalogService;
import cn.iocoder.yudao.module.pharmacy.service.inventory.InventoryCatalogUpdateService;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryCatalogUpdateReqVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 库存基础资料")
@RestController
@RequestMapping("/pharmacy/inventory")
@RequiredArgsConstructor
public class InventoryCatalogController {
    private final InventoryCatalogService service;
    private final InventoryCatalogUpdateService updates;

    @PutMapping("/warehouse/update")
    @Operation(summary = "编辑本门店仓库，不含默认仓切换")
    @PreAuthorize("@ss.hasPermission('pharmacy:inventory-warehouse:update')")
    public CommonResult<Boolean> updateWarehouse(@Valid @RequestBody InventoryCatalogUpdateReqVO.Warehouse request) {
        updates.updateWarehouse(request);
        return success(true);
    }

    @PutMapping("/location/update")
    @Operation(summary = "编辑本门店货位，检查容量、库存占用及作业门禁")
    @PreAuthorize("@ss.hasPermission('pharmacy:inventory-location:update')")
    public CommonResult<Boolean> updateLocation(@Valid @RequestBody InventoryCatalogUpdateReqVO.Location request) {
        updates.updateLocation(request);
        return success(true);
    }

    @PostMapping("/warehouse/create")
    @Operation(summary = "新增本门店普通仓库")
    @PreAuthorize("@ss.hasPermission('pharmacy:inventory-warehouse:create')")
    public CommonResult<Long> createWarehouse(@Valid @RequestBody InventoryCatalogCreateReqVO.Warehouse request) {
        return success(service.createWarehouse(request));
    }

    @PostMapping("/location/create")
    @Operation(summary = "在本门店启用仓库下新增货位")
    @PreAuthorize("@ss.hasPermission('pharmacy:inventory-location:create')")
    public CommonResult<Long> createLocation(@Valid @RequestBody InventoryCatalogCreateReqVO.Location request) {
        return success(service.createLocation(request));
    }
}
