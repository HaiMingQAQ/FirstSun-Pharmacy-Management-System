package cn.iocoder.yudao.module.pharmacy.api;

import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.pharmacy.api.dto.DrugRespDTO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.base.DrugDO;
import cn.iocoder.yudao.module.pharmacy.service.base.DrugService;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.Collection;
import java.util.List;

/**
 * 药品 API 实现类
 *
 * 不修改 DrugService 核心，仅做 DO → DTO 转换。
 *
 * @author A 成员
 */
@Service
public class DrugApiImpl implements DrugApi {

    @Resource
    private DrugService drugService;

    @Override
    public DrugRespDTO getDrug(Long id) {
        DrugDO drug = drugService.getDrug(id);
        return BeanUtils.toBean(drug, DrugRespDTO.class);
    }

    @Override
    public List<DrugRespDTO> getDrugList(Collection<Long> ids) {
        List<DrugDO> list = drugService.getDrugList(ids);
        return BeanUtils.toBean(list, DrugRespDTO.class);
    }

    @Override
    public void validateDrugList(Collection<Long> ids) {
        drugService.validateDrugList(ids);
    }

    @Override
    public DrugRespDTO getDrugByCode(String drugCode) {
        DrugDO drug = drugService.getDrugByCode(drugCode);
        return BeanUtils.toBean(drug, DrugRespDTO.class);
    }

}
