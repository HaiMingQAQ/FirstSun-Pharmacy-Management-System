package cn.iocoder.yudao.module.pharmacy.dal.mysql.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.pharmacy.controller.admin.pos.vo.SaleOrderPageReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhSaleOrderDO;
import lombok.Data;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SaleOrderMapper extends BaseMapperX<PhSaleOrderDO> {

    default PhSaleOrderDO selectByOrderNo(String orderNo) {
        return selectOne(PhSaleOrderDO::getOrderNo, orderNo);
    }

    default PageResult<PhSaleOrderDO> selectPage(SaleOrderPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<PhSaleOrderDO>()
                .eqIfPresent(PhSaleOrderDO::getStoreId, reqVO.getStoreId())
                .eqIfPresent(PhSaleOrderDO::getCashierId, reqVO.getCashierId())
                .likeIfPresent(PhSaleOrderDO::getOrderNo, reqVO.getOrderNo())
                .eqIfPresent(PhSaleOrderDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(PhSaleOrderDO::getSaleTime, reqVO.getSaleTime())
                .orderByDesc(PhSaleOrderDO::getSaleTime));
    }

    default List<PhSaleOrderDO> selectListByShiftId(Long shiftId) {
        return selectList(PhSaleOrderDO::getShiftId, shiftId);
    }

    /** 按日统计(仅完成/部分退款单,status IN (1,3)) */
    @Select("SELECT DATE_FORMAT(sale_time, '%Y-%m-%d') AS bizDate, COUNT(*) AS orderCount, " +
            "SUM(payable_amount) AS saleAmount, SUM(cost_amount) AS costAmount " +
            "FROM ph_sale_order WHERE store_id = #{storeId} AND status IN (1,3) " +
            "AND sale_time BETWEEN #{startTime} AND #{endTime} " +
            "GROUP BY bizDate ORDER BY bizDate")
    List<DailySaleRow> selectDailySummary(@Param("storeId") Long storeId,
                                          @Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);

    @Data
    class DailySaleRow {
        private String bizDate;
        private Long orderCount;
        private BigDecimal saleAmount;
        private BigDecimal costAmount;
    }
}
