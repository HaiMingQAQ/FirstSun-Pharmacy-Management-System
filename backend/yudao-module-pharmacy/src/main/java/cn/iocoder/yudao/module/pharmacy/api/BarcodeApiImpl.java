package cn.iocoder.yudao.module.pharmacy.api;

import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.pharmacy.api.dto.BarcodeRespDTO;
import cn.iocoder.yudao.module.pharmacy.api.dto.DrugRespDTO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.base.BarcodeDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.base.DrugDO;
import cn.iocoder.yudao.module.pharmacy.service.base.BarcodeService;
import cn.iocoder.yudao.module.pharmacy.service.base.DrugService;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.Collection;
import java.util.List;

/**
 * 药品条码 API 实现类
 *
 * 不修改 BarcodeService/DrugService 核心，仅做 DO → DTO 转换与跨表组合。
 *
 * @author A 成员
 */
@Service
public class BarcodeApiImpl implements BarcodeApi {

    @Resource
    private BarcodeService barcodeService;

    @Resource
    private DrugService drugService;

    @Override
    public DrugBarcodeRespDTO getDrugByBarcode(String barcode) {
        if (barcode == null || barcode.trim().isEmpty()) {
            return null;
        }
        // 1. 查条码
        BarcodeDO barcodeDO = barcodeService.getBarcodeByCode(barcode);
        if (barcodeDO == null) {
            return null;
        }
        // 2. 查关联药品
        DrugDO drug = drugService.getDrug(barcodeDO.getDrugId());
        if (drug == null) {
            return null;
        }
        // 3. 业务约束：仅已审核通过 + 启用的药品可销售
        if (drug.getStatus() == null || drug.getStatus() != 1) {
            return null;
        }
        if (drug.getApproveStatus() == null || drug.getApproveStatus() != 1) {
            return null;
        }
        // 4. 组合返回
        DrugBarcodeRespDTO resp = new DrugBarcodeRespDTO();
        resp.setBarcode(BeanUtils.toBean(barcodeDO, BarcodeRespDTO.class));
        resp.setDrug(BeanUtils.toBean(drug, DrugRespDTO.class));
        return resp;
    }

    @Override
    public List<BarcodeRespDTO> getBarcodeList(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        List<BarcodeDO> list = barcodeService.getBarcodeList(ids);
        return BeanUtils.toBean(list, BarcodeRespDTO.class);
    }

    @Override
    public List<BarcodeRespDTO> getBarcodeListByDrugId(Long drugId) {
        List<BarcodeDO> list = barcodeService.getBarcodeListByDrugId(drugId);
        return BeanUtils.toBean(list, BarcodeRespDTO.class);
    }

    @Override
    public void validateBarcodeExists(Long id) {
        barcodeService.validateBarcodeExists(id);
    }

}
