package cn.iocoder.yudao.module.pharmacy.dal.mysql.base;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.store.StorePageReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.base.StoreDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 门店 Mapper
 */
@Mapper
public interface StoreMapper extends BaseMapperX<StoreDO> {

    default PageResult<StoreDO> selectPage(StorePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<StoreDO>()
                .likeIfPresent(StoreDO::getStoreCode, reqVO.getStoreCode())
                .likeIfPresent(StoreDO::getStoreName, reqVO.getStoreName())
                .eqIfPresent(StoreDO::getIsMedical, reqVO.getIsMedical())
                .eqIfPresent(StoreDO::getStatus, reqVO.getStatus())
                .eqIfPresent(StoreDO::getDeptId, reqVO.getDeptId())
                .orderByAsc(StoreDO::getId));
    }

    default StoreDO selectByStoreCode(String storeCode) {
        return selectOne(StoreDO::getStoreCode, storeCode);
    }

    /**
     * 按租户+dept_id 唯一性校验
     *
     * 多租户由 yudao 自动注入 tenant_id 条件，故此处只需按 dept_id 查询
     */
    default StoreDO selectByDeptId(Long deptId) {
        return selectOne(StoreDO::getDeptId, deptId);
    }

}
