package cn.iocoder.yudao.module.pharmacy.controller.admin.member;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.pointrecord.MemberPointRecordPageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.pointrecord.MemberPointRecordRespVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.pointrecord.MemberPointRecordSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.MemberPointRecordDO;
import cn.iocoder.yudao.module.pharmacy.service.member.MemberPointRecordService;
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

@Tag(name = "管理后台 - 会员积分记录")
@RestController
@RequestMapping("/pharmacy/member/point-record")
@Validated
public class MemberPointRecordController {

    @Resource
    private MemberPointRecordService memberPointRecordService;

    @PostMapping("/create")
    @Operation(summary = "创建会员积分记录")
    @PreAuthorize("@ss.hasPermission('pharmacy:member:point-record:create')")
    public CommonResult<Long> createMemberPointRecord(@Valid @RequestBody MemberPointRecordSaveReqVO createReqVO) {
        Long id = memberPointRecordService.createMemberPointRecord(createReqVO);
        return success(id);
    }

    @PutMapping("/update")
    @Operation(summary = "更新会员积分记录")
    @PreAuthorize("@ss.hasPermission('pharmacy:member:point-record:update')")
    public CommonResult<Boolean> updateMemberPointRecord(@Valid @RequestBody MemberPointRecordSaveReqVO updateReqVO) {
        memberPointRecordService.updateMemberPointRecord(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除会员积分记录")
    @Parameter(name = "id", description = "记录编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pharmacy:member:point-record:delete')")
    public CommonResult<Boolean> deleteMemberPointRecord(@RequestParam("id") Long id) {
        memberPointRecordService.deleteMemberPointRecord(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得会员积分记录详情")
    @Parameter(name = "id", description = "记录编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pharmacy:member:point-record:query')")
    public CommonResult<MemberPointRecordRespVO> getMemberPointRecord(@RequestParam("id") Long id) {
        MemberPointRecordDO memberPointRecord = memberPointRecordService.getMemberPointRecord(id);
        return success(BeanUtils.toBean(memberPointRecord, MemberPointRecordRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得会员积分记录分页")
    @PreAuthorize("@ss.hasPermission('pharmacy:member:point-record:query')")
    public CommonResult<PageResult<MemberPointRecordRespVO>> getMemberPointRecordPage(@Validated MemberPointRecordPageReqVO pageReqVO) {
        PageResult<MemberPointRecordDO> pageResult = memberPointRecordService.getMemberPointRecordPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, MemberPointRecordRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出会员积分记录 Excel")
    @PreAuthorize("@ss.hasPermission('pharmacy:member:point-record:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportMemberPointRecord(HttpServletResponse response, @Validated MemberPointRecordPageReqVO reqVO) throws IOException {
        reqVO.setPageSize(cn.iocoder.yudao.framework.common.pojo.PageParam.PAGE_SIZE_NONE);
        List<MemberPointRecordDO> list = memberPointRecordService.getMemberPointRecordPage(reqVO).getList();
        ExcelUtils.write(response, "会员积分记录.xls", "积分记录列表", MemberPointRecordRespVO.class,
                BeanUtils.toBean(list, MemberPointRecordRespVO.class));
    }

}
