package cn.iocoder.yudao.module.pharmacy.controller.admin.pos;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.sale.SaleOrderMapper.DailySaleRow;
import cn.iocoder.yudao.module.pharmacy.service.sale.SalesStatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - POS 销售统计")
@RestController
@RequestMapping("/pharmacy/pos/statistics")
@Validated
public class SalesStatisticsController {

    @Resource
    private SalesStatisticsService salesStatisticsService;

    @GetMapping("/daily")
    @Operation(summary = "按日统计销售（笔数/销售额/成本）")
    @PreAuthorize("@ss.hasPermission('pharmacy:pos-statistics:query')")
    public CommonResult<List<DailySaleRow>> getDailyStatistics(
            @RequestParam("storeId") Long storeId,
            @RequestParam("startTime") @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND) LocalDateTime startTime,
            @RequestParam("endTime") @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND) LocalDateTime endTime) {
        return success(salesStatisticsService.getDailyStatistics(storeId, startTime, endTime));
    }

}
