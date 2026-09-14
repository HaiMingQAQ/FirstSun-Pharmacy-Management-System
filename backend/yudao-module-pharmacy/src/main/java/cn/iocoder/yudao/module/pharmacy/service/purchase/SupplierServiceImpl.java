package cn.iocoder.yudao.module.pharmacy.service.purchase;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.supplier.SupplierPageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.supplier.SupplierSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.base.EmployeeDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.purchase.SupplierDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.purchase.SupplierLicenseDO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.purchase.SupplierLicenseMapper;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.purchase.SupplierMapper;
import cn.iocoder.yudao.module.pharmacy.enums.PharmacyStatusEnum;
import cn.iocoder.yudao.module.pharmacy.enums.SupplierApproveStatusEnum;
import cn.iocoder.yudao.module.pharmacy.service.base.EmployeeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.*;

/**
 * 供应商 Service 实现类
 *
 * @author B 成员
 */
@Service
@Validated
public class SupplierServiceImpl implements SupplierService {

    /**
     * 默认折扣率缺省值（与建表脚本默认值一致）
     */
    private static final BigDecimal DEFAULT_DISCOUNT = BigDecimal.ONE;

    /**
     * 折扣率下限
     */
    private static final BigDecimal DISCOUNT_MIN = BigDecimal.ZERO;

    /**
     * 折扣率上限
     */
    private static final BigDecimal DISCOUNT_MAX = BigDecimal.ONE;

    @Resource
    private SupplierMapper supplierMapper;

    @Resource
    private SupplierLicenseMapper supplierLicenseMapper;

    /**
     * 审核人身份解析：与药品审核保持一致，单向依赖基础资料域的 EmployeeService
     */
    @Resource
    private EmployeeService employeeService;

    @Override
    public Long createSupplier(SupplierSaveReqVO createReqVO) {
        validateSupplierCodeUnique(null, createReqVO.getSupplierCode());
        validateCreditCodeUnique(null, createReqVO.getCreditCode());
        validateDiscount(createReqVO.getDefaultDiscount());
        // 写入
        SupplierDO supplier = BeanUtils.toBean(createReqVO, SupplierDO.class);
        if (supplier.getDefaultDiscount() == null) {
            supplier.setDefaultDiscount(DEFAULT_DISCOUNT);
        }
        // 新建供应商一律从「待审」开始，审核状态只能由审核接口维护
        supplier.setApproveStatus(SupplierApproveStatusEnum.WAIT.getStatus());
        supplier.setAuditBy(null);
        supplier.setAuditAt(null);
        supplier.setAuditOpinion(null);
        supplierMapper.insert(supplier);
        return supplier.getId();
    }

    @Override
    public void updateSupplier(SupplierSaveReqVO updateReqVO) {
        validateSupplierExists(updateReqVO.getId());
        validateSupplierCodeUnique(updateReqVO.getId(), updateReqVO.getSupplierCode());
        validateCreditCodeUnique(updateReqVO.getId(), updateReqVO.getCreditCode());
        validateDiscount(updateReqVO.getDefaultDiscount());
        // 更新：SaveReqVO 不含审核字段，BeanUtils 不会覆盖 approveStatus/auditBy/auditAt/auditOpinion
        SupplierDO updateObj = BeanUtils.toBean(updateReqVO, SupplierDO.class);
        supplierMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSupplier(Long id) {
        validateSupplierExists(id);
        // 被采购订单引用时不可删除，避免历史单据丢失供应商标识
        Long orderCount = supplierMapper.countOrdersBySupplierId(id);
        if (orderCount != null && orderCount > 0) {
            throw exception(PURCHASE_SUPPLIER_HAS_ORDER);
        }
        // 证照属于供应商的从属数据，随供应商一并逻辑删除（同一事务）
        supplierLicenseMapper.delete(new LambdaQueryWrapperX<SupplierLicenseDO>()
                .eq(SupplierLicenseDO::getSupplierId, id));
        supplierMapper.deleteById(id);
    }

    @Override
    public SupplierDO getSupplier(Long id) {
        return supplierMapper.selectById(id);
    }

    @Override
    public PageResult<SupplierDO> getSupplierPage(SupplierPageReqVO reqVO) {
        return supplierMapper.selectPage(reqVO);
    }

    @Override
    public List<SupplierDO> getSimpleSupplierList(String keyword) {
        return supplierMapper.selectSimpleList(keyword);
    }

    @Override
    public List<SupplierDO> getAllSupplierList() {
        return supplierMapper.selectList(new LambdaQueryWrapperX<SupplierDO>()
                .orderByAsc(SupplierDO::getSupplierCode));
    }

    @Override
    public List<SupplierDO> getSupplierList(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return supplierMapper.selectBatchIds(ids);
    }

    @Override
    public SupplierDO validateSupplierExists(Long id) {
        if (id == null) {
            throw exception(PURCHASE_SUPPLIER_NOT_EXISTS);
        }
        SupplierDO supplier = supplierMapper.selectById(id);
        if (supplier == null) {
            throw exception(PURCHASE_SUPPLIER_NOT_EXISTS);
        }
        return supplier;
    }

    @Override
    public SupplierDO validateSupplierPurchasable(Long id) {
        SupplierDO supplier = validateSupplierExists(id);
        if (!PharmacyStatusEnum.isEnable(supplier.getStatus())) {
            throw exception(PURCHASE_SUPPLIER_NOT_ENABLE, supplier.getSupplierName());
        }
        if (!SupplierApproveStatusEnum.isPass(supplier.getApproveStatus())) {
            throw exception(PURCHASE_SUPPLIER_NOT_APPROVED, supplier.getSupplierName());
        }
        return supplier;
    }

    @Override
    public void approveSupplier(Long id, Integer approveStatus, String auditOpinion) {
        // 校验审核入参：只能为 1(通过) 或 2(驳回)
        if (!SupplierApproveStatusEnum.isValidAuditStatus(approveStatus)) {
            throw exception(PURCHASE_SUPPLIER_APPROVE_STATUS_INVALID);
        }
        SupplierDO supplier = validateSupplierExists(id);
        // 仅允许从待审(0)流转到通过/驳回，防止重复审核
        if (!SupplierApproveStatusEnum.isWait(supplier.getApproveStatus())) {
            throw exception(PURCHASE_SUPPLIER_APPROVE_DUP);
        }
        // 解析审核人：当前登录 system 用户对应的药店员工
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        EmployeeDO employee = employeeService.getEmployeeByUserId(loginUserId);
        if (employee == null) {
            throw exception(PURCHASE_SUPPLIER_AUDITOR_NOT_EMPLOYEE);
        }
        SupplierDO update = new SupplierDO();
        update.setId(id);
        update.setApproveStatus(approveStatus);
        update.setAuditBy(employee.getId());
        update.setAuditAt(LocalDateTime.now());
        update.setAuditOpinion(auditOpinion);
        supplierMapper.updateById(update);
    }

    @Override
    public Long countOrdersBySupplier(Long supplierId) {
        return supplierMapper.countOrdersBySupplierId(supplierId);
    }

    private void validateSupplierCodeUnique(Long id, String supplierCode) {
        SupplierDO supplier = supplierMapper.selectBySupplierCode(supplierCode);
        if (supplier == null) {
            return;
        }
        if (id == null || !Objects.equals(supplier.getId(), id)) {
            throw exception(PURCHASE_SUPPLIER_CODE_DUPLICATE);
        }
    }

    private void validateCreditCodeUnique(Long id, String creditCode) {
        if (creditCode == null || creditCode.isEmpty()) {
            return;
        }
        SupplierDO supplier = supplierMapper.selectByCreditCode(creditCode);
        if (supplier == null) {
            return;
        }
        if (id == null || !Objects.equals(supplier.getId(), id)) {
            throw exception(PURCHASE_SUPPLIER_CREDIT_CODE_DUPLICATE);
        }
    }

    private void validateDiscount(BigDecimal discount) {
        if (discount == null) {
            return;
        }
        if (discount.compareTo(DISCOUNT_MIN) < 0 || discount.compareTo(DISCOUNT_MAX) > 0) {
            throw exception(PURCHASE_SUPPLIER_DISCOUNT_INVALID);
        }
    }

}
