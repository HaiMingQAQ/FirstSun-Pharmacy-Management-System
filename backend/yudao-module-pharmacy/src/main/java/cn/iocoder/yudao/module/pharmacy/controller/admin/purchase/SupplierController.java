package cn.iocoder.yudao.module.pharmacy.controller.admin.purchase;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.supplier.SupplierPageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.supplier.SupplierRespVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.supplier.SupplierSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.supplier.SupplierSimpleRespVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.purchase.SupplierDO;
import cn.iocoder.yudao.module.pharmacy.service.purchase.SupplierService;
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
import java.util.List;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 供应商
 *
 * @author B 成员
 */
@Tag(name = "管理后台 - 供应商")
@RestController
@RequestMapping("/pharmacy/purchase/supplier")
@Validated
public class SupplierController {

    @Resource
    private SupplierService supplierService;

    @PostMapping("/create")
    @Operation(summary = "创建供应商")
    @PreAuthorize("@ss.hasPermission('pharmacy:purchase:supplier:create')")
    public CommonResult<Long> createSupplier(@Valid @RequestBody SupplierSaveReqVO createReqVO) {
        return success(supplierService.createSupplier(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新供应商")
    @PreAuthorize("@ss.hasPermission('pharmacy:purchase:supplier:update')")
    public CommonResult<Boolean> updateSupplier(@Valid @RequestBody SupplierSaveReqVO updateReqVO) {
        supplierService.updateSupplier(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除供应商")
    @Parameter(name = "id", description = "供应商编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pharmacy:purchase:supplier:delete')")
    public CommonResult<Boolean> deleteSupplier(@RequestParam("id") Long id) {
        supplierService.deleteSupplier(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得供应商详情", description = "返回完整银行账号，供编辑表单使用；列表接口只返回掩码")
    @Parameter(name = "id", description = "供应商编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pharmacy:purchase:supplier:query')")
    public CommonResult<SupplierRespVO> getSupplier(@RequestParam("id") Long id) {
        SupplierDO supplier = supplierService.getSupplier(id);
        return success(BeanUtils.toBean(supplier, SupplierRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得供应商分页", description = "银行账号按 NFR-12 只返回掩码字段 bankAccountMasked，不返回原文")
    @PreAuthorize("@ss.hasPermission('pharmacy:purchase:supplier:query')")
    public CommonResult<PageResult<SupplierRespVO>> getSupplierPage(@Validated SupplierPageReqVO pageReqVO) {
        PageResult<SupplierDO> pageResult = supplierService.getSupplierPage(pageReqVO);
        PageResult<SupplierRespVO> result = BeanUtils.toBean(pageResult, SupplierRespVO.class);
        result.getList().forEach(SupplierController::maskForList);
        return success(result);
    }

    @GetMapping({"/simple-list", "/list-all-simple"})
    @Operation(summary = "获取可采购供应商精简列表", description = "只返回启用且首营审核通过的供应商，用于采购订单/收货单下拉")
    public CommonResult<List<SupplierSimpleRespVO>> getSimpleSupplierList(
            @RequestParam(value = "keyword", required = false) String keyword) {
        List<SupplierDO> list = supplierService.getSimpleSupplierList(keyword);
        return success(BeanUtils.toBean(list, SupplierSimpleRespVO.class));
    }

    @GetMapping("/all-simple-list")
    @Operation(summary = "获取全部供应商精简列表",
            description = "不限启停与审核状态，用于供应商证照登记与筛选（首营资料登记发生在审核之前）")
    public CommonResult<List<SupplierSimpleRespVO>> getAllSupplierList() {
        List<SupplierDO> list = supplierService.getAllSupplierList();
        return success(BeanUtils.toBean(list, SupplierSimpleRespVO.class));
    }

    @PutMapping("/approve")
    @Operation(summary = "供应商首营审核", description = "approveStatus 只能为 1(通过) 或 2(驳回)；只能从待审状态流转一次")
    @Parameter(name = "id", description = "供应商编号", required = true, example = "1024")
    @Parameter(name = "approveStatus", description = "审核结果 1通过/2驳回", required = true, example = "1")
    @Parameter(name = "auditOpinion", description = "审核意见", example = "资质齐全，同意合作")
    @PreAuthorize("@ss.hasPermission('pharmacy:purchase:supplier:approve')")
    public CommonResult<Boolean> approveSupplier(@RequestParam("id") Long id,
                                                 @RequestParam("approveStatus") Integer approveStatus,
                                                 @RequestParam(value = "auditOpinion", required = false) String auditOpinion) {
        supplierService.approveSupplier(id, approveStatus, auditOpinion);
        return success(true);
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出供应商 Excel")
    @PreAuthorize("@ss.hasPermission('pharmacy:purchase:supplier:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportSupplier(HttpServletResponse response, @Validated SupplierPageReqVO reqVO) throws IOException {
        reqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<SupplierDO> list = supplierService.getSupplierPage(reqVO).getList();
        List<SupplierRespVO> voList = BeanUtils.toBean(list, SupplierRespVO.class);
        // 导出只保留掩码账号（bankAccount 无 Excel 注解，不会出现在导出文件里）
        voList.forEach(SupplierController::maskForList);
        ExcelUtils.write(response, "供应商.xls", "供应商列表", SupplierRespVO.class, voList);
    }

    /**
     * 列表/导出场景的敏感字段处理：只保留掩码，清空原文（NFR-12）。
     *
     * 详情接口 {@link #getSupplier(Long)} 不经过本方法，仍返回完整账号供编辑表单回填。
     */
    private static void maskForList(SupplierRespVO vo) {
        vo.maskBankAccount();
        vo.setBankAccount(null);
    }

}
