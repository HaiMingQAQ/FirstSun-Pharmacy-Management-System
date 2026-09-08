package cn.iocoder.yudao.module.pharmacy.service.base;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.store.StorePageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.store.StoreSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.base.StoreDO;

import javax.validation.Valid;
import java.util.Collection;
import java.util.List;

/**
 * 门店 Service
 */
public interface StoreService {

    /**
     * 创建门店
     */
    Long createStore(@Valid StoreSaveReqVO createReqVO);

    /**
     * 更新门店
     */
    void updateStore(@Valid StoreSaveReqVO updateReqVO);

    /**
     * 删除门店
     */
    void deleteStore(Long id);

    /**
     * 获取门店详情
     */
    StoreDO getStore(Long id);

    /**
     * 批量获得门店（用于列表页补充门店名称）
     *
     * @param ids 门店编号集合
     * @return 门店列表（仅返回未删除的）
     */
    List<StoreDO> getStoreList(Collection<Long> ids);

    /**
     * 获取门店分页
     */
    PageResult<StoreDO> getStorePage(StorePageReqVO reqVO);

    /**
     * 获取营业状态的门店精简列表（用于下拉）
     */
    List<StoreDO> getEnabledStoreList();

    /**
     * 校验门店存在且营业，供员工/药品档案等场景使用
     */
    StoreDO validateStoreExistsAndOpen(Long id);

}
