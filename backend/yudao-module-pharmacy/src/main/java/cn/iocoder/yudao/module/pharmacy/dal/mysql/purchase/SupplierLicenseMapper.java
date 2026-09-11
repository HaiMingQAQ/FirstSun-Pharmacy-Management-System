package cn.iocoder.yudao.module.pharmacy.dal.mysql.purchase;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.license.SupplierLicensePageReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.purchase.SupplierLicenseDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;

/**
 * 供应商证照 Mapper
 *
 * @author B 成员
 */
@Mapper
public interface SupplierLicenseMapper extends BaseMapperX<SupplierLicenseDO> {

    default PageResult<SupplierLicenseDO> selectPage(SupplierLicensePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SupplierLicenseDO>()
                .eqIfPresent(SupplierLicenseDO::getSupplierId, reqVO.getSupplierId())
                .eqIfPresent(SupplierLicenseDO::getLicenseType, reqVO.getLicenseType())
                .likeIfPresent(SupplierLicenseDO::getLicenseNo, reqVO.getLicenseNo())
                .eqIfPresent(SupplierLicenseDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(SupplierLicenseDO::getExpireDate, reqVO.getExpireDate())
                .orderByAsc(SupplierLicenseDO::getExpireDate)
                .orderByDesc(SupplierLicenseDO::getId));
    }

    default List<SupplierLicenseDO> selectListBySupplierId(Long supplierId) {
        return selectList(new LambdaQueryWrapperX<SupplierLicenseDO>()
                .eq(SupplierLicenseDO::getSupplierId, supplierId)
                .orderByAsc(SupplierLicenseDO::getExpireDate));
    }

    default SupplierLicenseDO selectBySupplierIdAndLicenseNo(Long supplierId, String licenseNo) {
        return selectOne(new LambdaQueryWrapperX<SupplierLicenseDO>()
                .eq(SupplierLicenseDO::getSupplierId, supplierId)
                .eq(SupplierLicenseDO::getLicenseNo, licenseNo));
    }

    /**
     * 查询即将到期（含已过期）的证照，用于 ADM-002 证照到期提醒
     *
     * @param deadline 到期日上限（含当天）
     * @param status   证照状态，null 表示不限
     */
    default List<SupplierLicenseDO> selectExpiringList(LocalDate deadline, Integer status) {
        // 注意：MyBatis-Plus 原生方法（le/ge/lt/eq 等）返回的是 LambdaQueryWrapper，
        // 会丢失 LambdaQueryWrapperX 的扩展方法，因此必须在最后才调用原生方法
        return selectList(new LambdaQueryWrapperX<SupplierLicenseDO>()
                .eqIfPresent(SupplierLicenseDO::getStatus, status)
                .le(SupplierLicenseDO::getExpireDate, deadline)
                .orderByAsc(SupplierLicenseDO::getExpireDate));
    }

    default Long countBySupplierId(Long supplierId) {
        return selectCount(SupplierLicenseDO::getSupplierId, supplierId);
    }

    /**
     * 批量刷新证照状态：到期日早于指定日期的置为过期(0)，否则置为有效(1)
     *
     * @return 受影响行数
     */
    default int updateStatusByExpireDate(LocalDate today) {
        SupplierLicenseDO expired = new SupplierLicenseDO();
        expired.setStatus(0);
        int expiredRows = update(expired, new LambdaQueryWrapperX<SupplierLicenseDO>()
                .lt(SupplierLicenseDO::getExpireDate, today)
                .ne(SupplierLicenseDO::getStatus, 0));

        SupplierLicenseDO valid = new SupplierLicenseDO();
        valid.setStatus(1);
        int validRows = update(valid, new LambdaQueryWrapperX<SupplierLicenseDO>()
                .ge(SupplierLicenseDO::getExpireDate, today)
                .ne(SupplierLicenseDO::getStatus, 1));
        return expiredRows + validRows;
    }

}
