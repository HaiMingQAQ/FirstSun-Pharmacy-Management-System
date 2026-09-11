package cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 会员用户创建/修改 Request VO")
@Data
public class MemberUserSaveReqVO {

    @Schema(description = "用户编号", example = "1024")
    private Long id;

    @Schema(description = "手机号", requiredMode = Schema.RequiredMode.REQUIRED, example = "13800138000")
    @NotBlank(message = "手机号不能为空")
    @Size(max = 11, message = "手机号长度不能超过 11 个字符")
    private String mobile;

    @Schema(description = "密码", example = "123456")
    private String password;

    @Schema(description = "用户昵称", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    @NotBlank(message = "用户昵称不能为空")
    @Size(max = 30, message = "用户昵称长度不能超过 30 个字符")
    private String nickname;

    @Schema(description = "头像", example = "https://www.iocoder.cn/avatar.jpg")
    private String avatar;

    @Schema(description = "真实名字", example = "张三")
    @Size(max = 30, message = "真实名字长度不能超过 30 个字符")
    private String name;

    @Schema(description = "用户性别", example = "1")
    private Integer sex;

    @Schema(description = "状态 0启用/1禁用", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    private Integer status;

    @Schema(description = "所在地", example = "1")
    private Long areaId;

    @Schema(description = "出生日期")
    private LocalDateTime birthday;

    @Schema(description = "会员备注", example = "VIP客户")
    @Size(max = 255, message = "会员备注长度不能超过 255 个字符")
    private String mark;

    @Schema(description = "积分", example = "100")
    private Integer point;

    @Schema(description = "用户标签编号列表", example = "1,2,3")
    private String tagIds;

    @Schema(description = "等级编号", example = "1")
    private Long levelId;

    @Schema(description = "经验", example = "500")
    private Integer experience;

    @Schema(description = "用户分组编号", example = "1")
    private Long groupId;

    @Schema(description = "邮箱", example = "user@example.com")
    private String email;

}
