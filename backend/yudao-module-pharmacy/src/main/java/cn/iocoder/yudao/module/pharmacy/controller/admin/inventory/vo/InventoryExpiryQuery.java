package cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/** Filters for daily expiry alerts. Scope always comes from the authenticated employee. */
@Data
@EqualsAndHashCode(callSuper = true)
public class InventoryExpiryQuery extends PageParam {
    @Positive private Long warehouseId;
    @Positive private Long drugId;
    @Positive private Long batchId;
    @Min(1) @Max(3) private Integer alertLevel;
    @Min(0) @Max(4) private Integer handleType;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate alertDate;

    public long getOffset() {
        return ((long) getPageNo() - 1) * getPageSize();
    }
}
