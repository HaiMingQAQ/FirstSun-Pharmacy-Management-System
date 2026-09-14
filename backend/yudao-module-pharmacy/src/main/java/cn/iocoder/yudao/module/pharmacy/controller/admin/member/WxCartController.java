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

    // ========== 业务接口 ==========

    @PostMapping("/add")
    @Operation(summary = "加购：同门店同药品累加数量")
    @PreAuthorize("@ss.hasPermission('pharmacy:member:cart:create')")
    public CommonResult<Long> addToCart(
            @RequestParam("memberId") Long memberId,
            @RequestParam("drugId") Long drugId,
            @RequestParam("qty") Integer qty,
            @RequestParam("storeId") Long storeId) {
        Long id = wxCartService.addToCart(memberId, drugId, qty, storeId);
        return success(id);
    }

    @PutMapping("/update-qty")
    @Operation(summary = "修改购物车数量")
    @Parameter(name = "id", description = "购物车编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pharmacy:member:cart:update')")
    public CommonResult<Boolean> updateQty(
            @RequestParam("id") Long id,
            @RequestParam("qty") Integer qty) {
        wxCartService.updateQty(id, qty);
        return success(true);
    }

    @PutMapping("/update-selected")
    @Operation(summary = "勾选/取消勾选购物车")
    @Parameter(name = "id", description = "购物车编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pharmacy:member:cart:update')")
    public CommonResult<Boolean> updateSelected(
            @RequestParam("id") Long id,
            @RequestParam("selectedFlag") Integer selectedFlag) {
        wxCartService.updateSelected(id, selectedFlag);
        return success(true);
    }

    @PutMapping("/batch-update-selected")
    @Operation(summary = "批量勾选/取消勾选购物车")
    @PreAuthorize("@ss.hasPermission('pharmacy:member:cart:update')")
    public CommonResult<Boolean> batchUpdateSelected(
            @RequestBody java.util.List<Long> ids,
            @RequestParam("selectedFlag") Integer selectedFlag) {
        wxCartService.batchUpdateSelected(ids, selectedFlag);
        return success(true);
    }

    @DeleteMapping("/clear")
    @Operation(summary = "清空指定会员的购物车")
    @Parameter(name = "memberId", description = "会员编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pharmacy:member:cart:delete')")
    public CommonResult<Boolean> clearCart(@RequestParam("memberId") Long memberId) {
        wxCartService.clearCart(memberId);
        return success(true);
    }

    @GetMapping("/list-by-member")
    @Operation(summary = "获取指定会员的购物车列表")
    @Parameter(name = "memberId", description = "会员编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pharmacy:member:cart:query')")
    public CommonResult<List<WxCartRespVO>> getCartListByMemberId(@RequestParam("memberId") Long memberId) {
        List<WxCartDO> list = wxCartService.getCartListByMemberId(memberId);
        return success(BeanUtils.toBean(list, WxCartRespVO.class));
    }

    @GetMapping("/selected-list-by-member")
    @Operation(summary = "获取指定会员已勾选的购物车列表")
    @Parameter(name = "memberId", description = "会员编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pharmacy:member:cart:query')")
    public CommonResult<List<WxCartRespVO>> getSelectedCartListByMemberId(@RequestParam("memberId") Long memberId) {
        List<WxCartDO> list = wxCartService.getSelectedCartListByMemberId(memberId);
        return success(BeanUtils.toBean(list, WxCartRespVO.class));
    }

}
