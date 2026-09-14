package cn.iocoder.yudao.module.pharmacy.controller.admin.pos.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
public class SaleOrderPageReqVO extends PageParam {

    private Long storeId;

    private Long cashierId;

    private String orderNo;

    private Integer status;

    /** [开始, 结束] 销售时间 */
    private LocalDateTime[] saleTime;
}
