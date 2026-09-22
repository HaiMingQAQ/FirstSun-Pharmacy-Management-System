package cn.iocoder.yudao.framework.apilog.core.filter;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiAccessLogFilterSanitizeTest {

    private static final String[] SANITIZE_KEYS = {"code", "state", "session_key", "token"};

    @Test
    void sanitizeJson_normalAndNestedFields() throws Exception {
        String secretCode = "fake-code-7f4b";
        String secretToken = "fake-token-93aa";

        String result = invokeSanitizeJson("{\"code\":\"" + secretCode
                + "\",\"profile\":{\"token\":\"" + secretToken
                + "\",\"name\":\"demo\"},\"state\":\"fake-state\"}");

        assertTrue(result.contains("demo"));
        assertFalse(result.contains(secretCode));
        assertFalse(result.contains(secretToken));
        assertFalse(result.contains("fake-state"));
    }

    @Test
    void sanitizeJson_malformedJsonUsesPlaceholderWithoutLeakingLogs() throws Exception {
        String secret = "fake-secret-4c21";
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        Logger logger = (Logger) LoggerFactory.getLogger(ApiAccessLogFilter.class);
        Logger jsonLogger = (Logger) LoggerFactory.getLogger(JsonUtils.class);
        logger.addAppender(appender);
        jsonLogger.addAppender(appender);
        try {
            String result = invokeSanitizeJson("{\"code\":\"" + secret);

            assertTrue(result.contains("请求参数已脱敏"));
            assertFalse(result.contains(secret));
            List<ILoggingEvent> events = appender.list;
            assertFalse(events.isEmpty());
            for (ILoggingEvent event : events) {
                assertFalse(event.getFormattedMessage().contains(secret));
                assertNull(event.getThrowableProxy());
            }
        } finally {
            logger.detachAppender(appender);
            jsonLogger.detachAppender(appender);
            appender.stop();
        }
    }

    private String invokeSanitizeJson(String json) throws Exception {
        Method method = ApiAccessLogFilter.class.getDeclaredMethod(
                "sanitizeJson", String.class, String[].class);
        method.setAccessible(true);
        return (String) method.invoke(null, json, (Object) SANITIZE_KEYS);
    }

}
