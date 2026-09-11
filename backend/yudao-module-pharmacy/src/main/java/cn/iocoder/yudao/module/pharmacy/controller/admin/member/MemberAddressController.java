package cn.iocoder.yudao.module.pharmacy.controller.admin.member;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.address.MemberAddressPageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.address.MemberAddressRespVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.address.MemberAddressSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.MemberAddressDO;
import cn.iocoder.yudao.module.pharmacy.service.member.MemberAddressService;
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

@Tag(name = "管理后台 - 会员收件地址")
@RestController
@RequestMapping("/pharmacy/member/address")
@Validated
public class MemberAddressController {

    @Resource
    private MemberAddressService memberAddressService;

    @PostMapping("/create")
    @Operation(summary = "创建会员收件地址")
    @PreAuthorize("@ss.hasPermission('pharmacy:member:address:create')")
    public CommonResult<Long> createMemberAddress(@Valid @RequestBody MemberAddressSaveReqVO createReqVO) {
        Long id = memberAddressService.createMemberAddress(createReqVO);
        return success(id);
    }

    @PutMapping("/update")
    @Operation(summary = "更新会员收件地址")
    @PreAuthorize("@ss.hasPermission('pharmacy:member:address:update')")
    public CommonResult<Boolean> updateMemberAddress(@Valid @RequestBody MemberAddressSaveReqVO updateReqVO) {
        memberAddressService.updateMemberAddress(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除会员收件地址")
    @Parameter(name = "id", description = "地址编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pharmacy:member:address:delete')")
    public CommonResult<Boolean> deleteMemberAddress(@RequestParam("id") Long id) {
        memberAddressService.deleteMemberAddress(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得会员收件地址详情")
    @Parameter(name = "id", description = "地址编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pharmacy:member:address:query')")
    public CommonResult<MemberAddressRespVO> getMemberAddress(@RequestParam("id") Long id) {
        MemberAddressDO memberAddress = memberAddressService.getMemberAddress(id);
        return success(BeanUtils.toBean(memberAddress, MemberAddressRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得会员收件地址分页")
    @PreAuthorize("@ss.hasPermission('pharmacy:member:address:query')")
    public CommonResult<PageResult<MemberAddressRespVO>> getMemberAddressPage(@Validated MemberAddressPageReqVO pageReqVO) {
        PageResult<MemberAddressDO> pageResult = memberAddressService.getMemberAddressPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, MemberAddressRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出会员收件地址 Excel")
    @PreAuthorize("@ss.hasPermission('pharmacy:member:address:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportMemberAddress(HttpServletResponse response, @Validated MemberAddressPageReqVO reqVO) throws IOException {
        reqVO.setPageSize(cn.iocoder.yudao.framework.common.pojo.PageParam.PAGE_SIZE_NONE);
        List<MemberAddressDO> list = memberAddressService.getMemberAddressPage(reqVO).getList();
        ExcelUtils.write(response, "会员收件地址.xls", "地址列表", MemberAddressRespVO.class,
                BeanUtils.toBean(list, MemberAddressRespVO.class));
    }

}
