package cn.iocoder.yudao.module.pharmacy.dal.mysql.purchase;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.purchase.PurchaseReceiptLineDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * 采购收货明细 Mapper
 *
 * @author B 成员
 */
@Mapper
public interface PurchaseReceiptLineMapper extends BaseMapperX<PurchaseReceiptLineDO> {

    default List<PurchaseReceiptLineDO> selectListByReceiptId(Long receiptId) {
        return selectList(new LambdaQueryWrapperX<PurchaseReceiptLineDO>()
                .eq(PurchaseReceiptLineDO::getReceiptId, receiptId)
                .orderByAsc(PurchaseReceiptLineDO::getLineNo));
    }

    default List<PurchaseReceiptLineDO> selectListByReceiptIds(Collection<Long> receiptIds) {
        return selectList(new LambdaQueryWrapperX<PurchaseReceiptLineDO>()
                .in(PurchaseReceiptLineDO::getReceiptId, receiptIds)
                .orderByAsc(PurchaseReceiptLineDO::getReceiptId)
                .orderByAsc(PurchaseReceiptLineDO::getLineNo));
    }

    default int deleteByReceiptId(Long receiptId) {
        return delete(new LambdaQueryWrapperX<PurchaseReceiptLineDO>()
                .eq(PurchaseReceiptLineDO::getReceiptId, receiptId));
    }

    /**
     * 物理删除某收货单的全部明细（用于「编辑时明细整体重建」）
     *
     * 不能使用逻辑删除：唯一键 {@code uk_receipt_line(receipt_id, line_no)} 不包含 deleted 列，
     * 逻辑删除后旧行仍在表中，重新插入相同行号会触发主键冲突。
     */
    @Delete("DELETE FROM ph_po_receipt_line WHERE receipt_id = #{receiptId}")
    int physicalDeleteByReceiptId(@Param("receiptId") Long receiptId);

}
