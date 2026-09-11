package cn.iocoder.yudao.module.pharmacy.controller.admin.purchase;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.license.SupplierLicensePageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.license.SupplierLicenseRespVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.license.SupplierLicenseSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.purchase.SupplierDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.purchase.SupplierLicenseDO;
import cn.iocoder.yudao.module.pharmacy.service.purchase.SupplierLicenseService;
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
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 供应商证照
 *
 * @author B 成员
 */
@Tag(name = "管理后台 - 供应商证照")
@RestController
@RequestMapping("/pharmacy/purchase/license")
@Validated
public class SupplierLicenseController {

    @Resource
    private SupplierLicenseService supplierLicenseService;

    @Resource
    private SupplierService supplierService;

    @PostMapping("/create")
    @Operation(summary = "创建供应商证照")
    @PreAuthorize("@ss.hasPermission('pharmacy:purchase:license:create')")
    public CommonResult<Long> createLicense(@Valid @RequestBody SupplierLicenseSaveReqVO createReqVO) {
        return success(supplierLicenseService.createLicense(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新供应商证照")
    @PreAuthorize("@ss.hasPermission('pharmacy:purchase:license:update')")
    public CommonResult<Boolean> updateLicense(@Valid @RequestBody SupplierLicenseSaveReqVO updateReqVO) {
        supplierLicenseService.updateLicense(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除供应商证照")
    @Parameter(name = "id", description = "证照编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pharmacy:purchase:license:delete')")
    public CommonResult<Boolean> deleteLicense(@RequestParam("id") Long id) {
        supplierLicenseService.deleteLicense(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得供应商证照详情")
    @Parameter(name = "id", description = "证照编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pharmacy:purchase:license:query')")
    public CommonResult<SupplierLicenseRespVO> getLicense(@RequestParam("id") Long id) {
        // 走存在性校验：不存在时返回证照不存在的业务错误，而不是 data=null
        SupplierLicenseDO license = supplierLicenseService.validateLicenseExists(id);
        return success(buildRespVOList(Collections.singletonList(license)).stream().findFirst().orElse(null));
    }

    @GetMapping("/page")
    @Operation(summary = "获得供应商证照分页")
    @PreAuthorize("@ss.hasPermission('pharmacy:purchase:license:query')")
    public CommonResult<PageResult<SupplierLicenseRespVO>> getLicensePage(@Validated SupplierLicensePageReqVO pageReqVO) {
        PageResult<SupplierLicenseDO> pageResult = supplierLicenseService.getLicensePage(pageReqVO);
        PageResult<SupplierLicenseRespVO> result = new PageResult<>(buildRespVOList(pageResult.getList()), pageResult.getTotal());
        return success(result);
    }

    @GetMapping("/list-by-supplier")
    @Operation(summary = "获得指定供应商的全部证照", description = "用于供应商详情面板展示证照清单")
    @Parameter(name = "supplierId", description = "供应商编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pharmacy:purchase:license:query')")
    public CommonResult<List<SupplierLicenseRespVO>> getLicenseListBySupplier(@RequestParam("supplierId") Long supplierId) {
        return success(buildRespVOList(supplierLicenseService.getLicenseListBySupplierId(supplierId)));
    }

    @GetMapping("/expiring-list")
    @Operation(summary = "获得即将到期的证照列表", description = "证照到期提醒（ADM-002），默认 30 天窗口，含已过期证照")
    @Parameter(name = "days", description = "提醒窗口天数，默认 30", example = "30")
    @PreAuthorize("@ss.hasPermission('pharmacy:purchase:license:query')")
    public CommonResult<List<SupplierLicenseRespVO>> getExpiringLicenseList(
            @RequestParam(value = "days", required = false) Integer days) {
        return success(buildRespVOList(supplierLicenseService.getExpiringLicenseList(days)));
    }

    @PutMapping("/refresh-status")
    @Operation(summary = "刷新证照有效/过期状态", description = "按当天日期重算全部证照状态，返回变更行数")
    @PreAuthorize("@ss.hasPermission('pharmacy:purchase:license:update')")
    public CommonResult<Integer> refreshLicenseStatus() {
        return success(supplierLicenseService.refreshExpiredStatus());
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出供应商证照 Excel")
    @PreAuthorize("@ss.hasPermission('pharmacy:purchase:license:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportLicense(HttpServletResponse response, @Validated SupplierLicensePageReqVO reqVO) throws IOException {
        reqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<SupplierLicenseDO> list = supplierLicenseService.getLicensePage(reqVO).getList();
        ExcelUtils.write(response, "供应商证照.xls", "证照列表", SupplierLicenseRespVO.class, buildRespVOList(list));
    }

    /**
     * DO 转 RespVO，并回填供应商名称、计算距到期天数
     */
    private List<SupplierLicenseRespVO> buildRespVOList(Collection<SupplierLicenseDO> licenses) {
        if (licenses == null || licenses.isEmpty()) {
            return Collections.emptyList();
        }
        // BeanUtils.toBean 只接收 List，这里统一转一次
        List<SupplierLicenseDO> licenseList = new ArrayList<>(licenses);
        List<SupplierLicenseRespVO> voList = BeanUtils.toBean(licenseList, SupplierLicenseRespVO.class);
        // 回填供应商名称：一次性批量查询，避免循环单查
        List<Long> supplierIds = licenseList.stream()
                .map(SupplierLicenseDO::getSupplierId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, SupplierDO> supplierMap = supplierService.getSupplierList(supplierIds).stream()
                .collect(Collectors.toMap(SupplierDO::getId, Function.identity(), (a, b) -> a));
        LocalDate today = LocalDate.now();
        for (SupplierLicenseRespVO vo : voList) {
            if (vo.getSupplierId() != null) {
                SupplierDO supplier = supplierMap.get(vo.getSupplierId());
                if (supplier != null) {
                    vo.setSupplierName(supplier.getSupplierName());
                }
            }
            if (vo.getExpireDate() != null) {
                vo.setDaysToExpire(ChronoUnit.DAYS.between(today, vo.getExpireDate()));
            }
        }
        return voList;
    }

}
