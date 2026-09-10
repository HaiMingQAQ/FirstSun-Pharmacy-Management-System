package cn.iocoder.yudao.module.pharmacy.dal.mysql.base;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.employee.EmployeePageReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.base.EmployeeDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 员工 Mapper
 */
@Mapper
public interface EmployeeMapper extends BaseMapperX<EmployeeDO> {

    default PageResult<EmployeeDO> selectPage(EmployeePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<EmployeeDO>()
                .likeIfPresent(EmployeeDO::getEmpNo, reqVO.getEmpNo())
                .likeIfPresent(EmployeeDO::getEmpName, reqVO.getEmpName())
                .eqIfPresent(EmployeeDO::getStoreId, reqVO.getStoreId())
                .eqIfPresent(EmployeeDO::getPosition, reqVO.getPosition())
                .eqIfPresent(EmployeeDO::getStatus, reqVO.getStatus())
                .eqIfPresent(EmployeeDO::getUserId, reqVO.getUserId())
                .orderByAsc(EmployeeDO::getId));
    }

    default EmployeeDO selectByEmpNo(String empNo) {
        return selectOne(EmployeeDO::getEmpNo, empNo);
    }

    /**
     * 按租户+user_id 唯一性校验
     *
     * 多租户由 yudao 自动注入 tenant_id 条件，故此处只需按 user_id 查询
     */
    default EmployeeDO selectByUserId(Long userId) {
        return selectOne(EmployeeDO::getUserId, userId);
    }

    /**
     * 统计某门店下未删除的员工数量，用于删除门店前校验
     */
    default Long countByStoreId(Long storeId) {
        return selectCount(EmployeeDO::getStoreId, storeId);
    }

}
