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
     * <p><b>仅用于历史数据回填与单号对账</b>，不要用它来分配新单号：
     * MySQL 默认 REPEATABLE-READ 下 MAX() 是不加锁的一致性读，同一事务内重试读到的快照不变，
     * 并发时会算出同一个号并触发唯一键冲突/死锁。新单号请用
     * {@link PurchaseDocSeqMapper#allocateSeq}。
     */
    @Select("SELECT MAX(order_no) FROM ph_po_order WHERE order_no LIKE CONCAT(#{prefix}, '%')")
    String selectMaxOrderNo(@Param("prefix") String prefix);

    default List<PurchaseOrderDO> selectListByIds(Collection<Long> ids) {
        return selectBatchIds(ids);
    }

}
