package cn.iocoder.yudao.module.pharmacy.service.inventory;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryCatalogCreateReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.inventory.InventoryCatalogMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants.NOT_FOUND;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.*;

/** Catalog creation only. No stock changes or unapproved default-warehouse switch. */
@Service
@Validated
@RequiredArgsConstructor
public class InventoryCatalogService {
    private final InventoryReadAccess access;
    private final InventoryCatalogMapper mapper;

    @Transactional(rollbackFor = Exception.class)
    public long createWarehouse(@NotNull @Valid InventoryCatalogCreateReqVO.Warehouse request) {
        var scope = access.requireScope(null);
        var key = new InventoryCatalogMapper.GeneratedKey();
        try {
            return inserted(mapper.insertWarehouse(scope, request, actor(), key), key);
        } catch (DuplicateKeyException ex) {
            // Throw immediately: no reads/writes or pretend-success after a duplicate failure.
            throw invalidParamException("仓库编码已被使用（含已删除记录），请使用其他编码");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public long createLocation(@NotNull @Valid InventoryCatalogCreateReqVO.Location request) {
        var scope = access.requireScope(null);
        Integer status = mapper.lockWarehouseStatus(scope, request.getWarehouseId());
        if (status == null) { throw exception(NOT_FOUND); }
        if (status != 1) { throw invalidParamException("所属仓库未启用，不能新增货位"); }
        var key = new InventoryCatalogMapper.GeneratedKey();
        try {
            return inserted(mapper.insertLocation(scope, request, actor(), key), key);
        } catch (DuplicateKeyException ex) {
            throw invalidParamException("该仓库货位编码已被使用（含已删除记录），请使用其他编码");
        }
    }

    private static String actor() {
        return String.valueOf(SecurityFrameworkUtils.getLoginUserId());
    }

    private static long inserted(int rows, InventoryCatalogMapper.GeneratedKey key) {
        if (rows != 1 || key.getId() == null || key.getId() <= 0) {
            throw new IllegalStateException("库存资料新增未返回唯一有效主键");
        }
        return key.getId();
    }
}
