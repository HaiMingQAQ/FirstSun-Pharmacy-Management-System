package cn.iocoder.yudao.module.pharmacy.controller.admin.inventory;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryMovementReqVO;
import cn.iocoder.yudao.module.pharmacy.service.inventory.InventoryMovementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 库存上架移位")
@RestController
@RequestMapping("/pharmacy/inventory/movement")
@RequiredArgsConstructor
public class InventoryMovementController {
    private final InventoryMovementService service;

    @PostMapping("/execute")
    @Operation(summary = "同仓货位移位")
    @PreAuthorize("@ss.hasPermission('pharmacy:inventory-movement:execute')")
    public CommonResult<Boolean> execute(@Valid @RequestBody InventoryMovementReqVO request) {
        service.execute(request);
        return success(true);
    }
}
