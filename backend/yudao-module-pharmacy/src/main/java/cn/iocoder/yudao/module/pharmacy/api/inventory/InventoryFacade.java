package cn.iocoder.yudao.module.pharmacy.api.inventory;

import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.DeductResult;
import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.DeductItem;
import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.ReturnBackItem;

import java.util.List;

/**
 * 库存服务门面（POS 域与库存域的依赖边界）。
 * <p>
 * 由 C 成员实现选批/扣减/回补；当前 C 未实现，调用方需捕获
 * {@link UnsupportedOperationException} 并转换为 INV_SERVICE_UNAVAILABLE，
 * 保证销售/退单事务整体回滚，不静默放行。
 */
public interface InventoryFacade {

    /**
     * 销售出库扣减（同事务）
     *
     * @param storeId 门店编号
     * @param items   扣减明细（药品/批次/数量/货位）
     * @return 扣减结果
     */
    DeductResult deduct(Long storeId, List<DeductItem> items);

    /**
     * 退货回补（同事务，原批次退回）
     */
    void returnBack(Long storeId, List<ReturnBackItem> items);
}
