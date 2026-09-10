package cn.iocoder.yudao.module.pharmacy.api.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 药品跨模块 Response DTO
 *
 * 仅供其他模块（销售、库存、采购等）只读使用。
 * 字段为精简版，避免暴露成本价、审核意见、审核人等内部字段。
 *
 * @author A 成员
 */
@Data
public class DrugRespDTO {

    /**
     * 药品编号
     */
    private Long id;
    /**
     * 药品编码
     */
    private String drugCode;
    /**
     * 分类编号
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
     * 拼音码
     */
    private String spellCode;
    /**
     * 规格
     */
    private String specification;
    /**
     * 剂型
     */
    private String dosageForm;
    /**
     * 厂家
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
     * 是否处方药 0否/1是
     */
    private Integer isRx;
    /**
     * 是否特管药 0否/1是
     */
    private Integer isSpecial;
    /**
     * 是否含麻黄碱 0否/1是
     */
    private Integer isPseudoephedrine;
    /**
     * 是否冷链 0否/1是
     */
    private Integer isColdChain;
    /**
     * 销售单位
     */
    private String unit;
    /**
     * 零售价
     */
    private BigDecimal retailPrice;
    /**
     * 会员价
     */
    private BigDecimal memberPrice;
    /**
     * 最低限售价
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
     * 储存条件 0常温/1阴凉/2冷藏/3冷冻
     */
    private Integer storageCond;
    /**
     * 是否需要效期管理 0否/1是
     */
    private Integer needExpiry;
    /**
     * 是否支持线上销售 0否/1是
     */
    private Integer saleableOnline;
    /**
     * 启用状态 1启用/0停用
     */
    private Integer status;
    /**
     * 审核状态 0待审/1通过/2驳回（仅已通过 1 的药品方可销售）
     */
    private Integer approveStatus;

}
