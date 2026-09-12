package cn.iocoder.yudao.module.pharmacy.controller.admin.inventory.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.validation.constraints.AssertTrue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Read-only filters. Tenant and operator are deliberately absent. */
@Data
@EqualsAndHashCode(callSuper = true)
public class InventoryReadQuery extends PageParam {
    @Positive private Long storeId;
    @Positive private Long warehouseId;
    @Positive private Long batchId;
    @Positive private Long locationId;
    @Positive private Long drugId;
    @Size(max = 64) private String code;
    @Size(max = 64) private String name;
    @Size(max = 64) private String batchNo;
    @Size(max = 32) private String bizNo;
    @Min(0) @Max(1) private Integer status;
    @Positive private Long flowId;
    @Min(0) @Max(2) private Integer qualityStatus;
    @Min(0) @Max(99) private Integer flowType;
    @Min(0) @Max(99) private Integer bizType;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate expiryFrom;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate expiryTo;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) private LocalDateTime flowFrom;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) private LocalDateTime flowBefore;

    @JsonIgnore
    @AssertTrue(message = "效期起始日期不能晚于结束日期")
    public boolean isExpiryRangeValid() {
        return expiryFrom == null || expiryTo == null || !expiryFrom.isAfter(expiryTo);
    }

    @JsonIgnore
    @AssertTrue(message = "流水起始时间必须早于截止时间")
    public boolean isFlowRangeValid() {
        return flowFrom == null || flowBefore == null || flowFrom.isBefore(flowBefore);
    }

    public long getOffset() {
        return ((long) getPageNo() - 1) * getPageSize();
    }
}
