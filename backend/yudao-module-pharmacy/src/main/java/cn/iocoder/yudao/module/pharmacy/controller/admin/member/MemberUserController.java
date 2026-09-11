package cn.iocoder.yudao.module.pharmacy.controller.admin.member;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.user.MemberUserPageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.user.MemberUserRespVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.user.MemberUserSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.MemberUserDO;
import cn.iocoder.yudao.module.pharmacy.service.member.MemberUserService;
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

@Tag(name = "管理后台 - 会员用户")
@RestController
@RequestMapping("/pharmacy/member/user")
@Validated
public class MemberUserController {

    @Resource
    private MemberUserService memberUserService;

    @PostMapping("/create")
    @Operation(summary = "创建会员用户")
    @PreAuthorize("@ss.hasPermission('pharmacy:member:user:create')")
    public CommonResult<Long> createMemberUser(@Valid @RequestBody MemberUserSaveReqVO createReqVO) {
        Long id = memberUserService.createMemberUser(createReqVO);
        return success(id);
    }

    @PutMapping("/update")
    @Operation(summary = "更新会员用户")
    @PreAuthorize("@ss.hasPermission('pharmacy:member:user:update')")
    public CommonResult<Boolean> updateMemberUser(@Valid @RequestBody MemberUserSaveReqVO updateReqVO) {
        memberUserService.updateMemberUser(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除会员用户")
    @Parameter(name = "id", description = "用户编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pharmacy:member:user:delete')")
    public CommonResult<Boolean> deleteMemberUser(@RequestParam("id") Long id) {
        memberUserService.deleteMemberUser(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得会员用户详情")
    @Parameter(name = "id", description = "用户编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pharmacy:member:user:query')")
    public CommonResult<MemberUserRespVO> getMemberUser(@RequestParam("id") Long id) {
        MemberUserDO memberUser = memberUserService.getMemberUser(id);
        return success(BeanUtils.toBean(memberUser, MemberUserRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得会员用户分页")
    @PreAuthorize("@ss.hasPermission('pharmacy:member:user:query')")
    public CommonResult<PageResult<MemberUserRespVO>> getMemberUserPage(@Validated MemberUserPageReqVO pageReqVO) {
        PageResult<MemberUserDO> pageResult = memberUserService.getMemberUserPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, MemberUserRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出会员用户 Excel")
    @PreAuthorize("@ss.hasPermission('pharmacy:member:user:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportMemberUser(HttpServletResponse response, @Validated MemberUserPageReqVO reqVO) throws IOException {
        reqVO.setPageSize(cn.iocoder.yudao.framework.common.pojo.PageParam.PAGE_SIZE_NONE);
        List<MemberUserDO> list = memberUserService.getMemberUserPage(reqVO).getList();
        ExcelUtils.write(response, "会员用户.xls", "用户列表", MemberUserRespVO.class,
                BeanUtils.toBean(list, MemberUserRespVO.class));
    }

}
