package cn.iocoder.yudao.module.pharmacy.service.prescription;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.pharmacy.controller.admin.prescription.vo.PrescRecordPageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.prescription.vo.PrescRecordReviewReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.prescription.vo.PrescRecordSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.prescription.PhPrescRecordDO;

/**
 * 处方记录 Service（E 维护）
 */
public interface PrescRecordService {

    /**
     * 登记处方（审方状态为待审）
     *
     * @param createReqVO 登记请求
     * @return 处方编号
     */
    Long createPrescRecord(PrescRecordSaveReqVO createReqVO);

    /**
     * 药师审核处方：通过（1）或驳回（2）
     *
     * @param reviewReqVO 审核请求
     */
    void reviewPrescRecord(PrescRecordReviewReqVO reviewReqVO);

    /**
     * 作废处方（仅有效状态可作废）
     *
     * @param id 处方编号
     */
    void invalidatePrescRecord(Long id);

    /**
     * 获得处方分页（台账）
     */
    PageResult<PhPrescRecordDO> getPrescRecordPage(PrescRecordPageReqVO pageReqVO);

    /**
     * 获得处方详情
     *
     * @param id 处方编号
     * @return 处方记录
     */
    PhPrescRecordDO getPrescRecord(Long id);
}
