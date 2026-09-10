package cn.iocoder.yudao.module.pharmacy.service.base;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.category.CategoryPageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.category.CategorySaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.base.CategoryDO;

import jakarta.validation.Valid;
import java.util.Collection;
import java.util.List;

/**
 * 药品分类 Service
 */
public interface CategoryService {

    /**
     * 创建药品分类
     */
    Long createCategory(@Valid CategorySaveReqVO createReqVO);

    /**
     * 更新药品分类
     */
    void updateCategory(@Valid CategorySaveReqVO updateReqVO);

    /**
     * 删除药品分类
     *
     * 删除前校验：存在子分类或被药品引用时拒绝删除。
     */
    void deleteCategory(Long id);

    /**
     * 获取药品分类详情
     */
    CategoryDO getCategory(Long id);

    /**
     * 批量获得药品分类（用于列表页补充分类名称）
     *
     * @param ids 分类编号集合
     * @return 分类列表（仅返回未删除的）
     */
    List<CategoryDO> getCategoryList(Collection<Long> ids);

    /**
     * 获取药品分类分页
     */
    PageResult<CategoryDO> getCategoryPage(CategoryPageReqVO reqVO);

    /**
     * 获取启用状态的分类精简列表（用于前端下拉）
     */
    List<CategoryDO> getEnabledCategoryList();

    /**
     * 校验分类存在且启用，供药品档案等场景使用
     */
    CategoryDO validateCategoryExistsAndEnabled(Long id);

}
