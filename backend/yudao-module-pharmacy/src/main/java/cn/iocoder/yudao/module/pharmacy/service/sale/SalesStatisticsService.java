package cn.iocoder.yudao.module.pharmacy.service.sale;

import cn.iocoder.yudao.module.pharmacy.dal.mysql.sale.SaleOrderMapper.DailySaleRow;

import java.time.LocalDateTime;
import java.util.List;

public interface SalesStatisticsService {

    /** 按日统计：门店内完成/部分退销售单的笔数、销售额、成本 */
    List<DailySaleRow> getDailyStatistics(Long storeId, LocalDateTime startTime, LocalDateTime endTime);
}
