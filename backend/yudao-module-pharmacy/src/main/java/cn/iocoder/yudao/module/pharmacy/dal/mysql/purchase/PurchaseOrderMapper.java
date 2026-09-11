package cn.iocoder.yudao.module.pharmacy.dal.mysql.purchase;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.order.PurchaseOrderPageReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.purchase.PurchaseOrderDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * 采购订单 Mapper
 *
 * @author B 成员
 */
@Mapper
public interface PurchaseOrderMapper extends BaseMapperX<PurchaseOrderDO> {

    default PageResult<PurchaseOrderDO> selectPage(PurchaseOrderPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<PurchaseOrderDO>()
                .likeIfPresent(PurchaseOrderDO::getOrderNo, reqVO.getOrderNo())
                .eqIfPresent(PurchaseOrderDO::getStoreId, reqVO.getStoreId())
                .eqIfPresent(PurchaseOrderDO::getSupplierId, reqVO.getSupplierId())
                .eqIfPresent(PurchaseOrderDO::getStatus, reqVO.getStatus())
                .eqIfPresent(PurchaseOrderDO::getIsAuto, reqVO.getIsAuto())
                .betweenIfPresent(PurchaseOrderDO::getOrderDate, reqVO.getOrderDate())
                .orderByDesc(PurchaseOrderDO::getId));
    }

    default PurchaseOrderDO selectByOrderNo(String orderNo) {
        return selectOne(PurchaseOrderDO::getOrderNo, orderNo);
    }

    /**
     * 查询指定前缀（PO-门店-yyyyMMdd-）下已有的最大订单号（含逻辑删除行）
     *
     * 用于生成新单号：uk_order_no 不包含 deleted 列，逻辑删除的订单仍占号，
     * 因此必须包含 deleted=1 的行一起取最大值，否则会生成已被占用的单号。
     */
    @Select("SELECT MAX(order_no) FROM ph_po_order WHERE order_no LIKE CONCAT(#{prefix}, '%')")
    String selectMaxOrderNo(@Param("prefix") String prefix);

    default List<PurchaseOrderDO> selectListByIds(Collection<Long> ids) {
        return selectBatchIds(ids);
    }

}
