package cn.iocoder.yudao.module.pharmacy.dal.dataobject.prescription;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 处方记录（表：ph_presc_record）
 *
 * <p>任务E（E 维护）：处方登记、药师审核、处方台账。JSON 字段（images/prescribedItems）
 * 以字符串存储，由 Service 层负责序列化/反序列化。
 *
 * <p>审方状态机：reviewStatus 0 待审 → 1 通过 / 2 驳回（驳回后不可再审，需重新登记）；
 * status 0 有效 / 1 已完成（销售引用后） / 2 作废。
 */
@TableName("ph_presc_record")
@Data
@EqualsAndHashCode(callSuper = true)
public class PhPrescRecordDO extends BaseDO {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 处方号（PX-门店-yyyyMMdd-流水，唯一） */
    private String prescNo;

    /** 门店 */
    private Long storeId;

    /** 来源：0 纸质拍照 / 1 电子处方平台 / 2 复诊续方 */
    private Integer source;

    /** 开具医院 */
    private String hospital;

    /** 医师姓名 */
    private String doctorName;

    /** 患者姓名 */
    private String patientName;

    /** 年龄 */
    private Integer patientAge;

    /** 患者身份证（AES 加密） */
    private String patientIdNo;

    /** 诊断 */
    private String diagnosis;

    /** 用法用量 */
    private String usageDesc;

    /** 开方日期 */
    private LocalDate prescDate;

    /** 处方影像 URL */
    private String imageUrl;

    /** 审方：0 待审 / 1 通过 / 2 驳回 */
    private Integer reviewStatus;

    /** 审方药师（ph_employee） */
    private Long pharmacistId;

    /** 审方时间 */
    private LocalDateTime reviewAt;

    /** 审方意见（驳回必填） */
    private String reviewOpinion;

    /** 电子签名信息 */
    private String reviewSnapshot;

    /** 是否特管登记（双人复核）：0 否 / 1 是 */
    private Integer isSpecial;

    /** 双人复核人 */
    private Long dblCheckBy;

    /** 是否超量复核：0 否 / 1 是 */
    private Integer limitCheck;

    /** 0 有效 / 1 已完成 / 2 作废 */
    private Integer status;

    /** 小程序上传人 */
    private Long wxMemberId;

    /** 最多 5 张影像（JSON 字符串，由应用校验条数） */
    private String images;

    /** 药品 ID、核准数量与用法（JSON 字符串），购药累计在事务内核验 */
    private String prescribedItems;
}
