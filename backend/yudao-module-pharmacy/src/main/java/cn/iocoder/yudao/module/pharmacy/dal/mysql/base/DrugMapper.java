package cn.iocoder.yudao.module.pharmacy.dal.mysql.base;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.drug.DrugPageReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.base.DrugDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 药品档案 Mapper
 */
@Mapper
public interface DrugMapper extends BaseMapperX<DrugDO> {

    default PageResult<DrugDO> selectPage(DrugPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<DrugDO>()
                .eqIfPresent(DrugDO::getDrugCode, reqVO.getDrugCode())
                .likeIfPresent(DrugDO::getGenericName, reqVO.getGenericName())
                .likeIfPresent(DrugDO::getTradeName, reqVO.getTradeName())
                .eqIfPresent(DrugDO::getSpellCode, reqVO.getSpellCode())
                .eqIfPresent(DrugDO::getCategoryId, reqVO.getCategoryId())
                .eqIfPresent(DrugDO::getDrugType, reqVO.getDrugType())
                .eqIfPresent(DrugDO::getIsRx, reqVO.getIsRx())
                .eqIfPresent(DrugDO::getIsSpecial, reqVO.getIsSpecial())
                .eqIfPresent(DrugDO::getIsPseudoephedrine, reqVO.getIsPseudoephedrine())
                .eqIfPresent(DrugDO::getIsColdChain, reqVO.getIsColdChain())
                .eqIfPresent(DrugDO::getInsuranceType, reqVO.getInsuranceType())
                .eqIfPresent(DrugDO::getStorageCond, reqVO.getStorageCond())
                .eqIfPresent(DrugDO::getSaleableOnline, reqVO.getSaleableOnline())
                .eqIfPresent(DrugDO::getStatus, reqVO.getStatus())
                .eqIfPresent(DrugDO::getApproveStatus, reqVO.getApproveStatus())
                .likeIfPresent(DrugDO::getApprovalNo, reqVO.getApprovalNo())
                .likeIfPresent(DrugDO::getManufacturer, reqVO.getManufacturer())
                .orderByDesc(DrugDO::getId));
    }

    default DrugDO selectByDrugCode(String drugCode) {
        return selectOne(DrugDO::getDrugCode, drugCode);
    }

    default DrugDO selectByApprovalNo(String approvalNo) {
        return selectOne(DrugDO::getApprovalNo, approvalNo);
    }

    /**
     * 统计某分类下未删除的药品数量，用于删除分类前校验
     */
    default Long countByCategoryId(Long categoryId) {
        return selectCount(DrugDO::getCategoryId, categoryId);
    }

    /**
     * 查询启用状态的药品精简列表（下拉/条码页面选择使用）
     *
     * @param keyword 关键字（匹配药品编码/通用名/拼音码，可为空）
     * @return 药品列表（最多 100 条）
     */
    default java.util.List<DrugDO> selectSimpleList(String keyword) {
        return selectList(new LambdaQueryWrapperX<DrugDO>()
                .eq(DrugDO::getStatus, 1) // 仅启用
                .and(keyword != null && !keyword.trim().isEmpty(), w -> w
                        .like(DrugDO::getDrugCode, keyword)
                        .or().like(DrugDO::getGenericName, keyword)
                        .or().like(DrugDO::getSpellCode, keyword))
                .last("LIMIT 100"));
    }

}
