package cn.iocoder.yudao.module.pharmacy.service.base;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.drug.DrugPageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.drug.DrugSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.base.DrugDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.base.EmployeeDO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.base.BarcodeMapper;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.base.DrugMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.*;

/**
 * 药品档案 Service 实现类
 *
 * 不修改其他模块：通过 CategoryService 只读校验 category_id。
 * 镜像 DB CHECK 约束（ck_drug_price/ck_drug_stock/ck_drug_rx）于应用层，给前端更友好的错误提示。
 */
@Service
@Validated
public class DrugServiceImpl implements DrugService {

    @Resource
    private DrugMapper drugMapper;

    @Resource
    private CategoryService categoryService;

    /**
     * 员工服务。用于审核药品时根据当前登录 system 用户解析对应的药店员工编号。
     * DrugServiceImpl 不被 EmployeeServiceImpl 引用，为单向依赖。
     */
    @Resource
    private EmployeeService employeeService;

    /**
     * 条码 Mapper。仅用于删除药品前计数校验，直接注入 Mapper 以避免与 BarcodeService 的循环依赖。
     */
    @Resource
    private BarcodeMapper barcodeMapper;

    @Override
    public Long createDrug(DrugSaveReqVO createReqVO) {
        // 校验编码唯一
        validateDrugCodeUnique(null, createReqVO.getDrugCode());
        // 校验分类存在且启用
        validateCategoryId(createReqVO.getCategoryId());
        // 校验批准文号唯一（非空时）
        validateApprovalNoUnique(null, createReqVO.getApprovalNo());
        // 校验价格/库存/处方药规则
        validatePriceRules(createReqVO);
        validateStockRules(createReqVO.getMinStock(), createReqVO.getMaxStock());
        validateRxRule(createReqVO.getDrugType(), createReqVO.getIsRx());
        // 写入
        DrugDO drug = BeanUtils.toBean(createReqVO, DrugDO.class);
        drugMapper.insert(drug);
        return drug.getId();
    }

    @Override
    public void updateDrug(DrugSaveReqVO updateReqVO) {
        // 校验存在
        validateDrugExists(updateReqVO.getId());
        // 校验编码唯一
        validateDrugCodeUnique(updateReqVO.getId(), updateReqVO.getDrugCode());
        // 校验分类存在且启用
        validateCategoryId(updateReqVO.getCategoryId());
        // 校验批准文号唯一
        validateApprovalNoUnique(updateReqVO.getId(), updateReqVO.getApprovalNo());
        // 校验价格/库存/处方药规则
        validatePriceRules(updateReqVO);
        validateStockRules(updateReqVO.getMinStock(), updateReqVO.getMaxStock());
        validateRxRule(updateReqVO.getDrugType(), updateReqVO.getIsRx());
        // 更新（不通过保存接口修改审核字段，由审核接口单独维护）
        DrugDO updateObj = BeanUtils.toBean(updateReqVO, DrugDO.class);
        drugMapper.updateById(updateObj);
    }

    @Override
    public void deleteDrug(Long id) {
        validateDrugExists(id);
        // 校验是否被条码引用，被引用则拒绝删除
        Long barcodeCount = barcodeMapper.countByDrugId(id);
        if (barcodeCount != null && barcodeCount > 0) {
            throw exception(PHARMACY_DRUG_HAS_BARCODE);
        }
        drugMapper.deleteById(id);
    }

    @Override
    public DrugDO getDrug(Long id) {
        return drugMapper.selectById(id);
    }

    @Override
    public PageResult<DrugDO> getDrugPage(DrugPageReqVO reqVO) {
        return drugMapper.selectPage(reqVO);
    }

    @Override
    public DrugDO validateDrugExists(Long id) {
        if (id == null) {
            throw exception(PHARMACY_DRUG_NOT_EXISTS);
        }
        DrugDO drug = drugMapper.selectById(id);
        if (drug == null) {
            throw exception(PHARMACY_DRUG_NOT_EXISTS);
        }
        return drug;
    }

    @Override
    public Long countByCategoryId(Long categoryId) {
        return drugMapper.countByCategoryId(categoryId);
    }

    @Override
    public void approveDrug(Long id, Integer approveStatus, String auditOpinion) {
        // 校验审核状态参数：只能为 1(通过) 或 2(驳回)
        if (approveStatus == null || (approveStatus != 1 && approveStatus != 2)) {
            throw exception(PHARMACY_DRUG_APPROVE_STATUS_INVALID);
        }
        DrugDO drug = validateDrugExists(id);
        // 仅允许从待审(0)流转到通过(1)或驳回(2)
        if (drug.getApproveStatus() != null && drug.getApproveStatus() != 0) {
            throw exception(PHARMACY_DRUG_APPROVE_DUP);
        }
        // 解析审核人：根据当前登录 system 用户查对应的药店员工编号
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        EmployeeDO employee = employeeService.getEmployeeByUserId(loginUserId);
        if (employee == null) {
            throw exception(PHARMACY_DRUG_AUDITOR_NOT_EMPLOYEE);
        }
        DrugDO update = new DrugDO();
        update.setId(id);
        update.setApproveStatus(approveStatus);
        update.setAuditBy(employee.getId());
        update.setAuditAt(LocalDateTime.now());
        update.setAuditOpinion(auditOpinion);
        drugMapper.updateById(update);
    }

    @Override
    public List<DrugDO> getSimpleDrugList(String keyword) {
        return drugMapper.selectSimpleList(keyword);
    }

    @Override
    public List<DrugDO> getDrugList(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return drugMapper.selectBatchIds(ids);
    }

    @Override
    public void validateDrugList(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        List<DrugDO> drugs = drugMapper.selectBatchIds(ids);
        if (drugs.size() != ids.size()) {
            throw exception(PHARMACY_DRUG_NOT_EXISTS);
        }
        // 校验启用 + 审核通过
        for (DrugDO drug : drugs) {
            if (drug.getStatus() == null || drug.getStatus() != 1) {
                throw exception(PHARMACY_DRUG_NOT_SALEABLE); // 药品已停用
            }
            if (drug.getApproveStatus() == null || drug.getApproveStatus() != 1) {
                throw exception(PHARMACY_DRUG_NOT_SALEABLE); // 药品未审核通过，不可销售
            }
        }
    }

    @Override
    public DrugDO getDrugByCode(String drugCode) {
        if (drugCode == null || drugCode.trim().isEmpty()) {
            return null;
        }
        return drugMapper.selectByDrugCode(drugCode);
    }

    private void validateDrugCodeUnique(Long id, String drugCode) {
        DrugDO drug = drugMapper.selectByDrugCode(drugCode);
        if (drug == null) return;
        if (id == null || !Objects.equals(drug.getId(), id)) {
            throw exception(PHARMACY_DRUG_CODE_DUPLICATE);
        }
    }

    private void validateCategoryId(Long categoryId) {
        if (categoryId == null) return;
        try {
            categoryService.validateCategoryExistsAndEnabled(categoryId);
        } catch (Exception e) {
            throw exception(PHARMACY_DRUG_CATEGORY_NOT_EXISTS);
        }
    }

    private void validateApprovalNoUnique(Long id, String approvalNo) {
        if (approvalNo == null || approvalNo.trim().isEmpty()) return;
        DrugDO drug = drugMapper.selectByApprovalNo(approvalNo);
        if (drug == null) return;
        if (id == null || !Objects.equals(drug.getId(), id)) {
            throw exception(PHARMACY_DRUG_APPROVAL_NO_DUPLICATE);
        }
    }

    /**
     * 镜像 ck_drug_price：会员价/最低限售价 ≤ 零售价
     */
    private void validatePriceRules(DrugSaveReqVO reqVO) {
        BigDecimal retail = reqVO.getRetailPrice();
        if (retail == null) return;
        if (reqVO.getMemberPrice() != null && reqVO.getMemberPrice().compareTo(retail) > 0) {
            throw exception(PHARMACY_DRUG_PRICE_INVALID);
        }
        if (reqVO.getMinSalePrice() != null && reqVO.getMinSalePrice().compareTo(retail) > 0) {
            throw exception(PHARMACY_DRUG_PRICE_INVALID);
        }
    }

    /**
     * 镜像 ck_drug_stock：max_stock=0 或 ≥ min_stock
     */
    private void validateStockRules(Integer minStock, Integer maxStock) {
        if (minStock == null || maxStock == null) return;
        if (maxStock != 0 && maxStock < minStock) {
            throw exception(PHARMACY_DRUG_STOCK_INVALID);
        }
    }

    /**
     * 镜像 ck_drug_rx：drug_type=0(处方) 时 is_rx 必须为 1
     */
    private void validateRxRule(Integer drugType, Integer isRx) {
        if (drugType == null || isRx == null) return;
        if (drugType == 0 && isRx != 1) {
            throw exception(PHARMACY_DRUG_RX_INVALID);
        }
    }

}
