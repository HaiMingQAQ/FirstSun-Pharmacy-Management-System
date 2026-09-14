package cn.iocoder.yudao.module.pharmacy.service.base;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.store.StorePageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.store.StoreSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.base.StoreDO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.base.EmployeeMapper;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.base.StoreMapper;
import cn.iocoder.yudao.module.pharmacy.enums.PharmacyStatusEnum;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.Resource;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.*;

/**
 * 门店 Service 实现类
 */
@Service
@Validated
public class StoreServiceImpl implements StoreService {

    @Resource
    private StoreMapper storeMapper;

    @Resource
    private DeptApi deptApi;

    /**
     * 员工 Mapper。仅用于删除门店前计数校验，直接注入 Mapper 以避免与 EmployeeService 的循环依赖。
     */
    @Resource
    private EmployeeMapper employeeMapper;

    @Override
    public Long createStore(StoreSaveReqVO createReqVO) {
        // 校验编码唯一
        validateStoreCodeUnique(null, createReqVO.getStoreCode());
        // 校验 dept_id 存在且唯一关联
        validateDeptIdForSave(null, createReqVO.getDeptId());
        // 写入
        StoreDO store = BeanUtils.toBean(createReqVO, StoreDO.class);
        storeMapper.insert(store);
        return store.getId();
    }

    @Override
    public void updateStore(StoreSaveReqVO updateReqVO) {
        // 校验存在
        validateStoreExists(updateReqVO.getId());
        // 校验编码唯一
        validateStoreCodeUnique(updateReqVO.getId(), updateReqVO.getStoreCode());
        // 校验 dept_id 存在且唯一关联
        validateDeptIdForSave(updateReqVO.getId(), updateReqVO.getDeptId());
        // 更新
        StoreDO updateObj = BeanUtils.toBean(updateReqVO, StoreDO.class);
        storeMapper.updateById(updateObj);
    }

    @Override
    public void deleteStore(Long id) {
        // 校验存在
        validateStoreExists(id);
        // 校验是否有员工关联本门店；存在则拒绝删除
        Long empCount = employeeMapper.countByStoreId(id);
        if (empCount != null && empCount > 0) {
            throw exception(PHARMACY_STORE_HAS_EMPLOYEE);
        }
        storeMapper.deleteById(id);
    }

    @Override
    public StoreDO getStore(Long id) {
        return storeMapper.selectById(id);
    }

    @Override
    public List<StoreDO> getStoreList(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return storeMapper.selectBatchIds(ids);
    }

    @Override
    public PageResult<StoreDO> getStorePage(StorePageReqVO reqVO) {
        return storeMapper.selectPage(reqVO);
    }

    @Override
    public List<StoreDO> getEnabledStoreList() {
        return storeMapper.selectList(StoreDO::getStatus, PharmacyStatusEnum.ENABLE.getStatus());
    }

    @Override
    public StoreDO validateStoreExistsAndOpen(Long id) {
        StoreDO store = storeMapper.selectById(id);
        if (store == null) {
            throw exception(PHARMACY_STORE_NOT_EXISTS);
        }
        if (!PharmacyStatusEnum.isEnable(store.getStatus())) {
            throw exception(PHARMACY_STORE_NOT_OPEN, store.getStoreName());
        }
        return store;
    }

    private void validateStoreExists(Long id) {
        if (id == null) {
            throw exception(PHARMACY_STORE_NOT_EXISTS);
        }
        if (storeMapper.selectById(id) == null) {
            throw exception(PHARMACY_STORE_NOT_EXISTS);
        }
    }

    private void validateStoreCodeUnique(Long id, String storeCode) {
        StoreDO store = storeMapper.selectByStoreCode(storeCode);
        if (store == null) {
            return;
        }
        if (id == null) {
            throw exception(PHARMACY_STORE_CODE_DUPLICATE);
        }
        if (!Objects.equals(store.getId(), id)) {
            throw exception(PHARMACY_STORE_CODE_DUPLICATE);
        }
    }

    /**
     * 校验 dept_id：必须存在于 system_dept；且同一租户下未被其他门店关联
     *
     * 不修改 system 模块代码，仅通过 DeptApi 读取校验
     */
    private void validateDeptIdForSave(Long id, Long deptId) {
        if (deptId == null) {
            return;
        }
        // 校验部门存在
        if (deptApi.getDept(deptId) == null) {
            throw exception(PHARMACY_STORE_DEPT_NOT_EXISTS);
        }
        // 校验同一租户下 dept_id 未被其他门店关联
        StoreDO other = storeMapper.selectByDeptId(deptId);
        if (other == null) {
            return;
        }
        if (id == null || !Objects.equals(other.getId(), id)) {
            throw exception(PHARMACY_STORE_DEPT_DUPLICATE);
        }
    }

}
