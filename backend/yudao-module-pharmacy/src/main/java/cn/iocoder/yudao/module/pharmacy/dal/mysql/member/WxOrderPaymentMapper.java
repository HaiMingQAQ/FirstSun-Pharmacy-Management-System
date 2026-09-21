package cn.iocoder.yudao.module.pharmacy.dal.mysql.member;

import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.WxOrderDO;
import cn.iocoder.yudao.module.pay.dal.dataobject.order.PayOrderDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** Lock order before payment, always in that order. Explicit tenant predicates also protect custom SQL. */
@Mapper
public interface WxOrderPaymentMapper {
    @Select("SELECT * FROM ph_wx_order WHERE id=#{id} AND tenant_id=#{tenantId} AND deleted=0 FOR UPDATE")
    WxOrderDO lockOrder(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Select("SELECT * FROM pay_order WHERE id=#{id} AND tenant_id=#{tenantId} AND deleted=0 FOR UPDATE")
    PayOrderDO lockPayment(@Param("id") Long id, @Param("tenantId") Long tenantId);
}
