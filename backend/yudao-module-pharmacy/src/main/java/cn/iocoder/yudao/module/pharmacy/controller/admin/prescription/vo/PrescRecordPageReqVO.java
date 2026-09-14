package cn.iocoder.yudao.module.pharmacy.controller.admin.prescription.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@Data
public class PrescRecordPageReqVO extends PageParam {

    /** 处方号（模糊） */
    private String prescNo;

    /** 门店 */
    private Long storeId;

    /** 来源：0 纸质拍照 / 1 电子处方平台 / 2 复诊续方 */
    private Integer source;

    /** 审方：0 待审 / 1 通过 / 2 驳回 */
    private Integer reviewStatus;

    /** 0 有效 / 1 已完成 / 2 作废 */
    private Integer status;

    /** 患者姓名（模糊） */
    private String patientName;

    /** 审方药师 */
    private Long pharmacistId;

    /** [开始, 结束] 开方日期 */
    private LocalDate[] prescDate;
}
