package cn.iocoder.yudao.module.pharmacy.service.base;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.employee.EmployeePageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.employee.EmployeeSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.base.EmployeeDO;

import jakarta.validation.Valid;

/**
 * 员工 Service
 */
public interface EmployeeService {

    /**
     * 创建员工
     */
    Long createEmployee(@Valid EmployeeSaveReqVO createReqVO);

    /**
     * 更新员工
     */
    void updateEmployee(@Valid EmployeeSaveReqVO updateReqVO);

    /**
     * 删除员工
     */
    void deleteEmployee(Long id);

    /**
     * 获取员工详情
     */
    EmployeeDO getEmployee(Long id);

    /**
     * 获取员工分页
     */
    PageResult<EmployeeDO> getEmployeePage(EmployeePageReqVO reqVO);

    /**
     * 校验员工存在，供其他业务校验引用
     */
    EmployeeDO validateEmployeeExists(Long id);

    /**
     * 统计门店下员工数量，用于删除门店前校验
     */
    Long countByStoreId(Long storeId);

    /**
     * 根据关联的 system 用户编号获取药店员工，用于审核人等业务身份解析
     *
     * @param userId system 用户编号
     * @return 员工对象（未找到返回 null）
     */
    EmployeeDO getEmployeeByUserId(Long userId);

}
