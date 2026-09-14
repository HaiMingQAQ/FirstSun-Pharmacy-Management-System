package cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.level;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 会员等级分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class MemberLevelPageReqVO extends PageParam {

    @Schema(description = "等级名称，模糊匹配", example = "黄金会员")
    private String name;

    @Schema(description = "状态 0启用/1禁用", example = "0")
    private Integer status;

}
