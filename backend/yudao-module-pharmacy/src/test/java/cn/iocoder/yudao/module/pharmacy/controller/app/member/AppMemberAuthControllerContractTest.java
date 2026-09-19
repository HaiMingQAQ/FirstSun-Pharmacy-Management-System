package cn.iocoder.yudao.module.pharmacy.controller.app.member;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link AppMemberAuthController} 契约测试：手机号快捷登录的验证码必须走请求头。
 *
 * <p>原因：yudao 框架的 {@code ApiAccessLogInterceptor} 在非 prod 环境会把
 * query 参数与 JSON body 原样写进后端日志。验证码一旦放在这两处，
 * 「后端日志不泄露验证码」的要求就会被破坏，因此这里用测试把契约锁住：
 * 验证码只允许通过 {@code @RequestHeader("X-Sms-Code")} 提交。
 */
class AppMemberAuthControllerContractTest {

    @Test
    void testLoginOrRegister_codeMustComeFromHeader() throws NoSuchMethodException {
        Method method = AppMemberAuthController.class.getMethod("loginOrRegister", String.class, String.class);

        // 手机号：query 参数（非敏感信息）
        assertTrue(findAnnotation(method.getParameterAnnotations()[0], RequestParam.class) != null,
                "手机号应通过 query 参数提交");

        // 验证码：只允许请求头，禁止 query 参数 / JSON body
        Annotation[] codeAnnotations = method.getParameterAnnotations()[1];
        RequestHeader header = findAnnotation(codeAnnotations, RequestHeader.class);
        assertNotNull(header, "验证码必须通过 @RequestHeader 提交，避免被框架访问日志打印");
        assertTrue(Arrays.asList(header.value()).contains("X-Sms-Code"),
                "验证码请求头名称固定为 X-Sms-Code");
        assertFalse(header.required(), "验证码可以为空，由后端返回「请输入手机验证码」的业务提示");
        assertTrue(findAnnotation(codeAnnotations, RequestParam.class) == null,
                "验证码不允许作为 query 参数，否则会被 ApiAccessLogInterceptor 打印到日志");
        assertTrue(findAnnotation(codeAnnotations, RequestBody.class) == null,
                "验证码不允许作为请求体，否则会被 ApiAccessLogInterceptor 打印到日志");
    }

    private static <T extends Annotation> T findAnnotation(Annotation[] annotations, Class<T> type) {
        for (Annotation annotation : annotations) {
            if (type.isInstance(annotation)) {
                return type.cast(annotation);
            }
        }
        return null;
    }

}
