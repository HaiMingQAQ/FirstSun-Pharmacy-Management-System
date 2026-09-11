package cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.user;

import cn.iocoder.yudao.framework.excel.core.annotations.DictFormat;
import cn.iocoder.yudao.framework.excel.core.convert.DictConvert;
import cn.iocoder.yudao.module.pharmacy.enums.DictTypeConstants;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 会员用户 Response VO")
@Data
@ExcelIgnoreUnannotated
public class MemberUserRespVO {

    @Schema(description = "用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("用户编号")
    private Long id;

    @Schema(description = "手机号", requiredMode = Schema.RequiredMode.REQUIRED, example = "13800138000")
    @ExcelProperty("手机号")
    private String mobile;

    @Schema(description = "用户昵称", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    @ExcelProperty("用户昵称")
    private String nickname;

    @Schema(description = "头像", example = "https://www.iocoder.cn/avatar.jpg")
    private String avatar;

    @Schema(description = "真实名字", example = "张三")
    @ExcelProperty("真实名字")
    private String name;

    @Schema(description = "用户性别", example = "1")
    @ExcelProperty(value = "性别", converter = DictConvert.class)
    @DictFormat(cn.iocoder.yudao.module.system.enums.DictTypeConstants.USER_SEX)
    private Integer sex;

    @Schema(description = "状态 0启用/1禁用", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @ExcelProperty(value = "状态", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.PHARMACY_MEMBER_STATUS)
    private Integer status;

    @Schema(description = "积分", example = "100")
    @ExcelProperty("积分")
    private Integer point;

    @Schema(description = "等级编号", example = "1")
    private Long levelId;

    @Schema(description = "经验", example = "500")
    @ExcelProperty("经验")
    private Integer experience;

    @Schema(description = "注册 IP", example = "127.0.0.1")
    private String registerIp;

    @Schema(description = "注册终端", example = "1")
    private Integer registerTerminal;

    @Schema(description = "最后登录IP", example = "127.0.0.1")
    private String loginIp;

    @Schema(description = "最后登录时间")
    private LocalDateTime loginDate;

    @Schema(description = "所在地", example = "1")
    private Long areaId;

    @Schema(description = "出生日期")
    private LocalDateTime birthday;

    @Schema(description = "会员备注", example = "VIP客户")
    private String mark;

    @Schema(description = "用户标签编号列表", example = "1,2,3")
    private String tagIds;

    @Schema(description = "用户分组编号", example = "1")
    private Long groupId;

    @Schema(description = "邮箱", example = "user@example.com")
    private String email;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}
