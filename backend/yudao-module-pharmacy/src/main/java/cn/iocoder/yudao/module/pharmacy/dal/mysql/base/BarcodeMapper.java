package cn.iocoder.yudao.module.pharmacy.dal.mysql.base;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.barcode.BarcodePageReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.base.BarcodeDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 药品条码 Mapper
 */
@Mapper
public interface BarcodeMapper extends BaseMapperX<BarcodeDO> {

    default PageResult<BarcodeDO> selectPage(BarcodePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<BarcodeDO>()
                .eqIfPresent(BarcodeDO::getDrugId, reqVO.getDrugId())
                .likeIfPresent(BarcodeDO::getBarcode, reqVO.getBarcode())
                .eqIfPresent(BarcodeDO::getBarcodeType, reqVO.getBarcodeType())
                .eqIfPresent(BarcodeDO::getIsDefault, reqVO.getIsDefault())
                .orderByDesc(BarcodeDO::getId));
    }

    default BarcodeDO selectByBarcode(String barcode) {
        return selectOne(BarcodeDO::getBarcode, barcode);
    }

    /**
     * 同一药品下的默认条码（is_default=1）
     */
    default BarcodeDO selectDefaultByDrugId(Long drugId) {
        return selectOne((new LambdaQueryWrapperX<BarcodeDO>()
                .eq(BarcodeDO::getDrugId, drugId)
                .eq(BarcodeDO::getIsDefault, 1)));
    }

    /**
     * 统计某药品下未删除的条码数量，用于删除药品前校验
     */
    default Long countByDrugId(Long drugId) {
        return selectCount(BarcodeDO::getDrugId, drugId);
    }

}
