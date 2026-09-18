package cn.iocoder.yudao.module.ai.service.pharmacy;

import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService;
import cn.iocoder.yudao.module.ai.dal.dataobject.pharmacy.PharmacyAiCommandDO;
import cn.iocoder.yudao.module.ai.dal.mysql.pharmacy.PharmacyAiCommandMapper;
import cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.drug.DrugSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.base.DrugDO;
import cn.iocoder.yudao.module.pharmacy.service.base.DrugService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PharmacyAiCommandService {
    private static final Set<String> WRITABLE = Set.of("drugCode", "genericName", "tradeName", "spellCode",
            "categoryId", "drugType", "dosageForm", "specification", "unit", "manufacturer", "approvalNo",
            "conversionRatio", "retailPrice", "memberPrice", "costPrice", "minSalePrice", "taxRate", "isRx",
            "isSpecial", "isPseudoephedrine", "isColdChain", "insuranceType", "storageCond", "needExpiry",
            "defaultLocationId", "minStock", "maxStock", "saleableOnline", "status", "remark", "imageUrl",
            "images", "description", "instructionsUrl");
    private static final Set<String> FORBIDDEN = Set.of("approveStatus", "auditBy", "auditAt", "auditOpinion",
            "tenantId", "deleted", "creator", "createTime", "updater", "updateTime");
    private static final Set<String> BOOLEAN_AS_INTEGER = Set.of("isRx", "isSpecial", "isPseudoephedrine",
            "isColdChain", "needExpiry", "saleableOnline", "status");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PharmacyAiCommandMapper mapper;
    private final DrugService drugService;
    private final PharmacyAiContextService contextService;
    private final SecurityFrameworkService security;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public Preview prepare(String action, JsonNode arguments, Long conversationId, String clientMessageId) {
        String tool = switch (action) {
            case "prepareCreateDrug" -> "CREATE_DRUG";
            case "prepareUpdateDrug" -> "UPDATE_DRUG";
            case "prepareDeleteDrug" -> "DELETE_DRUG";
            default -> throw new IllegalArgumentException("不支持的写工具");
        };
        String permission = switch (tool) {
            case "CREATE_DRUG" -> "pharmacy:base:drug:create";
            case "UPDATE_DRUG" -> "pharmacy:base:drug:update";
            default -> "pharmacy:base:drug:delete";
        };
        require(permission);
        var context = contextService.requireContext(false);
        JsonNode normalized;
        JsonNode before = null;
        if ("CREATE_DRUG".equals(tool)) {
            normalized = sanitize(arguments.has("drug") ? arguments.get("drug") : arguments, false);
            validate(objectMapper.convertValue(normalized, DrugSaveReqVO.class));
        } else {
            Long id = requiredId(arguments);
            DrugDO current = drugService.validateDrugExists(id);
            before = objectMapper.valueToTree(BeanUtils.toBean(current, DrugSaveReqVO.class));
            if ("UPDATE_DRUG".equals(tool)) {
                JsonNode changes = sanitize(arguments.has("changes") ? arguments.get("changes") : arguments, true);
                ObjectNode merged = (ObjectNode) objectMapper.valueToTree(BeanUtils.toBean(current, DrugSaveReqVO.class));
                changes.fields().forEachRemaining(e -> merged.set(e.getKey(), e.getValue()));
                merged.put("id", id);
                validate(objectMapper.convertValue(merged, DrugSaveReqVO.class));
                normalized = merged;
            } else {
                normalized = objectMapper.createObjectNode().put("id", id);
            }
        }
        String requestJson = json(normalized);
        String beforeJson = before == null ? null : json(before);
        String requestHash = snapshotHash(tool, normalized, before);
        String token = randomToken();
        PharmacyAiCommandDO command = new PharmacyAiCommandDO();
        command.setTenantId(context.tenantId());
        command.setConversationId(conversationId);
        command.setClientMessageId(clientMessageId);
        command.setUserId(context.userId());
        command.setEmployeeId(context.employeeId());
        command.setStoreId(context.storeId());
        command.setToolName(tool);
        command.setPermission(permission);
        command.setRequestJson(requestJson);
        command.setBeforeJson(beforeJson);
        command.setAfterJson("DELETE_DRUG".equals(tool) ? null : requestJson);
        command.setRequestHash(requestHash);
        command.setTokenHash(sha256(token));
        command.setStatus("PENDING");
        command.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        mapper.insert(command);
        return toPreview(command, token);
    }

    public Map<String, Object> confirm(Long id, String token) {
        PharmacyAiCommandDO command = requireOwned(id);
        require(command.getPermission());
        if (!MessageDigest.isEqual(command.getTokenHash().getBytes(StandardCharsets.UTF_8),
                sha256(token).getBytes(StandardCharsets.UTF_8))) {
            throw new AccessDeniedException("确认凭证无效");
        }
        String actualHash = snapshotHash(command.getToolName(), read(command.getRequestJson()),
                readNullable(command.getBeforeJson()));
        if (!MessageDigest.isEqual(command.getRequestHash().getBytes(StandardCharsets.UTF_8),
                actualHash.getBytes(StandardCharsets.UTF_8))) {
            throw new AccessDeniedException("命令快照校验失败");
        }
        if (!"PENDING".equals(command.getStatus()) || !command.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new IllegalStateException("命令已使用、已取消或已过期");
        }
        verifyTargetUnchanged(command);
        if (mapper.claimPending(id, LocalDateTime.now()) != 1) {
            throw new IllegalStateException("命令已被执行或已过期");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            JsonNode request = read(command.getRequestJson());
            if ("CREATE_DRUG".equals(command.getToolName())) {
                Long drugId = drugService.createDrug(objectMapper.convertValue(request, DrugSaveReqVO.class));
                result.put("drugId", drugId);
            } else if ("UPDATE_DRUG".equals(command.getToolName())) {
                drugService.updateDrug(objectMapper.convertValue(request, DrugSaveReqVO.class));
                result.put("drugId", request.get("id").asLong());
            } else {
                long drugId = request.get("id").asLong();
                drugService.deleteDrug(drugId);
                result.put("drugId", drugId);
            }
            command.setStatus("EXECUTED");
            command.setExecutedAt(LocalDateTime.now());
            command.setResultJson(json(result));
            mapper.updateById(command);
            result.put("status", "EXECUTED");
            return result;
        } catch (RuntimeException ex) {
            command.setStatus("FAILED");
            command.setErrorMessage(safeMessage(ex));
            mapper.updateById(command);
            throw ex;
        }
    }

    public void cancel(Long id) {
        var context = contextService.requireContext(false);
        if (mapper.cancelPending(id, context.userId()) != 1) {
            throw new IllegalStateException("命令不存在或不能取消");
        }
    }

    public List<Preview> recent() {
        var context = contextService.requireContext(false);
        return mapper.selectRecentByUser(context.userId()).stream().map(v -> toPreview(v, null)).toList();
    }

    private void verifyTargetUnchanged(PharmacyAiCommandDO command) {
        if ("CREATE_DRUG".equals(command.getToolName())) return;
        long id = read(command.getRequestJson()).get("id").asLong();
        DrugDO current = drugService.getDrug(id);
        if (current == null || !json(objectMapper.valueToTree(BeanUtils.toBean(current, DrugSaveReqVO.class)))
                .equals(command.getBeforeJson())) {
            throw new IllegalStateException("目标药品已发生变化，请重新生成预览");
        }
    }

    private PharmacyAiCommandDO requireOwned(Long id) {
        var context = contextService.requireContext(false);
        PharmacyAiCommandDO command = mapper.selectById(id);
        if (command == null || !context.userId().equals(command.getUserId())
                || !context.tenantId().equals(command.getTenantId())) {
            throw new AccessDeniedException("命令不存在或无权访问");
        }
        return command;
    }

    private JsonNode sanitize(JsonNode node, boolean update) {
        if (node == null || !node.isObject()) throw new IllegalArgumentException("药品参数必须是对象");
        ObjectNode result = objectMapper.createObjectNode();
        node.fields().forEachRemaining(e -> {
            if (FORBIDDEN.contains(e.getKey())) throw new IllegalArgumentException("禁止修改字段：" + e.getKey());
            if (WRITABLE.contains(e.getKey())) {
                if (BOOLEAN_AS_INTEGER.contains(e.getKey()) && e.getValue().isBoolean()) {
                    result.put(e.getKey(), e.getValue().booleanValue() ? 1 : 0);
                } else {
                    result.set(e.getKey(), e.getValue());
                }
            }
            else if (!(update && "id".equals(e.getKey()))) throw new IllegalArgumentException("未知字段：" + e.getKey());
        });
        return result;
    }

    private Long requiredId(JsonNode node) {
        if (node == null || node.get("id") == null || !node.get("id").canConvertToLong()) {
            throw new IllegalArgumentException("缺少药品 id");
        }
        return node.get("id").asLong();
    }

    private void validate(DrugSaveReqVO request) {
        var violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String message = violations.stream().map(v -> v.getMessage()).sorted().distinct()
                    .collect(java.util.stream.Collectors.joining("；"));
            throw new IllegalArgumentException("操作信息不完整或不合法：" + message);
        }
    }

    private void require(String permission) {
        if (!security.hasPermission(permission)) throw new AccessDeniedException("缺少权限：" + permission);
    }

    private Preview toPreview(PharmacyAiCommandDO command, String token) {
        String status = "PENDING".equals(command.getStatus()) && !command.getExpiresAt().isAfter(LocalDateTime.now())
                ? "EXPIRED" : command.getStatus();
        return new Preview(command.getId(), command.getToolName(), status,
                readNullable(command.getBeforeJson()), readNullable(command.getAfterJson()),
                command.getExpiresAt(), token, command.getErrorMessage());
    }

    private JsonNode readNullable(String value) { return value == null ? null : read(value); }
    private JsonNode read(String value) {
        try { return objectMapper.readTree(value); }
        catch (JsonProcessingException e) { throw new IllegalStateException("命令快照损坏", e); }
    }
    private String json(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException e) { throw new IllegalArgumentException("参数无法序列化", e); }
    }
    private String randomToken() {
        byte[] bytes = new byte[32]; RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    private String snapshotHash(String tool, JsonNode request, JsonNode before) {
        return sha256(tool + "\n" + json(canonical(request)) + "\n"
                + (before == null ? "" : json(canonical(before))));
    }
    private JsonNode canonical(JsonNode node) {
        if (node.isObject()) {
            ObjectNode result = objectMapper.createObjectNode();
            java.util.TreeMap<String, JsonNode> fields = new java.util.TreeMap<>();
            node.fields().forEachRemaining(entry -> fields.put(entry.getKey(), canonical(entry.getValue())));
            fields.forEach(result::set);
            return result;
        }
        if (node.isArray()) {
            ArrayNode result = objectMapper.createArrayNode();
            node.forEach(value -> result.add(canonical(value)));
            return result;
        }
        return node;
    }
    private String sha256(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
    private String safeMessage(Exception ex) {
        String message = ex.getMessage();
        return message == null ? "执行失败" : message.substring(0, Math.min(message.length(), 500));
    }

    public record Preview(Long commandId, String tool, String status, JsonNode before, JsonNode after,
                          LocalDateTime expiresAt, String confirmationToken, String errorMessage) { }
}
