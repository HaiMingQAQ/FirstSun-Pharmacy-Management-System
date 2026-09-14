package cn.iocoder.yudao.module.pharmacy.service.base;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.drug.DrugPageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.drug.DrugSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.base.DrugDO;

import jakarta.validation.Valid;
import java.util.Collection;
import java.util.List;

/**
 * 药品档案 Service
 */
public interface DrugService {

    /**
     * 创建药品
     */
    Long createDrug(@Valid DrugSaveReqVO createReqVO);

    /**
     * 更新药品
     */
    void updateDrug(@Valid DrugSaveReqVO updateReqVO);

    /**
     * 删除药品
     */
    void deleteDrug(Long id);

    /**
     * 获取药品详情
     */
    DrugDO getDrug(Long id);

    /**
     * 获取药品分页
     */
    PageResult<DrugDO> getDrugPage(DrugPageReqVO reqVO);

    /**
     * 校验药品存在，供其他业务校验引用
     */
    DrugDO validateDrugExists(Long id);

    /**
     * 统计某分类下未删除的药品数量，用于删除分类前校验
     */
    Long countByCategoryId(Long categoryId);

    /**
     * 审核药品
     * <p>
     * 审核人（audit_by）保存的是 ph_employee.id，由 Service 根据
     * 当前登录的 system 用户解析出对应的药店员工编号；
     * 若当前用户未绑定员工，抛 {@link ErrorCodeConstants#PHARMACY_DRUG_AUDITOR_NOT_EMPLOYEE}。
     *
     * @param id 药品编号
     * @param approveStatus 审核结果 1通过/2驳回
     * @param auditOpinion 审核意见
     */
    void approveDrug(Long id, Integer approveStatus, String auditOpinion);

    /**
     * 获取启用状态的药品精简列表（用于下拉/条码页面选择药品）
     *
     * @param keyword 关键字（匹配药品编码/通用名/拼音码，可为空）
     * @return 药品精简列表
     */
    List<DrugDO> getSimpleDrugList(String keyword);

    /**
     * 批量获得药品（跨模块 DrugApi 使用）
     *
     * @param ids 药品编号集合
     * @return 药品列表（仅返回未删除的）
     */
    List<DrugDO> getDrugList(Collection<Long> ids);

    /**
     * 校验药品们是否有效（跨模块 DrugApi 使用）：
     * 1. 药品编号不存在
     * 2. 药品被禁用
     * 3. 药品未审核通过（approveStatus != 1）
     *
     * @param ids 药品编号集合
     */
    void validateDrugList(Collection<Long> ids);

    /**
     * 按药品编码精确查询（跨模块 DrugApi 使用）
     *
     * @param drugCode 药品编码
     * @return 药品信息（不存在返回 null）
     */
    DrugDO getDrugByCode(String drugCode);

}
