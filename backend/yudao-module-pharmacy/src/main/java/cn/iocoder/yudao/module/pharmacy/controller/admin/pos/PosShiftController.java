package cn.iocoder.yudao.module.pharmacy.controller.admin.pos;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.pharmacy.controller.admin.pos.vo.PosShiftPageReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhPosShiftDO;
import cn.iocoder.yudao.module.pharmacy.service.sale.PosShiftService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.math.BigDecimal;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - POS 收银班次")
@RestController
@RequestMapping("/pharmacy/pos/shift")
@Validated
public class PosShiftController {

    @Resource
    private PosShiftService posShiftService;

    @PostMapping("/open")
    @Operation(summary = "开台（创建营业中班次）")
    @PreAuthorize("@ss.hasPermission('pharmacy:pos-shift:create')")
    public CommonResult<Long> openShift(@RequestParam("storeId") Long storeId,
                                        @RequestParam("posNo") String posNo,
                                        @RequestParam("cashierId") Long cashierId) {
        return success(posShiftService.openShift(storeId, posNo, cashierId));
    }

    @PostMapping("/close")
    @Operation(summary = "交班（核对现金应收与实盘，差异必填原因）")
    @PreAuthorize("@ss.hasPermission('pharmacy:pos-shift:create')")
    public CommonResult<Boolean> closeShift(@RequestParam("shiftId") Long shiftId,
                                            @RequestParam(value = "cashActual", required = false) BigDecimal cashActual,
                                            @RequestParam(value = "diffReason", required = false) String diffReason) {
        posShiftService.closeShift(shiftId, cashActual, diffReason);
        return success(true);
    }

    @GetMapping("/page")
    @Operation(summary = "获得班次分页")
    @PreAuthorize("@ss.hasPermission('pharmacy:pos-shift:query')")
    public CommonResult<PageResult<PhPosShiftDO>> getShiftPage(@Valid PosShiftPageReqVO pageReqVO) {
        return success(posShiftService.getShiftPage(pageReqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得班次详情")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pharmacy:pos-shift:query')")
    public CommonResult<PhPosShiftDO> getShift(@RequestParam("id") Long id) {
        return success(posShiftService.getShift(id));
    }

}
