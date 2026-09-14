package cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class InventoryStocktakeQuery extends PageParam {
    @Positive private Long warehouseId;
    @Min(0) @Max(3) private Integer status;
    public long getOffset() { return ((long) getPageNo() - 1) * getPageSize(); }
}
