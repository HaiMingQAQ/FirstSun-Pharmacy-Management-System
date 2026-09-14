package cn.iocoder.yudao.module.pharmacy.controller.admin.member.vo.cart;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 小程序购物车分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class WxCartPageReqVO extends PageParam {

    @Schema(description = "会员用户编号", example = "1024")
    private Long memberId;

    @Schema(description = "商品编号", example = "1")
    private Long drugId;

    @Schema(description = "门店编号", example = "1")
    private Long storeId;

    @Schema(description = "是否勾选 1是/0否", example = "1")
    private Integer selectedFlag;

}
