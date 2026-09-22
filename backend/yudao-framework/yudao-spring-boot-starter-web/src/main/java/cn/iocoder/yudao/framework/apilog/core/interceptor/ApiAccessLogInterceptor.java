package cn.iocoder.yudao.framework.apilog.core.interceptor;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.util.servlet.ServletUtils;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.spring.SpringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StopWatch;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * API 访问日志 Interceptor
 *
 * 目的：在非 prod 环境时，打印 request 和 response 两条日志到日志文件（控制台）中。
 *
 * @author 芋道源码
 */
@Slf4j
public class ApiAccessLogInterceptor implements HandlerInterceptor {

    public static final String ATTRIBUTE_HANDLER_METHOD = "HANDLER_METHOD";

    private static final String ATTRIBUTE_STOP_WATCH = "ApiAccessLogInterceptor.StopWatch";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 记录 HandlerMethod，提供给 ApiAccessLogFilter 使用
        HandlerMethod handlerMethod = handler instanceof HandlerMethod ? (HandlerMethod) handler : null;
        if (handlerMethod != null) {
            request.setAttribute(ATTRIBUTE_HANDLER_METHOD, handlerMethod);
        }

        // 打印 request 日志
        if (!SpringUtils.isProd()) {
            Map<String, String> queryString = ServletUtils.getParamMap(request);
            String requestBody = ServletUtils.getBody(request);
            Map<String, String> safeQueryString = sanitizeQueryString(queryString, handlerMethod);
            if (CollUtil.isEmpty(queryString) && StrUtil.isEmpty(requestBody)) {
                log.info("[preHandle][开始请求 URL({}) 无参数]", request.getRequestURI());
            } else {
                String safeRequestBody = sanitizeRequestBody(requestBody, handlerMethod);
                log.info("[preHandle][开始请求 URL({}) 参数({})]", request.getRequestURI(),
                        StrUtil.blankToDefault(safeRequestBody, safeQueryString.toString()));
            }
            // 计时
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            request.setAttribute(ATTRIBUTE_STOP_WATCH, stopWatch);
            // 打印 Controller 路径
            printHandlerMethodPosition(handlerMethod);
        }
        return true;
    }

    private Map<String, String> sanitizeQueryString(Map<String, String> queryString, HandlerMethod handlerMethod) {
        if (CollUtil.isEmpty(queryString) || handlerMethod == null) {
            return queryString;
        }
        ApiAccessLog accessLog = handlerMethod.getMethodAnnotation(ApiAccessLog.class);
        if (accessLog == null || accessLog.sanitizeKeys().length == 0) {
            return queryString;
        }
        Map<String, String> safeQueryString = new HashMap<>(queryString);
        MapUtil.removeAny(safeQueryString, accessLog.sanitizeKeys());
        return safeQueryString;
    }

    private String sanitizeRequestBody(String requestBody, HandlerMethod handlerMethod) {
        if (StrUtil.isEmpty(requestBody) || handlerMethod == null) {
            return requestBody;
        }
        ApiAccessLog accessLog = handlerMethod.getMethodAnnotation(ApiAccessLog.class);
        if (accessLog == null || accessLog.sanitizeKeys().length == 0) {
            return requestBody;
        }
        try {
            JsonNode rootNode = JsonUtils.parseTree(requestBody);
            sanitizeJson(rootNode, accessLog.sanitizeKeys());
            return JsonUtils.toJsonString(rootNode);
        } catch (Exception ignore) {
            // 脱敏失败时不打印原始请求体，避免敏感参数泄露
            return "[请求参数已脱敏]";
        }
    }

    private void sanitizeJson(JsonNode node, String[] sanitizeKeys) {
        if (node == null) {
            return;
        }
        if (node.isArray()) {
            for (JsonNode childNode : node) {
                sanitizeJson(childNode, sanitizeKeys);
            }
            return;
        }
        if (!node.isObject()) {
            return;
        }
        Iterator<Map.Entry<String, JsonNode>> iterator = node.properties().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            if (ArrayUtil.contains(sanitizeKeys, entry.getKey())) {
                iterator.remove();
                continue;
            }
            sanitizeJson(entry.getValue(), sanitizeKeys);
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 打印 response 日志
        if (!SpringUtils.isProd()) {
            StopWatch stopWatch = (StopWatch) request.getAttribute(ATTRIBUTE_STOP_WATCH);
            stopWatch.stop();
            log.info("[afterCompletion][完成请求 URL({}) 耗时({} ms)]",
                    request.getRequestURI(), stopWatch.getTotalTimeMillis());
        }
    }

    /**
     * 打印 Controller 方法路径
     */
    private void printHandlerMethodPosition(HandlerMethod handlerMethod) {
        if (handlerMethod == null) {
            return;
        }
        Method method = handlerMethod.getMethod();
        Class<?> clazz = method.getDeclaringClass();
        try {
            // 获取 method 的 lineNumber
            List<String> clazzContents = FileUtil.readUtf8Lines(
                    ResourceUtil.getResource(null, clazz).getPath().replace("/target/classes/", "/src/main/java/")
                            + clazz.getSimpleName() + ".java");
            Optional<Integer> lineNumber = IntStream.range(0, clazzContents.size())
                    .filter(i -> clazzContents.get(i).contains(" " + method.getName() + "(")) // 简单匹配，不考虑方法重名
                    .mapToObj(i -> i + 1) // 行号从 1 开始
                    .findFirst();
            if (!lineNumber.isPresent()) {
                return;
            }
            // 打印结果
            System.out.printf("\tController 方法路径：%s(%s.java:%d)\n", clazz.getName(), clazz.getSimpleName(), lineNumber.get());
        } catch (Exception ignore) {
            // 忽略异常。原因：仅仅打印，非重要逻辑
        }
    }

}
