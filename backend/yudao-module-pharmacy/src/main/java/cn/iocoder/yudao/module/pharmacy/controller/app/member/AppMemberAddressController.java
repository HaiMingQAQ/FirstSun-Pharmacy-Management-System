package cn.iocoder.yudao.module.pharmacy.controller.app.member;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.address.MemberAddressSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.app.member.vo.address.AppMemberAddressRespVO;
import cn.iocoder.yudao.module.pharmacy.controller.app.member.vo.address.AppMemberAddressSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.MemberAddressDO;
import cn.iocoder.yudao.module.pharmacy.service.member.MemberAddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PHARMACY_MEMBER_ADDRESS_NOT_OWNER;

/**
 * 用户 APP - 会员收件地址
 *
 * 用户编号一律取自登录令牌，不接受前端传入，避免越权操作他人地址。
 */
@Tag(name = "用户 APP - 会员收件地址")
@RestController
@RequestMapping("/member/address")
@Validated
@Slf4j
public class AppMemberAddressController {

    @Resource
    private MemberAddressService memberAddressService;

    @GetMapping("/list")
    @Operation(summary = "获得本人收件地址列表")
    public CommonResult<List<AppMemberAddressRespVO>> getAddressList() {
        List<MemberAddressDO> list = memberAddressService.getMemberAddressListByUserId(getLoginUserId());
        return success(BeanUtils.toBean(list, AppMemberAddressRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "获得本人收件地址详情")
    @Parameter(name = "id", description = "地址编号", required = true, example = "1024")
    public CommonResult<AppMemberAddressRespVO> getAddress(@RequestParam("id") Long id) {
        MemberAddressDO address = validateAddressOwner(id);
        return success(BeanUtils.toBean(address, AppMemberAddressRespVO.class));
    }

    @GetMapping("/get-default")
    @Operation(summary = "获得本人默认收件地址")
    public CommonResult<AppMemberAddressRespVO> getDefaultAddress() {
        MemberAddressDO address = memberAddressService.getDefaultMemberAddress(getLoginUserId());
        return success(BeanUtils.toBean(address, AppMemberAddressRespVO.class));
    }

    @PostMapping("/create")
    @Operation(summary = "新增收件地址")
    public CommonResult<Long> createAddress(@RequestBody @Valid AppMemberAddressSaveReqVO reqVO) {
        MemberAddressSaveReqVO saveReqVO = BeanUtils.toBean(reqVO, MemberAddressSaveReqVO.class);
        // 用户编号取自登录令牌，防止越权为他人创建地址
        saveReqVO.setUserId(getLoginUserId());
        return success(memberAddressService.createMemberAddress(saveReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改收件地址")
    public CommonResult<Boolean> updateAddress(@RequestBody @Valid AppMemberAddressSaveReqVO reqVO) {
        validateAddressOwner(reqVO.getId());
        MemberAddressSaveReqVO saveReqVO = BeanUtils.toBean(reqVO, MemberAddressSaveReqVO.class);
        saveReqVO.setUserId(getLoginUserId());
        memberAddressService.updateMemberAddress(saveReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除收件地址")
    @Parameter(name = "id", description = "地址编号", required = true, example = "1024")
    public CommonResult<Boolean> deleteAddress(@RequestParam("id") Long id) {
        validateAddressOwner(id);
        memberAddressService.deleteMemberAddress(id);
        return success(true);
    }

    // ========== 私有方法 ==========

    /**
     * 校验地址归属，防止越权访问他人地址
     */
    private MemberAddressDO validateAddressOwner(Long id) {
        MemberAddressDO address = memberAddressService.validateMemberAddressExists(id);
        if (!Objects.equals(address.getUserId(), getLoginUserId())) {
            throw exception(PHARMACY_MEMBER_ADDRESS_NOT_OWNER);
        }
        return address;
    }

}
