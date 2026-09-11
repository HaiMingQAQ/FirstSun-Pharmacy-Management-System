package cn.iocoder.yudao.module.pharmacy.controller.admin.purchase;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.pharmacy.api.DrugApi;
import cn.iocoder.yudao.module.pharmacy.api.dto.DrugRespDTO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.receipt.PurchaseReceiptDetailRespVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.receipt.PurchaseReceiptLineRespVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.receipt.PurchaseReceiptPageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.receipt.PurchaseReceiptRespVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.receipt.PurchaseReceiptSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.base.EmployeeDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.base.StoreDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.purchase.PurchaseOrderDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.purchase.PurchaseReceiptDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.purchase.PurchaseReceiptLineDO;
import cn.iocoder.yudao.module.pharmacy.service.base.EmployeeService;
import cn.iocoder.yudao.module.pharmacy.service.base.StoreService;
import cn.iocoder.yudao.module.pharmacy.service.purchase.PurchaseOrderService;
import cn.iocoder.yudao.module.pharmacy.service.purchase.PurchaseReceiptService;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 采购收货
 *
 * 入账接口 post 会在同一事务内调用库存服务并累计订单已收数量；
 * 库存服务（C）未就绪时返回明确业务错误，收货单不会进入「已入账」。
 *
 * @author B 成员
 */
@Tag(name = "管理后台 - 采购收货")
@RestController
@RequestMapping("/pharmacy/purchase/receipt")
@Validated
public class PurchaseReceiptController {

    @Resource
    private PurchaseReceiptService purchaseReceiptService;

    @Resource
    private PurchaseOrderService purchaseOrderService;

    @Resource
    private StoreService storeService;

    @Resource
    private EmployeeService employeeService;

    /**
     * 药品只读门面：回填药品名称/规格/单位
     */
    @Resource
    private DrugApi drugApi;

    @PostMapping("/create")
    @Operation(summary = "创建采购收货单", description = "有单收货关联采购订单；无单收货需 isFreeReceipt=1 且当前员工为店长")
    @PreAuthorize("@ss.hasPermission('pharmacy:purchase:receipt:create')")
    public CommonResult<Long> createReceipt(@Valid @RequestBody PurchaseReceiptSaveReqVO createReqVO) {
        return success(purchaseReceiptService.createReceipt(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新采购收货单", description = "仅待提交状态可改，明细整体重建")
    @PreAuthorize("@ss.hasPermission('pharmacy:purchase:receipt:update')")
    public CommonResult<Boolean> updateReceipt(@Valid @RequestBody PurchaseReceiptSaveReqVO updateReqVO) {
        purchaseReceiptService.updateReceipt(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除采购收货单", description = "仅待提交或已作废")
    @Parameter(name = "id", description = "收货单编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pharmacy:purchase:receipt:delete')")
    public CommonResult<Boolean> deleteReceipt(@RequestParam("id") Long id) {
        purchaseReceiptService.deleteReceipt(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得采购收货单详情", description = "含明细行与药品信息")
    @Parameter(name = "id", description = "收货单编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pharmacy:purchase:receipt:query')")
    public CommonResult<PurchaseReceiptDetailRespVO> getReceipt(@RequestParam("id") Long id) {
        PurchaseReceiptDO receipt = purchaseReceiptService.getReceipt(id);
        return success(buildDetail(receipt));
    }

    @GetMapping("/page")
    @Operation(summary = "获得采购收货单分页")
    @PreAuthorize("@ss.hasPermission('pharmacy:purchase:receipt:query')")
    public CommonResult<PageResult<PurchaseReceiptRespVO>> getReceiptPage(@Validated PurchaseReceiptPageReqVO pageReqVO) {
        PageResult<PurchaseReceiptDO> pageResult = purchaseReceiptService.getReceiptPage(pageReqVO);
        PageResult<PurchaseReceiptRespVO> result = BeanUtils.toBean(pageResult, PurchaseReceiptRespVO.class);
        fillHeaderNames(result.getList());
        return success(result);
    }

    @PutMapping("/submit")
    @Operation(summary = "提交收货单", description = "待提交 → 已提交")
    @Parameter(name = "id", description = "收货单编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pharmacy:purchase:receipt:submit')")
    public CommonResult<Boolean> submitReceipt(@RequestParam("id") Long id) {
        purchaseReceiptService.submitReceipt(id);
        return success(true);
    }

    @PutMapping("/post")
    @Operation(summary = "收货入账",
            description = "已提交 → 已入账：调用库存服务写入批次与货位库存，并累计订单已收数量；重复调用会被状态 CAS 拒绝")
    @Parameter(name = "id", description = "收货单编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pharmacy:purchase:receipt:post')")
    public CommonResult<Boolean> postReceipt(@RequestParam("id") Long id) {
        purchaseReceiptService.postReceipt(id);
        return success(true);
    }

    @PutMapping("/void")
    @Operation(summary = "作废收货单", description = "仅待提交可作废；已入账需先做库存回补，当前不支持直接作废")
    @Parameter(name = "id", description = "收货单编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pharmacy:purchase:receipt:void')")
    public CommonResult<Boolean> voidReceipt(@RequestParam("id") Long id) {
        purchaseReceiptService.voidReceipt(id);
        return success(true);
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出采购收货单 Excel")
    @PreAuthorize("@ss.hasPermission('pharmacy:purchase:receipt:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportReceipt(HttpServletResponse response, @Validated PurchaseReceiptPageReqVO reqVO) throws IOException {
        reqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<PurchaseReceiptDO> list = purchaseReceiptService.getReceiptPage(reqVO).getList();
        List<PurchaseReceiptRespVO> voList = BeanUtils.toBean(list, PurchaseReceiptRespVO.class);
        fillHeaderNames(voList);
        ExcelUtils.write(response, "采购收货单.xls", "收货单列表", PurchaseReceiptRespVO.class, voList);
    }

    // ==================== 私有方法：回填展示字段 ====================

    private PurchaseReceiptDetailRespVO buildDetail(PurchaseReceiptDO receipt) {
        if (receipt == null) {
            return null;
        }
        PurchaseReceiptDetailRespVO detail = BeanUtils.toBean(receipt, PurchaseReceiptDetailRespVO.class);
        fillHeaderNames(Collections.singletonList(detail));
        List<PurchaseReceiptLineDO> lines = purchaseReceiptService.getReceiptLines(receipt.getId());
        List<PurchaseReceiptLineRespVO> lineVOs = BeanUtils.toBean(lines, PurchaseReceiptLineRespVO.class);
        fillDrugNames(lineVOs);
        detail.setLines(lineVOs);
        return detail;
    }

    /**
     * 回填门店名称、采购订单号、收货人姓名（批量查询，避免循环单查）
     */
    private void fillHeaderNames(List<? extends PurchaseReceiptRespVO> voList) {
        if (voList == null || voList.isEmpty()) {
            return;
        }
        List<Long> storeIds = voList.stream().map(PurchaseReceiptRespVO::getStoreId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, String> storeMap = storeIds.isEmpty() ? Collections.emptyMap()
                : storeService.getStoreList(storeIds).stream()
                .collect(Collectors.toMap(StoreDO::getId, StoreDO::getStoreName, (a, b) -> a));

        List<Long> orderIds = voList.stream().map(PurchaseReceiptRespVO::getOrderId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, String> orderMap = orderIds.isEmpty() ? Collections.emptyMap()
                : purchaseOrderService.getOrderList(orderIds).stream()
                .collect(Collectors.toMap(PurchaseOrderDO::getId, PurchaseOrderDO::getOrderNo, (a, b) -> a));

        // 收货人姓名：按员工编号逐个查询并缓存，页内去重
        Map<Long, String> employeeMap = new HashMap<>();
        for (PurchaseReceiptRespVO vo : voList) {
            vo.setStoreName(storeMap.get(vo.getStoreId()));
            vo.setOrderNo(orderMap.get(vo.getOrderId()));
            Long receiveBy = vo.getReceiveBy();
            if (receiveBy == null) {
                continue;
            }
            if (!employeeMap.containsKey(receiveBy)) {
                EmployeeDO employee = employeeService.getEmployee(receiveBy);
                employeeMap.put(receiveBy, employee == null ? null : employee.getEmpName());
            }
            vo.setReceiveByName(employeeMap.get(receiveBy));
        }
    }

    /**
     * 回填明细行的药品编码/名称/规格/单位
     */
    private void fillDrugNames(List<PurchaseReceiptLineRespVO> lineVOs) {
        if (lineVOs == null || lineVOs.isEmpty()) {
            return;
        }
        List<Long> drugIds = lineVOs.stream().map(PurchaseReceiptLineRespVO::getDrugId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (drugIds.isEmpty()) {
            return;
        }
        Map<Long, DrugRespDTO> drugMap = drugApi.getDrugList(drugIds).stream()
                .collect(Collectors.toMap(DrugRespDTO::getId, Function.identity(), (a, b) -> a));
        for (PurchaseReceiptLineRespVO vo : lineVOs) {
            DrugRespDTO drug = drugMap.get(vo.getDrugId());
            if (drug == null) {
                continue;
            }
            vo.setDrugCode(drug.getDrugCode());
            vo.setDrugName(drug.getGenericName());
            vo.setSpecification(drug.getSpecification());
            vo.setUnit(drug.getUnit());
        }
    }

}
