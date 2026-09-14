package cn.iocoder.yudao.module.pharmacy.controller.app.member.vo.order;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "用户 APP - 小程序订单分页 Request VO（仅查询本人订单）")
@Data
@EqualsAndHashCode(callSuper = true)
public class AppWxOrderPageReqVO extends PageParam {

    @Schema(description = "订单状态 0待支付/1待拣货/2拣货中/3待自提/4完成/-1取消", example = "0")
    private Integer status;

    @Schema(description = "支付状态 0待支付/1已支付/2已退款", example = "0")
    private Integer payStatus;

}
