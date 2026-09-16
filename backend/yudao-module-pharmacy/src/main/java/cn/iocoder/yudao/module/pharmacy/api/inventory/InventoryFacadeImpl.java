package cn.iocoder.yudao.module.pharmacy.api.inventory;

import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.DeductItem;
import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.DeductResult;
import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.ReceiveItem;
import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.ReceiveResult;
import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.ReturnBackItem;
import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.ReserveItem;
import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.ReserveResult;
import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.ReleaseItem;
import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.ConsumeItem;
import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.AvailableQty;

import java.util.List;

/**
 * 库存门面【临时降级实现】。
 * <p>
 * C 成员实现 {@link InventoryFacade} 后，本 Bean 因 @ConditionalOnMissingBean 自动失效；
 * 在此之前，调用方（POS 销售/退货、B 采购收货）会收到 UnsupportedOperationException，
 * 由 Service 层转换为 INV_SERVICE_UNAVAILABLE，事务整体回滚，绝不静默放行。
 */
public class InventoryFacadeImpl implements InventoryFacade {

    @Override
    public DeductResult deduct(Long storeId, List<DeductItem> items) {
        throw new UnsupportedOperationException("C 库存服务未实现，销售扣库不可用");
    }

    @Override
    public void returnBack(Long storeId, List<ReturnBackItem> items) {
        throw new UnsupportedOperationException("C 库存服务未实现，退货回补不可用");
    }

    @Override
    public ReceiveResult receive(Long storeId, String receiptNo, List<ReceiveItem> items) {
        throw new UnsupportedOperationException("C 库存服务未实现，采购收货入库不可用");
    }

    @Override public ReserveResult reserve(Long storeId, List<ReserveItem> items) { throw new UnsupportedOperationException("C 库存服务未实现，库存冻结不可用"); }
    @Override public void release(Long storeId, List<ReleaseItem> items) { throw new UnsupportedOperationException("C 库存服务未实现，库存释放不可用"); }
    @Override public DeductResult consumeReservation(Long storeId, List<ConsumeItem> items) { throw new UnsupportedOperationException("C 库存服务未实现，冻结转出库不可用"); }
    @Override public List<AvailableQty> getAvailableQty(Long storeId, List<Long> drugIds) { throw new UnsupportedOperationException("C 库存服务未实现，可售量查询不可用"); }

}
