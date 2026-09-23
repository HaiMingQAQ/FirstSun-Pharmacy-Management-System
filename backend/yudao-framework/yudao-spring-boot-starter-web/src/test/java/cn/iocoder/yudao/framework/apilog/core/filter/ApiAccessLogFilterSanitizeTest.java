package cn.iocoder.yudao.framework.apilog.core.filter;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import cn.iocoder.yudao.framework.apilog.core.interceptor.ApiAccessLogInterceptor;
import cn.iocoder.yudao.framework.common.biz.infra.logger.ApiAccessLogCommonApi;
import cn.iocoder.yudao.framework.common.biz.infra.logger.ApiErrorLogCommonApi;
import cn.iocoder.yudao.framework.common.biz.infra.logger.dto.ApiAccessLogCreateReqDTO;
import cn.iocoder.yudao.framework.common.biz.infra.logger.dto.ApiErrorLogCreateReqDTO;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.web.config.WebProperties;
import cn.iocoder.yudao.framework.web.core.filter.CacheRequestBodyWrapper;
import cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils;
import cn.iocoder.yudao.framework.web.core.handler.GlobalExceptionHandler;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.method.HandlerMethod;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApiAccessLogFilterSanitizeTest {

    private static final String PASSWORD = "test-password-4831";
    private static final String TOKEN = "test-token-4831";
    private static final String SECRET = "test-secret-4831";

    @Test
    void unannotatedLoginRequestSanitizesConsoleAndPersistedLog() throws Exception {
        runRequest("{\"username\":\"0407\",\"password\":\"" + PASSWORD
                + "\",\"appSecret\":\"" + SECRET + "\",\"nested\":{\"access_token\":\"" + TOKEN + "\"}}",
                false, false, false, true);
    }

    @Test
    void malformedBodyAndExceptionFailClosed() throws Exception {
        runRequest("{\"password\":\"" + PASSWORD, true, true, false, false);
    }

    @Test
    void accessLogPersistenceFailureDoesNotExposeExceptionMessage() throws Exception {
        runRequest("{\"password\":\"" + PASSWORD + "\"}", false, false, true, true);
    }

    @Test
    void validationErrorResponsesDoNotEchoRejectedValues() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler("test", mock(ApiErrorLogCommonApi.class));
        var typeMismatch = new MethodArgumentTypeMismatchException(TOKEN, Integer.class,
                "socialType", null, new NumberFormatException(TOKEN));
        assertFalse(handler.methodArgumentTypeMismatchExceptionHandler(typeMismatch).getMsg().contains(TOKEN));
        var invalidFormat = InvalidFormatException.from(null, "bad input", SECRET, Integer.class);
        var unreadable = new HttpMessageNotReadableException("bad input", invalidFormat,
                new MockHttpInputMessage(new byte[0]));
        assertFalse(handler.methodArgumentTypeInvalidFormatExceptionHandler(unreadable).getMsg().contains(SECRET));
    }

    @Test
    void authenticationServiceErrorDoesNotEchoExceptionMessage() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler("test", mock(ApiErrorLogCommonApi.class));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/admin-api/system/auth/login");
        assertFalse(handler.serviceExceptionHandler(request,
                new ServiceException(400, "failure " + SECRET)).getMsg().contains(SECRET));
    }

    @Test
    void authenticationPathHidesValuesInUnexpectedFields() throws Exception {
        runRequest("{\"username\":\"0407\",\"socialType\":\"" + SECRET + "\"}",
                false, false, false, true);
    }

    @Test
    void globalExceptionDoesNotLogOrPersistRawInput() {
        ApiErrorLogCommonApi errorApi = mock(ApiErrorLogCommonApi.class);
        GlobalExceptionHandler handler = new GlobalExceptionHandler("test", errorApi);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/admin-api/system/auth/login");
        request.setContentType("application/json");
        request.setContent(("{\"password\":\"" + PASSWORD + "\"}").getBytes(StandardCharsets.UTF_8));
        request.addParameter("appSecret", SECRET);
        WebFrameworkUtils.setLoginUserType(request, 2);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        logger.addAppender(appender);
        try {
            handler.defaultExceptionHandler(request, new IllegalStateException("failed " + TOKEN));
            ArgumentCaptor<ApiErrorLogCreateReqDTO> captor = ArgumentCaptor.forClass(ApiErrorLogCreateReqDTO.class);
            verify(errorApi).createApiErrorLogAsync(captor.capture());
            ApiErrorLogCreateReqDTO saved = captor.getValue();
            String persisted = saved.getRequestParams() + saved.getExceptionMessage()
                    + saved.getExceptionRootCauseMessage() + saved.getExceptionStackTrace();
            assertFalse(persisted.contains(PASSWORD));
            assertFalse(persisted.contains(TOKEN));
            assertFalse(persisted.contains(SECRET));
            for (ILoggingEvent event : appender.list) {
                assertFalse(event.getFormattedMessage().contains(TOKEN));
                assertNull(event.getThrowableProxy());
            }
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    private void runRequest(String body, boolean throwFromHandler, boolean malformed,
                            boolean logPersistenceFails, boolean authenticationPath) throws Exception {
        ApiAccessLogCommonApi logApi = mock(ApiAccessLogCommonApi.class);
        if (logPersistenceFails) {
            doThrow(new IllegalStateException("failure " + SECRET)).when(logApi).createApiAccessLogAsync(any());
        }
        ApiAccessLogFilter filter = new ApiAccessLogFilter(new WebProperties(), "test", logApi);
        ApiAccessLogInterceptor interceptor = new ApiAccessLogInterceptor();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", authenticationPath
                ? "/admin-api/system/auth/login" : "/admin-api/pharmacy/drug/list");
        request.setContentType("application/json");
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        request.addParameter("refreshToken", TOKEN);
        request.addParameter("visible", "ok");
        HttpServletRequest cachedRequest = new CacheRequestBodyWrapper(request);
        WebFrameworkUtils.setLoginUserType(cachedRequest, 2);
        MockHttpServletResponse response = new MockHttpServletResponse();
        HandlerMethod handler = new HandlerMethod(new LoginHandler(), LoginHandler.class.getMethod("login"));

        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        Logger consoleLogger = (Logger) LoggerFactory.getLogger(ApiAccessLogInterceptor.class);
        Logger filterLogger = (Logger) LoggerFactory.getLogger(ApiAccessLogFilter.class);
        consoleLogger.addAppender(appender);
        filterLogger.addAppender(appender);
        try {
            if (throwFromHandler) {
                assertThrows(ServletException.class, () -> filter.doFilter(cachedRequest, response, (req, res) -> {
                    interceptor.preHandle(cachedRequest, response, handler);
                    throw new ServletException("failure " + SECRET);
                }));
            } else {
                filter.doFilter(cachedRequest, response, (req, res) -> {
                    interceptor.preHandle(cachedRequest, response, handler);
                    WebFrameworkUtils.setCommonResult(cachedRequest, CommonResult.error(500, "failure " + SECRET));
                });
            }
            ArgumentCaptor<ApiAccessLogCreateReqDTO> captor = ArgumentCaptor.forClass(ApiAccessLogCreateReqDTO.class);
            verify(logApi).createApiAccessLogAsync(captor.capture());
            ApiAccessLogCreateReqDTO saved = captor.getValue();
            String persisted = saved.getRequestParams() + saved.getResultMsg() + saved.getResponseBody();
            assertFalse(persisted.contains(PASSWORD));
            assertFalse(persisted.contains(TOKEN));
            assertFalse(persisted.contains(SECRET));
            if (authenticationPath) {
                assertFalse(saved.getRequestParams().contains("visible"));
                assertTrue(saved.getRequestParams().contains("请求参数已脱敏"));
            } else {
                assertTrue(saved.getRequestParams().contains("visible"));
            }
            if (malformed) {
                assertTrue(saved.getRequestParams().contains("请求参数已脱敏"));
            }
            List<String> messages = new ArrayList<>();
            for (ILoggingEvent event : appender.list) {
                messages.add(event.getFormattedMessage());
                assertNull(event.getThrowableProxy());
            }
            String console = String.join("\n", messages);
            assertFalse(console.contains(PASSWORD));
            assertFalse(console.contains(TOKEN));
            assertFalse(console.contains(SECRET));
        } finally {
            consoleLogger.detachAppender(appender);
            filterLogger.detachAppender(appender);
            appender.stop();
        }
    }

    static class LoginHandler {
        public void login() {
        }
    }
}
