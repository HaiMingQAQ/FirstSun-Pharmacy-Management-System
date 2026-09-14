package cn.iocoder.yudao.module.pharmacy.service.inventory;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryReconciliationQuery;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryReconciliationVO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.inventory.InventoryReconciliationMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Objects;

/** Consistent read-only comparison of batch, location, flow and active-lock ledgers. */
@Service
@Validated
@RequiredArgsConstructor
public class InventoryReconciliationService {
    private final InventoryReadAccess access;
    private final InventoryReconciliationMapper mapper;

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public PageResult<InventoryReconciliationVO.Row> page(@Valid InventoryReconciliationQuery query) {
        var scope = access.requireScope(null);
        List<InventoryReconciliationVO.Row> rows = list(scope, query);
        return new PageResult<>(rows, mapper.countRows(scope, query));
    }

    /** Export uses the same authenticated store scope and repeatable-read projection as page query. */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public List<InventoryReconciliationVO.Row> export(@Valid InventoryReconciliationQuery query) {
        query.setPageSize(cn.iocoder.yudao.framework.common.pojo.PageParam.PAGE_SIZE_NONE);
        return list(access.requireScope(null), query);
    }

    private List<InventoryReconciliationVO.Row> list(InventoryReadAccess.Scope scope,
                                                     InventoryReconciliationQuery query) {
        return mapper.selectRows(scope, query).stream()
                .map(InventoryReconciliationService::project).toList();
    }

    private static InventoryReconciliationVO.Row project(InventoryReconciliationMapper.Row source) {
        var row = new InventoryReconciliationVO.Row();
        row.setBatchId(source.getBatchId());
        row.setWarehouseId(source.getWarehouseId());
        row.setDrugId(source.getDrugId());
        row.setBatchNo(source.getBatchNo());
        row.setBatchTotal(asInt(source.getBatchTotal()));
        row.setBatchAvail(asInt(source.getBatchAvail()));
        row.setBatchFrozen(asInt(source.getBatchFrozen()));
        row.setLocationTotal(asInt(source.getLocationTotal()));
        row.setLocationFrozen(asInt(source.getLocationFrozen()));
        row.setInvalidLocationRows(asInt(source.getInvalidLocationRows()));
        row.setFlowNet(asInt(source.getFlowNet()));
        row.setOpeningFlowCount(asInt(source.getOpeningFlowCount()));
        row.setActiveLockQty(asInt(source.getActiveLockQty()));
        row.setLastFlowId(source.getLastFlowId());
        row.setLocationMatches(Objects.equals(row.getBatchTotal(), row.getLocationTotal())
                && Objects.equals(row.getBatchFrozen(), row.getLocationFrozen())
                && Objects.equals(row.getInvalidLocationRows(), 0));
        row.setFlowMatches(Objects.equals(row.getBatchTotal(), row.getFlowNet()));
        row.setFrozenMatches(Objects.equals(row.getBatchFrozen(), row.getActiveLockQty()));
        row.setOpeningBaselinePresent(row.getOpeningFlowCount() != null && row.getOpeningFlowCount() > 0);
        row.setDifference(!Boolean.TRUE.equals(row.getLocationMatches())
                || !Boolean.TRUE.equals(row.getFlowMatches())
                || !Boolean.TRUE.equals(row.getFrozenMatches())
                || !Boolean.TRUE.equals(row.getOpeningBaselinePresent()));
        return row;
    }

    private static int asInt(Long value) {
        if (value == null) return 0;
        if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
            throw new IllegalStateException("库存对账数量超出数据库 INT 范围");
        }
        return value.intValue();
    }
}
