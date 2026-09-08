package cn.iocoder.yudao.module.pharmacy.api;

import cn.iocoder.yudao.module.pharmacy.api.dto.BarcodeRespDTO;
import cn.iocoder.yudao.module.pharmacy.api.dto.DrugRespDTO;

import java.util.Collection;
import java.util.List;

/**
 * 药品条码 API 接口
 *
 * 供其他模块（销售、库存等）跨模块调用，**只读**，不修改 pharmacy 模块核心 Service。
 *
 * 典型场景：
 * 1. 收银扫码：扫条码 → 返回药品精简信息 + 价格（核心场景）
 * 2. 库存批次查询某药品的所有条码
 * 3. 校验条码是否存在
 *
 * @author A 成员
 */
public interface BarcodeApi {

    /**
     * 按条码字符串查询，并附带关联药品信息（用于收银扫码场景）
     *
     * 复合返回结构：条码信息 + 药品信息
     * 如果条码不存在或对应药品未审核通过/未启用，返回 null
     *
     * @param barcode 条码字符串
     * @return 条码 + 药品信息（不存在/不可销售返回 null）
     */
    DrugBarcodeRespDTO getDrugByBarcode(String barcode);

    /**
     * 批量获得条码信息
     *
     * @param ids 条码编号集合
     * @return 条码信息列表
     */
    List<BarcodeRespDTO> getBarcodeList(Collection<Long> ids);

    /**
     * 查询某药品下的所有条码
     *
     * @param drugId 药品编号
     * @return 条码列表
     */
    List<BarcodeRespDTO> getBarcodeListByDrugId(Long drugId);

    /**
     * 校验条码是否存在（不存在抛异常）
     *
     * @param id 条码编号
     */
    void validateBarcodeExists(Long id);

    /**
     * 复合返回结构：条码 + 药品
     */
    @lombok.Data
    class DrugBarcodeRespDTO {
        /**
         * 条码信息
         */
        private BarcodeRespDTO barcode;
        /**
         * 药品信息
         */
        private DrugRespDTO drug;
    }

}
