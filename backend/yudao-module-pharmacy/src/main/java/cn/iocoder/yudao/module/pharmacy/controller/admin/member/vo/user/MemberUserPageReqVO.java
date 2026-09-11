package cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.user;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 会员用户分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class MemberUserPageReqVO extends PageParam {

    @Schema(description = "手机号，模糊匹配", example = "13800138000")
    private String mobile;

    @Schema(description = "用户昵称，模糊匹配", example = "张三")
    private String nickname;

    @Schema(description = "状态 0启用/1禁用", example = "0")
    private Integer status;

    @Schema(description = "等级编号", example = "1")
    private Long levelId;

}
