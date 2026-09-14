package cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.address;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 会员收件地址分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class MemberAddressPageReqVO extends PageParam {

    @Schema(description = "用户编号", example = "1024")
    private Long userId;

    @Schema(description = "收件人名称，模糊匹配", example = "张三")
    private String name;

    @Schema(description = "手机号，模糊匹配", example = "13800138000")
    private String mobile;

}
