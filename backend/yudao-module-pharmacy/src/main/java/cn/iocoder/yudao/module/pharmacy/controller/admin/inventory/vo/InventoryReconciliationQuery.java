package cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class InventoryReconciliationQuery extends PageParam {
    @Positive private Long warehouseId;
    @Positive private Long drugId;
    /** Only return rows with at least one detected difference. */
    private Boolean onlyDifference;

    public long getOffset() {
        return ((long) getPageNo() - 1) * getPageSize();
    }
}
