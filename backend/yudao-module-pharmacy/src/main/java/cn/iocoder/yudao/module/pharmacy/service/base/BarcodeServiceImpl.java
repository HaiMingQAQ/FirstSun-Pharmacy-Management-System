package cn.iocoder.yudao.module.pharmacy.service.base;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.barcode.BarcodePageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.barcode.BarcodeSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.base.BarcodeDO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.base.BarcodeMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.Resource;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.*;

/**
 * 药品条码 Service 实现类
 *
 * 不修改其他模块：通过 DrugService 只读校验 drug_id。
 */
@Service
@Validated
public class BarcodeServiceImpl implements BarcodeService {

    @Resource
    private BarcodeMapper barcodeMapper;

    @Resource
    private DrugService drugService;

    @Override
    public Long createBarcode(BarcodeSaveReqVO createReqVO) {
        // 校验药品存在
        drugService.validateDrugExists(createReqVO.getDrugId());
        // 校验条码唯一
        validateBarcodeUnique(null, createReqVO.getBarcode());
        // 校验默认条码不重复
        validateDefaultUnique(null, createReqVO.getDrugId(), createReqVO.getIsDefault());
        // 写入
        BarcodeDO barcode = BeanUtils.toBean(createReqVO, BarcodeDO.class);
        barcodeMapper.insert(barcode);
        return barcode.getId();
    }

    @Override
    public void updateBarcode(BarcodeSaveReqVO updateReqVO) {
        // 校验存在
        validateBarcodeExists(updateReqVO.getId());
        // 校验药品存在
        drugService.validateDrugExists(updateReqVO.getDrugId());
        // 校验条码唯一
        validateBarcodeUnique(updateReqVO.getId(), updateReqVO.getBarcode());
        // 校验默认条码不重复
        validateDefaultUnique(updateReqVO.getId(), updateReqVO.getDrugId(), updateReqVO.getIsDefault());
        // 更新
        BarcodeDO updateObj = BeanUtils.toBean(updateReqVO, BarcodeDO.class);
        barcodeMapper.updateById(updateObj);
    }

    @Override
    public void deleteBarcode(Long id) {
        validateBarcodeExists(id);
        barcodeMapper.deleteById(id);
    }

    @Override
    public BarcodeDO getBarcode(Long id) {
        return barcodeMapper.selectById(id);
    }

    @Override
    public PageResult<BarcodeDO> getBarcodePage(BarcodePageReqVO reqVO) {
        return barcodeMapper.selectPage(reqVO);
    }

    @Override
    public BarcodeDO validateBarcodeExists(Long id) {
        if (id == null) {
            throw exception(PHARMACY_BARCODE_NOT_EXISTS);
        }
        BarcodeDO barcode = barcodeMapper.selectById(id);
        if (barcode == null) {
            throw exception(PHARMACY_BARCODE_NOT_EXISTS);
        }
        return barcode;
    }

    @Override
    public Long countByDrugId(Long drugId) {
        return barcodeMapper.countByDrugId(drugId);
    }

    @Override
    public BarcodeDO getBarcodeByCode(String barcode) {
        return barcodeMapper.selectByBarcode(barcode);
    }

    @Override
    public List<BarcodeDO> getBarcodeListByDrugId(Long drugId) {
        return barcodeMapper.selectList(BarcodeDO::getDrugId, drugId);
    }

    @Override
    public List<BarcodeDO> getBarcodeList(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return barcodeMapper.selectBatchIds(ids);
    }

    private void validateBarcodeUnique(Long id, String barcode) {
        BarcodeDO other = barcodeMapper.selectByBarcode(barcode);
        if (other == null) return;
        if (id == null || !Objects.equals(other.getId(), id)) {
            throw exception(PHARMACY_BARCODE_DUPLICATE);
        }
    }

    /**
     * 校验默认条码不重复：同一药品下只能有一个 is_default=1
     */
    private void validateDefaultUnique(Long id, Long drugId, Integer isDefault) {
        if (isDefault == null || isDefault != 1) return;
        BarcodeDO other = barcodeMapper.selectDefaultByDrugId(drugId);
        if (other == null) return;
        if (id == null || !Objects.equals(other.getId(), id)) {
            throw exception(PHARMACY_BARCODE_DEFAULT_DUPLICATE);
        }
    }

}
