package cn.iocoder.yudao.module.pharmacy.service.purchase;

import cn.iocoder.yudao.module.pharmacy.dal.mysql.purchase.PurchaseDocSeqMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 采购单号序列分配服务（B 模块并发取号）
 *
 * <p><b>为什么单独成一个 Bean，而不是直接在调用方事务里分配：</b>
 * <ul>
 *   <li>{@code createReceipt} / {@code createOrder} 都在 {@code @Transactional} 内。
 *       序列行的「确保存在」若用 {@code INSERT IGNORE} 在<b>同一事务</b>里执行，
 *       InnoDB 在 REPEATABLE-READ 下会对该唯一键记录加 <b>S 锁</b>；随后的
 *       {@code UPDATE next_seq = next_seq + 1} 需要把同一记录的锁<b>升级为 X 锁</b>。
 *       两个并发事务各持 S 锁、又都在等对方释放 → 经典锁升级死锁
 *       （实测死锁日志确认：双方均为 {@code lock mode S} 持有、
 *       {@code lock_mode X locks rec but not gap waiting} 等待）。</li>
 *   <li>把「确保存在」与「推进」放进 <b>独立事务</b>（{@code REQUIRES_NEW}）后，
 *       行锁在方法返回时立刻提交释放，调用方事务内不再做锁升级，
 *       并发调用改由数据库行锁自然串行，不再死锁。</li>
 * </ul>
 *
 * <p><b>代价与取舍：</b>独立事务提交后，若调用方事务随后失败回滚，该次分配的流水号会被跳过，
 * 表现为单号出现空档。对单据流水号而言，空档是可接受的，
 * 而「唯一、递增、不死锁」是必须的 —— 唯一性优先于连续性。
 *
 * @author B 成员
 */
@Service
public class PurchaseDocSeqService {

    @Resource
    private PurchaseDocSeqMapper seqMapper;

    /**
     * 原子分配下一个流水号（独立事务，提交后立即释放行锁）
     *
     * @param storeId 门店编号
     * @param bizDate 业务日期（yyyyMMdd）
     * @param bizType 单据类型：ORDER / RECEIPT
     * @return 本次分配到的流水号（从 1 开始递增）
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public long allocate(Long storeId, String bizDate, String bizType) {
        return seqMapper.allocateSeq(storeId, bizDate, bizType);
    }

}
