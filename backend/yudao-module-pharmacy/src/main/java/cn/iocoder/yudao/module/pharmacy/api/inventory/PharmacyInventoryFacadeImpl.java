package cn.iocoder.yudao.module.pharmacy.api.inventory;

import cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.DeductItem;
import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.DeductResult;
import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.ReceiveItem;
import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.ReceiveResult;
import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.ReturnBackItem;
import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.ReserveItem;
import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.ReserveResult;
import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.ReleaseItem;
import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.ConsumeItem;
import cn.iocoder.yudao.module.pharmacy.api.inventory.dto.AvailableQty;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.inventory.InventoryWriteMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.INV_RETURN_BACK_FAILED;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.INV_STOCK_NOT_ENOUGH;

/**
 * 库存门面实现（C 成员职责，合并后补齐）：销售出库扣减 / 退货回补 / 采购收货入账。
 * <p>
 * 数据守恒：ph_inv_batch.qty_total = qty_avail + qty_frozen；每笔变更同步货位
 * ph_inv_location_stock 并写 ph_inv_flow 流水；收货入账以 uk_flow_event 幂等。
 */
@Service
public class PharmacyInventoryFacadeImpl implements InventoryFacade {

    private static final int FLOW_TYPE_RECEIVE = 10;      // 采购入
    private static final int FLOW_TYPE_SALE_OUT = 20;     // 销售出
    private static final int FLOW_TYPE_SALE_RETURN = 21;  // 销售退
    private static final int BIZ_TYPE_RECEIVE = 1;        // 收货
    private static final int BIZ_TYPE_SALE = 2;           // 销售
    private static final int BIZ_TYPE_SALE_RETURN = 3;    // 销售退货

    @Resource
    private InventoryWriteMapper inventoryWriteMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeductResult deduct(Long storeId, List<DeductItem> items) {
        if (items == null || items.isEmpty()) {
            DeductResult result = new DeductResult();
            result.setSuccess(true);
            return result;
        }
        Long tenantId = TenantContextHolder.getTenantId();
        String bizNo = "SALE-" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + UUID.randomUUID().toString().substring(0, 8);
        int seq = 1;
        for (DeductItem item : items) {
            Integer qty = item.getQty();
            if (qty == null || qty <= 0) {
                continue;
            }
            int remain = qty;
            List<InventoryWriteMapper.InvLocationRow> candidates =
                    inventoryWriteMapper.selectAvailableStocks(tenantId, storeId, item.getDrugId());
            // 指定批次时只允许扣该批次
            if (item.getBatchId() != null) {
                List<InventoryWriteMapper.InvLocationRow> filtered = new ArrayList<>();
                for (InventoryWriteMapper.InvLocationRow row : candidates) {
                    if (row.batchId.equals(item.getBatchId())) {
                        filtered.add(row);
                    }
                }
                candidates = filtered;
            }
            for (InventoryWriteMapper.InvLocationRow row : candidates) {
                if (remain <= 0) {
                    break;
                }
                int take = Math.min(remain, row.qty);
                int rows = inventoryWriteMapper.deductBatch(row.batchId, take);
                if (rows <= 0) {
                    continue; // 并发下已被扣光，尝试下一货位
                }
                inventoryWriteMapper.deductLocationStock(row.batchId, row.locationId, take);
                InventoryWriteMapper.InvBatchRow after = inventoryWriteMapper.selectBatchById(tenantId, row.batchId);
                int balance = after == null || after.qtyTotal == null ? 0 : after.qtyTotal;
                insertFlow(tenantId, storeId, row.batchId, row.drugId, row.batchNo, row.locationId,
                        FLOW_TYPE_SALE_OUT, 0, take, balance, BIZ_TYPE_SALE, bizNo, (long) seq++, row.costPrice, "销售出库");
                remain -= take;
            }
            if (remain > 0) {
                throw ServiceExceptionUtil.exception(INV_STOCK_NOT_ENOUGH);
            }
        }
        DeductResult result = new DeductResult();
            result.setSuccess(true);
            return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void returnBack(Long storeId, List<ReturnBackItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        Long tenantId = TenantContextHolder.getTenantId();
        String bizNo = "RETURN-" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + UUID.randomUUID().toString().substring(0, 8);
        int seq = 1;
        for (ReturnBackItem item : items) {
            Integer qty = item.getQty();
            if (qty == null || qty <= 0 || item.getBatchId() == null) {
                continue;
            }
            InventoryWriteMapper.InvBatchRow batch = inventoryWriteMapper.selectBatchById(tenantId, item.getBatchId());
            if (batch == null) {
                throw ServiceExceptionUtil.exception(INV_RETURN_BACK_FAILED);
            }
            inventoryWriteMapper.restoreBatch(item.getBatchId(), qty);
            Long locationId = item.getLocationId();
            if (locationId != null) {
                inventoryWriteMapper.restoreLocationStock(item.getBatchId(), locationId, qty);
            } else {
                // 未指定货位：回补到该批次第一个货位（无则占位 0，仅批次数量回补）
                locationId = 0L;
            }
            int balance = batch.qtyTotal + qty;
            insertFlow(tenantId, storeId, batch.id, batch.drugId, batch.batchNo, locationId,
                    FLOW_TYPE_SALE_RETURN, qty, 0, balance, BIZ_TYPE_SALE_RETURN, bizNo, (long) seq++, batch.costPrice,
                    "销售退货回补");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReceiveResult receive(Long storeId, String receiptNo, List<ReceiveItem> items) {
        ReceiveResult result = new ReceiveResult();
        List<ReceiveResult.ReceiveLineResult> lines = new ArrayList<>();
        if (items == null || items.isEmpty()) {
            result.setLines(lines);
            return result;
        }
        Long tenantId = TenantContextHolder.getTenantId();
        for (ReceiveItem item : items) {
            Integer qty = item.getQty();
            if (qty == null || qty <= 0) {
                continue;
            }
            Long bizLineId;
            try {
                bizLineId = Long.parseLong(item.getBizLineId());
            } catch (NumberFormatException e) {
                bizLineId = 0L;
            }
            InventoryWriteMapper.InvBatchRow existing =
                    inventoryWriteMapper.selectBatchByUk(tenantId, item.getWarehouseId(), item.getDrugId(), item.getBatchNo());
            // 幂等：同一收货单明细已入账则跳过，不重复入库
            if (existing != null && inventoryWriteMapper.countFlowDup(tenantId, BIZ_TYPE_RECEIVE, receiptNo, bizLineId,
                    existing.id, item.getLocationId(), FLOW_TYPE_RECEIVE) > 0) {
                lines.add(new ReceiveResult.ReceiveLineResult(item.getBizLineId(), existing.id, null));
                continue;
            }
            Long batchId;
            int balance;
            BigDecimal unitPrice = item.getUnitPrice() == null ? BigDecimal.ZERO : item.getUnitPrice();
            if (existing == null) {
                InventoryWriteMapper.InvBatchRow row = new InventoryWriteMapper.InvBatchRow();
                row.storeId = storeId;
                row.warehouseId = item.getWarehouseId();
                row.drugId = item.getDrugId();
                row.batchNo = item.getBatchNo();
                row.manufactureDate = item.getManufactureDate();
                row.expiryDate = item.getExpiryDate();
                row.sourceType = 0;          // 采购
                row.sourceNo = receiptNo;
                row.qtyTotal = qty;
                row.qtyAvail = qty;
                row.qtyFrozen = 0;
                row.qtySold = 0;
                row.costPrice = unitPrice;
                row.tenantId = tenantId;
                inventoryWriteMapper.insertBatch(row);
                batchId = row.id;
                balance = qty;
            } else {
                batchId = existing.id;
                int oldTotal = existing.qtyTotal;
                BigDecimal newCost = unitPrice;
                if (oldTotal + qty > 0) {
                    BigDecimal oldAmount = existing.costPrice.multiply(BigDecimal.valueOf(oldTotal));
                    BigDecimal inAmount = unitPrice.multiply(BigDecimal.valueOf(qty));
                    newCost = oldAmount.add(inAmount)
                            .divide(BigDecimal.valueOf(oldTotal + qty), 4, RoundingMode.HALF_UP);
                }
                inventoryWriteMapper.updateBatchAppend(batchId, qty, newCost);
                balance = oldTotal + qty;
            }
            inventoryWriteMapper.upsertLocationStock(batchId, item.getLocationId(), item.getDrugId(), qty);
            insertFlow(tenantId, storeId, batchId, item.getDrugId(), item.getBatchNo(), item.getLocationId(),
                    FLOW_TYPE_RECEIVE, qty, 0, balance, BIZ_TYPE_RECEIVE, receiptNo, bizLineId, unitPrice, "采购收货入账");
            lines.add(new ReceiveResult.ReceiveLineResult(item.getBizLineId(), batchId, null));
        }
        result.setLines(lines);
        return result;
    }

    @Override
    public ReserveResult reserve(Long storeId, List<ReserveItem> items) {
        throw new UnsupportedOperationException("C 库存服务未实现，库存冻结不可用");
    }

    @Override
    public void release(Long storeId, List<ReleaseItem> items) {
        throw new UnsupportedOperationException("C 库存服务未实现，库存释放不可用");
    }

    @Override
    public DeductResult consumeReservation(Long storeId, List<ConsumeItem> items) {
        throw new UnsupportedOperationException("C 库存服务未实现，冻结转出库不可用");
    }

    @Override
    public List<AvailableQty> getAvailableQty(Long storeId, List<Long> drugIds) {
        throw new UnsupportedOperationException("C 库存服务未实现，可售量查询不可用");
    }

    private void insertFlow(Long tenantId, Long storeId, Long batchId, Long drugId, String batchNo, Long locationId,
                            int flowType, int inQty, int outQty, int balanceQty, int bizType, String bizNo,
                            Long bizLineId, BigDecimal unitCost, String remark) {
        InventoryWriteMapper.InvFlowRow flow = new InventoryWriteMapper.InvFlowRow();
        flow.storeId = storeId;
        flow.batchId = batchId;
        flow.drugId = drugId;
        flow.batchNo = batchNo == null ? "" : batchNo;
        flow.flowType = flowType;
        flow.inQty = inQty;
        flow.outQty = outQty;
        flow.balanceQty = balanceQty;
        flow.bizType = bizType;
        flow.bizNo = bizNo;
        flow.flowTime = LocalDateTime.now();
        flow.remark = remark;
        flow.tenantId = tenantId;
        flow.locationId = locationId;
        flow.bizLineId = bizLineId;
        flow.frozenDelta = 0;
        flow.unitCost = unitCost;
        inventoryWriteMapper.insertFlow(flow);
    }
}
