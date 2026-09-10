package cn.iocoder.yudao.module.pharmacy.service.base;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.barcode.BarcodePageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.barcode.BarcodeSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.base.BarcodeDO;

import javax.validation.Valid;
import java.util.Collection;
import java.util.List;

/**
 * 药品条码 Service
 */
public interface BarcodeService {

    /**
     * 创建条码
     */
    Long createBarcode(@Valid BarcodeSaveReqVO createReqVO);

    /**
     * 更新条码
     */
    void updateBarcode(@Valid BarcodeSaveReqVO updateReqVO);

    /**
     * 删除条码
     */
    void deleteBarcode(Long id);

    /**
     * 获取条码详情
     */
    BarcodeDO getBarcode(Long id);

    /**
     * 获取条码分页
     */
    PageResult<BarcodeDO> getBarcodePage(BarcodePageReqVO reqVO);

    /**
     * 校验条码存在，供其他业务校验引用
     */
    BarcodeDO validateBarcodeExists(Long id);

    /**
     * 统计某药品下未删除的条码数量，用于删除药品前校验
     */
    Long countByDrugId(Long drugId);

    /**
     * 根据条码字符串查询药品编号（用于收银扫码/跨模块商品查询）
     */
    BarcodeDO getBarcodeByCode(String barcode);

    /**
     * 查询某药品下的所有条码（用于跨模块商品详情）
     */
    List<BarcodeDO> getBarcodeListByDrugId(Long drugId);

    /**
     * 批量获得条码（跨模块 BarcodeApi 使用）
     *
     * @param ids 条码编号集合
     * @return 条码列表（仅返回未删除的）
     */
    List<BarcodeDO> getBarcodeList(Collection<Long> ids);

}
