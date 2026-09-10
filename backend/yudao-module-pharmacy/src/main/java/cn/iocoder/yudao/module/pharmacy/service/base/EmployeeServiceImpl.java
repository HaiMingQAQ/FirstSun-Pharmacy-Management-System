package cn.iocoder.yudao.module.pharmacy.service.base;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.employee.EmployeePageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.employee.EmployeeSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.base.EmployeeDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.base.StoreDO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.base.EmployeeMapper;
import cn.iocoder.yudao.module.pharmacy.enums.EmployeeStatusEnum;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.*;

/**
 * 员工 Service 实现类
 *
 * 不修改 system 核心代码：通过 {@link AdminUserApi} 只读校验 user_id 存在与否。
 */
@Service
@Validated
public class EmployeeServiceImpl implements EmployeeService {

    @Resource
    private EmployeeMapper employeeMapper;

    @Resource
    private StoreService storeService;

    @Resource
    private AdminUserApi adminUserApi;

    @Override
    public Long createEmployee(EmployeeSaveReqVO createReqVO) {
        // 校验工号唯一
        validateEmpNoUnique(null, createReqVO.getEmpNo());
        // 校验门店存在；在职员工要求门店营业
        StoreDO store = storeService.validateStoreExistsAndOpen(createReqVO.getStoreId());
        // 校验关联用户（存在、未被禁用、同租户未被其他员工关联）
        validateUserIdForSave(null, createReqVO.getUserId(), createReqVO.getStatus());
        // 写入
        EmployeeDO employee = BeanUtils.toBean(createReqVO, EmployeeDO.class);
        employeeMapper.insert(employee);
        return employee.getId();
    }

    @Override
    public void updateEmployee(EmployeeSaveReqVO updateReqVO) {
        // 校验存在
        validateEmployeeExists(updateReqVO.getId());
        // 校验工号唯一
        validateEmpNoUnique(updateReqVO.getId(), updateReqVO.getEmpNo());
        // 校验门店存在；在职员工要求门店营业
        StoreDO store = storeService.validateStoreExistsAndOpen(updateReqVO.getStoreId());
        // 校验关联用户
        validateUserIdForSave(updateReqVO.getId(), updateReqVO.getUserId(), updateReqVO.getStatus());
        // 更新
        EmployeeDO updateObj = BeanUtils.toBean(updateReqVO, EmployeeDO.class);
        employeeMapper.updateById(updateObj);
    }

    @Override
    public void deleteEmployee(Long id) {
        // 校验存在
        validateEmployeeExists(id);
        employeeMapper.deleteById(id);
    }

    @Override
    public EmployeeDO getEmployee(Long id) {
        return employeeMapper.selectById(id);
    }

    @Override
    public PageResult<EmployeeDO> getEmployeePage(EmployeePageReqVO reqVO) {
        return employeeMapper.selectPage(reqVO);
    }

    @Override
    public EmployeeDO validateEmployeeExists(Long id) {
        if (id == null) {
            throw exception(PHARMACY_EMPLOYEE_NOT_EXISTS);
        }
        EmployeeDO employee = employeeMapper.selectById(id);
        if (employee == null) {
            throw exception(PHARMACY_EMPLOYEE_NOT_EXISTS);
        }
        return employee;
    }

    @Override
    public Long countByStoreId(Long storeId) {
        return employeeMapper.countByStoreId(storeId);
    }

    @Override
    public EmployeeDO getEmployeeByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        return employeeMapper.selectByUserId(userId);
    }

    private void validateEmpNoUnique(Long id, String empNo) {
        EmployeeDO employee = employeeMapper.selectByEmpNo(empNo);
        if (employee == null) {
            return;
        }
        if (id == null) {
            throw exception(PHARMACY_EMPLOYEE_NO_DUPLICATE);
        }
        if (!Objects.equals(employee.getId(), id)) {
            throw exception(PHARMACY_EMPLOYEE_NO_DUPLICATE);
        }
    }

    /**
     * 校验关联用户：
     * 1. 用户存在（通过 AdminUserApi 只读，不修改 system）
     * 2. 用户未被禁用
     * 3. 同一租户下未被其他员工关联
     * 4. 离职员工可关联禁用用户（仅要求存在）
     */
    private void validateUserIdForSave(Long id, Long userId, Integer status) {
        if (userId == null) {
            return;
        }
        // 1. 用户存在
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        if (user == null) {
            throw exception(PHARMACY_EMPLOYEE_USER_NOT_EXISTS);
        }
        // 2. 在职员工要求用户启用（CommonStatusEnum.ENABLE=0）
        if (EmployeeStatusEnum.isActive(status) && user.getStatus() != null && user.getStatus() != 0) {
            throw exception(PHARMACY_EMPLOYEE_USER_DISABLED, user.getNickname());
        }
        // 3. 同租户下 user_id 唯一关联
        EmployeeDO other = employeeMapper.selectByUserId(userId);
        if (other == null) {
            return;
        }
        if (id == null || !Objects.equals(other.getId(), id)) {
            throw exception(PHARMACY_EMPLOYEE_USER_DUPLICATE);
        }
    }

}
