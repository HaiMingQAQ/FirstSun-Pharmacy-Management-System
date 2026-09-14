package cn.iocoder.yudao.module.pharmacy.controller.app.member.vo.point;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "用户 APP - 会员积分记录分页 Request VO（仅查询本人积分）")
@Data
@EqualsAndHashCode(callSuper = true)
public class AppMemberPointRecordPageReqVO extends PageParam {

    @Schema(description = "业务类型", example = "2")
    private Integer bizType;

}
