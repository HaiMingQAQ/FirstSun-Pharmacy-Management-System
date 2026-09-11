package cn.iocoder.yudao.module.pharmacy.controller.admin.member;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.cart.WxCartPageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.cart.WxCartRespVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.cart.WxCartSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.WxCartDO;
import cn.iocoder.yudao.module.pharmacy.service.member.WxCartService;
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
import java.util.List;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 小程序购物车")
@RestController
@RequestMapping("/pharmacy/member/cart")
@Validated
public class WxCartController {

    @Resource
    private WxCartService wxCartService;

    @PostMapping("/create")
    @Operation(summary = "创建小程序购物车")
    @PreAuthorize("@ss.hasPermission('pharmacy:member:cart:create')")
    public CommonResult<Long> createWxCart(@Valid @RequestBody WxCartSaveReqVO createReqVO) {
        Long id = wxCartService.createWxCart(createReqVO);
        return success(id);
    }

    @PutMapping("/update")
    @Operation(summary = "更新小程序购物车")
    @PreAuthorize("@ss.hasPermission('pharmacy:member:cart:update')")
    public CommonResult<Boolean> updateWxCart(@Valid @RequestBody WxCartSaveReqVO updateReqVO) {
        wxCartService.updateWxCart(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除小程序购物车")
    @Parameter(name = "id", description = "购物车编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pharmacy:member:cart:delete')")
    public CommonResult<Boolean> deleteWxCart(@RequestParam("id") Long id) {
        wxCartService.deleteWxCart(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得小程序购物车详情")
    @Parameter(name = "id", description = "购物车编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pharmacy:member:cart:query')")
    public CommonResult<WxCartRespVO> getWxCart(@RequestParam("id") Long id) {
        WxCartDO wxCart = wxCartService.getWxCart(id);
        return success(BeanUtils.toBean(wxCart, WxCartRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得小程序购物车分页")
    @PreAuthorize("@ss.hasPermission('pharmacy:member:cart:query')")
    public CommonResult<PageResult<WxCartRespVO>> getWxCartPage(@Validated WxCartPageReqVO pageReqVO) {
        PageResult<WxCartDO> pageResult = wxCartService.getWxCartPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, WxCartRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出小程序购物车 Excel")
    @PreAuthorize("@ss.hasPermission('pharmacy:member:cart:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportWxCart(HttpServletResponse response, @Validated WxCartPageReqVO reqVO) throws IOException {
        reqVO.setPageSize(cn.iocoder.yudao.framework.common.pojo.PageParam.PAGE_SIZE_NONE);
        List<WxCartDO> list = wxCartService.getWxCartPage(reqVO).getList();
        ExcelUtils.write(response, "小程序购物车.xls", "购物车列表", WxCartRespVO.class,
                BeanUtils.toBean(list, WxCartRespVO.class));
    }

}
