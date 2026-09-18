package cn.iocoder.yudao.module.pharmacy.service.prescription;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.pharmacy.controller.admin.prescription.vo.PrescItemVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.prescription.vo.PrescRecordPageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.prescription.vo.PrescRecordRespVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.prescription.vo.PrescRecordReviewReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.prescription.vo.PrescRecordSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.base.DrugDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.base.EmployeeDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.base.StoreDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.prescription.PhPrescRecordDO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.prescription.PrescRecordMapper;
import cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants;
import cn.iocoder.yudao.module.pharmacy.service.base.DrugService;
import cn.iocoder.yudao.module.pharmacy.service.base.EmployeeService;
import cn.iocoder.yudao.module.pharmacy.service.base.StoreService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;

/**
 * 处方记录 Service 实现（E 维护）
 *
 * <p>业务规则：
 * <ul>
 *   <li>登记：处方号可前端预生成（幂等键），否则后端生成 PX-{门店}-{yyyyMMdd}-{4位流水}；影像最多 5 张。</li>
 *   <li>审核：仅待审可审；驳回必填意见；通过时特管处方（isSpecial=1）必须双人复核，超量处方（limitCheck=1）必须填写复核意见。</li>
 *   <li>作废：仅有效（status=0）可作废；已完成（status=1）不可作废。</li>
 * </ul>
 */
@Service
@Slf4j
public class PrescRecordServiceImpl implements PrescRecordService {

    /** 处方号日期格式 */
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    /** 影像最多 5 张 */
    private static final int IMAGES_MAX = 5;

    @Resource
    private PrescRecordMapper prescRecordMapper;
    @Resource
    private EmployeeService employeeService;
    @Resource
    private StoreService storeService;
    @Resource
    private DrugService drugService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPrescRecord(PrescRecordSaveReqVO createReqVO) {
        // 0. 校验门店存在（处方必须归属真实门店，保证与门店档案关联）
        Long storeId = createReqVO.getStoreId();
        if (storeId == null || storeService.getStore(storeId) == null) {
            throw exception(ErrorCodeConstants.PHARMACY_STORE_NOT_EXISTS);
        }
        // 1. 校验来源
        Integer source = createReqVO.getSource();
        if (source == null || source < 0 || source > 2) {
            throw exception(ErrorCodeConstants.PRESC_SOURCE_INVALID);
        }
        // 2. 处方号：前端预生成则校验唯一，否则后端生成
        String prescNo = createReqVO.getPrescNo();
        if (StrUtil.isBlank(prescNo)) {
            prescNo = generatePrescNo(createReqVO.getStoreId());
        } else if (prescRecordMapper.selectByPrescNo(prescNo) != null) {
            throw exception(ErrorCodeConstants.PRESC_NO_DUPLICATE);
        }
        // 3. 影像最多 5 张
        List<String> images = createReqVO.getImages();
        if (images != null && images.size() > IMAGES_MAX) {
            throw exception(ErrorCodeConstants.PRESC_IMAGES_EXCEED);
        }
        // 3.5 校验药品明细：药品必须存在于药品档案，并自动带出名称/规格快照（保证数据来源）
        List<PrescItemVO> items = createReqVO.getItems();
        if (items == null || items.isEmpty()) {
            throw exception(ErrorCodeConstants.PRESC_ITEMS_REQUIRED);
        }
        for (PrescItemVO item : items) {
            if (item.getDrugId() == null) {
                throw exception(ErrorCodeConstants.PHARMACY_DRUG_NOT_EXISTS);
            }
            DrugDO drug = drugService.getDrug(item.getDrugId());
            if (drug == null) {
                throw exception(ErrorCodeConstants.PHARMACY_DRUG_NOT_EXISTS);
            }
            if (StrUtil.isBlank(item.getDrugName())) {
                item.setDrugName(StrUtil.isBlank(drug.getTradeName())
                        ? drug.getGenericName()
                        : drug.getGenericName() + "(" + drug.getTradeName() + ")");
            }
            if (StrUtil.isBlank(item.getSpecification())) {
                item.setSpecification(drug.getSpecification());
            }
        }
        // 4. 组装 DO
        PhPrescRecordDO record = new PhPrescRecordDO();
        record.setPrescNo(prescNo);
        record.setStoreId(createReqVO.getStoreId());
        record.setSource(source);
        record.setHospital(createReqVO.getHospital());
        record.setDoctorName(createReqVO.getDoctorName());
        record.setPatientName(createReqVO.getPatientName());
        record.setPatientAge(createReqVO.getPatientAge());
        record.setPatientIdNo(createReqVO.getPatientIdNo());
        record.setDiagnosis(createReqVO.getDiagnosis());
        record.setUsageDesc(createReqVO.getUsageDesc());
        record.setPrescDate(createReqVO.getPrescDate());
        record.setImageUrl(createReqVO.getImageUrl());
        record.setIsSpecial(createReqVO.getIsSpecial() != null ? createReqVO.getIsSpecial() : 0);
        record.setLimitCheck(createReqVO.getLimitCheck() != null ? createReqVO.getLimitCheck() : 0);
        record.setImages(images != null ? JsonUtils.toJsonString(images) : null);
        record.setPrescribedItems(JsonUtils.toJsonString(items));
        record.setReviewStatus(0); // 待审
        record.setStatus(0);       // 有效
        record.setWxMemberId(createReqVO.getWxMemberId());
        prescRecordMapper.insert(record);
        log.info("[createPrescRecord] 处方登记成功, id={}, prescNo={}, patient={}", record.getId(), prescNo, createReqVO.getPatientName());
        return record.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewPrescRecord(PrescRecordReviewReqVO reviewReqVO) {
        // 1. 校验处方存在且待审
        PhPrescRecordDO record = prescRecordMapper.selectById(reviewReqVO.getId());
        if (record == null) {
            throw exception(ErrorCodeConstants.PRESC_NOT_EXISTS);
        }
        if (!Integer.valueOf(0).equals(record.getReviewStatus())) {
            throw exception(ErrorCodeConstants.PRESC_REVIEW_ALREADY);
        }
        // 2. 校验审核结果与意见
        Integer reviewStatus = reviewReqVO.getReviewStatus();
        if (reviewStatus == null || (reviewStatus != 1 && reviewStatus != 2)) {
            throw exception(ErrorCodeConstants.PRESC_REVIEW_STATUS_INVALID);
        }
        if (reviewStatus == 2 && StrUtil.isBlank(reviewReqVO.getReviewOpinion())) {
            throw exception(ErrorCodeConstants.PRESC_REVIEW_OPINION_REQUIRED);
        }
        // 3. 审方权限：当前登录用户必须绑定药店员工
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        EmployeeDO employee = employeeService.getEmployeeByUserId(loginUserId);
        if (employee == null) {
            throw exception(ErrorCodeConstants.PRESC_REVIEW_AUDITOR_NOT_EMPLOYEE);
        }
        // 4. 通过时特管/超量复核校验
        if (reviewStatus == 1) {
            if (Integer.valueOf(1).equals(record.getIsSpecial()) && reviewReqVO.getDblCheckBy() == null) {
                throw exception(ErrorCodeConstants.PRESC_REVIEW_DBL_REQUIRED);
            }
            if (Integer.valueOf(1).equals(record.getLimitCheck()) && StrUtil.isBlank(reviewReqVO.getReviewOpinion())) {
                throw exception(ErrorCodeConstants.PRESC_REVIEW_LIMIT_OPINION_REQUIRED);
            }
        }
        // 5. 回填审核信息
        PhPrescRecordDO update = new PhPrescRecordDO();
        update.setId(record.getId());
        update.setReviewStatus(reviewStatus);
        update.setPharmacistId(employee.getId());
        update.setReviewAt(LocalDateTime.now());
        update.setReviewOpinion(reviewReqVO.getReviewOpinion());
        update.setReviewSnapshot(reviewReqVO.getReviewSnapshot());
        update.setDblCheckBy(reviewReqVO.getDblCheckBy());
        prescRecordMapper.updateById(update);
        log.info("[reviewPrescRecord] 处方审核完成, id={}, prescNo={}, result={}, pharmacist={}",
                record.getId(), record.getPrescNo(), reviewStatus, employee.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void invalidatePrescRecord(Long id) {
        PhPrescRecordDO record = prescRecordMapper.selectById(id);
        if (record == null) {
            throw exception(ErrorCodeConstants.PRESC_NOT_EXISTS);
        }
        if (!Integer.valueOf(0).equals(record.getStatus())) {
            throw exception(ErrorCodeConstants.PRESC_STATUS_INVALID);
        }
        PhPrescRecordDO update = new PhPrescRecordDO();
        update.setId(id);
        update.setStatus(2); // 作废
        prescRecordMapper.updateById(update);
        log.info("[invalidatePrescRecord] 处方作废, id={}, prescNo={}", id, record.getPrescNo());
    }

    @Override
    public PageResult<PrescRecordRespVO> getPrescRecordPage(PrescRecordPageReqVO pageReqVO) {
        PageResult<PhPrescRecordDO> pageResult = prescRecordMapper.selectPage(pageReqVO);
        List<PrescRecordRespVO> list = pageResult.getList().stream()
                .map(this::toRespVO)
                .collect(java.util.stream.Collectors.toList());
        return new PageResult<>(list, pageResult.getTotal());
    }

    @Override
    public PrescRecordRespVO getPrescRecord(Long id) {
        return toRespVO(prescRecordMapper.selectById(id));
    }

    /**
     * 组装响应 VO，并填充门店名称 / 审方药师姓名 / 双人复核人姓名（保证数据关联可读）
     */
    private PrescRecordRespVO toRespVO(PhPrescRecordDO record) {
        if (record == null) {
            return null;
        }
        PrescRecordRespVO respVO = new PrescRecordRespVO();
        org.springframework.beans.BeanUtils.copyProperties(record, respVO);
        if (record.getStoreId() != null) {
            StoreDO store = storeService.getStore(record.getStoreId());
            respVO.setStoreName(store != null ? store.getStoreName() : null);
        }
        if (record.getPharmacistId() != null) {
            EmployeeDO pharmacist = employeeService.getEmployee(record.getPharmacistId());
            respVO.setPharmacistName(pharmacist != null ? pharmacist.getEmpName() : null);
        }
        if (record.getDblCheckBy() != null) {
            EmployeeDO dblChecker = employeeService.getEmployee(record.getDblCheckBy());
            respVO.setDblCheckByName(dblChecker != null ? dblChecker.getEmpName() : null);
        }
        return respVO;
    }

    /**
     * 生成处方号：PX-{门店}-{yyyyMMdd}-{4位流水}
     */
    private String generatePrescNo(Long storeId) {
        String yyyymmdd = LocalDate.now().format(DATE_FMT);
        Integer maxSeq = prescRecordMapper.selectMaxSeqByDate(storeId, yyyymmdd);
        int nextSeq = (maxSeq == null ? 0 : maxSeq) + 1;
        return StrUtil.format("PX-{}-{}-{}", storeId, yyyymmdd, StrUtil.padPre(String.valueOf(nextSeq), 4, '0'));
    }

    @SuppressWarnings("unused")
    private List<PrescItemVO> parseItems(String json) {
        return StrUtil.isBlank(json) ? new ArrayList<>() : JsonUtils.parseArray(json, PrescItemVO.class);
    }
}
