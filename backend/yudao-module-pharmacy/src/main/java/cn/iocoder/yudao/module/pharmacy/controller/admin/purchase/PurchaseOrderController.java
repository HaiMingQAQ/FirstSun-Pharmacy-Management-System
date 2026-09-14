package cn.iocoder.yudao.module.pharmacy.controller.admin.purchase;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.pharmacy.api.DrugApi;
import cn.iocoder.yudao.module.pharmacy.api.dto.DrugRespDTO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.order.PurchaseOrderDetailRespVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.order.PurchaseOrderLineRespVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.order.PurchaseOrderPageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.order.PurchaseOrderRespVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.order.PurchaseOrderSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.base.StoreDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.purchase.PurchaseOrderDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.purchase.PurchaseOrderLineDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.purchase.SupplierDO;
import cn.iocoder.yudao.module.pharmacy.service.base.StoreService;
import cn.iocoder.yudao.module.pharmacy.service.purchase.PurchaseOrderService;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 采购订单
 *
 * 金额由服务端按明细重算；状态流转接口（提交/审批/发出/取消）各自独立权限，前端按钮不能替代服务端校验。
 *
 * @author B 成员
 */
@Tag(name = "管理后台 - 采购订单")
@RestController
@RequestMapping("/pharmacy/purchase/order")
@Validated
public class PurchaseOrderController {

    @Resource
    private PurchaseOrderService purchaseOrderService;

    @Resource
    private SupplierService supplierService;

    @Resource
    private StoreService storeService;

    /**
     * 药品只读门面：回填药品名称/规格/单位
     */
    @Resource
    private DrugApi drugApi;

    @PostMapping("/create")
    @Operation(summary = "创建采购订单")
    @PreAuthorize("@ss.hasPermission('pharmacy:purchase:order:create')")
    public CommonResult<Long> createOrder(@Valid @RequestBody PurchaseOrderSaveReqVO createReqVO) {
        return success(purchaseOrderService.createOrder(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新采购订单", description = "仅草稿状态可改，明细整体重建")
    @PreAuthorize("@ss.hasPermission('pharmacy:purchase:order:update')")
    public CommonResult<Boolean> updateOrder(@Valid @RequestBody PurchaseOrderSaveReqVO updateReqVO) {
        purchaseOrderService.updateOrder(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除采购订单", description = "仅草稿且无收货记录")
    @Parameter(name = "id", description = "订单编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pharmacy:purchase:order:delete')")
    public CommonResult<Boolean> deleteOrder(@RequestParam("id") Long id) {
        purchaseOrderService.deleteOrder(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得采购订单详情", description = "含明细行与药品信息")
    @Parameter(name = "id", description = "订单编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pharmacy:purchase:order:query')")
    public CommonResult<PurchaseOrderDetailRespVO> getOrder(@RequestParam("id") Long id) {
        PurchaseOrderDO order = purchaseOrderService.getOrder(id);
        return success(buildDetail(order));
    }

    @GetMapping("/page")
    @Operation(summary = "获得采购订单分页")
    @PreAuthorize("@ss.hasPermission('pharmacy:purchase:order:query')")
    public CommonResult<PageResult<PurchaseOrderRespVO>> getOrderPage(@Validated PurchaseOrderPageReqVO pageReqVO) {
        PageResult<PurchaseOrderDO> pageResult = purchaseOrderService.getOrderPage(pageReqVO);
        PageResult<PurchaseOrderRespVO> result = BeanUtils.toBean(pageResult, PurchaseOrderRespVO.class);
        fillHeaderNames(result.getList());
        return success(result);
    }

    @PutMapping("/submit")
    @Operation(summary = "提交采购订单审批", description = "草稿 → 已提交")
    @Parameter(name = "id", description = "订单编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pharmacy:purchase:order:submit')")
    public CommonResult<Boolean> submitOrder(@RequestParam("id") Long id) {
        purchaseOrderService.submitOrder(id);
        return success(true);
    }

    @PutMapping("/approve")
    @Operation(summary = "审批采购订单", description = "已提交 → 已审批，记录审批人（当前登录员工）")
    @Parameter(name = "id", description = "订单编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pharmacy:purchase:order:approve')")
    public CommonResult<Boolean> approveOrder(@RequestParam("id") Long id) {
        purchaseOrderService.approveOrder(id);
        return success(true);
    }

    @PutMapping("/issue")
    @Operation(summary = "标记采购订单已发出", description = "已审批 → 已发出")
    @Parameter(name = "id", description = "订单编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pharmacy:purchase:order:issue')")
    public CommonResult<Boolean> issueOrder(@RequestParam("id") Long id) {
        purchaseOrderService.issueOrder(id);
        return success(true);
    }

    @PutMapping("/cancel")
    @Operation(summary = "取消采购订单", description = "草稿/已提交/已审批 → 已取消；存在未作废收货单时拒绝")
    @Parameter(name = "id", description = "订单编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pharmacy:purchase:order:cancel')")
    public CommonResult<Boolean> cancelOrder(@RequestParam("id") Long id) {
        purchaseOrderService.cancelOrder(id);
        return success(true);
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出采购订单 Excel")
    @PreAuthorize("@ss.hasPermission('pharmacy:purchase:order:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportOrder(HttpServletResponse response, @Validated PurchaseOrderPageReqVO reqVO) throws IOException {
        reqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<PurchaseOrderDO> list = purchaseOrderService.getOrderPage(reqVO).getList();
        List<PurchaseOrderRespVO> voList = BeanUtils.toBean(list, PurchaseOrderRespVO.class);
        fillHeaderNames(voList);
        ExcelUtils.write(response, "采购订单.xls", "采购订单列表", PurchaseOrderRespVO.class, voList);
    }

    // ==================== 私有方法：回填展示字段 ====================

    /**
     * 详情：订单头 + 明细 + 供应商/门店/药品名称
     */
    private PurchaseOrderDetailRespVO buildDetail(PurchaseOrderDO order) {
        if (order == null) {
            return null;
        }
        PurchaseOrderDetailRespVO detail = BeanUtils.toBean(order, PurchaseOrderDetailRespVO.class);
        fillHeaderNames(Collections.singletonList(detail));
        List<PurchaseOrderLineDO> lines = purchaseOrderService.getOrderLines(order.getId());
        List<PurchaseOrderLineRespVO> lineVOs = BeanUtils.toBean(lines, PurchaseOrderLineRespVO.class);
        fillDrugNames(lineVOs);
        detail.setLines(lineVOs);
        return detail;
    }

    /**
     * 回填门店名称与供应商名称（批量查询，避免循环单查）
     */
    private void fillHeaderNames(List<? extends PurchaseOrderRespVO> voList) {
        if (voList == null || voList.isEmpty()) {
            return;
        }
        List<Long> storeIds = voList.stream().map(PurchaseOrderRespVO::getStoreId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, String> storeMap = storeIds.isEmpty() ? Collections.emptyMap()
                : storeService.getStoreList(storeIds).stream()
                .collect(Collectors.toMap(StoreDO::getId, StoreDO::getStoreName, (a, b) -> a));
        List<Long> supplierIds = voList.stream().map(PurchaseOrderRespVO::getSupplierId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, String> supplierMap = supplierIds.isEmpty() ? Collections.emptyMap()
                : supplierService.getSupplierList(supplierIds).stream()
                .collect(Collectors.toMap(SupplierDO::getId, SupplierDO::getSupplierName, (a, b) -> a));

        for (PurchaseOrderRespVO vo : voList) {
            vo.setStoreName(storeMap.get(vo.getStoreId()));
            vo.setSupplierName(supplierMap.get(vo.getSupplierId()));
        }
    }

    /**
     * 回填明细行的药品编码/名称/规格/单位
     */
    private void fillDrugNames(List<PurchaseOrderLineRespVO> lineVOs) {
        if (lineVOs == null || lineVOs.isEmpty()) {
            return;
        }
        List<Long> drugIds = lineVOs.stream().map(PurchaseOrderLineRespVO::getDrugId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (drugIds.isEmpty()) {
            return;
        }
        Map<Long, DrugRespDTO> drugMap = drugApi.getDrugList(drugIds).stream()
                .collect(Collectors.toMap(DrugRespDTO::getId, Function.identity(), (a, b) -> a));
        for (PurchaseOrderLineRespVO vo : lineVOs) {
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
