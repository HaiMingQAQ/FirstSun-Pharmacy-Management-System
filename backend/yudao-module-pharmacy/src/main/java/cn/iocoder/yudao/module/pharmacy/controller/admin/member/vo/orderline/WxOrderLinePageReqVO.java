package cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.orderline;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 小程序订单明细分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class WxOrderLinePageReqVO extends PageParam {

    @Schema(description = "线上订单编号", example = "1024")
    private Long wxOrderId;

    @Schema(description = "商品编号", example = "1")
    private Long drugId;

    @Schema(description = "下单名称快照，模糊匹配", example = "阿莫西林")
    private String drugName;

}
