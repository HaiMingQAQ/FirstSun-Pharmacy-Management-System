package cn.iocoder.yudao.module.pharmacy.service.member;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.cart.WxCartPageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.cart.WxCartSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.WxCartDO;

import javax.validation.Valid;
import java.util.List;

/**
 * 小程序购物车 Service
 */
public interface WxCartService {

    /**
     * 创建购物车记录
     */
    Long createWxCart(@Valid WxCartSaveReqVO createReqVO);

    /**
     * 更新购物车记录
     */
    void updateWxCart(@Valid WxCartSaveReqVO updateReqVO);

    /**
     * 删除购物车记录
     */
    void deleteWxCart(Long id);

    /**
     * 获取购物车详情
     */
    WxCartDO getWxCart(Long id);

    /**
     * 获取购物车分页
     */
    PageResult<WxCartDO> getWxCartPage(WxCartPageReqVO reqVO);

    /**
     * 校验购物车记录存在
     */
    WxCartDO validateWxCartExists(Long id);

    // ========== 业务方法 ==========

    /**
     * 加购：同一会员同一门店同一药品，如已存在则累加数量，否则新建
     */
    Long addToCart(Long memberId, Long drugId, Integer qty, Long storeId);

    /**
     * 修改数量：校验数量 > 0
     */
    void updateQty(Long id, Integer qty);

    /**
     * 勾选/取消勾选（单条）
     */
    void updateSelected(Long id, Integer selectedFlag);

    /**
     * 批量勾选/取消勾选
     */
    void batchUpdateSelected(List<Long> ids, Integer selectedFlag);

    /**
     * 清空指定会员的购物车
     */
    void clearCart(Long memberId);

    /**
     * 获取指定会员的购物车列表
     */
    List<WxCartDO> getCartListByMemberId(Long memberId);

    /**
     * 获取指定会员已勾选的购物车列表
     */
    List<WxCartDO> getSelectedCartListByMemberId(Long memberId);

}
