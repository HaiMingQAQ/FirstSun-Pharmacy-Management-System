package cn.iocoder.yudao.module.pharmacy.controller.admin.member;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.level.MemberLevelPageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.level.MemberLevelRespVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.level.MemberLevelSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.MemberLevelDO;
import cn.iocoder.yudao.module.pharmacy.service.member.MemberLevelService;
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

@Tag(name = "管理后台 - 会员等级")
@RestController
@RequestMapping("/pharmacy/member/level")
@Validated
public class MemberLevelController {

    @Resource
    private MemberLevelService memberLevelService;

    @PostMapping("/create")
    @Operation(summary = "创建会员等级")
    @PreAuthorize("@ss.hasPermission('pharmacy:member:level:create')")
    public CommonResult<Long> createMemberLevel(@Valid @RequestBody MemberLevelSaveReqVO createReqVO) {
        Long id = memberLevelService.createMemberLevel(createReqVO);
        return success(id);
    }

    @PutMapping("/update")
    @Operation(summary = "更新会员等级")
    @PreAuthorize("@ss.hasPermission('pharmacy:member:level:update')")
    public CommonResult<Boolean> updateMemberLevel(@Valid @RequestBody MemberLevelSaveReqVO updateReqVO) {
        memberLevelService.updateMemberLevel(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除会员等级")
    @Parameter(name = "id", description = "等级编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pharmacy:member:level:delete')")
    public CommonResult<Boolean> deleteMemberLevel(@RequestParam("id") Long id) {
        memberLevelService.deleteMemberLevel(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得会员等级详情")
    @Parameter(name = "id", description = "等级编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pharmacy:member:level:query')")
    public CommonResult<MemberLevelRespVO> getMemberLevel(@RequestParam("id") Long id) {
        MemberLevelDO memberLevel = memberLevelService.getMemberLevel(id);
        return success(BeanUtils.toBean(memberLevel, MemberLevelRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得会员等级分页")
    @PreAuthorize("@ss.hasPermission('pharmacy:member:level:query')")
    public CommonResult<PageResult<MemberLevelRespVO>> getMemberLevelPage(@Validated MemberLevelPageReqVO pageReqVO) {
        PageResult<MemberLevelDO> pageResult = memberLevelService.getMemberLevelPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, MemberLevelRespVO.class));
    }

    @GetMapping({"/simple-list", "/list-all-simple"})
    @Operation(summary = "获取启用的会员等级精简列表", description = "只返回启用状态的等级，用于前端下拉")
    public CommonResult<List<MemberLevelRespVO>> getSimpleMemberLevelList() {
        List<MemberLevelDO> list = memberLevelService.getEnabledMemberLevelList();
        list.sort(Comparator.comparing(MemberLevelDO::getLevel));
        return success(BeanUtils.toBean(list, MemberLevelRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出会员等级 Excel")
    @PreAuthorize("@ss.hasPermission('pharmacy:member:level:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportMemberLevel(HttpServletResponse response, @Validated MemberLevelPageReqVO reqVO) throws IOException {
        reqVO.setPageSize(cn.iocoder.yudao.framework.common.pojo.PageParam.PAGE_SIZE_NONE);
        List<MemberLevelDO> list = memberLevelService.getMemberLevelPage(reqVO).getList();
        ExcelUtils.write(response, "会员等级.xls", "等级列表", MemberLevelRespVO.class,
                BeanUtils.toBean(list, MemberLevelRespVO.class));
    }

}
