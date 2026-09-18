package cn.iocoder.yudao.module.ai.dal.dataobject.pharmacy;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@TableName("ph_ai_command")
@KeySequence("ph_ai_command_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class PharmacyAiCommandDO extends TenantBaseDO {
    @TableId
    private Long id;
    private Long conversationId;
    private String clientMessageId;
    private Long userId;
    private Long employeeId;
    private Long storeId;
    private String toolName;
    private String permission;
    private String requestJson;
    private String beforeJson;
    private String afterJson;
    private String requestHash;
    private String tokenHash;
    private String status;
    private LocalDateTime expiresAt;
    private LocalDateTime executedAt;
    private String resultJson;
    private String errorMessage;
}
