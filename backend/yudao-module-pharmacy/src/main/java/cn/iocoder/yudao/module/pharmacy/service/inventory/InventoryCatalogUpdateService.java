package cn.iocoder.yudao.module.pharmacy.service.inventory;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryCatalogUpdateReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryReadVO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.inventory.InventoryCatalogUpdateMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.validation.annotation.Validated;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants.NOT_FOUND;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.*;

@Service
@Validated
@RequiredArgsConstructor
public class InventoryCatalogUpdateService {
    private final InventoryReadAccess access;
    private final InventoryCatalogUpdateMapper mapper;

    // READ_COMMITTED is deliberate: after waiting for the parent lock, aggregate checks must
    // see transactions committed by the previous lock owner, even if caller read earlier.
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public void updateWarehouse(@NotNull @Valid InventoryCatalogUpdateReqVO.Warehouse request) {
        requireCompatibleTransaction();
        var scope = access.requireScope(null);
        var current = mapper.lockWarehouse(scope, request.getId());
        if (current == null) throw exception(NOT_FOUND);
        var expected = request.getExpected();
        var value = request.getValue();
        if (!matches(current, expected)) throw invalidParamException("仓库已被修改，请刷新后重新编辑");
        if (expected.equals(value)) return;
        boolean zoneChanged = !Objects.equals(expected.getTempZone(), value.getTempZone());
        boolean statusChanged = !Objects.equals(expected.getStatus(), value.getStatus());
        if (statusChanged && !Objects.equals(current.getIsDefault(), 0)) {
            throw invalidParamException("默认仓启停需先完成默认仓切换协调");
        }
        if (zoneChanged || statusChanged) {
            if (mapper.hasOpenWork(scope, request.getId())) throw invalidParamException("仓库存在未完成盘点、报损或库存锁");
            if ((zoneChanged || value.getStatus() == 0) && mapper.hasWarehouseStock(scope, request.getId())) {
                throw invalidParamException("仓库仍有库存，不能停用或变更温区");
            }
        }
        try {
            requireUpdated(mapper.updateWarehouse(scope, request, actor()));
        } catch (DuplicateKeyException ex) {
            throw invalidParamException("仓库编码已被使用（含已删除记录）");
        }
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public void updateLocation(@NotNull @Valid InventoryCatalogUpdateReqVO.Location request) {
        requireCompatibleTransaction();
        var scope = access.requireScope(null);
        var warehouse = mapper.lockWarehouse(scope, request.getWarehouseId());
        if (warehouse == null) throw exception(NOT_FOUND);
        var current = mapper.lockLocation(scope, request.getWarehouseId(), request.getId());
        if (current == null) throw exception(NOT_FOUND);
        var expected = request.getExpected();
        var value = request.getValue();
        if (!matches(current, expected)) throw invalidParamException("货位已被修改，请刷新后重新编辑");
        if (expected.equals(value)) return;
        if (!Objects.equals(warehouse.getStatus(), 1)) throw invalidParamException("所属仓库未启用");
        boolean typeChanged = !Objects.equals(expected.getLocationType(), value.getLocationType());
        boolean statusChanged = !Objects.equals(expected.getStatus(), value.getStatus());
        boolean capacityChanged = !Objects.equals(expected.getMaxCapacity(), value.getMaxCapacity());
        if (typeChanged || statusChanged || capacityChanged) {
            if (mapper.hasOpenWork(scope, request.getWarehouseId())) throw invalidParamException("所属仓库存在未完成盘点、报损或库存锁");
            var usage = mapper.locationUsage(scope, request.getWarehouseId(), request.getId());
            if (usage == null || usage.getQuantity() == null || usage.getInvalidRows() == null
                    || usage.getInvalidRows() != 0 || usage.getQuantity() < 0) {
                throw invalidParamException("货位库存异常，请先核对库存账");
            }
            if ((typeChanged || (statusChanged && value.getStatus() == 0)) && usage.getQuantity() != 0) {
                throw invalidParamException("货位仍有库存，不能停用或变更类型");
            }
            if (value.getMaxCapacity() != null && value.getMaxCapacity() < usage.getQuantity()) {
                throw invalidParamException("容量不能小于当前所有批次的占用总量（含冻结量）");
            }
        }
        try {
            requireUpdated(mapper.updateLocation(scope, request, actor()));
        } catch (DuplicateKeyException ex) {
            throw invalidParamException("货位编码已被使用（含已删除记录）");
        }
    }

    /**
     * Logically delete an empty, already disabled warehouse. Historical inventory or
     * document references also block deletion so audit joins never become orphaned.
     */
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public void deleteWarehouse(@NotNull @Positive Long id) {
        requireCompatibleTransaction();
        var scope = access.requireScope(null);
        var current = mapper.lockWarehouse(scope, id);
        if (current == null) throw exception(NOT_FOUND);
        if (!Objects.equals(current.getIsDefault(), 0)) {
            throw invalidParamException("默认仓不能删除，请先完成默认仓切换协调");
        }
        if (!Objects.equals(current.getStatus(), 0)) {
            throw invalidParamException("请先停用仓库后再删除");
        }
        if (mapper.hasWarehouseReferences(scope, id) || mapper.hasOpenWork(scope, id)) {
            throw invalidParamException("仓库存在库存、历史业务引用或未完成作业，不能删除");
        }
        requireUpdated(mapper.deleteWarehouse(scope, id, actor()));
    }

    /** Logically delete an empty, already disabled location while preserving audit history. */
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public void deleteLocation(@NotNull @Positive Long warehouseId, @NotNull @Positive Long id) {
        requireCompatibleTransaction();
        var scope = access.requireScope(null);
        var warehouse = mapper.lockWarehouse(scope, warehouseId);
        if (warehouse == null) throw exception(NOT_FOUND);
        var current = mapper.lockLocation(scope, warehouseId, id);
        if (current == null) throw exception(NOT_FOUND);
        if (!Objects.equals(current.getStatus(), 0)) {
            throw invalidParamException("请先停用货位后再删除");
        }
        if (mapper.hasLocationReferences(scope, warehouseId, id)) {
            throw invalidParamException("货位存在库存或历史业务引用，不能删除");
        }
        requireUpdated(mapper.deleteLocation(scope, warehouseId, id, actor()));
    }

    private static boolean matches(InventoryReadVO.Warehouse row, InventoryCatalogUpdateReqVO.WarehouseFields fields) {
        return Objects.equals(row.getWhCode(), fields.getWhCode()) && Objects.equals(row.getWhName(), fields.getWhName())
                && Objects.equals(row.getTempZone(), fields.getTempZone()) && Objects.equals(row.getStatus(), fields.getStatus());
    }
    private static boolean matches(InventoryReadVO.Location row, InventoryCatalogUpdateReqVO.LocationFields fields) {
        return Objects.equals(row.getLocationCode(), fields.getLocationCode()) && Objects.equals(row.getLocationType(), fields.getLocationType())
                && Objects.equals(row.getMaxCapacity(), fields.getMaxCapacity()) && Objects.equals(row.getStatus(), fields.getStatus());
    }
    private static String actor() { return String.valueOf(SecurityFrameworkUtils.getLoginUserId()); }
    private static void requireCompatibleTransaction() {
        // Spring may join an ambient transaction without applying this method's isolation.
        // Refuse incompatible ambient isolation instead of trusting a stale aggregate snapshot.
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && !Objects.equals(TransactionSynchronizationManager.getCurrentTransactionIsolationLevel(),
                    Isolation.READ_COMMITTED.value())) {
            throw new IllegalStateException("库存资料编辑要求独立的 READ_COMMITTED 管理事务");
        }
    }
    private static void requireUpdated(int rows) {
        if (rows != 1) throw new IllegalStateException("库存资料更新未影响唯一记录");
    }
}
