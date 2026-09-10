package cn.iocoder.yudao.module.pharmacy.controller.admin.base;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.store.StorePageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.store.StoreRespVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.store.StoreSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.store.StoreSimpleRespVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.base.StoreDO;
import cn.iocoder.yudao.module.pharmacy.service.base.StoreService;
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
import java.util.Comparator;
import java.util.List;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 门店")
@RestController
@RequestMapping("/pharmacy/base/store")
@Validated
public class StoreController {

    @Resource
    private StoreService storeService;

    @PostMapping("/create")
    @Operation(summary = "创建门店")
    @PreAuthorize("@ss.hasPermission('pharmacy:base:store:create')")
    public CommonResult<Long> createStore(@Valid @RequestBody StoreSaveReqVO createReqVO) {
        Long id = storeService.createStore(createReqVO);
        return success(id);
    }

    @PutMapping("/update")
    @Operation(summary = "更新门店")
    @PreAuthorize("@ss.hasPermission('pharmacy:base:store:update')")
    public CommonResult<Boolean> updateStore(@Valid @RequestBody StoreSaveReqVO updateReqVO) {
        storeService.updateStore(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除门店")
    @Parameter(name = "id", description = "门店编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pharmacy:base:store:delete')")
    public CommonResult<Boolean> deleteStore(@RequestParam("id") Long id) {
        storeService.deleteStore(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得门店详情")
    @Parameter(name = "id", description = "门店编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pharmacy:base:store:query')")
    public CommonResult<StoreRespVO> getStore(@RequestParam("id") Long id) {
        StoreDO store = storeService.getStore(id);
        return success(BeanUtils.toBean(store, StoreRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得门店分页")
    @PreAuthorize("@ss.hasPermission('pharmacy:base:store:query')")
    public CommonResult<PageResult<StoreRespVO>> getStorePage(@Validated StorePageReqVO pageReqVO) {
        PageResult<StoreDO> pageResult = storeService.getStorePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, StoreRespVO.class));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获取营业状态的门店精简列表", description = "只返回营业状态的门店，用于前端下拉")
    public CommonResult<List<StoreSimpleRespVO>> getSimpleStoreList() {
        List<StoreDO> list = storeService.getEnabledStoreList();
        list.sort(Comparator.comparing(StoreDO::getId));
        return success(BeanUtils.toBean(list, StoreSimpleRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出门店 Excel")
    @PreAuthorize("@ss.hasPermission('pharmacy:base:store:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportStore(HttpServletResponse response, @Validated StorePageReqVO reqVO) throws IOException {
        reqVO.setPageSize(cn.iocoder.yudao.framework.common.pojo.PageParam.PAGE_SIZE_NONE);
        List<StoreDO> list = storeService.getStorePage(reqVO).getList();
        ExcelUtils.write(response, "门店.xls", "门店列表", StoreRespVO.class,
                BeanUtils.toBean(list, StoreRespVO.class));
    }

}
