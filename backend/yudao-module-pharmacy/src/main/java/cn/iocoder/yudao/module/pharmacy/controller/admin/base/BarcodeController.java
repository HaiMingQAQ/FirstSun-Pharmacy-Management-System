package cn.iocoder.yudao.module.pharmacy.controller.admin.base;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.barcode.BarcodePageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.barcode.BarcodeRespVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.barcode.BarcodeSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.base.BarcodeDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.base.DrugDO;
import cn.iocoder.yudao.module.pharmacy.service.base.BarcodeService;
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

@Tag(name = "管理后台 - 药品条码")
@RestController
@RequestMapping("/pharmacy/base/barcode")
@Validated
public class BarcodeController {

    @Resource
    private BarcodeService barcodeService;

    @Resource
    private DrugService drugService;

    @PostMapping("/create")
    @Operation(summary = "创建条码")
    @PreAuthorize("@ss.hasPermission('pharmacy:base:barcode:create')")
    public CommonResult<Long> createBarcode(@Valid @RequestBody BarcodeSaveReqVO createReqVO) {
        Long id = barcodeService.createBarcode(createReqVO);
        return success(id);
    }

    @PutMapping("/update")
    @Operation(summary = "更新条码")
    @PreAuthorize("@ss.hasPermission('pharmacy:base:barcode:update')")
    public CommonResult<Boolean> updateBarcode(@Valid @RequestBody BarcodeSaveReqVO updateReqVO) {
        barcodeService.updateBarcode(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除条码")
    @Parameter(name = "id", description = "条码编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pharmacy:base:barcode:delete')")
    public CommonResult<Boolean> deleteBarcode(@RequestParam("id") Long id) {
        barcodeService.deleteBarcode(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得条码详情")
    @Parameter(name = "id", description = "条码编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pharmacy:base:barcode:query')")
    public CommonResult<BarcodeRespVO> getBarcode(@RequestParam("id") Long id) {
        BarcodeDO barcode = barcodeService.getBarcode(id);
        BarcodeRespVO respVO = BeanUtils.toBean(barcode, BarcodeRespVO.class);
        populateDrugName(respVO);
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得条码分页")
    @PreAuthorize("@ss.hasPermission('pharmacy:base:barcode:query')")
    public CommonResult<PageResult<BarcodeRespVO>> getBarcodePage(@Validated BarcodePageReqVO pageReqVO) {
        PageResult<BarcodeDO> pageResult = barcodeService.getBarcodePage(pageReqVO);
        PageResult<BarcodeRespVO> result = BeanUtils.toBean(pageResult, BarcodeRespVO.class);
        populateDrugNames(result.getList());
        return success(result);
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出条码 Excel")
    @PreAuthorize("@ss.hasPermission('pharmacy:base:barcode:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportBarcode(HttpServletResponse response, @Validated BarcodePageReqVO reqVO) throws IOException {
        reqVO.setPageSize(cn.iocoder.yudao.framework.common.pojo.PageParam.PAGE_SIZE_NONE);
        List<BarcodeDO> list = barcodeService.getBarcodePage(reqVO).getList();
        List<BarcodeRespVO> respList = BeanUtils.toBean(list, BarcodeRespVO.class);
        populateDrugNames(respList);
        ExcelUtils.write(response, "药品条码.xls", "条码列表", BarcodeRespVO.class, respList);
    }

    /**
     * 批量补充药品通用名
     */
    private void populateDrugNames(List<BarcodeRespVO> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Set<Long> drugIds = list.stream().map(BarcodeRespVO::getDrugId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> drugMap = new HashMap<>();
        if (!drugIds.isEmpty()) {
            List<DrugDO> drugs = drugService.getDrugList(drugIds);
            for (DrugDO drug : drugs) {
                drugMap.put(drug.getId(), drug.getGenericName());
            }
        }
        for (BarcodeRespVO vo : list) {
            if (vo.getDrugId() != null) {
                vo.setDrugName(drugMap.get(vo.getDrugId()));
            }
        }
    }

    private void populateDrugName(BarcodeRespVO vo) {
        if (vo == null || vo.getDrugId() == null) {
            return;
        }
        DrugDO drug = drugService.getDrug(vo.getDrugId());
        if (drug != null) {
            vo.setDrugName(drug.getGenericName());
        }
    }

}
