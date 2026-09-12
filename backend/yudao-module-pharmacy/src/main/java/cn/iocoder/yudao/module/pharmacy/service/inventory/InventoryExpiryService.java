package cn.iocoder.yudao.module.pharmacy.service.inventory;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryExpiryHandleReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryExpiryQuery;
import cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo.InventoryExpiryVO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.inventory.InventoryExpiryMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants.NOT_FOUND;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

/** Daily expiry warning lifecycle. It never changes stock quantities or sale eligibility. */
@Service
@Validated
@RequiredArgsConstructor
public class InventoryExpiryService {
    private final InventoryReadAccess access;
    private final InventoryExpiryMapper mapper;

    @Transactional(readOnly = true)
    public PageResult<InventoryExpiryVO.Alert> page(@Valid InventoryExpiryQuery query) {
        var scope = access.requireScope(null);
        return new PageResult<>(mapper.selectAlerts(scope, query), mapper.countAlerts(scope, query));
    }

    @Transactional(rollbackFor = Exception.class)
    public RefreshResult refresh() {
        var scope = access.requireScope(null);
        LocalDate today = LocalDate.now();
        int affected = mapper.refreshDaily(scope, today, actor());
        return new RefreshResult(today, affected);
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(@NotNull @Valid InventoryExpiryHandleReqVO request) {
        var scope = access.requireScope(null);
        Integer old = mapper.selectHandleType(scope, request.getId());
        if (old == null) throw exception(NOT_FOUND);
        if (Objects.equals(old, request.getHandleType())) return;
        if (old != 0) throw invalidParamException("该效期预警已处理，不能重复修改");
        if (mapper.updateHandle(scope, request.getId(), request.getHandleType(), operator()) != 1) {
            throw invalidParamException("效期预警状态已变化，请刷新后重试");
        }
    }

    private static String actor() {
        return String.valueOf(SecurityFrameworkUtils.getLoginUserId());
    }

    private static long operator() {
        Long id = SecurityFrameworkUtils.getLoginUserId();
        if (id == null || id <= 0) throw invalidParamException("缺少有效操作人");
        return id;
    }

    public record RefreshResult(LocalDate alertDate, int affectedRows) { }
}
