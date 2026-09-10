package cn.iocoder.yudao.module.pharmacy.dal.dataobject.base;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.module.pharmacy.enums.PharmacyStatusEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 药品分类 DO
 *
 * 对应数据表 {@code ph_category}。
 * 字段以 {@code sql/firstsun_pharmacy_init.sql} 真实定义为准。
 *
 * 注意：{@link #status} 取值与 yudao {@code CommonStatusEnum} 方向相反，
 * 使用 {@link PharmacyStatusEnum}（1=启用，0=停用）。
 */
@TableName("ph_category")
@KeySequence("ph_category_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class CategoryDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 分类编码
     *
     * 唯一，参见建表脚本 uk_cat_code
     */
    private String catCode;
    /**
     * 分类名
     */
    private String catName;
    /**
     * 上级分类编号
     *
     * 0 表示顶级分类；NULL 视为顶级
     */
    private Long parentId;
    /**
     * 分类类型
     *
     * 0药品/1保健品/2医疗器械/3中药饮片/4日化/5其他
     */
    private Integer catType;
    /**
     * 排序
     */
    private Integer sort;
    /**
     * 启用状态
     *
     * 枚举 {@link PharmacyStatusEnum}
     */
    private Integer status;

}
