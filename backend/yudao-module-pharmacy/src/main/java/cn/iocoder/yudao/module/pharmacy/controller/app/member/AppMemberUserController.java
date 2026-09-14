package cn.iocoder.yudao.module.pharmacy.controller.app.member;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.pharmacy.controller.app.member.vo.user.AppMemberUserInfoRespVO;
import cn.iocoder.yudao.module.pharmacy.controller.app.member.vo.user.AppMemberUserUpdateReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.MemberLevelDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.MemberUserDO;
import cn.iocoder.yudao.module.pharmacy.service.member.MemberLevelService;
import cn.iocoder.yudao.module.pharmacy.service.member.MemberUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/**
 * 用户 APP - 会员个人中心
 *
 * 所有查询与修改均基于登录令牌中的会员编号，不接受前端传入会员编号。
 */
@Tag(name = "用户 APP - 会员个人中心")
@RestController
@RequestMapping("/member/user")
@Validated
@Slf4j
public class AppMemberUserController {

    @Resource
    private MemberUserService memberUserService;

    @Resource
    private MemberLevelService memberLevelService;

    @GetMapping("/get")
    @Operation(summary = "获得会员基本信息")
    public CommonResult<AppMemberUserInfoRespVO> getUserInfo() {
        MemberUserDO user = memberUserService.validateMemberUserExists(getLoginUserId());
        AppMemberUserInfoRespVO respVO = BeanUtils.toBean(user, AppMemberUserInfoRespVO.class);
        // 补充会员等级名称
        if (user.getLevelId() != null) {
            MemberLevelDO level = memberLevelService.getMemberLevel(user.getLevelId());
            if (level != null) {
                respVO.setLevelName(level.getName());
            }
        }
        return success(respVO);
    }

    @PutMapping("/update")
    @Operation(summary = "修改会员基本信息")
    public CommonResult<Boolean> updateUserInfo(@RequestBody @Valid AppMemberUserUpdateReqVO reqVO) {
        memberUserService.updateMemberUserProfile(getLoginUserId(),
                reqVO.getNickname(), reqVO.getAvatar(), reqVO.getSex());
        return success(true);
    }

}
