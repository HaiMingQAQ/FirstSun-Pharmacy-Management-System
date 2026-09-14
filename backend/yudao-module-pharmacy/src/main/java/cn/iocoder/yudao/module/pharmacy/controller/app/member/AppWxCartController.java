package cn.iocoder.yudao.module.pharmacy.controller.app.member;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.pharmacy.api.DrugApi;
import cn.iocoder.yudao.module.pharmacy.api.dto.DrugRespDTO;
import cn.iocoder.yudao.module.pharmacy.controller.app.member.vo.cart.AppWxCartAddReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.app.member.vo.cart.AppWxCartRespVO;
import cn.iocoder.yudao.module.pharmacy.controller.app.member.vo.cart.AppWxCartUpdateQtyReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.WxCartDO;
import cn.iocoder.yudao.module.pharmacy.service.member.WxCartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PHARMACY_WX_CART_NOT_OWNER;

/**
 * 用户 APP - 小程序购物车
 *
 * 所有操作均基于登录令牌中的会员编号，并对购物车记录做归属校验。
 */
@Tag(name = "用户 APP - 小程序购物车")
@RestController
@RequestMapping("/member/wx-cart")
@Validated
@Slf4j
public class AppWxCartController {

    @Resource
    private WxCartService wxCartService;

    @Resource
    private DrugApi drugApi;

    @GetMapping("/list")
    @Operation(summary = "获得本人购物车列表（含商品信息）")
    public CommonResult<List<AppWxCartRespVO>> getCartList() {
        List<WxCartDO> list = wxCartService.getCartListByMemberId(getLoginUserId());
        return success(buildRespList(list));
    }

    @GetMapping("/count")
    @Operation(summary = "获得本人购物车商品数量合计")
    public CommonResult<Integer> getCartCount() {
        List<WxCartDO> list = wxCartService.getCartListByMemberId(getLoginUserId());
        int count = list.stream().mapToInt(item -> item.getQty() == null ? 0 : item.getQty()).sum();
        return success(count);
    }

    @PostMapping("/add")
    @Operation(summary = "加购：同一门店同一商品累加数量")
    public CommonResult<Long> addToCart(@RequestBody @Valid AppWxCartAddReqVO reqVO) {
        Long id = wxCartService.addToCart(getLoginUserId(), reqVO.getDrugId(), reqVO.getQty(), reqVO.getStoreId());
        return success(id);
    }

    @PutMapping("/update-qty")
    @Operation(summary = "修改购物车数量")
    public CommonResult<Boolean> updateQty(@RequestBody @Valid AppWxCartUpdateQtyReqVO reqVO) {
        validateCartOwner(reqVO.getId());
        wxCartService.updateQty(reqVO.getId(), reqVO.getQty());
        return success(true);
    }

    @PutMapping("/update-selected")
    @Operation(summary = "勾选/取消勾选购物车")
    @Parameter(name = "id", description = "购物车编号", required = true, example = "1024")
    public CommonResult<Boolean> updateSelected(@RequestParam("id") Long id,
                                                @RequestParam("selectedFlag") Integer selectedFlag) {
        validateCartOwner(id);
        wxCartService.updateSelected(id, selectedFlag);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除购物车记录")
    @Parameter(name = "id", description = "购物车编号", required = true, example = "1024")
    public CommonResult<Boolean> deleteCart(@RequestParam("id") Long id) {
        validateCartOwner(id);
        wxCartService.deleteWxCart(id);
        return success(true);
    }

    @DeleteMapping("/clear")
    @Operation(summary = "清空本人购物车")
    public CommonResult<Boolean> clearCart() {
        wxCartService.clearCart(getLoginUserId());
        return success(true);
    }

    // ========== 私有方法 ==========

    /**
     * 校验购物车记录归属，防止越权操作他人购物车
     */
    private void validateCartOwner(Long id) {
        WxCartDO cart = wxCartService.validateWxCartExists(id);
        if (!Objects.equals(cart.getMemberId(), getLoginUserId())) {
            throw exception(PHARMACY_WX_CART_NOT_OWNER);
        }
    }

    /**
     * 组装购物车响应，补充 A 提供的商品信息（名称、规格、单位、价格、处方药标识）
     */
    private List<AppWxCartRespVO> buildRespList(List<WxCartDO> list) {
        if (list == null || list.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        List<Long> drugIds = list.stream().map(WxCartDO::getDrugId).distinct().collect(Collectors.toList());
        Map<Long, DrugRespDTO> drugMap = drugApi.getDrugList(drugIds).stream()
                .collect(Collectors.toMap(DrugRespDTO::getId, Function.identity(), (a, b) -> a));
        return list.stream().map(item -> {
            AppWxCartRespVO vo = new AppWxCartRespVO();
            vo.setId(item.getId());
            vo.setDrugId(item.getDrugId());
            vo.setQty(item.getQty());
            vo.setSelectedFlag(item.getSelectedFlag());
            vo.setStoreId(item.getStoreId());
            vo.setAddTime(item.getAddTime());
            DrugRespDTO drug = drugMap.get(item.getDrugId());
            if (drug != null) {
                vo.setDrugName(drug.getGenericName() != null ? drug.getGenericName() : drug.getTradeName());
                vo.setSpecification(drug.getSpecification());
                vo.setUnit(drug.getUnit());
                vo.setIsRx(drug.getIsRx());
                vo.setRetailPrice(drug.getRetailPrice());
                vo.setMemberPrice(drug.getMemberPrice());
            }
            return vo;
        }).collect(Collectors.toList());
    }

}
