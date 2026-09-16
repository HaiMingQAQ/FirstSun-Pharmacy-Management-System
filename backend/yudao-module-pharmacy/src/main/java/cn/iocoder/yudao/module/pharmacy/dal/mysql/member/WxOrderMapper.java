package cn.iocoder.yudao.module.pharmacy.dal.mysql.member;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.order.WxOrderPageReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.WxOrderDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 小程序订单 Mapper
 */
@Mapper
public interface WxOrderMapper extends BaseMapperX<WxOrderDO> {

    default PageResult<WxOrderDO> selectPage(WxOrderPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<WxOrderDO>()
                .likeIfPresent(WxOrderDO::getOrderNo, reqVO.getOrderNo())
                .eqIfPresent(WxOrderDO::getMemberId, reqVO.getMemberId())
                .eqIfPresent(WxOrderDO::getStoreId, reqVO.getStoreId())
                .eqIfPresent(WxOrderDO::getOrderType, reqVO.getOrderType())
                .eqIfPresent(WxOrderDO::getStatus, reqVO.getStatus())
                .eqIfPresent(WxOrderDO::getPayStatus, reqVO.getPayStatus())
                .betweenIfPresent(WxOrderDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(WxOrderDO::getId));
    }

    default WxOrderDO selectByOrderNo(String orderNo) {
        return selectOne(WxOrderDO::getOrderNo, orderNo);
    }

    default List<WxOrderDO> selectListByMemberId(Long memberId) {
        return selectList(WxOrderDO::getMemberId, memberId);
    }

    default WxOrderDO selectByPickupCode(String pickupCode) {
        return selectOne(WxOrderDO::getPickupCode, pickupCode);
    }

    /**
     * 统计指定订单号前缀的订单数量，用于生成当日流水号
     */
    default Long selectCountByOrderNoPrefix(String orderNoPrefix) {
        return selectCount(new LambdaQueryWrapperX<WxOrderDO>()
                .likeRight(WxOrderDO::getOrderNo, orderNoPrefix));
    }

    /**
     * 查询某门店已超时未支付的订单（用于门店节点关闭超时订单并释放冻结库存）
     *
     * @param storeId 门店编号
     * @param now     当前时间
     * @param status  订单状态（待支付）
     * @param limit   单次处理条数上限
     */
    default List<WxOrderDO> selectExpiredList(Long storeId, Integer status, LocalDateTime now, int limit) {
        return selectList(new LambdaQueryWrapperX<WxOrderDO>()
                .eq(WxOrderDO::getStoreId, storeId)
                .eq(WxOrderDO::getStatus, status)
                .lt(WxOrderDO::getExpireAt, now)
                .orderByAsc(WxOrderDO::getId)
                .last("LIMIT " + limit));
    }

    /**
     * 按门店 + 状态查询订单（用于门店节点批量清理库存）
     */
    default List<WxOrderDO> selectListByStoreIdAndStatus(Long storeId, Integer status, int limit) {
        return selectList(new LambdaQueryWrapperX<WxOrderDO>()
                .eq(WxOrderDO::getStoreId, storeId)
                .eq(WxOrderDO::getStatus, status)
                .orderByAsc(WxOrderDO::getId)
                .last("LIMIT " + limit));
    }

}
