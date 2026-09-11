package cn.iocoder.yudao.module.pharmacy.service.member;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.orderline.WxOrderLinePageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.orderline.WxOrderLineSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.WxOrderLineDO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.member.WxOrderLineMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.*;

/**
 * 小程序订单明细 Service 实现类
 */
@Service
@Validated
public class WxOrderLineServiceImpl implements WxOrderLineService {

    @Resource
    private WxOrderLineMapper wxOrderLineMapper;

    @Override
    public Long createWxOrderLine(WxOrderLineSaveReqVO createReqVO) {
        // 写入
        WxOrderLineDO wxOrderLine = BeanUtils.toBean(createReqVO, WxOrderLineDO.class);
        wxOrderLineMapper.insert(wxOrderLine);
        return wxOrderLine.getId();
    }

    @Override
    public void updateWxOrderLine(WxOrderLineSaveReqVO updateReqVO) {
        // 校验存在
        validateWxOrderLineExists(updateReqVO.getId());
        // 更新
        WxOrderLineDO updateObj = BeanUtils.toBean(updateReqVO, WxOrderLineDO.class);
        wxOrderLineMapper.updateById(updateObj);
    }

    @Override
    public void deleteWxOrderLine(Long id) {
        // 校验存在
        validateWxOrderLineExists(id);
        // 删除
        wxOrderLineMapper.deleteById(id);
    }

    @Override
    public WxOrderLineDO getWxOrderLine(Long id) {
        return wxOrderLineMapper.selectById(id);
    }

    @Override
    public List<WxOrderLineDO> getWxOrderLineList(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return wxOrderLineMapper.selectBatchIds(ids);
    }

    @Override
    public PageResult<WxOrderLineDO> getWxOrderLinePage(WxOrderLinePageReqVO reqVO) {
        return wxOrderLineMapper.selectPage(reqVO);
    }

    @Override
    public List<WxOrderLineDO> getWxOrderLineListByWxOrderId(Long wxOrderId) {
        return wxOrderLineMapper.selectListByWxOrderId(wxOrderId);
    }

    @Override
    public WxOrderLineDO validateWxOrderLineExists(Long id) {
        if (id == null) {
            throw exception(PHARMACY_WX_ORDER_LINE_NOT_EXISTS);
        }
        WxOrderLineDO wxOrderLine = wxOrderLineMapper.selectById(id);
        if (wxOrderLine == null) {
            throw exception(PHARMACY_WX_ORDER_LINE_NOT_EXISTS);
        }
        return wxOrderLine;
    }

}
