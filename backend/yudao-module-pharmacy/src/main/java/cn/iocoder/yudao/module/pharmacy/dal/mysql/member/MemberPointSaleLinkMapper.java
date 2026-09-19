package cn.iocoder.yudao.module.pharmacy.dal.mysql.member;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

/**
 * 积分与销售单联动 Mapper（F 会员模块自有）。
 *
 * <p>F 侧积分结算需要读写 POS 单据上的两个字段：
 * <ul>
 *   <li>写：{@code ph_sale_order.points_earned}，让收银台与订单列表能看到本单赠送积分；</li>
 *   <li>读：{@code ph_sale_return} 的累计退货金额，用于判断本次退货是否已构成整单全退。</li>
 * </ul>
 * 这里用 F 自己的 SQL 完成，<b>不修改 D 的任何文件</b>；D 后续若在自身流程里直接调用
 * {@code MemberPointFacade}，本 Mapper 可随之简化。
 */
@Mapper
public interface MemberPointSaleLinkMapper {

    /**
     * 回写销售单的「奖励积分」字段（幂等：同值重复写入不产生副作用）
     */
    @Update("UPDATE ph_sale_order SET points_earned = #{pointsEarned}, update_time = NOW() "
            + "WHERE id = #{orderId} AND deleted = 0")
    int updateSaleOrderPointsEarned(@Param("orderId") Long orderId,
                                    @Param("pointsEarned") Integer pointsEarned);

    /**
     * 统计某张销售单「已生效」的累计退货金额（排除已取消的退货单，status=4）
     *
     * <p>调用时本次退货单已落库，因此结果天然包含本次。
     */
    @Select("SELECT COALESCE(SUM(total_amount), 0) FROM ph_sale_return "
            + "WHERE sale_order_id = #{saleOrderId} AND status <> 4 AND deleted = 0")
    BigDecimal sumReturnedAmountByOrderId(@Param("saleOrderId") Long saleOrderId);

}
