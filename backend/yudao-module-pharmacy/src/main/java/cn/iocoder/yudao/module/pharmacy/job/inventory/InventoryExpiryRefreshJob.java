package cn.iocoder.yudao.module.pharmacy.job.inventory;

import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.pharmacy.service.inventory.InventoryExpiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/** Daily expiry alert refresh. Schedule this handler from the infra job console when Quartz is enabled. */
@Component
@RequiredArgsConstructor
public class InventoryExpiryRefreshJob implements JobHandler {
    private final InventoryExpiryService expiryService;

    @Override
    @TenantJob
    public String execute(String param) {
        int affected = expiryService.refreshForTenant(LocalDate.now());
        return "刷新效期预警 " + affected + " 条";
    }
}
