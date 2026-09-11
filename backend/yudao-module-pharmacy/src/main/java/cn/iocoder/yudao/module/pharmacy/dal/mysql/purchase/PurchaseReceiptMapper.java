package cn.iocoder.yudao.module.pharmacy.dal.mysql.purchase;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.receipt.PurchaseReceiptPageReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.purchase.PurchaseReceiptDO;
import cn.iocoder.yudao.module.pharmacy.enums.PurchaseReceiptStatusEnum;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 采购收货单 Mapper
 *
 * @author B 成员
 */
@Mapper
public interface PurchaseReceiptMapper extends BaseMapperX<PurchaseReceiptDO> {

    default PageResult<PurchaseReceiptDO> selectPage(PurchaseReceiptPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<PurchaseReceiptDO>()
                .likeIfPresent(PurchaseReceiptDO::getReceiptNo, reqVO.getReceiptNo())
                .eqIfPresent(PurchaseReceiptDO::getOrderId, reqVO.getOrderId())
                .eqIfPresent(PurchaseReceiptDO::getStoreId, reqVO.getStoreId())
                .eqIfPresent(PurchaseReceiptDO::getWarehouseId, reqVO.getWarehouseId())
                .eqIfPresent(PurchaseReceiptDO::getStatus, reqVO.getStatus())
                .eqIfPresent(PurchaseReceiptDO::getDiffType, reqVO.getDiffType())
                .eqIfPresent(PurchaseReceiptDO::getIsFreeReceipt, reqVO.getIsFreeReceipt())
                .betweenIfPresent(PurchaseReceiptDO::getReceiveDate, reqVO.getReceiveDate())
                .orderByDesc(PurchaseReceiptDO::getId));
    }

    default PurchaseReceiptDO selectByReceiptNo(String receiptNo) {
        return selectOne(PurchaseReceiptDO::getReceiptNo, receiptNo);
    }

    /**
     * 查询指定前缀（GR-门店-yyyyMMdd-）下已有的最大收货单号（含逻辑删除行）
     *
     * 用于生成新单号：uk_receipt_no 不包含 deleted 列，逻辑删除的单据仍占号，
     * 因此必须包含 deleted=1 的行一起取最大值，否则会生成已被占用的单号。
     */
    @Select("SELECT MAX(receipt_no) FROM ph_po_receipt WHERE receipt_no LIKE CONCAT(#{prefix}, '%')")
    String selectMaxReceiptNo(@Param("prefix") String prefix);

    default List<PurchaseReceiptDO> selectListByOrderId(Long orderId) {
        return selectList(new LambdaQueryWrapperX<PurchaseReceiptDO>()
                .eq(PurchaseReceiptDO::getOrderId, orderId)
                .orderByAsc(PurchaseReceiptDO::getId));
    }

    /**
     * 统计采购订单下「未作废」的收货单数量
     *
     * 用于订单删除/取消前校验：已存在收货记录时不允许删除或取消订单。
     */
    default Long countValidByOrderId(Long orderId) {
        return selectCount(new LambdaQueryWrapperX<PurchaseReceiptDO>()
                .eq(PurchaseReceiptDO::getOrderId, orderId)
                .ne(PurchaseReceiptDO::getStatus, PurchaseReceiptStatusEnum.VOIDED.getStatus()));
    }

}
