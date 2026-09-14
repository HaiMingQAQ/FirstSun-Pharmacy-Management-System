package cn.iocoder.yudao.module.pharmacy.service.purchase;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.license.SupplierLicensePageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.license.SupplierLicenseSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.purchase.SupplierDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.purchase.SupplierLicenseDO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.purchase.SupplierLicenseMapper;
import cn.iocoder.yudao.module.pharmacy.enums.SupplierLicenseTypeEnum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.*;

/**
 * 供应商证照 Service 实现类
 *
 * 证照状态（1有效/0过期）不在前端传入，统一由到期日与当天比较后写入，避免前后端状态不一致。
 *
 * @author B 成员
 */
@Service
@Validated
public class SupplierLicenseServiceImpl implements SupplierLicenseService {

    /**
     * 默认到期提醒窗口（天）
     */
    private static final int DEFAULT_EXPIRE_WARNING_DAYS = 30;

    @Resource
    private SupplierLicenseMapper supplierLicenseMapper;

    @Resource
    private SupplierService supplierService;

    @Override
    public Long createLicense(SupplierLicenseSaveReqVO createReqVO) {
        validateDateRange(createReqVO);
        validateSupplier(createReqVO.getSupplierId());
        validateLicenseNoUnique(null, createReqVO.getSupplierId(), createReqVO.getLicenseNo());
        // 写入：状态按到期日计算
        SupplierLicenseDO license = BeanUtils.toBean(createReqVO, SupplierLicenseDO.class);
        license.setStatus(calcStatus(license.getExpireDate()));
        supplierLicenseMapper.insert(license);
        return license.getId();
    }

    @Override
    public void updateLicense(SupplierLicenseSaveReqVO updateReqVO) {
        validateLicenseExists(updateReqVO.getId());
        validateDateRange(updateReqVO);
        validateSupplier(updateReqVO.getSupplierId());
        validateLicenseNoUnique(updateReqVO.getId(), updateReqVO.getSupplierId(), updateReqVO.getLicenseNo());
        // 更新：状态随到期日重新计算
        SupplierLicenseDO updateObj = BeanUtils.toBean(updateReqVO, SupplierLicenseDO.class);
        updateObj.setStatus(calcStatus(updateObj.getExpireDate()));
        supplierLicenseMapper.updateById(updateObj);
    }

    @Override
    public void deleteLicense(Long id) {
        validateLicenseExists(id);
        supplierLicenseMapper.deleteById(id);
    }

    @Override
    public SupplierLicenseDO getLicense(Long id) {
        return supplierLicenseMapper.selectById(id);
    }

    @Override
    public PageResult<SupplierLicenseDO> getLicensePage(SupplierLicensePageReqVO reqVO) {
        return supplierLicenseMapper.selectPage(reqVO);
    }

    @Override
    public List<SupplierLicenseDO> getLicenseListBySupplierId(Long supplierId) {
        return supplierLicenseMapper.selectListBySupplierId(supplierId);
    }

    @Override
    public List<SupplierLicenseDO> getExpiringLicenseList(Integer days) {
        int warningDays = days == null || days <= 0 ? DEFAULT_EXPIRE_WARNING_DAYS : days;
        return supplierLicenseMapper.selectExpiringList(LocalDate.now().plusDays(warningDays), null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int refreshExpiredStatus() {
        return supplierLicenseMapper.updateStatusByExpireDate(LocalDate.now());
    }

    @Override
    public void validateOperateLicenseValid(Long supplierId) {
        SupplierDO supplier = supplierService.validateSupplierExists(supplierId);
        List<SupplierLicenseDO> licenses = supplierLicenseMapper.selectListBySupplierId(supplierId);
        LocalDate today = LocalDate.now();
        // 经营类证照（经营许可证 / GSP 证）中，至少一张在有效期内
        List<SupplierLicenseDO> operateLicenses = licenses.stream()
                .filter(license -> SupplierLicenseTypeEnum.isOperateLicense(license.getLicenseType()))
                .collect(Collectors.toList());
        if (operateLicenses.isEmpty()) {
            throw exception(PURCHASE_LICENSE_OPERATE_MISSING, supplier.getSupplierName());
        }
        boolean hasValid = operateLicenses.stream()
                .anyMatch(license -> license.getExpireDate() != null && !license.getExpireDate().isBefore(today));
        if (!hasValid) {
            String licenseNo = operateLicenses.get(0).getLicenseNo();
            throw exception(PURCHASE_LICENSE_EXPIRED, licenseNo);
        }
    }

    @Override
    public SupplierLicenseDO validateLicenseExists(Long id) {
        if (id == null) {
            throw exception(PURCHASE_LICENSE_NOT_EXISTS);
        }
        SupplierLicenseDO license = supplierLicenseMapper.selectById(id);
        if (license == null) {
            throw exception(PURCHASE_LICENSE_NOT_EXISTS);
        }
        return license;
    }

    private void validateSupplier(Long supplierId) {
        if (supplierId == null) {
            throw exception(PURCHASE_LICENSE_SUPPLIER_NOT_EXISTS);
        }
        // 复用供应商存在性校验，供应商不存在时抛出供应商业务的错误码
        supplierService.validateSupplierExists(supplierId);
    }

    private void validateDateRange(SupplierLicenseSaveReqVO reqVO) {
        if (reqVO.getIssueDate() != null && reqVO.getExpireDate() != null
                && reqVO.getExpireDate().isBefore(reqVO.getIssueDate())) {
            throw exception(PURCHASE_LICENSE_DATE_INVALID);
        }
    }

    private void validateLicenseNoUnique(Long id, Long supplierId, String licenseNo) {
        SupplierLicenseDO license = supplierLicenseMapper.selectBySupplierIdAndLicenseNo(supplierId, licenseNo);
        if (license == null) {
            return;
        }
        if (id == null || !Objects.equals(license.getId(), id)) {
            throw exception(PURCHASE_LICENSE_NO_DUPLICATE);
        }
    }

    /**
     * 按到期日计算证照状态：到期日早于当天为过期(0)，否则有效(1)
     */
    private Integer calcStatus(LocalDate expireDate) {
        if (expireDate == null) {
            return 0;
        }
        return expireDate.isBefore(LocalDate.now()) ? 0 : 1;
    }

}
