package cn.iocoder.yudao.module.pharmacy.controller.admin.base;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.drug.DrugPageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.drug.DrugRespVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.drug.DrugSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.drug.DrugSimpleRespVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.base.CategoryDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.base.DrugDO;
import cn.iocoder.yudao.module.pharmacy.service.base.CategoryService;
import cn.iocoder.yudao.module.pharmacy.service.base.DrugService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 药品档案")
@RestController
@RequestMapping("/pharmacy/base/drug")
@Validated
public class DrugController {

    @Resource
    private DrugService drugService;

    @Resource
    private CategoryService categoryService;

    @PostMapping("/create")
    @Operation(summary = "创建药品")
    @PreAuthorize("@ss.hasPermission('pharmacy:base:drug:create')")
    public CommonResult<Long> createDrug(@Valid @RequestBody DrugSaveReqVO createReqVO) {
        Long id = drugService.createDrug(createReqVO);
        return success(id);
    }

    @PutMapping("/update")
    @Operation(summary = "更新药品")
    @PreAuthorize("@ss.hasPermission('pharmacy:base:drug:update')")
    public CommonResult<Boolean> updateDrug(@Valid @RequestBody DrugSaveReqVO updateReqVO) {
        drugService.updateDrug(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除药品")
    @Parameter(name = "id", description = "药品编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pharmacy:base:drug:delete')")
    public CommonResult<Boolean> deleteDrug(@RequestParam("id") Long id) {
        drugService.deleteDrug(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得药品详情")
    @Parameter(name = "id", description = "药品编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pharmacy:base:drug:query')")
    public CommonResult<DrugRespVO> getDrug(@RequestParam("id") Long id) {
        DrugDO drug = drugService.getDrug(id);
        DrugRespVO respVO = BeanUtils.toBean(drug, DrugRespVO.class);
        populateCategoryName(respVO);
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得药品分页")
    @PreAuthorize("@ss.hasPermission('pharmacy:base:drug:query')")
    public CommonResult<PageResult<DrugRespVO>> getDrugPage(@Validated DrugPageReqVO pageReqVO) {
        PageResult<DrugDO> pageResult = drugService.getDrugPage(pageReqVO);
        PageResult<DrugRespVO> result = BeanUtils.toBean(pageResult, DrugRespVO.class);
        populateCategoryNames(result.getList());
        return success(result);
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出药品 Excel")
    @PreAuthorize("@ss.hasPermission('pharmacy:base:drug:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportDrug(HttpServletResponse response, @Validated DrugPageReqVO reqVO) throws IOException {
        reqVO.setPageSize(cn.iocoder.yudao.framework.common.pojo.PageParam.PAGE_SIZE_NONE);
        List<DrugDO> list = drugService.getDrugPage(reqVO).getList();
        List<DrugRespVO> respList = BeanUtils.toBean(list, DrugRespVO.class);
        populateCategoryNames(respList);
        ExcelUtils.write(response, "药品档案.xls", "药品列表", DrugRespVO.class, respList);
    }

    @PutMapping("/approve")
    @Operation(summary = "审核药品")
    @PreAuthorize("@ss.hasPermission('pharmacy:base:drug:approve')")
    public CommonResult<Boolean> approveDrug(
            @RequestParam("id") Long id,
            @RequestParam("approveStatus") Integer approveStatus,
            @RequestParam(value = "auditOpinion", required = false) String auditOpinion) {
        // 审核人在 Service 内部根据当前登录用户解析为 ph_employee.id
        drugService.approveDrug(id, approveStatus, auditOpinion);
        return success(true);
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获取药品精简列表（下拉/条码页面选择使用）")
    @Parameter(name = "keyword", description = "关键字（匹配药品编码/通用名/拼音码）", example = "阿莫西林")
    public CommonResult<List<DrugSimpleRespVO>> getSimpleDrugList(@RequestParam(value = "keyword", required = false) String keyword) {
        List<DrugDO> list = drugService.getSimpleDrugList(keyword);
        return success(BeanUtils.toBean(list, DrugSimpleRespVO.class));
    }

    /**
     * 批量补充分类名称
     */
    private void populateCategoryNames(List<DrugRespVO> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Set<Long> categoryIds = list.stream().map(DrugRespVO::getCategoryId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> categoryMap = new HashMap<>();
        if (!categoryIds.isEmpty()) {
            List<CategoryDO> categories = categoryService.getCategoryList(categoryIds);
            for (CategoryDO category : categories) {
                categoryMap.put(category.getId(), category.getCatName());
            }
        }
        for (DrugRespVO vo : list) {
            if (vo.getCategoryId() != null) {
                vo.setCategoryName(categoryMap.get(vo.getCategoryId()));
            }
        }
    }

    private void populateCategoryName(DrugRespVO vo) {
        if (vo == null || vo.getCategoryId() == null) {
            return;
        }
        CategoryDO category = categoryService.getCategory(vo.getCategoryId());
        if (category != null) {
            vo.setCategoryName(category.getCatName());
        }
    }

}
