package cn.iocoder.yudao.module.ai.dal.mysql.pharmacy;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.ai.dal.dataobject.pharmacy.PharmacyAiCommandDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PharmacyAiCommandMapper extends BaseMapperX<PharmacyAiCommandDO> {
    default List<PharmacyAiCommandDO> selectRecentByUser(Long userId) {
        return selectList(new LambdaQueryWrapperX<PharmacyAiCommandDO>()
                .eq(PharmacyAiCommandDO::getUserId, userId)
                .orderByDesc(PharmacyAiCommandDO::getId).last("LIMIT 50"));
    }

    default int claimPending(Long id, LocalDateTime now) {
        return update(null, new LambdaUpdateWrapper<PharmacyAiCommandDO>()
                .eq(PharmacyAiCommandDO::getId, id)
                .eq(PharmacyAiCommandDO::getStatus, "PENDING")
                .gt(PharmacyAiCommandDO::getExpiresAt, now)
                .set(PharmacyAiCommandDO::getStatus, "EXECUTING"));
    }

    default int cancelPending(Long id, Long userId) {
        return update(null, new LambdaUpdateWrapper<PharmacyAiCommandDO>()
                .eq(PharmacyAiCommandDO::getId, id)
                .eq(PharmacyAiCommandDO::getUserId, userId)
                .eq(PharmacyAiCommandDO::getStatus, "PENDING")
                .set(PharmacyAiCommandDO::getStatus, "CANCELLED"));
    }
}
