package cn.iocoder.yudao.module.pharmacy.service.member;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.orderline.WxOrderLinePageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.orderline.WxOrderLineSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.WxOrderLineDO;

import javax.validation.Valid;
import java.util.Collection;
import java.util.List;

/**
 * 小程序订单明细 Service
 */
public interface WxOrderLineService {

    /**
     * 创建小程序订单明细
     */
    Long createWxOrderLine(@Valid WxOrderLineSaveReqVO createReqVO);

    /**
     * 更新小程序订单明细
     */
    void updateWxOrderLine(@Valid WxOrderLineSaveReqVO updateReqVO);

    /**
     * 删除小程序订单明细
     */
    void deleteWxOrderLine(Long id);

    /**
     * 获取小程序订单明细详情
     */
    WxOrderLineDO getWxOrderLine(Long id);

    /**
     * 批量获得小程序订单明细列表
     *
     * @param ids 明细编号集合
     * @return 明细列表（仅返回未删除的）
     */
    List<WxOrderLineDO> getWxOrderLineList(Collection<Long> ids);

    /**
     * 获取小程序订单明细分页
     */
    PageResult<WxOrderLineDO> getWxOrderLinePage(WxOrderLinePageReqVO reqVO);

    /**
     * 根据订单编号获取明细列表
     */
    List<WxOrderLineDO> getWxOrderLineListByWxOrderId(Long wxOrderId);

    /**
     * 校验订单明细存在
     */
    WxOrderLineDO validateWxOrderLineExists(Long id);

}
