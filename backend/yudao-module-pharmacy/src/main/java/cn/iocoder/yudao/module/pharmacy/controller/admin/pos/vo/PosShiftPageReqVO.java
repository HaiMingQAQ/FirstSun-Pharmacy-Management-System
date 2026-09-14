package cn.iocoder.yudao.module.pharmacy.controller.admin.pos.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class PosShiftPageReqVO extends PageParam {

    private Long storeId;

    private Long cashierId;

    private Integer status;
}
