package cn.iocoder.yudao.module.pharmacy.dal.mysql.purchase;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.purchase.PurchaseOrderLineDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * 采购订单明细 Mapper
 *
 * @author B 成员
 */
@Mapper
public interface PurchaseOrderLineMapper extends BaseMapperX<PurchaseOrderLineDO> {

    default List<PurchaseOrderLineDO> selectListByOrderId(Long orderId) {
        return selectList(new LambdaQueryWrapperX<PurchaseOrderLineDO>()
                .eq(PurchaseOrderLineDO::getOrderId, orderId)
                .orderByAsc(PurchaseOrderLineDO::getLineNo));
    }

    default List<PurchaseOrderLineDO> selectListByOrderIds(Collection<Long> orderIds) {
        return selectList(new LambdaQueryWrapperX<PurchaseOrderLineDO>()
                .in(PurchaseOrderLineDO::getOrderId, orderIds)
                .orderByAsc(PurchaseOrderLineDO::getOrderId)
                .orderByAsc(PurchaseOrderLineDO::getLineNo));
    }

    default int deleteByOrderId(Long orderId) {
        return delete(new LambdaQueryWrapperX<PurchaseOrderLineDO>()
                .eq(PurchaseOrderLineDO::getOrderId, orderId));
    }

    /**
     * 物理删除某采购订单的全部明细（用于「编辑时明细整体重建」）
     *
     * 不能使用逻辑删除：唯一键 {@code uk_po_line(order_id, line_no)} 不包含 deleted 列，
     * 逻辑删除后旧行仍在表中，重新插入相同行号会触发主键冲突。
     */
    @Delete("DELETE FROM ph_po_order_line WHERE order_id = #{orderId}")
    int physicalDeleteByOrderId(@Param("orderId") Long orderId);

}
