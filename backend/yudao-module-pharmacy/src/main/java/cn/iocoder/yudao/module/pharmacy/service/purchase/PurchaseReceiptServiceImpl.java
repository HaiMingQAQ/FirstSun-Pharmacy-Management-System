package cn.iocoder.yudao.module.pharmacy.service.purchase;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.pharmacy.api.DrugApi;
import cn.iocoder.yudao.module.pharmacy.api.dto.DrugRespDTO;
import cn.iocoder.yudao.module.pharmacy.api.inventory.InventoryFacade;
import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.ReceiveItem;
import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.ReceiveResult;
import cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.receipt.PurchaseReceiptLineSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.receipt.PurchaseReceiptPageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.receipt.PurchaseReceiptSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.base.EmployeeDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.purchase.PurchaseOrderDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.purchase.PurchaseOrderLineDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.purchase.PurchaseReceiptDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.purchase.PurchaseReceiptLineDO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.purchase.PurchaseReceiptLineMapper;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.purchase.PurchaseReceiptMapper;
import cn.iocoder.yudao.module.pharmacy.enums.PurchaseDocSeqTypeEnum;
import cn.iocoder.yudao.module.pharmacy.enums.PurchaseOrderStatusEnum;
import cn.iocoder.yudao.module.pharmacy.enums.PurchaseReceiptStatusEnum;
import cn.iocoder.yudao.module.pharmacy.service.base.EmployeeService;
import cn.iocoder.yudao.module.pharmacy.service.base.StoreService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.*;

/**
 * 采购收货 Service 实现类
 *
 * @author B 成员
 */
@Service
@Validated
public class PurchaseReceiptServiceImpl implements PurchaseReceiptService {

    /**
     * 单号日期段格式
     */
    private static final DateTimeFormatter NO_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final String NO_SEQ_FORMAT = "%04d";

    private static final int NO_MAX_RETRY = 5;

    /**
     * 业务日期下限（B-3）。
     *
     * <p>用于拒绝「1970-01-01」这类由 {@code LocalDateTime} 被静默解析为 0（epoch）产生的脏日期。
     * 取 2000-01-01 而非「今天」这类滚动值：药店系统的历史单据补录是合理场景，
     * 但不可能有 2000 年之前的采购收货业务。
     */
    private static final LocalDate MIN_BUSINESS_DATE = LocalDate.of(2000, 1, 1);

    /**
     * 库管员及以上岗位中，允许无单收货的岗位：店长(1)
     */
    private static final Integer POSITION_MANAGER = 1;

    /**
     * 差异标记
     */
    private static final int DIFF_NONE = 0;
    private static final int DIFF_QTY = 1;
    private static final int DIFF_PRICE = 2;

    /**
     * 质检结果：0未检/1合格/2有异常
     */
    private static final int QUALITY_UNCHECKED = 0;
    private static final int QUALITY_PASS = 1;
    private static final int QUALITY_ABNORMAL = 2;

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    @Resource
    private PurchaseReceiptMapper receiptMapper;

    @Resource
    private PurchaseReceiptLineMapper receiptLineMapper;

    /**
     * 采购单号序列表：并发取号的原子分配入口
     */
    @Resource
    private PurchaseDocSeqService seqService;

    @Resource
    private PurchaseOrderService purchaseOrderService;

    @Resource
    private SupplierLicenseService supplierLicenseService;

    @Resource
    private StoreService storeService;

    @Resource
    private EmployeeService employeeService;

    @Resource
    private DrugApi drugApi;

    /**
     * C 的库存服务门面：未实现时抛 UnsupportedOperationException，本类转换为业务错误并回滚
     */
    @Resource
    private InventoryFacade inventoryFacade;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createReceipt(PurchaseReceiptSaveReqVO createReqVO) {
        storeService.validateStoreExistsAndOpen(createReqVO.getStoreId());
        EmployeeDO receiver = resolveReceiver();
        PurchaseOrderDO order = resolveOrder(createReqVO, receiver);
        // B-3：先校验收货时间并解析业务日期，避免 1970 等非法日期进库/进单号
        LocalDate businessDate = resolveBusinessDate(createReqVO.getReceiveDate());

        PurchaseReceiptDO receipt = new PurchaseReceiptDO();
        receipt.setOrderId(order == null ? null : order.getId());
        receipt.setStoreId(createReqVO.getStoreId());
        receipt.setWarehouseId(createReqVO.getWarehouseId());
        receipt.setReceiveBy(receiver.getId());
        receipt.setReceiveDate(createReqVO.getReceiveDate());
        receipt.setIsFreeReceipt(createReqVO.getIsFreeReceipt() == null ? 0 : createReqVO.getIsFreeReceipt());
        receipt.setStatus(PurchaseReceiptStatusEnum.DRAFT.getStatus());
        fillTotalsAndFlags(receipt, order, createReqVO.getLines(), null);

        for (int attempt = 0; attempt < NO_MAX_RETRY; attempt++) {
            receipt.setReceiptNo(generateReceiptNo(createReqVO.getStoreId(), businessDate));
            try {
                receiptMapper.insert(receipt);
                insertLines(receipt.getId(), createReqVO.getLines());
                return receipt.getId();
            } catch (DuplicateKeyException e) {
                // 并发下单号被抢占：清空主键后重算单号再试
                receipt.setId(null);
            }
        }
        throw exception(PURCHASE_RECEIPT_NO_DUPLICATE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateReceipt(PurchaseReceiptSaveReqVO updateReqVO) {
        PurchaseReceiptDO receipt = validateReceiptExists(updateReqVO.getId());
        if (!PurchaseReceiptStatusEnum.isDraft(receipt.getStatus())) {
            throw exception(PURCHASE_RECEIPT_STATUS_INVALID);
        }
        storeService.validateStoreExistsAndOpen(updateReqVO.getStoreId());
        EmployeeDO receiver = resolveReceiver();
        PurchaseOrderDO order = resolveOrder(updateReqVO, receiver);

        PurchaseReceiptDO update = new PurchaseReceiptDO();
        update.setId(receipt.getId());
        update.setOrderId(order == null ? null : order.getId());
        update.setStoreId(updateReqVO.getStoreId());
        update.setWarehouseId(updateReqVO.getWarehouseId());
        update.setReceiveBy(receiver.getId());
        update.setReceiveDate(updateReqVO.getReceiveDate());
        update.setIsFreeReceipt(updateReqVO.getIsFreeReceipt() == null ? 0 : updateReqVO.getIsFreeReceipt());
        fillTotalsAndFlags(update, order, updateReqVO.getLines(), receipt.getId());
        receiptMapper.updateById(update);

        // 明细整体重建：必须物理删除旧明细（uk_receipt_line(receipt_id, line_no) 不含 deleted 列，
        // 逻辑删除后重新插入相同行号会触发唯一键冲突，曾导致更新接口 500）
        receiptLineMapper.physicalDeleteByReceiptId(receipt.getId());
        insertLines(receipt.getId(), updateReqVO.getLines());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReceipt(Long id) {
        PurchaseReceiptDO receipt = validateReceiptExists(id);
        if (!PurchaseReceiptStatusEnum.isDraft(receipt.getStatus())
                && !PurchaseReceiptStatusEnum.isVoided(receipt.getStatus())) {
            throw exception(PURCHASE_RECEIPT_STATUS_INVALID);
        }
        receiptLineMapper.deleteByReceiptId(id);
        receiptMapper.deleteById(id);
    }

    @Override
    public PurchaseReceiptDO getReceipt(Long id) {
        return receiptMapper.selectById(id);
    }

    @Override
    public PageResult<PurchaseReceiptDO> getReceiptPage(PurchaseReceiptPageReqVO reqVO) {
        return receiptMapper.selectPage(reqVO);
    }

    @Override
    public List<PurchaseReceiptLineDO> getReceiptLines(Long receiptId) {
        if (receiptId == null) {
            return Collections.emptyList();
        }
        return receiptLineMapper.selectListByReceiptId(receiptId);
    }

    @Override
    public List<PurchaseReceiptLineDO> getReceiptLinesByReceiptIds(Collection<Long> receiptIds) {
        if (receiptIds == null || receiptIds.isEmpty()) {
            return Collections.emptyList();
        }
        return receiptLineMapper.selectListByReceiptIds(receiptIds);
    }

    @Override
    public PurchaseReceiptDO validateReceiptExists(Long id) {
        if (id == null) {
            throw exception(PURCHASE_RECEIPT_NOT_EXISTS);
        }
        PurchaseReceiptDO receipt = receiptMapper.selectById(id);
        if (receipt == null) {
            throw exception(PURCHASE_RECEIPT_NOT_EXISTS);
        }
        return receipt;
    }

    @Override
    public void submitReceipt(Long id) {
        PurchaseReceiptDO receipt = validateReceiptExists(id);
        if (!PurchaseReceiptStatusEnum.isDraft(receipt.getStatus())) {
            throw exception(PURCHASE_RECEIPT_STATUS_INVALID);
        }
        List<PurchaseReceiptLineDO> lines = receiptLineMapper.selectListByReceiptId(id);
        if (lines.isEmpty()) {
            throw exception(PURCHASE_RECEIPT_LINE_EMPTY);
        }
        PurchaseReceiptDO update = new PurchaseReceiptDO();
        update.setId(id);
        update.setStatus(PurchaseReceiptStatusEnum.SUBMITTED.getStatus());
        receiptMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void postReceipt(Long id) {
        PurchaseReceiptDO receipt = validateReceiptExists(id);
        if (PurchaseReceiptStatusEnum.isPosted(receipt.getStatus())) {
            throw exception(PURCHASE_RECEIPT_POST_DUP);
        }
        if (!PurchaseReceiptStatusEnum.isSubmitted(receipt.getStatus())) {
            throw exception(PURCHASE_RECEIPT_STATUS_INVALID);
        }

        // 有单收货：校验采购订单状态与供应商证照有效性（REG-012 / WH-002）
        PurchaseOrderDO order = null;
        if (receipt.getOrderId() != null) {
            order = purchaseOrderService.validateOrderExists(receipt.getOrderId());
            if (!PurchaseOrderStatusEnum.canReceive(order.getStatus())) {
                throw exception(PURCHASE_RECEIPT_ORDER_NOT_RECEIVABLE);
            }
            supplierLicenseService.validateOperateLicenseValid(order.getSupplierId());
        }

        List<PurchaseReceiptLineDO> lines = receiptLineMapper.selectListByReceiptId(id);
        if (lines.isEmpty()) {
            throw exception(PURCHASE_RECEIPT_LINE_EMPTY);
        }
        List<ReceiveItem> items = new ArrayList<>(lines.size());
        for (PurchaseReceiptLineDO line : lines) {
            if (line.getLocationId() == null) {
                throw exception(PURCHASE_RECEIPT_LOCATION_REQUIRED);
            }
            items.add(ReceiveItem.builder()
                    .bizLineId(String.valueOf(line.getId()))
                    .storeId(receipt.getStoreId())
                    .drugId(line.getDrugId())
                    .batchNo(line.getBatchNo())
                    .manufactureDate(line.getManufactureDate())
                    .expiryDate(line.getExpiryDate())
                    .qty(line.getQty())
                    .unitPrice(line.getUnitPrice())
                    .warehouseId(receipt.getWarehouseId())
                    .locationId(line.getLocationId())
                    .build());
        }

        // ① 状态 CAS：仅「已提交且未入账」的记录能置为已入账，重复提交影响 0 行
        PurchaseReceiptDO cas = new PurchaseReceiptDO();
        cas.setStatus(PurchaseReceiptStatusEnum.POSTED.getStatus());
        cas.setPostedAt(LocalDateTime.now());
        int updated = receiptMapper.update(cas, new LambdaQueryWrapperX<PurchaseReceiptDO>()
                .eq(PurchaseReceiptDO::getId, id)
                .eq(PurchaseReceiptDO::getStatus, PurchaseReceiptStatusEnum.SUBMITTED.getStatus())
                .isNull(PurchaseReceiptDO::getPostedAt));
        if (updated == 0) {
            throw exception(PURCHASE_RECEIPT_POST_DUP);
        }

        // ② 调用 C 的库存服务（同事务）；未实现时转为业务错误，整单回滚
        ReceiveResult result;
        try {
            result = inventoryFacade.receive(receipt.getStoreId(), receipt.getReceiptNo(), items);
        } catch (UnsupportedOperationException e) {
            throw exception(INV_SERVICE_UNAVAILABLE);
        }

        // ③ 回写库存服务生成的批次编号，便于后续追溯库存流水
        writeBackBatchIds(result);

        // ④ 累计采购订单已收数量并推进订单状态（部分到货/完成）
        if (order != null) {
            Map<Long, Integer> qtyByOrderLineId = new HashMap<>();
            for (PurchaseReceiptLineDO line : lines) {
                if (line.getOrderLineId() == null) {
                    continue;
                }
                qtyByOrderLineId.merge(line.getOrderLineId(), line.getQty(), Integer::sum);
            }
            purchaseOrderService.applyReceiptPosted(order.getId(), qtyByOrderLineId);
        }
    }

    @Override
    public void voidReceipt(Long id) {
        PurchaseReceiptDO receipt = validateReceiptExists(id);
        if (!PurchaseReceiptStatusEnum.isDraft(receipt.getStatus())) {
            // 已提交/已入账需要先撤回或做库存回补，当前不支持直接作废
            throw exception(PURCHASE_RECEIPT_STATUS_INVALID);
        }
        PurchaseReceiptDO update = new PurchaseReceiptDO();
        update.setId(id);
        update.setStatus(PurchaseReceiptStatusEnum.VOIDED.getStatus());
        receiptMapper.updateById(update);
    }

    // ==================== 私有方法 ====================

    /**
     * 解析收货人：当前登录 system 用户对应的药店员工
     */
    private EmployeeDO resolveReceiver() {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        EmployeeDO employee = employeeService.getEmployeeByUserId(loginUserId);
        if (employee == null) {
            throw exception(PURCHASE_RECEIPT_RECEIVER_NOT_EMPLOYEE);
        }
        return employee;
    }

    /**
     * 解析并校验关联采购订单：
     * 1）无单收货（orderId 为空）必须是「无单收货」标记 + 店长岗位（WH-002）；
     * 2）有单收货要求订单处于可收货状态。
     */
    private PurchaseOrderDO resolveOrder(PurchaseReceiptSaveReqVO reqVO, EmployeeDO receiver) {
        boolean free = reqVO.getOrderId() == null;
        if (free) {
            if (reqVO.getIsFreeReceipt() == null || reqVO.getIsFreeReceipt() != 1) {
                throw exception(PURCHASE_RECEIPT_ORDER_NOT_RECEIVABLE);
            }
            if (!Objects.equals(POSITION_MANAGER, receiver.getPosition())) {
                throw exception(PURCHASE_RECEIPT_FREE_NOT_MANAGER);
            }
            return null;
        }
        if (reqVO.getIsFreeReceipt() != null && reqVO.getIsFreeReceipt() == 1) {
            // 有订单却标记无单收货，属于参数矛盾
            throw exception(PURCHASE_RECEIPT_ORDER_NOT_RECEIVABLE);
        }
        PurchaseOrderDO order = purchaseOrderService.validateOrderExists(reqVO.getOrderId());
        if (!PurchaseOrderStatusEnum.canReceive(order.getStatus())) {
            throw exception(PURCHASE_RECEIPT_ORDER_NOT_RECEIVABLE);
        }
        return order;
    }

    /**
     * 校验明细并计算收货单头统计字段
     */
    private void fillTotalsAndFlags(PurchaseReceiptDO receipt, PurchaseOrderDO order,
                                    List<PurchaseReceiptLineSaveReqVO> lineVOs, Long excludeReceiptId) {
        if (lineVOs == null || lineVOs.isEmpty()) {
            throw exception(PURCHASE_RECEIPT_LINE_EMPTY);
        }
        LocalDate today = LocalDate.now();
        List<Long> drugIds = new ArrayList<>();
        int totalQty = 0;
        BigDecimal totalAmount = ZERO;
        boolean priceDiff = false;
        boolean qtyDiff = false;
        int qualityStatus = QUALITY_UNCHECKED;
        int checkedCount = 0;

        // 订单行索引：校验行归属、药品一致、剩余可收数量
        Map<Long, PurchaseOrderLineDO> orderLineMap = new HashMap<>();
        Map<Long, Integer> occupiedMap = new HashMap<>();
        if (order != null) {
            List<PurchaseOrderLineDO> orderLines = purchaseOrderService.getOrderLines(order.getId());
            for (PurchaseOrderLineDO orderLine : orderLines) {
                orderLineMap.put(orderLine.getId(), orderLine);
            }
            occupiedMap = loadOccupiedQty(order.getId(), excludeReceiptId);
        }

        for (PurchaseReceiptLineSaveReqVO line : lineVOs) {
            if (line.getBatchNo() == null || line.getBatchNo().trim().isEmpty()) {
                throw exception(PURCHASE_RECEIPT_BATCH_NO_REQUIRED);
            }
            if (line.getQty() == null || line.getQty() <= 0) {
                throw exception(PURCHASE_RECEIPT_QTY_EXCEED);
            }
            // 有效期：不能早于生产日期，且不能早于今天（过期药品不得入库）
            if (line.getExpiryDate() == null || line.getExpiryDate().isBefore(today)) {
                throw exception(PURCHASE_RECEIPT_EXPIRY_INVALID);
            }
            if (line.getManufactureDate() != null && line.getExpiryDate().isBefore(line.getManufactureDate())) {
                throw exception(PURCHASE_RECEIPT_EXPIRY_INVALID);
            }
            drugIds.add(line.getDrugId());

            if (order != null) {
                if (line.getOrderLineId() == null) {
                    throw exception(PURCHASE_RECEIPT_ORDER_LINE_MISMATCH);
                }
                PurchaseOrderLineDO orderLine = orderLineMap.get(line.getOrderLineId());
                if (orderLine == null) {
                    throw exception(PURCHASE_RECEIPT_ORDER_LINE_MISMATCH);
                }
                if (!Objects.equals(orderLine.getDrugId(), line.getDrugId())) {
                    throw exception(PURCHASE_RECEIPT_ORDER_LINE_MISMATCH);
                }
                // 剩余可收 = 订购数量 - 已入账数量(receivedQty) - 在途占用数量(未入账的草稿/已提交单)
                // 注意：occupied 已排除「已入账」单据，否则会与 receivedQty 重复计数（B-1 根因）
                int occupied = occupiedMap.getOrDefault(orderLine.getId(), 0);
                int received = orderLine.getReceivedQty() == null ? 0 : orderLine.getReceivedQty();
                if (line.getQty() + occupied + received > orderLine.getOrderQty()) {
                    throw exception(PURCHASE_RECEIPT_QTY_EXCEED);
                }
                if (line.getUnitPrice() != null && orderLine.getUnitPrice() != null
                        && line.getUnitPrice().compareTo(orderLine.getUnitPrice()) != 0) {
                    priceDiff = true;
                }
            }

            totalQty += line.getQty();
            totalAmount = totalAmount.add(BigDecimal.valueOf(line.getQty())
                    .multiply(line.getUnitPrice() == null ? ZERO : line.getUnitPrice())
                    .setScale(2, RoundingMode.HALF_UP));
            if (line.getQualityFlag() != null) {
                checkedCount++;
                if (line.getQualityFlag() == 2) {
                    qualityStatus = QUALITY_ABNORMAL;
                } else if (qualityStatus != QUALITY_ABNORMAL && line.getQualityFlag() == 1) {
                    qualityStatus = QUALITY_PASS;
                }
            }
        }

        // 差异标记：价格差异看单价，数量差异看本次实收总量与订单订购总量
        // 注意：分批发货时「本次实收 < 订单订购」属正常提醒，统一标记为数量差异，不用金额判断价格差异
        if (order != null) {
            int orderTotalQty = 0;
            for (PurchaseReceiptLineSaveReqVO line : lineVOs) {
                PurchaseOrderLineDO orderLine = orderLineMap.get(line.getOrderLineId());
                if (orderLine != null) {
                    orderTotalQty += orderLine.getOrderQty();
                }
            }
            qtyDiff = totalQty != orderTotalQty;
        }

        // 药品存在性校验（只读门面，不改药品数据）
        List<DrugRespDTO> drugs = drugApi.getDrugList(drugIds);
        if (drugs == null || drugs.size() < collectDistinctCount(drugIds)) {
            throw exception(PURCHASE_RECEIPT_DRUG_INVALID);
        }

        receipt.setTotalQty(totalQty);
        receipt.setTotalAmount(totalAmount);
        receipt.setDiffType(priceDiff ? DIFF_PRICE : (qtyDiff ? DIFF_QTY : DIFF_NONE));
        receipt.setQualityStatus(checkedCount == 0 ? QUALITY_UNCHECKED : qualityStatus);
    }

    /**
     * 汇总同一订单下「在途占用」的收货数量（按订单行）。
     *
     * <p>口径（B-1 修复）：剩余可收 = 订购数量 − 已入账数量 − 在途占用数量，三者的取值来源必须互不重叠。
     * <ul>
     *   <li><b>已入账（POSTED）不计入占用</b>：该批数量已由 {@code postReceipt} 通过
     *       {@code applyReceiptPosted} 累加进 {@code ph_po_order_line.received_qty}，
     *       若这里再统计一次就会<b>重复计数</b>，导致「已收 40 再收 60」被误判为
     *       60+40(占用)+40(received_qty)=140&gt;100 而报超量。这是 B-1 的根因。</li>
     *   <li><b>待提交（DRAFT）/已提交（SUBMITTED）计入占用</b>：属已录单但尚未入账的在途数量，
     *       防止同一订单被超量录单。</li>
     *   <li><b>已作废（VOIDED）不计入</b>：单据已失效，不再占用。</li>
     * </ul>
     *
     * @param excludeReceiptId 编辑场景下需排除的本单 id（其自身占用不应算作「他人占用」）
     */
    private Map<Long, Integer> loadOccupiedQty(Long orderId, Long excludeReceiptId) {
        List<PurchaseReceiptDO> receipts = receiptMapper.selectListByOrderId(orderId).stream()
                // 仅统计「在途」单据：排除已作废（失效）与已入账（已计入 received_qty，避免重复计数）
                .filter(item -> !PurchaseReceiptStatusEnum.isVoided(item.getStatus()))
                .filter(item -> !PurchaseReceiptStatusEnum.isPosted(item.getStatus()))
                .filter(item -> !Objects.equals(item.getId(), excludeReceiptId))
                .collect(Collectors.toList());
        if (receipts.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> receiptIds = receipts.stream().map(PurchaseReceiptDO::getId).collect(Collectors.toList());
        Map<Long, Integer> occupied = new HashMap<>();
        for (PurchaseReceiptLineDO line : receiptLineMapper.selectListByReceiptIds(receiptIds)) {
            if (line.getOrderLineId() == null) {
                continue;
            }
            occupied.merge(line.getOrderLineId(), line.getQty() == null ? 0 : line.getQty(), Integer::sum);
        }
        return occupied;
    }

    /**
     * 写入收货明细行，金额按「实收数量 × 采购单价」重算
     */
    private void insertLines(Long receiptId, List<PurchaseReceiptLineSaveReqVO> lineVOs) {
        int lineNo = 1;
        for (PurchaseReceiptLineSaveReqVO lineVO : lineVOs) {
            PurchaseReceiptLineDO line = new PurchaseReceiptLineDO();
            line.setReceiptId(receiptId);
            // 行号由服务端按顺序生成，忽略前端传值：避免前端重复行号触发 uk_receipt_line 唯一键冲突
            line.setLineNo(lineNo);
            line.setOrderLineId(lineVO.getOrderLineId());
            line.setDrugId(lineVO.getDrugId());
            line.setBatchNo(lineVO.getBatchNo());
            line.setManufactureDate(lineVO.getManufactureDate());
            line.setExpiryDate(lineVO.getExpiryDate());
            line.setQty(lineVO.getQty());
            line.setUnitPrice(lineVO.getUnitPrice());
            line.setAmount(BigDecimal.valueOf(lineVO.getQty())
                    .multiply(lineVO.getUnitPrice() == null ? ZERO : lineVO.getUnitPrice())
                    .setScale(2, RoundingMode.HALF_UP));
            line.setLocationId(lineVO.getLocationId());
            line.setQualityFlag(lineVO.getQualityFlag() == null ? 0 : lineVO.getQualityFlag());
            line.setQaRemark(lineVO.getQaRemark());
            line.setColdChainTemp(lineVO.getColdChainTemp());
            receiptLineMapper.insert(line);
            lineNo++;
        }
    }

    /**
     * 按行回写库存服务生成的批次编号
     */
    private void writeBackBatchIds(ReceiveResult result) {
        if (result == null || result.getLines() == null) {
            return;
        }
        for (ReceiveResult.ReceiveLineResult lineResult : result.getLines()) {
            if (lineResult.getBizLineId() == null || lineResult.getBatchId() == null) {
                continue;
            }
            long lineId;
            try {
                lineId = Long.parseLong(lineResult.getBizLineId());
            } catch (NumberFormatException e) {
                // 库存服务返回了无法识别的业务行标识，忽略该行但不影响整体入账结果
                continue;
            }
            PurchaseReceiptLineDO update = new PurchaseReceiptLineDO();
            update.setId(lineId);
            update.setCreateBatchId(lineResult.getBatchId());
            receiptLineMapper.updateById(update);
        }
    }

    /**
     * 解析并校验收货时间，返回用于生成单号的业务日期（B-3）。
     *
     * <p>背景：yudao 全局把 {@code LocalDateTime} 映射为**毫秒时间戳**
     * （{@code TimestampLocalDateTimeDeserializer} 使用 {@code getValueAsLong()}）。
     * 若前端传的是日期字符串，会被**静默解析为 0**，即 {@code 1970-01-01T00:00}，
     * 从而生成 {@code GR407-19700101-xxxx} 这类脏单号。历史上确实出现过该现象。
     *
     * <p>本方法做两层保护，明确拒绝而不是静默回退：
     * <ol>
     *   <li>为空（null）→ 直接报业务错误，绝不回退到 1970；</li>
     *   <li>落在合理业务区间之外（早于 {@link #MIN_BUSINESS_DATE} 或晚于「明天」）→ 报业务错误。
     *       「明天」容忍前端与服务端的时区/夏令时偏差，但不接受 1970 或明显错误的未来时间。</li>
     * </ol>
     *
     * @param receiveDate 前端提交的收货时间
     * @return 该时间对应的业务日期（服务器时区）
     */
    private LocalDate resolveBusinessDate(LocalDateTime receiveDate) {
        if (receiveDate == null) {
            throw exception(PURCHASE_RECEIPT_DATE_INVALID);
        }
        LocalDate date = receiveDate.toLocalDate();
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        if (date.isBefore(MIN_BUSINESS_DATE) || date.isAfter(tomorrow)) {
            throw exception(PURCHASE_RECEIPT_DATE_INVALID);
        }
        return date;
    }

    /**
     * 生成收货单号：GR-门店-yyyyMMdd-流水
     * <p>
     * 日期段取自<b>真实收货时间对应的业务日期</b>（由 {@link #resolveBusinessDate} 校验），
     * 因此不会出现 19700101；调用方必须已完成校验。
     * <p>
     * 流水号来自序列表 {@code ph_po_doc_seq} 的**原子分配**
     * （{@link PurchaseDocSeqMapper#allocateSeq}），不再用 {@code MAX(单号) + 1} 推算。
     * 原因：MySQL 默认 REPEATABLE-READ 下 MAX() 是不加锁的一致性读，
     * 同一事务内重试读到的快照恒定不变，5 次重试会算出同一个号；
     * 并发插入又争抢 uk_receipt_no 索引锁产生死锁（实测并发 5 张仅 1 张成功 + 3 个 500）。
     * 序列表方案把取号下沉为数据库原子自增，与隔离级别和重试无关。
     * <p>
     * 该原子分配天然覆盖「已被逻辑删除单据占号」的历史问题（D6）：
     * 序号由序列表推进，不再受 deleted 过滤影响。
     */
    private String generateReceiptNo(Long storeId, LocalDate businessDate) {
        String prefix = "GR" + storeId + "-" + businessDate.format(NO_DATE) + "-";
        long seq = seqService.allocate(storeId, businessDate.format(NO_DATE),
                PurchaseDocSeqTypeEnum.RECEIPT.getType());
        return prefix + String.format(NO_SEQ_FORMAT, seq);
    }

    private int collectDistinctCount(List<Long> ids) {
        return (int) ids.stream().filter(Objects::nonNull).distinct().count();
    }

}
