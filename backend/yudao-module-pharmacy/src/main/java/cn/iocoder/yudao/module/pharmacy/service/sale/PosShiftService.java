package cn.iocoder.yudao.module.pharmacy.service.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.pharmacy.controller.admin.pos.vo.PosShiftPageReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhPosShiftDO;

import java.math.BigDecimal;

public interface PosShiftService {

    /** 开台：创建营业中班次 */
    Long openShift(Long storeId, String posNo, Long cashierId);

    /** 交班：汇总销售笔数/金额，核对现金应收与实际，差异必须填原因 */
    void closeShift(Long shiftId, BigDecimal cashActual, String diffReason);

    PageResult<PhPosShiftDO> getShiftPage(PosShiftPageReqVO reqVO);

    PhPosShiftDO getShift(Long id);
}
