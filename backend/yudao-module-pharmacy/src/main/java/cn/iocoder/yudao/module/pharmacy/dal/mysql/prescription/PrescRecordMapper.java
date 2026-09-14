package cn.iocoder.yudao.module.pharmacy.dal.mysql.prescription;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.pharmacy.controller.admin.prescription.vo.PrescRecordPageReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.prescription.PhPrescRecordDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 处方记录 Mapper（E 维护）
 */
@Mapper
public interface PrescRecordMapper extends BaseMapperX<PhPrescRecordDO> {

    default PhPrescRecordDO selectByPrescNo(String prescNo) {
        return selectOne(PhPrescRecordDO::getPrescNo, prescNo);
    }

    default PageResult<PhPrescRecordDO> selectPage(PrescRecordPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<PhPrescRecordDO>()
                .likeIfPresent(PhPrescRecordDO::getPrescNo, reqVO.getPrescNo())
                .eqIfPresent(PhPrescRecordDO::getStoreId, reqVO.getStoreId())
                .eqIfPresent(PhPrescRecordDO::getSource, reqVO.getSource())
                .eqIfPresent(PhPrescRecordDO::getReviewStatus, reqVO.getReviewStatus())
                .eqIfPresent(PhPrescRecordDO::getStatus, reqVO.getStatus())
                .likeIfPresent(PhPrescRecordDO::getPatientName, reqVO.getPatientName())
                .eqIfPresent(PhPrescRecordDO::getPharmacistId, reqVO.getPharmacistId())
                .betweenIfPresent(PhPrescRecordDO::getPrescDate, reqVO.getPrescDate())
                .orderByDesc(PhPrescRecordDO::getCreateTime));
    }

    /**
     * 查询某门店某日期已使用的最大流水号（处方号后缀）
     * 处方号格式：PX-{storeId}-{yyyyMMdd}-{4位流水}
     */
    @Select("SELECT MAX(CAST(SUBSTRING_INDEX(presc_no, '-', -1) AS UNSIGNED)) " +
            "FROM ph_presc_record " +
            "WHERE store_id = #{storeId} AND presc_no LIKE CONCAT('PX-', #{storeId}, '-', #{yyyymmdd}, '-%') AND deleted = 0")
    Integer selectMaxSeqByDate(@Param("storeId") Long storeId, @Param("yyyymmdd") String yyyymmdd);
}
