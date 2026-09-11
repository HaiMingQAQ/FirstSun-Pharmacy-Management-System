package cn.iocoder.yudao.module.pharmacy.service.member;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.order.WxOrderPageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.order.WxOrderSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.WxOrderDO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.member.WxOrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.*;

/**
 * 小程序订单 Service 实现类
 */
@Service
@Validated
public class WxOrderServiceImpl implements WxOrderService {

    @Resource
    private WxOrderMapper wxOrderMapper;

    @Override
    public Long createWxOrder(WxOrderSaveReqVO createReqVO) {
        // 校验订单号唯一
        validateOrderNoUnique(null, createReqVO.getOrderNo());
        // 写入
        WxOrderDO wxOrder = BeanUtils.toBean(createReqVO, WxOrderDO.class);
        wxOrderMapper.insert(wxOrder);
        return wxOrder.getId();
    }

    @Override
    public void updateWxOrder(WxOrderSaveReqVO updateReqVO) {
        // 校验存在
        validateWxOrderExists(updateReqVO.getId());
        // 校验订单号唯一
        validateOrderNoUnique(updateReqVO.getId(), updateReqVO.getOrderNo());
        // 更新
        WxOrderDO updateObj = BeanUtils.toBean(updateReqVO, WxOrderDO.class);
        wxOrderMapper.updateById(updateObj);
    }

    @Override
    public void deleteWxOrder(Long id) {
        // 校验存在
        validateWxOrderExists(id);
        // 删除
        wxOrderMapper.deleteById(id);
    }

    @Override
    public WxOrderDO getWxOrder(Long id) {
        return wxOrderMapper.selectById(id);
    }

    @Override
    public List<WxOrderDO> getWxOrderList(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return wxOrderMapper.selectBatchIds(ids);
    }

    @Override
    public PageResult<WxOrderDO> getWxOrderPage(WxOrderPageReqVO reqVO) {
        return wxOrderMapper.selectPage(reqVO);
    }

    @Override
    public WxOrderDO getWxOrderByOrderNo(String orderNo) {
        return wxOrderMapper.selectByOrderNo(orderNo);
    }

    @Override
    public List<WxOrderDO> getWxOrderListByMemberId(Long memberId) {
        return wxOrderMapper.selectListByMemberId(memberId);
    }

    @Override
    public WxOrderDO getWxOrderByPickupCode(String pickupCode) {
        return wxOrderMapper.selectByPickupCode(pickupCode);
    }

    @Override
    public WxOrderDO validateWxOrderExists(Long id) {
        if (id == null) {
            throw exception(PHARMACY_WX_ORDER_NOT_EXISTS);
        }
        WxOrderDO wxOrder = wxOrderMapper.selectById(id);
        if (wxOrder == null) {
            throw exception(PHARMACY_WX_ORDER_NOT_EXISTS);
        }
        return wxOrder;
    }

    private void validateOrderNoUnique(Long id, String orderNo) {
        WxOrderDO wxOrder = wxOrderMapper.selectByOrderNo(orderNo);
        if (wxOrder == null) {
            return;
        }
        if (id == null) {
            throw exception(PHARMACY_WX_ORDER_NO_DUPLICATE);
        }
        if (!Objects.equals(wxOrder.getId(), id)) {
            throw exception(PHARMACY_WX_ORDER_NO_DUPLICATE);
        }
    }

}
