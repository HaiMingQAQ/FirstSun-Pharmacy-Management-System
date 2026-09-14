package cn.iocoder.yudao.module.pharmacy.service.purchase;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.supplier.SupplierPageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.supplier.SupplierSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.purchase.SupplierDO;

import jakarta.validation.Valid;
import java.util.Collection;
import java.util.List;

/**
 * 供应商 Service
 *
 * @author B 成员
 */
public interface SupplierService {

    /**
     * 创建供应商
     *
     * @return 供应商编号
     */
    Long createSupplier(@Valid SupplierSaveReqVO createReqVO);

    /**
     * 更新供应商
     *
     * 审核相关字段（approveStatus/auditBy/auditAt/auditOpinion）只能通过 {@link #approveSupplier} 维护。
     */
    void updateSupplier(@Valid SupplierSaveReqVO updateReqVO);

    /**
     * 删除供应商
     *
     * 已存在采购订单引用时拒绝删除；存在证照时连同证照一并删除（同一事务）。
     */
    void deleteSupplier(Long id);

    /**
     * 获取供应商详情
     */
    SupplierDO getSupplier(Long id);

    /**
     * 获取供应商分页
     */
    PageResult<SupplierDO> getSupplierPage(SupplierPageReqVO reqVO);

    /**
     * 获取启用且首营审核通过的供应商精简列表，用于采购订单/收货单下拉
     *
     * @param keyword 供应商编码/名称模糊关键字，可为空
     */
    List<SupplierDO> getSimpleSupplierList(String keyword);

    /**
     * 获取全部未删除的供应商精简列表（不限启停与审核状态），
     * 用于供应商证照登记与筛选：首营资料登记发生在审核之前，不能只放已审核通过的供应商。
     */
    List<SupplierDO> getAllSupplierList();

    /**
     * 批量获取供应商（用于列表回填供应商名称）
     */
    List<SupplierDO> getSupplierList(Collection<Long> ids);

    /**
     * 校验供应商存在
     */
    SupplierDO validateSupplierExists(Long id);

    /**
     * 校验供应商可用于采购：存在 + 启用 + 首营审核通过
     */
    SupplierDO validateSupplierPurchasable(Long id);

    /**
     * 首营审核（通过 / 驳回）
     *
     * @param id            供应商编号
     * @param approveStatus 审核结果，只能为 1(通过) 或 2(驳回)
     * @param auditOpinion  审核意见
     */
    void approveSupplier(Long id, Integer approveStatus, String auditOpinion);

    /**
     * 统计引用该供应商的采购订单数量，用于删除前校验
     */
    Long countOrdersBySupplier(Long supplierId);

}
