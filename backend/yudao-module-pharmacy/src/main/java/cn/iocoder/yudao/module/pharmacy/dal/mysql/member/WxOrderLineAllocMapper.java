package cn.iocoder.yudao.module.pharmacy.dal.mysql.member;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.WxOrderLineAllocDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 线上订单出库分配 Mapper
 *
 * <p>出库分配是「取消 / 退款 / 退货回补」的唯一依据，查询必须按订单维度返回全部批次分配，
 * 不能只取第一条，否则 FEFO 拆分到多批次的订单会漏补库存。
 */
@Mapper
public interface WxOrderLineAllocMapper extends BaseMapperX<WxOrderLineAllocDO> {

    /**
     * 查询某订单的全部出库分配（含已回补记录，用于幂等判断）
     */
    default List<WxOrderLineAllocDO> selectListByWxOrderId(Long wxOrderId) {
        return selectList(WxOrderLineAllocDO::getWxOrderId, wxOrderId);
    }

    /**
     * 按订单号查询出库分配
     */
    default List<WxOrderLineAllocDO> selectListByOrderNo(String orderNo) {
        return selectList(WxOrderLineAllocDO::getOrderNo, orderNo);
    }

}
