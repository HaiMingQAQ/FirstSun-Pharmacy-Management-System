package cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.order;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 小程序订单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class WxOrderPageReqVO extends PageParam {

    @Schema(description = "线上订单号，模糊匹配", example = "WX-001-20240101-0001")
    private String orderNo;

    @Schema(description = "会员用户编号", example = "1024")
    private Long memberId;

    @Schema(description = "履约门店编号", example = "1")
    private Long storeId;

    @Schema(description = "订单类型 0到店自提/1同城配送", example = "0")
    private Integer orderType;

    @Schema(description = "订单状态 0待支付/1待拣货/2拣货中/3待自提/4完成/-1取消", example = "0")
    private Integer status;

    @Schema(description = "支付状态 0待支付/1已支付/2已退款", example = "0")
    private Integer payStatus;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
