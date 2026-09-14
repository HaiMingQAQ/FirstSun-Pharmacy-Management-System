package cn.iocoder.yudao.module.pharmacy.controller.app.member;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.pointrecord.MemberPointRecordPageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.app.member.vo.point.AppMemberPointRecordPageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.app.member.vo.point.AppMemberPointRecordRespVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.MemberPointRecordDO;
import cn.iocoder.yudao.module.pharmacy.service.member.MemberPointRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/**
 * 用户 APP - 会员积分记录
 *
 * 仅查询本人积分流水，用户编号取自登录令牌。
 */
@Tag(name = "用户 APP - 会员积分记录")
@RestController
@RequestMapping("/member/point-record")
@Validated
@Slf4j
public class AppMemberPointRecordController {

    @Resource
    private MemberPointRecordService memberPointRecordService;

    @GetMapping("/page")
    @Operation(summary = "获得本人积分记录分页")
    public CommonResult<PageResult<AppMemberPointRecordRespVO>> getPointRecordPage(
            @Valid AppMemberPointRecordPageReqVO pageReqVO) {
        // 强制使用登录会员编号，防止越权查询他人积分
        MemberPointRecordPageReqVO reqVO = new MemberPointRecordPageReqVO();
        reqVO.setPageNo(pageReqVO.getPageNo());
        reqVO.setPageSize(pageReqVO.getPageSize());
        reqVO.setUserId(getLoginUserId());
        reqVO.setBizType(pageReqVO.getBizType());
        PageResult<MemberPointRecordDO> pageResult = memberPointRecordService.getMemberPointRecordPage(reqVO);
        return success(BeanUtils.toBean(pageResult, AppMemberPointRecordRespVO.class));
    }

}
