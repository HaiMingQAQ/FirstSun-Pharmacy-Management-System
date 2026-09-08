package cn.iocoder.yudao.module.pharmacy.dal.mysql.base;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.category.CategoryPageReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.base.CategoryDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 药品分类 Mapper
 *
 * 注意：{@link #countDrugsByCategoryId(Long)} 用于删除前校验：被药品引用的分类不可删除。
 * 该查询读取 A 自己维护的 {@code ph_drug} 表。后续阶段 DrugMapper 完整实现后可迁移该方法。
 */
@Mapper
public interface CategoryMapper extends BaseMapperX<CategoryDO> {

    default PageResult<CategoryDO> selectPage(CategoryPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<CategoryDO>()
                .likeIfPresent(CategoryDO::getCatCode, reqVO.getCatCode())
                .likeIfPresent(CategoryDO::getCatName, reqVO.getCatName())
                .eqIfPresent(CategoryDO::getCatType, reqVO.getCatType())
                .eqIfPresent(CategoryDO::getStatus, reqVO.getStatus())
                .eqIfPresent(CategoryDO::getParentId, reqVO.getParentId())
                .orderByAsc(CategoryDO::getSort)
                .orderByDesc(CategoryDO::getId));
    }

    default CategoryDO selectByCatCode(String catCode) {
        return selectOne(CategoryDO::getCatCode, catCode);
    }

    default List<CategoryDO> selectListByParentId(Long parentId) {
        return selectList(CategoryDO::getParentId, parentId);
    }

    /**
     * 统计某分类下未删除的药品数量。
     *
     * 用于删除分类前校验：存在引用时拒绝删除。{@code ph_drug} 为 A 维护表。
     */
    @Select("SELECT COUNT(*) FROM ph_drug WHERE category_id = #{categoryId} AND deleted = 0")
    Long countDrugsByCategoryId(@Param("categoryId") Long categoryId);

}
