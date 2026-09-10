package cn.iocoder.yudao.module.pharmacy.controller.admin.base;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.category.CategoryPageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.category.CategoryRespVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.category.CategorySaveReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.category.CategorySimpleRespVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.base.CategoryDO;
import cn.iocoder.yudao.module.pharmacy.service.base.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 药品分类")
@RestController
@RequestMapping("/pharmacy/base/category")
@Validated
public class CategoryController {

    @Resource
    private CategoryService categoryService;

    @PostMapping("/create")
    @Operation(summary = "创建药品分类")
    @PreAuthorize("@ss.hasPermission('pharmacy:base:category:create')")
    public CommonResult<Long> createCategory(@Valid @RequestBody CategorySaveReqVO createReqVO) {
        Long id = categoryService.createCategory(createReqVO);
        return success(id);
    }

    @PutMapping("/update")
    @Operation(summary = "更新药品分类")
    @PreAuthorize("@ss.hasPermission('pharmacy:base:category:update')")
    public CommonResult<Boolean> updateCategory(@Valid @RequestBody CategorySaveReqVO updateReqVO) {
        categoryService.updateCategory(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除药品分类")
    @Parameter(name = "id", description = "分类编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pharmacy:base:category:delete')")
    public CommonResult<Boolean> deleteCategory(@RequestParam("id") Long id) {
        categoryService.deleteCategory(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得药品分类详情")
    @Parameter(name = "id", description = "分类编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pharmacy:base:category:query')")
    public CommonResult<CategoryRespVO> getCategory(@RequestParam("id") Long id) {
        CategoryDO category = categoryService.getCategory(id);
        return success(BeanUtils.toBean(category, CategoryRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得药品分类分页")
    @PreAuthorize("@ss.hasPermission('pharmacy:base:category:query')")
    public CommonResult<PageResult<CategoryRespVO>> getCategoryPage(@Validated CategoryPageReqVO pageReqVO) {
        PageResult<CategoryDO> pageResult = categoryService.getCategoryPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, CategoryRespVO.class));
    }

    @GetMapping({"/simple-list", "/list-all-simple"})
    @Operation(summary = "获取启用的药品分类精简列表", description = "只返回启用状态的分类，用于前端下拉")
    public CommonResult<List<CategorySimpleRespVO>> getSimpleCategoryList() {
        List<CategoryDO> list = categoryService.getEnabledCategoryList();
        list.sort(Comparator.comparing(CategoryDO::getSort));
        return success(BeanUtils.toBean(list, CategorySimpleRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出药品分类 Excel")
    @PreAuthorize("@ss.hasPermission('pharmacy:base:category:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportCategory(HttpServletResponse response, @Validated CategoryPageReqVO reqVO) throws IOException {
        reqVO.setPageSize(cn.iocoder.yudao.framework.common.pojo.PageParam.PAGE_SIZE_NONE);
        List<CategoryDO> list = categoryService.getCategoryPage(reqVO).getList();
        ExcelUtils.write(response, "药品分类.xls", "分类列表", CategoryRespVO.class,
                BeanUtils.toBean(list, CategoryRespVO.class));
    }

}
