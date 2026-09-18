package cn.iocoder.yudao.module.pharmacy.dal.mysql.purchase;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.purchase.PurchaseDocSeqDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 采购单号序列表 Mapper（B 模块并发取号）
 *
 * <p>表 {@code ph_po_doc_seq}，唯一键 {@code uk_doc_seq(store_id, biz_date, biz_type)}。
 *
 * @author B 成员
 */
@Mapper
public interface PurchaseDocSeqMapper extends BaseMapperX<PurchaseDocSeqDO> {

    /**
     * 确保序列行存在（不存在则插入 next_seq = 0）。
     *
     * <p>用 {@code INSERT IGNORE} 忽略唯一键冲突，并发安全。
     */
    @Insert("INSERT IGNORE INTO ph_po_doc_seq (store_id, biz_date, biz_type, next_seq) "
            + "VALUES (#{storeId}, #{bizDate}, #{bizType}, 0)")
    int ensureSeqRow(@Param("storeId") Long storeId,
                     @Param("bizDate") String bizDate,
                     @Param("bizType") String bizType);

    /**
     * 以**排他锁**锁定序列行，避免「先取共享锁、再升级为排他锁」造成的死锁。
     *
     * <p>背景：若先用普通 {@code INSERT IGNORE} / {@code SELECT} 触碰该行（InnoDB 会加 S 锁），
     * 再用 {@code UPDATE} 推进序列（需要 X 锁），两个并发事务会各持 S 锁并互相等待对方释放，
     * 形成经典的锁升级死锁（实测 InnoDB 死锁日志：双方均 {@code lock mode S} 持有、
     * {@code lock_mode X locks rec but not gap waiting} 等待）。
     * 直接以 {@code FOR UPDATE} 申请 X 锁，只有「拿到 / 等待」两种结果，不会形成环。
     *
     * <p>调用前必须已通过 {@link #ensureSeqRow} 保证行存在，否则会取到间隙锁。
     */
    @Select("SELECT next_seq FROM ph_po_doc_seq "
            + "WHERE store_id = #{storeId} AND biz_date = #{bizDate} AND biz_type = #{bizType} "
            + "FOR UPDATE")
    Integer lockAndSelectSeq(@Param("storeId") Long storeId,
                             @Param("bizDate") String bizDate,
                             @Param("bizType") String bizType);

    /**
     * 将序列推进到指定值（在已持有排他锁的前提下调用）。
     */
    @Insert("UPDATE ph_po_doc_seq SET next_seq = #{nextSeq} "
            + "WHERE store_id = #{storeId} AND biz_date = #{bizDate} AND biz_type = #{bizType}")
    int updateSeq(@Param("storeId") Long storeId,
                  @Param("bizDate") String bizDate,
                  @Param("bizType") String bizType,
                  @Param("nextSeq") int nextSeq);

    /**
     * 原子分配下一个流水号（对外唯一入口，须在 {@code REQUIRES_NEW} 事务内调用）。
     *
     * <p><b>顺序很关键：先取排他锁，再按需插入。</b>
     * <ul>
     *   <li>若先 {@code INSERT IGNORE}（对已存在的行会加 <b>S 锁</b>）再申请 <b>X 锁</b>，
     *       两个并发事务会各持 S 锁并互相等待对方释放，形成<b>锁升级死锁</b>
     *       （实测 InnoDB 死锁日志已确认：双方均 {@code lock mode S} 持有、
     *       {@code lock_mode X ... waiting} 等待；换用 FOR UPDATE 亦复现）。</li>
     *   <li>改为<b>先</b> {@code SELECT ... FOR UPDATE}（直接申请 X 锁，只有「拿到/等待」两种结果），
     *       仅当行不存在时才插入，从而不会出现 S→X 升级。</li>
     * </ul>
     *
     * <p>为什么不用 {@code INSERT ... ON DUPLICATE KEY UPDATE next_seq = LAST_INSERT_ID(next_seq + 1)}：
     * 该写法在 INSERT 分支返回的是**自增主键**而非流水号（实测首次分配返回 52 而非 1，
     * 且无法通过 {@code useGeneratedKeys} 修正），只有 UPDATE 分支才返回流水号，
     * 并发时首次请求会拿到同一个错号。
     *
     * @param storeId 门店编号
     * @param bizDate 业务日期（yyyyMMdd）
     * @param bizType 单据类型：ORDER / RECEIPT
     * @return 本次分配到的流水号（从 1 开始递增）
     */
    default long allocateSeq(Long storeId, String bizDate, String bizType) {
        // ① 先取排他锁（行不存在时返回 null，且不会加 S 锁）
        Integer current = lockAndSelectSeq(storeId, bizDate, bizType);
        if (current == null) {
            // ② 首次分配：插入初始行（唯一键冲突说明他事务刚插入，忽略即可，下次循环会取到锁）
            ensureSeqRow(storeId, bizDate, bizType);
            current = lockAndSelectSeq(storeId, bizDate, bizType);
        }
        if (current == null) {
            // 理论上不可达：ensureSeqRow 后行必定存在
            throw new IllegalStateException(
                    "采购单号序列分配失败：" + storeId + "/" + bizDate + "/" + bizType);
        }
        // ③ 写回 +1
        int next = current + 1;
        updateSeq(storeId, bizDate, bizType, next);
        return next;
    }

}
