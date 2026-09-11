package cn.iocoder.yudao.module.pharmacy.service.purchase;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.pharmacy.api.DrugApi;
import cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.order.PurchaseOrderLineSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.order.PurchaseOrderPageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.purchase.vo.order.PurchaseOrderSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.base.EmployeeDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.purchase.PurchaseOrderDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.purchase.PurchaseOrderLineDO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.purchase.PurchaseOrderLineMapper;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.purchase.PurchaseOrderMapper;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.purchase.PurchaseReceiptMapper;
import cn.iocoder.yudao.module.pharmacy.enums.PurchaseOrderStatusEnum;
import cn.iocoder.yudao.module.pharmacy.service.base.EmployeeService;
import cn.iocoder.yudao.module.pharmacy.service.base.StoreService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.*;

/**
 * 采购订单 Service 实现类
 *
 * @author B 成员
 */
@Service
@Validated
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    /**
     * 单号日期段格式
     */
    private static final DateTimeFormatter NO_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 单号流水号位数
     */
    private static final String NO_SEQ_FORMAT = "%04d";

    /**
     * 单号生成最大重试次数（并发下唯一键冲突后重新取号）
     */
    private static final int NO_MAX_RETRY = 5;

    private static final BigDecimal ONE = BigDecimal.ONE;

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    @Resource
    private PurchaseOrderMapper orderMapper;

    @Resource
    private PurchaseOrderLineMapper orderLineMapper;

    @Resource
    private PurchaseReceiptMapper receiptMapper;

    @Resource
    private StoreService storeService;

    @Resource
    private SupplierService supplierService;

    @Resource
    private EmployeeService employeeService;

    /**
     * 药品只读门面：校验药品存在且可采购（已审核通过 + 启用）
     */
    @Resource
    private DrugApi drugApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(PurchaseOrderSaveReqVO createReqVO) {
        validateStore(createReqVO.getStoreId());
        // 供应商必须启用且首营审核通过
        supplierService.validateSupplierPurchasable(createReqVO.getSupplierId());
        validateLines(createReqVO.getLines());

        PurchaseOrderDO order = new PurchaseOrderDO();
        order.setStoreId(createReqVO.getStoreId());
        order.setWarehouseId(createReqVO.getWarehouseId());
        order.setSupplierId(createReqVO.getSupplierId());
        order.setOrderDate(createReqVO.getOrderDate());
        order.setExpectDate(createReqVO.getExpectDate());
        order.setIsAuto(createReqVO.getIsAuto() == null ? 0 : createReqVO.getIsAuto());
        order.setRemark(createReqVO.getRemark());
        order.setStatus(PurchaseOrderStatusEnum.DRAFT.getStatus());
        applyAmounts(order, createReqVO.getLines());

        // 单号：PO-门店-yyyyMMdd-流水；并发冲突时重新取号
        for (int attempt = 0; attempt < NO_MAX_RETRY; attempt++) {
            order.setOrderNo(generateOrderNo(createReqVO.getStoreId(), createReqVO.getOrderDate()));
            try {
                orderMapper.insert(order);
                insertLines(order.getId(), createReqVO.getLines());
                return order.getId();
            } catch (DuplicateKeyException e) {
                // 单号被并发占用，重新取号
                order.setId(null);
            }
        }
        throw exception(PURCHASE_ORDER_NO_DUPLICATE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrder(PurchaseOrderSaveReqVO updateReqVO) {
        PurchaseOrderDO order = validateOrderExists(updateReqVO.getId());
        if (!PurchaseOrderStatusEnum.isDraft(order.getStatus())) {
            throw exception(PURCHASE_ORDER_STATUS_INVALID);
        }
        validateStore(updateReqVO.getStoreId());
        supplierService.validateSupplierPurchasable(updateReqVO.getSupplierId());
        validateLines(updateReqVO.getLines());

        PurchaseOrderDO update = new PurchaseOrderDO();
        update.setId(order.getId());
        update.setStoreId(updateReqVO.getStoreId());
        update.setWarehouseId(updateReqVO.getWarehouseId());
        update.setSupplierId(updateReqVO.getSupplierId());
        update.setOrderDate(updateReqVO.getOrderDate());
        update.setExpectDate(updateReqVO.getExpectDate());
        update.setIsAuto(updateReqVO.getIsAuto() == null ? order.getIsAuto() : updateReqVO.getIsAuto());
        update.setRemark(updateReqVO.getRemark());
        applyAmounts(update, updateReqVO.getLines());
        orderMapper.updateById(update);

        // 明细整体重建：草稿阶段允许增删行。
        // 必须物理删除旧明细：uk_po_line(order_id, line_no) 不含 deleted 列，
        // 逻辑删除后重新插入相同行号会触发唯一键冲突（曾经导致更新接口 500）。
        orderLineMapper.physicalDeleteByOrderId(order.getId());
        insertLines(order.getId(), updateReqVO.getLines());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrder(Long id) {
        PurchaseOrderDO order = validateOrderExists(id);
        if (!PurchaseOrderStatusEnum.isDraft(order.getStatus())) {
            throw exception(PURCHASE_ORDER_STATUS_INVALID);
        }
        if (hasValidReceipt(id)) {
            throw exception(PURCHASE_ORDER_HAS_RECEIPT);
        }
        orderLineMapper.deleteByOrderId(id);
        orderMapper.deleteById(id);
    }

    @Override
    public PurchaseOrderDO getOrder(Long id) {
        return orderMapper.selectById(id);
    }

    @Override
    public List<PurchaseOrderDO> getOrderList(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return orderMapper.selectListByIds(ids);
    }

    @Override
    public PageResult<PurchaseOrderDO> getOrderPage(PurchaseOrderPageReqVO reqVO) {
        return orderMapper.selectPage(reqVO);
    }

    @Override
    public List<PurchaseOrderLineDO> getOrderLines(Long orderId) {
        if (orderId == null) {
            return Collections.emptyList();
        }
        return orderLineMapper.selectListByOrderId(orderId);
    }

    @Override
    public List<PurchaseOrderLineDO> getOrderLinesByOrderIds(Collection<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Collections.emptyList();
        }
        return orderLineMapper.selectListByOrderIds(orderIds);
    }

    @Override
    public PurchaseOrderDO validateOrderExists(Long id) {
        if (id == null) {
            throw exception(PURCHASE_ORDER_NOT_EXISTS);
        }
        PurchaseOrderDO order = orderMapper.selectById(id);
        if (order == null) {
            throw exception(PURCHASE_ORDER_NOT_EXISTS);
        }
        return order;
    }

    @Override
    public void submitOrder(Long id) {
        PurchaseOrderDO order = validateOrderExists(id);
        if (!PurchaseOrderStatusEnum.isDraft(order.getStatus())) {
            throw exception(PURCHASE_ORDER_STATUS_INVALID);
        }
        PurchaseOrderDO update = new PurchaseOrderDO();
        update.setId(id);
        update.setStatus(PurchaseOrderStatusEnum.SUBMITTED.getStatus());
        orderMapper.updateById(update);
    }

    @Override
    public void approveOrder(Long id) {
        PurchaseOrderDO order = validateOrderExists(id);
        if (PurchaseOrderStatusEnum.isApproved(order.getStatus())) {
            throw exception(PURCHASE_ORDER_APPROVE_DUP);
        }
        if (!PurchaseOrderStatusEnum.isSubmitted(order.getStatus())) {
            throw exception(PURCHASE_ORDER_STATUS_INVALID);
        }
        // 审批人：当前登录 system 用户对应的药店员工
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        EmployeeDO employee = employeeService.getEmployeeByUserId(loginUserId);
        if (employee == null) {
            throw exception(PURCHASE_ORDER_AUDITOR_NOT_EMPLOYEE);
        }
        PurchaseOrderDO update = new PurchaseOrderDO();
        update.setId(id);
        update.setStatus(PurchaseOrderStatusEnum.APPROVED.getStatus());
        update.setAuditBy(employee.getId());
        update.setAuditAt(LocalDateTime.now());
        orderMapper.updateById(update);
    }

    @Override
    public void issueOrder(Long id) {
        PurchaseOrderDO order = validateOrderExists(id);
        if (!PurchaseOrderStatusEnum.isApproved(order.getStatus())) {
            throw exception(PURCHASE_ORDER_STATUS_INVALID);
        }
        PurchaseOrderDO update = new PurchaseOrderDO();
        update.setId(id);
        update.setStatus(PurchaseOrderStatusEnum.ISSUED.getStatus());
        orderMapper.updateById(update);
    }

    @Override
    public void cancelOrder(Long id) {
        PurchaseOrderDO order = validateOrderExists(id);
        if (!PurchaseOrderStatusEnum.canCancel(order.getStatus())) {
            throw exception(PURCHASE_ORDER_STATUS_INVALID);
        }
        if (hasValidReceipt(id)) {
            throw exception(PURCHASE_ORDER_HAS_RECEIPT);
        }
        PurchaseOrderDO update = new PurchaseOrderDO();
        update.setId(id);
        update.setStatus(PurchaseOrderStatusEnum.CANCEL.getStatus());
        orderMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyReceiptPosted(Long orderId, Map<Long, Integer> qtyByOrderLineId) {
        if (orderId == null || qtyByOrderLineId == null || qtyByOrderLineId.isEmpty()) {
            return;
        }
        PurchaseOrderDO order = validateOrderExists(orderId);
        List<PurchaseOrderLineDO> orderLines = orderLineMapper.selectListByOrderId(orderId);
        // 只允许累加本订单的行，防止越权改到别的订单
        Set<Long> ownLineIds = orderLines.stream()
                .map(PurchaseOrderLineDO::getId)
                .collect(Collectors.toSet());

        // 原子累加 + 上限校验（避免「读-改-写」在并发入账时丢更新）
        for (Map.Entry<Long, Integer> entry : qtyByOrderLineId.entrySet()) {
            Long lineId = entry.getKey();
            Integer delta = entry.getValue();
            if (lineId == null || !ownLineIds.contains(lineId)) {
                throw exception(PURCHASE_RECEIPT_ORDER_LINE_MISMATCH);
            }
            if (delta == null || delta <= 0) {
                throw exception(PURCHASE_RECEIPT_QTY_EXCEED);
            }
            int rows = orderLineMapper.update(null, new LambdaUpdateWrapper<PurchaseOrderLineDO>()
                    .setSql("received_qty = received_qty + " + delta)
                    .eq(PurchaseOrderLineDO::getId, lineId)
                    .apply("received_qty + {0} <= order_qty", delta));
            if (rows == 0) {
                // 条件不满足（超过订购数量）或记录不存在
                throw exception(PURCHASE_RECEIPT_QTY_EXCEED);
            }
        }

        // 累加完成后重新读取，判定「部分到货 / 完成」
        boolean allFinished = true;
        boolean anyReceived = false;
        for (PurchaseOrderLineDO line : orderLineMapper.selectListByOrderId(orderId)) {
            int received = line.getReceivedQty() == null ? 0 : line.getReceivedQty();
            if (received > 0) {
                anyReceived = true;
            }
            if (received < line.getOrderQty()) {
                allFinished = false;
            }
        }

        PurchaseOrderDO update = new PurchaseOrderDO();
        update.setId(order.getId());
        if (allFinished && anyReceived) {
            update.setStatus(PurchaseOrderStatusEnum.FINISHED.getStatus());
        } else if (anyReceived) {
            update.setStatus(PurchaseOrderStatusEnum.PARTIAL_RECEIVED.getStatus());
        } else {
            return;
        }
        orderMapper.updateById(update);
    }

    // ==================== 私有方法 ====================

    private void validateStore(Long storeId) {
        storeService.validateStoreExistsAndOpen(storeId);
    }

    /**
     * 校验明细：数量、折扣率、药品有效性，并保证不出现空明细
     */
    private void validateLines(List<PurchaseOrderLineSaveReqVO> lines) {
        if (lines == null || lines.isEmpty()) {
            throw exception(PURCHASE_ORDER_LINE_EMPTY);
        }
        List<Long> drugIds = new ArrayList<>();
        for (PurchaseOrderLineSaveReqVO line : lines) {
            if (line.getOrderQty() == null || line.getOrderQty() <= 0) {
                throw exception(PURCHASE_ORDER_QTY_INVALID);
            }
            if (line.getDiscountRate() != null
                    && (line.getDiscountRate().compareTo(ZERO) < 0 || line.getDiscountRate().compareTo(ONE) > 0)) {
                throw exception(PURCHASE_ORDER_DISCOUNT_INVALID);
            }
            drugIds.add(line.getDrugId());
        }
        // 药品只读门面校验：不存在 / 已停用 / 未审核通过都会抛业务异常。
        // 必须先按药品去重：DrugService#validateDrugList 用 drugs.size() != ids.size() 判断缺失，
        // 同一药品出现在多行（同药不同批次/价格）时不去重会被误判为"药品不存在"。
        List<Long> distinctDrugIds = drugIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        drugApi.validateDrugList(distinctDrugIds);
    }

    /**
     * 服务端重算订单金额与总数量
     */
    private void applyAmounts(PurchaseOrderDO order, List<PurchaseOrderLineSaveReqVO> lines) {
        BigDecimal totalAmount = ZERO;
        BigDecimal discountAmount = ZERO;
        BigDecimal payableAmount = ZERO;
        int totalQty = 0;
        for (PurchaseOrderLineSaveReqVO line : lines) {
            BigDecimal rate = line.getDiscountRate() == null ? ONE : line.getDiscountRate();
            BigDecimal qty = BigDecimal.valueOf(line.getOrderQty());
            BigDecimal gross = qty.multiply(line.getUnitPrice()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal net = qty.multiply(line.getUnitPrice()).multiply(rate).setScale(2, RoundingMode.HALF_UP);
            totalAmount = totalAmount.add(gross);
            discountAmount = discountAmount.add(gross.subtract(net));
            payableAmount = payableAmount.add(net);
            totalQty += line.getOrderQty();
        }
        order.setTotalQty(totalQty);
        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(discountAmount);
        order.setPayableAmount(payableAmount);
        if (payableAmount.compareTo(ZERO) < 0 || totalAmount.compareTo(ZERO) < 0) {
            throw exception(PURCHASE_ORDER_AMOUNT_INVALID);
        }
    }

    /**
     * 写入明细行，行金额按「数量 × 单价 × 折扣率」重算
     */
    private void insertLines(Long orderId, List<PurchaseOrderLineSaveReqVO> lines) {
        int lineNo = 1;
        for (PurchaseOrderLineSaveReqVO lineVO : lines) {
            PurchaseOrderLineDO line = BeanUtils.toBean(lineVO, PurchaseOrderLineDO.class);
            line.setId(null);
            line.setOrderId(orderId);
            // 行号由服务端按顺序生成，忽略前端传值：避免前端重复行号触发 uk_po_line 唯一键冲突
            line.setLineNo(lineNo);
            line.setReceivedQty(0);
            BigDecimal rate = lineVO.getDiscountRate() == null ? ONE : lineVO.getDiscountRate();
            line.setDiscountRate(rate);
            line.setLineAmount(BigDecimal.valueOf(lineVO.getOrderQty())
                    .multiply(lineVO.getUnitPrice())
                    .multiply(rate)
                    .setScale(2, RoundingMode.HALF_UP));
            orderLineMapper.insert(line);
            lineNo++;
        }
    }

    /**
     * 生成订单号：PO-门店-yyyyMMdd-流水
     * <p>
     * 必须取「前缀内已有的最大流水 + 1」，不能用 count + 1：uk_order_no 唯一键不包含 deleted 列，
     * 而 MyBatis-Plus 的 selectCount 会自动过滤逻辑删除行，两者口径不一致会生成已被占用的单号。
     */
    private String generateOrderNo(Long storeId, LocalDate orderDate) {
        String prefix = "PO" + storeId + "-" + orderDate.format(NO_DATE) + "-";
        String maxNo = orderMapper.selectMaxOrderNo(prefix);
        long seq = 1L;
        if (maxNo != null) {
            seq = Long.parseLong(maxNo.substring(prefix.length())) + 1;
        }
        return prefix + String.format(NO_SEQ_FORMAT, seq);
    }

    private boolean hasValidReceipt(Long orderId) {
        Long receiptCount = receiptMapper.countValidByOrderId(orderId);
        return receiptCount != null && receiptCount > 0;
    }

}
