package cn.iocoder.yudao.module.pharmacy.controller.app.pharmacy;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.pharmacy.controller.app.pharmacy.vo.AppInventoryAvailableRespVO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.inventory.AppAvailableInventoryMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * Anonymous, tenant-scoped catalogue stock projection. Filters active stores,
 * approved online drugs and FEFO-eligible locations/batches; does not write stock.
 */
@Tag(name = "用户 APP - 药品可售库存")
@RestController
@RequestMapping("/pharmacy/inventory")
@Validated
public class AppInventoryController {

    @Resource
    private AppAvailableInventoryMapper inventoryMapper;

    @GetMapping("/available")
    @Operation(summary = "获得门店下药品的可售数量（只读）",
            description = "drugIds 逗号分隔；仅当前租户启用门店、审核通过且线上可售药品；排除冻结、过期和质量异常库存")
    @Parameter(name = "storeId", description = "门店编号", required = true, example = "407")
    @Parameter(name = "drugIds", description = "药品编号列表（逗号分隔）", required = true, example = "163101,163102")
    @PermitAll
    public CommonResult<List<AppInventoryAvailableRespVO>> getAvailableQty(
            @RequestParam("storeId") @jakarta.validation.constraints.Positive Long storeId,
            @RequestParam("drugIds") @jakarta.validation.constraints.Size(min = 1, max = 100)
            List<@jakarta.validation.constraints.Positive Long> drugIds) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null || tenantId < 0 || TenantContextHolder.isIgnore()) {
            throw new org.springframework.security.access.AccessDeniedException("需要有效租户上下文");
        }
        if (storeId == null || storeId <= 0 || drugIds == null || drugIds.isEmpty()
                || drugIds.size() > 100 || drugIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException("门店及药品编号无效，每次最多查询100种药品");
        }
        return success(inventoryMapper.selectAvailable(tenantId, storeId, drugIds.stream().distinct().toList()));
    }

}
