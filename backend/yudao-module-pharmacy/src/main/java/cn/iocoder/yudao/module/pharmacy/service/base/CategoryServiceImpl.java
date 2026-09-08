package cn.iocoder.yudao.module.pharmacy.service.base;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.category.CategoryPageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.category.CategorySaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.base.CategoryDO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.base.CategoryMapper;
import cn.iocoder.yudao.module.pharmacy.enums.PharmacyStatusEnum;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.*;

/**
 * 药品分类 Service 实现类
 */
@Service
@Validated
public class CategoryServiceImpl implements CategoryService {

    /**
     * 顶级分类的 parentId 约定值：0 或 NULL 均视为顶级
     */
    private static final Long TOP_PARENT_ID = 0L;

    @Resource
    private CategoryMapper categoryMapper;

    @Override
    public Long createCategory(CategorySaveReqVO createReqVO) {
        // 校验编码唯一、上级分类存在
        validateCatCodeUnique(null, createReqVO.getCatCode());
        validateParentExists(createReqVO.getParentId());
        // 写入
        CategoryDO category = BeanUtils.toBean(createReqVO, CategoryDO.class);
        // 顶级分类统一存 0，便于查询
        if (category.getParentId() == null) {
            category.setParentId(TOP_PARENT_ID);
        }
        categoryMapper.insert(category);
        return category.getId();
    }

    @Override
    public void updateCategory(CategorySaveReqVO updateReqVO) {
        // 校验存在
        validateCategoryExists(updateReqVO.getId());
        // 校验编码唯一
        validateCatCodeUnique(updateReqVO.getId(), updateReqVO.getCatCode());
        // 校验上级分类存在且不能是自己或自己的子分类
        validateParentForUpdate(updateReqVO.getId(), updateReqVO.getParentId());
        // 更新
        CategoryDO updateObj = BeanUtils.toBean(updateReqVO, CategoryDO.class);
        if (updateObj.getParentId() == null) {
            updateObj.setParentId(TOP_PARENT_ID);
        }
        categoryMapper.updateById(updateObj);
    }

    @Override
    public void deleteCategory(Long id) {
        // 校验存在
        validateCategoryExists(id);
        // 校验子分类：存在子分类时不可删除
        List<CategoryDO> children = categoryMapper.selectListByParentId(id);
        if (!children.isEmpty()) {
            throw exception(PHARMACY_CATEGORY_EXISTS_CHILDREN);
        }
        // 校验药品引用：被药品引用时不可删除
        Long drugCount = categoryMapper.countDrugsByCategoryId(id);
        if (drugCount != null && drugCount > 0) {
            throw exception(PHARMACY_CATEGORY_HAS_DRUG);
        }
        categoryMapper.deleteById(id);
    }

    @Override
    public CategoryDO getCategory(Long id) {
        return categoryMapper.selectById(id);
    }

    @Override
    public List<CategoryDO> getCategoryList(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return categoryMapper.selectBatchIds(ids);
    }

    @Override
    public PageResult<CategoryDO> getCategoryPage(CategoryPageReqVO reqVO) {
        return categoryMapper.selectPage(reqVO);
    }

    @Override
    public List<CategoryDO> getEnabledCategoryList() {
        return categoryMapper.selectList(CategoryDO::getStatus, PharmacyStatusEnum.ENABLE.getStatus());
    }

    @Override
    public CategoryDO validateCategoryExistsAndEnabled(Long id) {
        CategoryDO category = categoryMapper.selectById(id);
        if (category == null) {
            throw exception(PHARMACY_CATEGORY_NOT_EXISTS);
        }
        if (!PharmacyStatusEnum.isEnable(category.getStatus())) {
            throw exception(PHARMACY_CATEGORY_NOT_ENABLE, category.getCatName());
        }
        return category;
    }

    private void validateCategoryExists(Long id) {
        if (id == null) {
            throw exception(PHARMACY_CATEGORY_NOT_EXISTS);
        }
        if (categoryMapper.selectById(id) == null) {
            throw exception(PHARMACY_CATEGORY_NOT_EXISTS);
        }
    }

    private void validateCatCodeUnique(Long id, String catCode) {
        CategoryDO category = categoryMapper.selectByCatCode(catCode);
        if (category == null) {
            return;
        }
        if (id == null) {
            throw exception(PHARMACY_CATEGORY_CODE_DUPLICATE);
        }
        if (!Objects.equals(category.getId(), id)) {
            throw exception(PHARMACY_CATEGORY_CODE_DUPLICATE);
        }
    }

    private void validateParentExists(Long parentId) {
        if (parentId == null || TOP_PARENT_ID.equals(parentId)) {
            return;
        }
        if (categoryMapper.selectById(parentId) == null) {
            throw exception(PHARMACY_CATEGORY_PARENT_NOT_EXISTS);
        }
    }

    private void validateParentForUpdate(Long id, Long parentId) {
        if (parentId == null || TOP_PARENT_ID.equals(parentId)) {
            return;
        }
        // 不能设自己为上级
        if (Objects.equals(parentId, id)) {
            throw exception(PHARMACY_CATEGORY_PARENT_ERROR);
        }
        // 上级必须存在
        CategoryDO parent = categoryMapper.selectById(parentId);
        if (parent == null) {
            throw exception(PHARMACY_CATEGORY_PARENT_NOT_EXISTS);
        }
        // 不能设自己的子分类为上级：沿子链向下查找
        Long cursor = parentId;
        while (cursor != null && !TOP_PARENT_ID.equals(cursor)) {
            CategoryDO parentOfCursor = categoryMapper.selectById(cursor);
            if (parentOfCursor == null) {
                break;
            }
            if (Objects.equals(parentOfCursor.getId(), id)) {
                throw exception(PHARMACY_CATEGORY_PARENT_IS_CHILD);
            }
            cursor = parentOfCursor.getParentId();
        }
    }

}
