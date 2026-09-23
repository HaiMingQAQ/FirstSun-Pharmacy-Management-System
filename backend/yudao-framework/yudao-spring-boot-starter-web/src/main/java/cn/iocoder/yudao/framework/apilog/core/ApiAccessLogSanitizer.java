package cn.iocoder.yudao.framework.apilog.core;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Shared, fail-closed sanitization for persisted and console API logs. */
public final class ApiAccessLogSanitizer {

    public static final String REDACTED = "[请求参数已脱敏]";
    public static final String RESULT_REDACTED = "[结果信息已脱敏]";

    /**
     * 已知安全修饰前缀 + Token（如 accessToken、refreshToken、confirmationToken）。
     * 不使用 endsWith("token")，以免误伤 AI 模块中表示数量的 tokens、maxTokens、segmentMaxTokens。
     */
    private static final String[] TOKEN_PREFIXES = {
            "access", "refresh", "confirmation", "confirm", "id", "auth", "share",
            "csrf", "xsrf", "reset", "verify", "session", "user", "admin",
            "login", "register", "invite", "device", "client"
    };

    private ApiAccessLogSanitizer() {
    }

    public static boolean isAuthenticationPath(String uri) {
        return uri != null && uri.contains("/auth/");
    }

    public static String query(Map<String, ?> query, String[] extraKeys) {
        if (query == null || query.isEmpty()) {
            return null;
        }
        try {
            Map<String, Object> safe = new LinkedHashMap<>();
            query.forEach((key, value) -> {
                if (!sensitive(key, extraKeys)) {
                    safe.put(key, value);
                }
            });
            return JsonUtils.toJsonString(safe);
        } catch (Exception ignored) {
            return REDACTED;
        }
    }

    public static String json(String body, String[] extraKeys) {
        if (body == null || body.isEmpty()) {
            return null;
        }
        try {
            JsonNode node = JsonUtils.getObjectMapper().readTree(body);
            if (node == null || !node.isContainerNode()) {
                return REDACTED;
            }
            sanitize(node, extraKeys);
            return JsonUtils.toJsonString(node);
        } catch (Exception ignored) {
            return REDACTED;
        }
    }

    private static void sanitize(JsonNode node, String[] extraKeys) {
        if (node.isArray()) {
            node.forEach(child -> sanitize(child, extraKeys));
        } else if (node.isObject()) {
            var fields = node.properties().iterator();
            while (fields.hasNext()) {
                var field = fields.next();
                if (sensitive(field.getKey(), extraKeys)) {
                    fields.remove();
                } else {
                    sanitize(field.getValue(), extraKeys);
                }
            }
        }
    }

    private static boolean sensitive(String key, String[] extraKeys) {
        if (key == null) {
            return false;
        }
        String normalized = key.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
        // 密码：password 及任何 XxxPassword（old/new/confirm/mqtt/keyStore/trustStore 等）
        if (normalized.endsWith("password")) {
            return true;
        }
        // 密钥：secret 及任何 XxxSecret（app/client/api/access/device/product 等），含 secretKey
        if (normalized.endsWith("secret") || normalized.equals("secretkey")) {
            return true;
        }
        // 令牌：精确 token、社交原始令牌 rawTokenInfo，或限定安全前缀 + Token。
        // 不做宽泛的 token 子串/endsWith 匹配，避免误伤 AI 的 tokens/maxTokens 等数量字段。
        if (normalized.equals("token") || normalized.equals("rawtokeninfo")) {
            return true;
        }
        for (String prefix : TOKEN_PREFIXES) {
            if (normalized.equals(prefix + "token")) {
                return true;
            }
        }
        switch (normalized) {
            case "authorization": case "sessionkey":
            case "code": case "mobile": case "phone": case "idcard": case "idnumber":
                return true;
            default:
                if (extraKeys != null) {
                    for (String extraKey : extraKeys) {
                        if (key.equalsIgnoreCase(extraKey)) {
                            return true;
                        }
                    }
                }
                return false;
        }
    }
}
