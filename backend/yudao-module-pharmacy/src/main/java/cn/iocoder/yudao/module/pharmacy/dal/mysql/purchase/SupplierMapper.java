package cn.iocoder.yudao.module.pharmacy.dal.mysql.purchase;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.supplier.SupplierPageReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.purchase.SupplierDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 供应商 Mapper
 *
 * @author B 成员
 */
@Mapper
public interface SupplierMapper extends BaseMapperX<SupplierDO> {

    default PageResult<SupplierDO> selectPage(SupplierPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SupplierDO>()
                .likeIfPresent(SupplierDO::getSupplierCode, reqVO.getSupplierCode())
                .likeIfPresent(SupplierDO::getSupplierName, reqVO.getSupplierName())
                .likeIfPresent(SupplierDO::getCreditCode, reqVO.getCreditCode())
                .likeIfPresent(SupplierDO::getContact, reqVO.getContact())
                .likeIfPresent(SupplierDO::getPhone, reqVO.getPhone())
                .eqIfPresent(SupplierDO::getApproveStatus, reqVO.getApproveStatus())
                .eqIfPresent(SupplierDO::getStatus, reqVO.getStatus())
                .orderByDesc(SupplierDO::getId));
    }

    default SupplierDO selectBySupplierCode(String supplierCode) {
        return selectOne(SupplierDO::getSupplierCode, supplierCode);
    }

    default SupplierDO selectByCreditCode(String creditCode) {
        return selectOne(SupplierDO::getCreditCode, creditCode);
    }

    /**
     * 查询启用且首营审核通过的供应商，用于采购订单、收货单下拉选择
     *
     * @param keyword 供应商编码/名称模糊关键字，可为空
     */
    default List<SupplierDO> selectSimpleList(String keyword) {
        return selectList(new LambdaQueryWrapperX<SupplierDO>()
                .eq(SupplierDO::getStatus, 1)
                .eq(SupplierDO::getApproveStatus, 1)
                .and(keyword != null && !keyword.isEmpty(), w -> w
                        .like(SupplierDO::getSupplierCode, keyword)
                        .or().like(SupplierDO::getSupplierName, keyword))
                .orderByAsc(SupplierDO::getSupplierCode));
    }

    /**
     * 统计引用该供应商的采购订单数量（未删除）。
     *
     * 用于删除供应商前校验：存在采购订单引用时拒绝删除。
     * {@code ph_po_order} 同属采购域（B 维护）。
     */
    @Select("SELECT COUNT(*) FROM ph_po_order WHERE supplier_id = #{supplierId} AND deleted = 0")
    Long countOrdersBySupplierId(@Param("supplierId") Long supplierId);

}
