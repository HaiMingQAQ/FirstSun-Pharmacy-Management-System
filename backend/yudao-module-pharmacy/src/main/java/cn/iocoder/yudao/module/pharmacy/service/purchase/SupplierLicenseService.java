package cn.iocoder.yudao.module.pharmacy.service.purchase;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.license.SupplierLicensePageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.license.SupplierLicenseSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.purchase.SupplierLicenseDO;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 供应商证照 Service
 *
 * @author B 成员
 */
public interface SupplierLicenseService {

    /**
     * 创建证照
     *
     * @return 证照编号
     */
    Long createLicense(@Valid SupplierLicenseSaveReqVO createReqVO);

    /**
     * 更新证照
     */
    void updateLicense(@Valid SupplierLicenseSaveReqVO updateReqVO);

    /**
     * 删除证照
     */
    void deleteLicense(Long id);

    /**
     * 获取证照详情
     */
    SupplierLicenseDO getLicense(Long id);

    /**
     * 获取证照分页
     */
    PageResult<SupplierLicenseDO> getLicensePage(SupplierLicensePageReqVO reqVO);

    /**
     * 获取某供应商的全部证照，按到期日升序
     */
    List<SupplierLicenseDO> getLicenseListBySupplierId(Long supplierId);

    /**
     * 获取即将到期（含已过期）的证照，用于证照到期提醒（ADM-002）
     *
     * @param days 提醒窗口天数，默认 30 天
     */
    List<SupplierLicenseDO> getExpiringLicenseList(Integer days);

    /**
     * 按当天日期刷新全部证照的有效/过期状态
     *
     * @return 变更行数
     */
    int refreshExpiredStatus();

    /**
     * 校验供应商具备有效的经营类证照（经营许可证或 GSP 证），用于采购收货入账前校验
     *
     * @param supplierId 供应商编号
     */
    void validateOperateLicenseValid(Long supplierId);

    /**
     * 校验证照存在
     */
    SupplierLicenseDO validateLicenseExists(Long id);

}
