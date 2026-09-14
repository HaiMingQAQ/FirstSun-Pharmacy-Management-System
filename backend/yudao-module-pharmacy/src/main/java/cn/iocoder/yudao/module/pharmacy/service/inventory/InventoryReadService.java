package cn.iocoder.yudao.module.pharmacy.service.inventory;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryReadQuery;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryReadVO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.inventory.InventoryReadMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;

import static cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants.NOT_FOUND;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;

/** Count and rows use the same repeatable-read snapshot. No inventory write facade is registered. */
@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class InventoryReadService {
    private final InventoryReadAccess access;
    private final InventoryReadMapper mapper;

    public PageResult<InventoryReadVO.Warehouse> warehouses(@Valid InventoryReadQuery query) {
        var scope = access.requireScope(query.getStoreId());
        return new PageResult<>(mapper.selectWarehouses(scope, query), mapper.countWarehouses(scope, query));
    }

    public InventoryReadVO.Warehouse warehouse(long id) {
        var result = mapper.selectWarehouse(access.requireScope(null), id);
        if (result == null) { throw exception(NOT_FOUND); }
        return result;
    }

    public PageResult<InventoryReadVO.Location> locations(@Valid InventoryReadQuery query) {
        var scope = access.requireScope(query.getStoreId());
        return new PageResult<>(mapper.selectLocations(scope, query), mapper.countLocations(scope, query));
    }

    public InventoryReadVO.Location location(long id) {
        var query = new InventoryReadQuery(); query.setLocationId(id);
        var rows = mapper.selectLocations(access.requireScope(null), query);
        if (rows.isEmpty()) throw exception(NOT_FOUND);
        return rows.get(0);
    }

    public InventoryReadVO.Batch batch(long id) {
        var query = new InventoryReadQuery(); query.setBatchId(id);
        var rows = mapper.selectBatches(access.requireScope(null), query);
        if (rows.isEmpty()) throw exception(NOT_FOUND);
        return rows.get(0);
    }

    public InventoryReadVO.Flow flow(long id) {
        var query = new InventoryReadQuery(); query.setFlowId(id);
        var rows = mapper.selectFlows(access.requireScope(null), query);
        if (rows.isEmpty()) throw exception(NOT_FOUND);
        return rows.get(0);
    }

    public PageResult<InventoryReadVO.Batch> batches(@Valid InventoryReadQuery query) {
        var scope = access.requireScope(query.getStoreId());
        return new PageResult<>(mapper.selectBatches(scope, query), mapper.countBatches(scope, query));
    }

    public PageResult<InventoryReadVO.LocationStock> locationStock(@Valid InventoryReadQuery query) {
        var scope = access.requireScope(query.getStoreId());
        return new PageResult<>(mapper.selectLocationStock(scope, query), mapper.countLocationStock(scope, query));
    }

    public PageResult<InventoryReadVO.Flow> flows(@Valid InventoryReadQuery query) {
        var scope = access.requireScope(query.getStoreId());
        return new PageResult<>(mapper.selectFlows(scope, query), mapper.countFlows(scope, query));
    }
}
