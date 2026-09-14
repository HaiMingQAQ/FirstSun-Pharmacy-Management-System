package cn.iocoder.yudao.module.pharmacy.controller.app.member.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "用户 APP - 会员个人中心信息 Response VO")
@Data
public class AppMemberUserInfoRespVO {

    @Schema(description = "会员编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "手机号", example = "13800138000")
    private String mobile;

    @Schema(description = "用户昵称", example = "张三")
    private String nickname;

    @Schema(description = "用户头像", example = "https://www.iocoder.cn/xxx.png")
    private String avatar;

    @Schema(description = "性别", example = "1")
    private Integer sex;

    @Schema(description = "积分", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Integer point;

    @Schema(description = "经验", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Integer experience;

    @Schema(description = "会员等级编号", example = "1")
    private Long levelId;

    @Schema(description = "会员等级名称", example = "黄金会员")
    private String levelName;

    @Schema(description = "注册时间")
    private LocalDateTime createTime;

}
