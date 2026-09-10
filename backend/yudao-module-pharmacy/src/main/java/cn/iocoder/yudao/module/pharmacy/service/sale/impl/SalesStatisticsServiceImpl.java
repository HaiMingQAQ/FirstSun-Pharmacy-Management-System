package cn.iocoder.yudao.module.pharmacy.service.sale.impl;

import cn.iocoder.yudao.module.pharmacy.dal.mysql.sale.SaleOrderMapper;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.sale.SaleOrderMapper.DailySaleRow;
import cn.iocoder.yudao.module.pharmacy.service.sale.SalesStatisticsService;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SalesStatisticsServiceImpl implements SalesStatisticsService {

    @Resource
    private SaleOrderMapper saleOrderMapper;

    @Override
    public List<DailySaleRow> getDailyStatistics(Long storeId, LocalDateTime startTime, LocalDateTime endTime) {
        return saleOrderMapper.selectDailySummary(storeId, startTime, endTime);
    }
}
