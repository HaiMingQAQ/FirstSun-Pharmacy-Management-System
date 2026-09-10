package cn.iocoder.yudao.module.pharmacy.api;

import cn.iocoder.yudao.module.pharmacy.api.dto.DrugRespDTO;

import java.util.Collection;
import java.util.List;

/**
 * 药品 API 接口
 *
 * 供其他模块（销售、库存、采购等）跨模块调用，**只读**，不修改 pharmacy 模块核心 Service。
 *
 * 典型场景：
 * 1. 销售单创建/更新时校验药品存在且可销售（已审核通过 + 启用）
 * 2. 库存盘点时通过药品编号批量获取药品精简信息
 * 3. 收银扫码：扫条码 → BarcodeApi.getDrugByBarcode → 返回 DrugRespDTO
 *
 * @author A 成员
 */
public interface DrugApi {

    /**
     * 获得药品信息
     *
     * @param id 药品编号
     * @return 药品信息（不存在返回 null）
     */
    DrugRespDTO getDrug(Long id);

    /**
     * 批量获得药品信息
     *
     * @param ids 药品编号集合
     * @return 药品信息列表
     */
    List<DrugRespDTO> getDrugList(Collection<Long> ids);

    /**
     * 校验药品们是否有效。如下情况，视为无效：
     * 1. 药品编号不存在
     * 2. 药品被禁用
     * 3. 药品未审核通过（approveStatus != 1）
     *
     * @param ids 药品编号集合
     */
    void validateDrugList(Collection<Long> ids);

    /**
     * 按药品编码精确查询（用于销售单录入药品编码场景）
     *
     * @param drugCode 药品编码
     * @return 药品信息（不存在返回 null）
     */
    DrugRespDTO getDrugByCode(String drugCode);

}
