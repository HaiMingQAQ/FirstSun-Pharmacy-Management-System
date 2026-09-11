package cn.iocoder.yudao.module.pharmacy.service.purchase;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.supplier.SupplierSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.base.EmployeeDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.purchase.SupplierDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.purchase.SupplierLicenseDO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.purchase.SupplierLicenseMapper;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.purchase.SupplierMapper;
import cn.iocoder.yudao.module.pharmacy.enums.PharmacyStatusEnum;
import cn.iocoder.yudao.module.pharmacy.enums.SupplierApproveStatusEnum;
import cn.iocoder.yudao.module.pharmacy.service.base.EmployeeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PURCHASE_SUPPLIER_APPROVE_DUP;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PURCHASE_SUPPLIER_APPROVE_STATUS_INVALID;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PURCHASE_SUPPLIER_AUDITOR_NOT_EMPLOYEE;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PURCHASE_SUPPLIER_CODE_DUPLICATE;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PURCHASE_SUPPLIER_CREDIT_CODE_DUPLICATE;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PURCHASE_SUPPLIER_DISCOUNT_INVALID;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PURCHASE_SUPPLIER_HAS_ORDER;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PURCHASE_SUPPLIER_NOT_APPROVED;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PURCHASE_SUPPLIER_NOT_ENABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link SupplierServiceImpl} 单元测试
 *
 * 覆盖：编码/信用代码唯一性、折扣率范围、新建一律待审、首营审核状态机与审核人身份、删除保护（被订单引用）
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SupplierServiceImplTest {

    @Mock
    private SupplierMapper supplierMapper;
    @Mock
    private SupplierLicenseMapper supplierLicenseMapper;
    @Mock
    private EmployeeService employeeService;

    @InjectMocks
    private SupplierServiceImpl supplierService;

    @Test
    void createSupplier_shouldStartAsWaitApproveAndKeepDiscount() {
        when(supplierMapper.selectBySupplierCode("SUP001")).thenReturn(null);
        when(supplierMapper.selectByCreditCode("91330100MA2X")).thenReturn(null);

        supplierService.createSupplier(reqVO("SUP001", "91330100MA2X", "0.95"));

        ArgumentCaptor<SupplierDO> captor = ArgumentCaptor.forClass(SupplierDO.class);
        verify(supplierMapper).insert(captor.capture());
        SupplierDO saved = captor.getValue();
        // 新建一律「待审」，审核字段不允许由保存接口写入
        assertEquals(SupplierApproveStatusEnum.WAIT.getStatus(), saved.getApproveStatus());
        assertEquals(null, saved.getAuditBy());
        assertEquals(null, saved.getAuditAt());
        assertEquals(new BigDecimal("0.95"), saved.getDefaultDiscount());
    }

    @Test
    void createSupplier_shouldRejectDuplicateCode() {
        when(supplierMapper.selectBySupplierCode("SUP001")).thenReturn(supplier(9L, "SUP001", "X"));
        ServiceException ex = assertThrows(ServiceException.class,
                () -> supplierService.createSupplier(reqVO("SUP001", "91330100MA2X", "1.00")));
        assertEquals(PURCHASE_SUPPLIER_CODE_DUPLICATE.getCode(), ex.getCode());
    }

    @Test
    void createSupplier_shouldRejectDuplicateCreditCode() {
        when(supplierMapper.selectBySupplierCode("SUP001")).thenReturn(null);
        when(supplierMapper.selectByCreditCode("91330100MA2X")).thenReturn(supplier(9L, "SUP009", "91330100MA2X"));
        ServiceException ex = assertThrows(ServiceException.class,
                () -> supplierService.createSupplier(reqVO("SUP001", "91330100MA2X", "1.00")));
        assertEquals(PURCHASE_SUPPLIER_CREDIT_CODE_DUPLICATE.getCode(), ex.getCode());
    }

    @Test
    void createSupplier_shouldRejectDiscountOutOfRange() {
        when(supplierMapper.selectBySupplierCode("SUP001")).thenReturn(null);
        when(supplierMapper.selectByCreditCode("91330100MA2X")).thenReturn(null);
        ServiceException ex = assertThrows(ServiceException.class,
                () -> supplierService.createSupplier(reqVO("SUP001", "91330100MA2X", "1.20")));
        assertEquals(PURCHASE_SUPPLIER_DISCOUNT_INVALID.getCode(), ex.getCode());
    }

    @Test
    void deleteSupplier_shouldRejectWhenOrderExists() {
        when(supplierMapper.selectById(1L)).thenReturn(supplier(1L, "SUP001", "X"));
        when(supplierMapper.countOrdersBySupplierId(1L)).thenReturn(2L);

        ServiceException ex = assertThrows(ServiceException.class, () -> supplierService.deleteSupplier(1L));
        assertEquals(PURCHASE_SUPPLIER_HAS_ORDER.getCode(), ex.getCode());
        verify(supplierMapper, never()).deleteById(anyLong());
    }

    @Test
    void deleteSupplier_shouldRemoveLicensesTogether() {
        when(supplierMapper.selectById(1L)).thenReturn(supplier(1L, "SUP001", "X"));
        when(supplierMapper.countOrdersBySupplierId(1L)).thenReturn(0L);

        supplierService.deleteSupplier(1L);

        verify(supplierLicenseMapper).delete(any(LambdaQueryWrapperX.class));
        verify(supplierMapper).deleteById(1L);
    }

    @Test
    void validateSupplierPurchasable_shouldRejectDisabledAndUnapproved() {
        SupplierDO disabled = supplier(1L, "SUP001", "X");
        disabled.setStatus(PharmacyStatusEnum.DISABLE.getStatus());
        disabled.setApproveStatus(SupplierApproveStatusEnum.PASS.getStatus());
        when(supplierMapper.selectById(1L)).thenReturn(disabled);
        ServiceException ex1 = assertThrows(ServiceException.class, () -> supplierService.validateSupplierPurchasable(1L));
        assertEquals(PURCHASE_SUPPLIER_NOT_ENABLE.getCode(), ex1.getCode());

        SupplierDO wait = supplier(2L, "SUP002", "Y");
        wait.setStatus(PharmacyStatusEnum.ENABLE.getStatus());
        wait.setApproveStatus(SupplierApproveStatusEnum.WAIT.getStatus());
        when(supplierMapper.selectById(2L)).thenReturn(wait);
        ServiceException ex2 = assertThrows(ServiceException.class, () -> supplierService.validateSupplierPurchasable(2L));
        assertEquals(PURCHASE_SUPPLIER_NOT_APPROVED.getCode(), ex2.getCode());
    }

    @Test
    void approveSupplier_shouldRejectInvalidStatusAndDuplicateApprove() {
        ServiceException ex1 = assertThrows(ServiceException.class, () -> supplierService.approveSupplier(1L, 0, null));
        assertEquals(PURCHASE_SUPPLIER_APPROVE_STATUS_INVALID.getCode(), ex1.getCode());

        SupplierDO approved = supplier(1L, "SUP001", "X");
        approved.setApproveStatus(SupplierApproveStatusEnum.PASS.getStatus());
        when(supplierMapper.selectById(1L)).thenReturn(approved);
        ServiceException ex2 = assertThrows(ServiceException.class, () -> supplierService.approveSupplier(1L, 1, "again"));
        assertEquals(PURCHASE_SUPPLIER_APPROVE_DUP.getCode(), ex2.getCode());
    }

    @Test
    void approveSupplier_shouldRequireBoundEmployee() {
        SupplierDO wait = supplier(1L, "SUP001", "X");
        wait.setApproveStatus(SupplierApproveStatusEnum.WAIT.getStatus());
        when(supplierMapper.selectById(1L)).thenReturn(wait);
        when(employeeService.getEmployeeByUserId(anyLong())).thenReturn(null);

        try (MockedStatic<SecurityFrameworkUtils> mocked = Mockito.mockStatic(SecurityFrameworkUtils.class)) {
            mocked.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(100L);
            ServiceException ex = assertThrows(ServiceException.class, () -> supplierService.approveSupplier(1L, 1, "ok"));
            assertEquals(PURCHASE_SUPPLIER_AUDITOR_NOT_EMPLOYEE.getCode(), ex.getCode());
        }
    }

    @Test
    void approveSupplier_shouldRecordAuditorAndTime() {
        SupplierDO wait = supplier(1L, "SUP001", "X");
        wait.setApproveStatus(SupplierApproveStatusEnum.WAIT.getStatus());
        when(supplierMapper.selectById(1L)).thenReturn(wait);
        EmployeeDO employee = new EmployeeDO();
        employee.setId(407L);
        when(employeeService.getEmployeeByUserId(anyLong())).thenReturn(employee);

        try (MockedStatic<SecurityFrameworkUtils> mocked = Mockito.mockStatic(SecurityFrameworkUtils.class)) {
            mocked.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(100L);
            supplierService.approveSupplier(1L, 1, "资质齐全");

            ArgumentCaptor<SupplierDO> captor = ArgumentCaptor.forClass(SupplierDO.class);
            verify(supplierMapper).updateById(captor.capture());
            SupplierDO updated = captor.getValue();
            assertEquals(SupplierApproveStatusEnum.PASS.getStatus(), updated.getApproveStatus());
            assertEquals(407L, updated.getAuditBy());
            assertNotNull(updated.getAuditAt());
            assertEquals("资质齐全", updated.getAuditOpinion());
        }
    }

    private SupplierSaveReqVO reqVO(String code, String creditCode, String discount) {
        SupplierSaveReqVO vo = new SupplierSaveReqVO();
        vo.setSupplierCode(code);
        vo.setSupplierName("测试供应商");
        vo.setCreditCode(creditCode);
        vo.setDefaultDiscount(new BigDecimal(discount));
        vo.setStatus(PharmacyStatusEnum.ENABLE.getStatus());
        return vo;
    }

    private SupplierDO supplier(Long id, String code, String creditCode) {
        SupplierDO supplier = new SupplierDO();
        supplier.setId(id);
        supplier.setSupplierCode(code);
        supplier.setSupplierName("测试供应商");
        supplier.setCreditCode(creditCode);
        supplier.setStatus(PharmacyStatusEnum.ENABLE.getStatus());
        supplier.setApproveStatus(SupplierApproveStatusEnum.WAIT.getStatus());
        supplier.setAuditAt(LocalDateTime.of(2026, 9, 11, 10, 0));
        return supplier;
    }
}
