package cn.iocoder.yudao.module.pharmacy.dal.dataobject.base;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.module.pharmacy.enums.PharmacyStatusEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 药品档案 DO
 *
 * 对应数据表 {@code ph_drug}，字段以 {@code sql/firstsun_pharmacy_init.sql} 真实定义为准。
 *
 * 唯一约束：
 * 1. {@code uk_drug_code(drug_code)} - 药品编码全局唯一
 *
 * 关联：
 * - {@link #categoryId} → ph_category.id（通过 CategoryService 只读校验）
 * - {@link #auditBy} → ph_employee.id（审核员工，弱关联，仅记录编号）
 *
 * DB CHECK 约束（应用层镜像校验）：
 * - ck_drug_price：retail_price≥0、member/cost/min_sale_price≥0
 * - ck_drug_stock：max_stock=0 或 max_stock≥min_stock
 * - ck_drug_rx：drug_type=0(处方) 时 is_rx 必须为 1
 *
 * 状态 {@link #status} 沿用 {@link PharmacyStatusEnum}（1启用/0停用）。
 * {@link #approveStatus} 三态：0待审/1通过/2驳回，独立于 status。
 */
@TableName(value = "ph_drug", autoResultMap = true)
@KeySequence("ph_drug_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class DrugDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 药品编码（系统内码，唯一）
     */
    private String drugCode;
    /**
     * 分类（ph_category.id）
     */
    private Long categoryId;
    /**
     * 通用名
     */
    private String genericName;
    /**
     * 商品名
     */
    private String tradeName;
    /**
     * 拼音码（如 amxljn）
     */
    private String spellCode;
    /**
     * 规格（如 0.25g*24粒）
     */
    private String specification;
    /**
     * 剂型（字典）
     */
    private String dosageForm;
    /**
     * 生产厂家
     */
    private String manufacturer;
    /**
     * 批准文号
     */
    private String approvalNo;
    /**
     * 药品类型 0处方/1OTC甲/2OTC乙/3特管/4饮片/5保健/6器械/7日化/8其他
     */
    private Integer drugType;
    /**
     * 是否处方药（收银强制审方 POS-005）
     */
    private Integer isRx;
    /**
     * 是否特殊管理药品（REG-002）
     */
    private Integer isSpecial;
    /**
     * 含麻黄碱类（REG-006 限购）
     */
    private Integer isPseudoephedrine;
    /**
     * 是否冷链（REG-010）
     */
    private Integer isColdChain;
    /**
     * 销售单位（盒/瓶/支）
     */
    private String unit;
    /**
     * 转换比（1盒=12粒）
     */
    private Integer conversionRatio;
    /**
     * 零售价
     */
    private BigDecimal retailPrice;
    /**
     * 会员价
     */
    private BigDecimal memberPrice;
    /**
     * 参考成本价
     */
    private BigDecimal costPrice;
    /**
     * 最低限售价（POS-004）
     */
    private BigDecimal minSalePrice;
    /**
     * 税率
     */
    private BigDecimal taxRate;
    /**
     * 医保类别 0自费/1甲类/2乙类
     */
    private Integer insuranceType;
    /**
     * 库存下限
     */
    private Integer minStock;
    /**
     * 库存上限
     */
    private Integer maxStock;
    /**
     * 储存条件 0常温/1阴凉/2冷藏/3冷冻
     */
    private Integer storageCond;
    /**
     * 是否效期管理
     */
    private Integer needExpiry;
    /**
     * 默认货位编号
     */
    private Long defaultLocationId;
    /**
     * 线上可售开关
     */
    private Integer saleableOnline;
    /**
     * 启用状态 1启用/0停用（{@link PharmacyStatusEnum}）
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;
    /**
     * 封面图片
     */
    private String imageUrl;
    /**
     * 图片地址数组
     *
     * DB 类型 JSON，使用 {@link JacksonTypeHandler} 序列化为 List<String>
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> images;
    /**
     * 药品说明信息
     */
    private String description;
    /**
     * 说明书文件
     */
    private String instructionsUrl;
    /**
     * 品种审核状态 0待审/1通过/2驳回
     */
    private Integer approveStatus;
    /**
     * 审核员工（ph_employee.id）
     */
    private Long auditBy;
    /**
     * 审核时间
     */
    private LocalDateTime auditAt;
    /**
     * 审核意见
     */
    private String auditOpinion;

}
