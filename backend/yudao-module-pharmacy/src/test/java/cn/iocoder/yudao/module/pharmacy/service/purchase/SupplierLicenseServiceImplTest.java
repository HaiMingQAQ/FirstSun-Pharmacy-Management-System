package cn.iocoder.yudao.module.pharmacy.service.purchase;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.license.SupplierLicenseSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.purchase.SupplierDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.purchase.SupplierLicenseDO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.purchase.SupplierLicenseMapper;
import cn.iocoder.yudao.module.pharmacy.enums.SupplierLicenseTypeEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PURCHASE_LICENSE_DATE_INVALID;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PURCHASE_LICENSE_EXPIRED;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PURCHASE_LICENSE_NO_DUPLICATE;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PURCHASE_LICENSE_OPERATE_MISSING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * {@link SupplierLicenseServiceImpl} 单元测试
 *
 * 覆盖：证照日期校验、同一供应商证照号唯一、状态按到期日自动计算、
 * 以及收货入账前的「经营类证照有效性」校验（REG-012：缺证照 / 已过期都要拦截）
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SupplierLicenseServiceImplTest {

    @Mock
    private SupplierLicenseMapper supplierLicenseMapper;
    @Mock
    private SupplierService supplierService;

    @InjectMocks
    private SupplierLicenseServiceImpl licenseService;

    @Test
    void createLicense_shouldRejectExpireBeforeIssue() {
        SupplierLicenseSaveReqVO vo = reqVO(SupplierLicenseTypeEnum.BUSINESS_LICENSE.getType(),
                LocalDate.of(2026, 9, 11), LocalDate.of(2026, 1, 1));
        ServiceException ex = assertThrows(ServiceException.class, () -> licenseService.createLicense(vo));
        assertEquals(PURCHASE_LICENSE_DATE_INVALID.getCode(), ex.getCode());
    }

    @Test
    void createLicense_shouldRejectDuplicateLicenseNo() {
        SupplierLicenseSaveReqVO vo = reqVO(SupplierLicenseTypeEnum.BUSINESS_LICENSE.getType(),
                LocalDate.of(2026, 9, 11), LocalDate.of(2028, 9, 11));
        when(supplierLicenseMapper.selectBySupplierIdAndLicenseNo(1L, "浙AA1234567"))
                .thenReturn(license(9L, SupplierLicenseTypeEnum.BUSINESS_LICENSE.getType(), LocalDate.of(2028, 9, 11)));
        ServiceException ex = assertThrows(ServiceException.class, () -> licenseService.createLicense(vo));
        assertEquals(PURCHASE_LICENSE_NO_DUPLICATE.getCode(), ex.getCode());
    }

    @Test
    void createLicense_shouldComputeStatusFromExpireDate() {
        SupplierLicenseSaveReqVO valid = reqVO(SupplierLicenseTypeEnum.BUSINESS_LICENSE.getType(),
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
        licenseService.createLicense(valid);

        // 已过期：状态自动为 0（前端不回传状态）
        SupplierLicenseSaveReqVO expired = reqVO(SupplierLicenseTypeEnum.GSP_LICENSE.getType(),
                LocalDate.now().minusYears(3), LocalDate.now().minusDays(1));
        licenseService.createLicense(expired);

        ArgumentCaptor<SupplierLicenseDO> captor = ArgumentCaptor.forClass(SupplierLicenseDO.class);
        org.mockito.Mockito.verify(supplierLicenseMapper, org.mockito.Mockito.times(2)).insert(captor.capture());
        assertEquals(1, captor.getAllValues().get(0).getStatus(), "未到期应为有效(1)");
        assertEquals(0, captor.getAllValues().get(1).getStatus(), "已过期应为过期(0)");
    }

    @Test
    void validateOperateLicenseValid_shouldRejectWhenNoOperateLicense() {
        when(supplierService.validateSupplierExists(1L)).thenReturn(supplier(1L));
        // 只有营业执照，没有经营许可证/GSP 证
        when(supplierLicenseMapper.selectListBySupplierId(1L)).thenReturn(
                List.of(license(1L, SupplierLicenseTypeEnum.LICENSE.getType(), LocalDate.now().plusYears(1))));

        ServiceException ex = assertThrows(ServiceException.class, () -> licenseService.validateOperateLicenseValid(1L));
        assertEquals(PURCHASE_LICENSE_OPERATE_MISSING.getCode(), ex.getCode());
    }

    @Test
    void validateOperateLicenseValid_shouldRejectExpiredOperateLicense() {
        when(supplierService.validateSupplierExists(1L)).thenReturn(supplier(1L));
        when(supplierLicenseMapper.selectListBySupplierId(1L)).thenReturn(
                List.of(license(1L, SupplierLicenseTypeEnum.BUSINESS_LICENSE.getType(), LocalDate.now().minusDays(1))));

        ServiceException ex = assertThrows(ServiceException.class, () -> licenseService.validateOperateLicenseValid(1L));
        assertEquals(PURCHASE_LICENSE_EXPIRED.getCode(), ex.getCode());
    }

    @Test
    void validateOperateLicenseValid_shouldPassWithValidGspLicense() {
        when(supplierService.validateSupplierExists(1L)).thenReturn(supplier(1L));
        when(supplierLicenseMapper.selectListBySupplierId(1L)).thenReturn(
                List.of(license(1L, SupplierLicenseTypeEnum.GSP_LICENSE.getType(), LocalDate.now().plusDays(1))));

        // 不抛异常即通过
        licenseService.validateOperateLicenseValid(1L);
    }

    @Test
    void validateOperateLicenseValid_shouldRejectEmptyLicenseList() {
        when(supplierService.validateSupplierExists(1L)).thenReturn(supplier(1L));
        when(supplierLicenseMapper.selectListBySupplierId(1L)).thenReturn(Collections.emptyList());

        ServiceException ex = assertThrows(ServiceException.class, () -> licenseService.validateOperateLicenseValid(1L));
        assertEquals(PURCHASE_LICENSE_OPERATE_MISSING.getCode(), ex.getCode());
    }

    @Test
    void getExpiringLicenseList_shouldDefaultTo30Days() {
        when(supplierLicenseMapper.selectExpiringList(any(LocalDate.class), any())).thenReturn(Collections.emptyList());
        licenseService.getExpiringLicenseList(null);
        LocalDate expected = LocalDate.now().plusDays(30);
        org.mockito.Mockito.verify(supplierLicenseMapper).selectExpiringList(expected, null);
    }

    private SupplierLicenseSaveReqVO reqVO(Integer type, LocalDate issue, LocalDate expire) {
        SupplierLicenseSaveReqVO vo = new SupplierLicenseSaveReqVO();
        vo.setSupplierId(1L);
        vo.setLicenseType(type);
        vo.setLicenseNo("浙AA1234567");
        vo.setIssueDate(issue);
        vo.setExpireDate(expire);
        return vo;
    }

    private SupplierLicenseDO license(Long id, Integer type, LocalDate expire) {
        SupplierLicenseDO license = new SupplierLicenseDO();
        license.setId(id);
        license.setSupplierId(1L);
        license.setLicenseType(type);
        license.setLicenseNo("浙AA1234567");
        license.setExpireDate(expire);
        return license;
    }

    private SupplierDO supplier(Long id) {
        SupplierDO supplier = new SupplierDO();
        supplier.setId(id);
        supplier.setSupplierName("测试供应商");
        return supplier;
    }
}
