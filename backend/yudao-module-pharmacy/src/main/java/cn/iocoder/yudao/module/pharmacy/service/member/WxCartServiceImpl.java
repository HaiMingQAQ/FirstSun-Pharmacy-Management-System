package cn.iocoder.yudao.module.pharmacy.service.member;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.pharmacy.api.DrugApi;
import cn.iocoder.yudao.module.pharmacy.api.dto.DrugRespDTO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.cart.WxCartPageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.cart.WxCartSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.WxCartDO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.member.WxCartMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.*;

/**
 * 小程序购物车 Service 实现类
 */
@Service
@Validated
public class WxCartServiceImpl implements WxCartService {

    @Resource
    private WxCartMapper wxCartMapper;

    @Resource
    private DrugApi drugApi;

    // ========== 基础 CRUD ==========

    @Override
    public Long createWxCart(WxCartSaveReqVO createReqVO) {
        // 校验同门店同药品不重复
        validateCartUnique(createReqVO.getMemberId(), createReqVO.getDrugId(), createReqVO.getStoreId(), null);
        // 清理历史残留行，避免唯一键冲突
        wxCartMapper.deleteByKeyPhysical(createReqVO.getMemberId(), createReqVO.getStoreId(), createReqVO.getDrugId());
        // 写入
        WxCartDO wxCart = BeanUtils.toBean(createReqVO, WxCartDO.class);
        wxCartMapper.insert(wxCart);
        return wxCart.getId();
    }

    @Override
    public void updateWxCart(WxCartSaveReqVO updateReqVO) {
        // 校验存在
        validateWxCartExists(updateReqVO.getId());
        // 校验同门店同药品不重复
        validateCartUnique(updateReqVO.getMemberId(), updateReqVO.getDrugId(), updateReqVO.getStoreId(), updateReqVO.getId());
        // 更新
        WxCartDO updateObj = BeanUtils.toBean(updateReqVO, WxCartDO.class);
        wxCartMapper.updateById(updateObj);
    }

    @Override
    public void deleteWxCart(Long id) {
        // 校验存在
        validateWxCartExists(id);
        // 删除（购物车为临时数据，物理删除，避免唯一键残留冲突）
        wxCartMapper.deleteByIdPhysical(id);
    }

    @Override
    public WxCartDO getWxCart(Long id) {
        return wxCartMapper.selectById(id);
    }

    @Override
    public PageResult<WxCartDO> getWxCartPage(WxCartPageReqVO reqVO) {
        return wxCartMapper.selectPage(reqVO);
    }

    @Override
    public WxCartDO validateWxCartExists(Long id) {
        if (id == null) {
            throw exception(PHARMACY_WX_CART_NOT_EXISTS);
        }
        WxCartDO wxCart = wxCartMapper.selectById(id);
        if (wxCart == null) {
            throw exception(PHARMACY_WX_CART_NOT_EXISTS);
        }
        return wxCart;
    }

    // ========== 业务方法 ==========

    @Override
    public Long addToCart(Long memberId, Long drugId, Integer qty, Long storeId) {
        if (qty == null || qty < 1) {
            throw exception(PHARMACY_WX_CART_QTY_INVALID);
        }
        // 对接 A 的商品查询接口：校验药品存在且可销售
        validateDrugSaleable(drugId);
        // 查询同一会员同一门店同一药品是否已存在
        WxCartDO existing = wxCartMapper.selectByMemberIdAndDrugIdAndStoreId(memberId, drugId, storeId);
        if (existing != null) {
            // 累加数量
            existing.setQty(existing.getQty() + qty);
            existing.setAddTime(LocalDateTime.now());
            wxCartMapper.updateById(existing);
            return existing.getId();
        }
        // 清理同一会员+门店+商品的历史残留行（含逻辑删除），避免唯一键 uk_member_drug 冲突
        wxCartMapper.deleteByKeyPhysical(memberId, storeId, drugId);
        // 新建购物车记录
        WxCartDO wxCart = new WxCartDO();
        wxCart.setMemberId(memberId);
        wxCart.setDrugId(drugId);
        wxCart.setQty(qty);
        wxCart.setSelectedFlag(1);
        wxCart.setAddTime(LocalDateTime.now());
        wxCart.setStoreId(storeId);
        wxCartMapper.insert(wxCart);
        return wxCart.getId();
    }

    @Override
    public void updateQty(Long id, Integer qty) {
        if (qty == null || qty < 1) {
            throw exception(PHARMACY_WX_CART_QTY_INVALID);
        }
        WxCartDO wxCart = validateWxCartExists(id);
        wxCart.setQty(qty);
        wxCartMapper.updateById(wxCart);
    }

    @Override
    public void updateSelected(Long id, Integer selectedFlag) {
        WxCartDO wxCart = validateWxCartExists(id);
        wxCart.setSelectedFlag(selectedFlag);
        wxCartMapper.updateById(wxCart);
    }

    @Override
    public void batchUpdateSelected(List<Long> ids, Integer selectedFlag) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        List<WxCartDO> list = wxCartMapper.selectBatchIds(ids);
        for (WxCartDO item : list) {
            item.setSelectedFlag(selectedFlag);
            wxCartMapper.updateById(item);
        }
    }

    @Override
    public void clearCart(Long memberId) {
        // 物理清空，避免唯一键 uk_member_drug 残留冲突
        wxCartMapper.deleteByMemberIdPhysical(memberId);
    }

    @Override
    public List<WxCartDO> getCartListByMemberId(Long memberId) {
        return wxCartMapper.selectListByMemberId(memberId);
    }

    @Override
    public List<WxCartDO> getSelectedCartListByMemberId(Long memberId) {
        return wxCartMapper.selectSelectedListByMemberId(memberId);
    }

    // ========== 私有校验方法 ==========

    /**
     * 对接 A 的商品查询接口：校验药品存在且可销售（已审核通过 + 启用）
     *
     * 处方药（isRx=1）线上加购仅做记录，实际下单时需关联处方（prescId），由订单侧校验。
     */
    private void validateDrugSaleable(Long drugId) {
        DrugRespDTO drug = drugApi.getDrug(drugId);
        if (drug == null) {
            throw exception(PHARMACY_DRUG_NOT_EXISTS);
        }
        // 已停用或未审核通过的药品不可销售
        if (!Objects.equals(drug.getStatus(), 1) || !Objects.equals(drug.getApproveStatus(), 1)) {
            throw exception(PHARMACY_DRUG_NOT_SALEABLE);
        }
    }

    private void validateCartUnique(Long memberId, Long drugId, Long storeId, Long id) {
        WxCartDO existing = wxCartMapper.selectByMemberIdAndDrugIdAndStoreId(memberId, drugId, storeId);
        if (existing == null) {
            return;
        }
        if (id == null || !Objects.equals(existing.getId(), id)) {
            throw exception(PHARMACY_WX_CART_ITEM_DUPLICATE);
        }
    }

}
