package cn.iocoder.yudao.module.pharmacy.service.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.pharmacy.controller.admin.pos.vo.SaleReturnDetailRespVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.pos.vo.SaleReturnPageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.pos.vo.SaleReturnSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.sale.PhSaleReturnDO;

public interface SaleReturnService {

    /**
     * 创建退货单（全退/部分退）。
     * <p>
     * 校验：原单状态可退、逐行数量不超过可退量、处方药需药师复核；
     * 副作用：原批次回补库存（C 服务）、积分回退（F 服务）、渠道退款（E 服务）；
     * 更新原销售单 returnFlag/status。任一依赖未实现，整体回滚。
     */
    Long createReturn(SaleReturnSaveReqVO reqVO);

    PageResult<PhSaleReturnDO> getReturnPage(SaleReturnPageReqVO reqVO);

    PhSaleReturnDO getReturn(Long id);

    /**
     * 获得退货单详情（含明细行）。
     */
    SaleReturnDetailRespVO getReturnDetail(Long id);
}
