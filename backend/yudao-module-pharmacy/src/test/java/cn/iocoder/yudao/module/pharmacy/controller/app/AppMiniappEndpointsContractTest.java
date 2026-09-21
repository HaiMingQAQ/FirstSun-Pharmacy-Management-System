package cn.iocoder.yudao.module.pharmacy.controller.app;

import cn.iocoder.yudao.module.pharmacy.controller.app.member.AppWxOrderController;
import cn.iocoder.yudao.module.pharmacy.controller.app.pharmacy.AppInventoryController;
import cn.iocoder.yudao.module.pharmacy.controller.app.prescription.AppPrescRecordController;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 小程序端新增 app-api 端点的 HTTP 契约测试（纯反射，不依赖 Spring 容器）。
 *
 * <p>锁住本轮为小程序课堂演示补充的端点表面，防止后续重构改路径/改方法导致前端适配器静默失配：
 * - 订单：模拟支付 / 确认收货（POST，路径后缀固定）；
 * - 处方：登记 / 我的列表 / 详情；
 * - 库存：门店可售量只读投影。
 */
class AppMiniappEndpointsContractTest {

    @Test
    void testWxOrderController_hasSimulatePayEndpoint() throws NoSuchMethodException {
        Method method = AppWxOrderController.class.getMethod("simulatePay", Long.class);
        PostMapping mapping = findAnnotation(method.getAnnotations(), PostMapping.class);
        assertNotNull(mapping, "模拟支付必须是 POST");
        assertTrue(Arrays.asList(mapping.value()).contains("/simulate-pay"), "模拟支付路径必须为 /simulate-pay");
        RequestParam param = findAnnotation(method.getParameterAnnotations()[0], RequestParam.class);
        assertNotNull(param, "模拟支付必须携带订单编号参数");
    }

    @Test
    void testWxOrderController_hasConfirmReceiveEndpoint() throws NoSuchMethodException {
        Method method = AppWxOrderController.class.getMethod("confirmReceive", Long.class);
        PostMapping mapping = findAnnotation(method.getAnnotations(), PostMapping.class);
        assertNotNull(mapping, "确认收货必须是 POST");
        assertTrue(Arrays.asList(mapping.value()).contains("/confirm-receive"), "确认收货路径必须为 /confirm-receive");
        RequestParam param = findAnnotation(method.getParameterAnnotations()[0], RequestParam.class);
        assertNotNull(param, "确认收货必须携带订单编号参数");
    }

    @Test
    void testWxOrderController_classMappingKept() {
        RequestMapping mapping = AppWxOrderController.class.getAnnotation(RequestMapping.class);
        assertNotNull(mapping, "订单控制器必须有类级路由");
        assertTrue(Arrays.asList(mapping.value()).contains("/member/wx-order"),
                "订单控制器类级路由必须为 /member/wx-order");
    }

    @Test
    void testPrescRecordController_hasCreatePageGet() throws NoSuchMethodException {
        Method create = AppPrescRecordController.class.getMethod("createPrescRecord",
                cn.iocoder.yudao.module.pharmacy.controller.app.prescription.vo.AppPrescRecordCreateReqVO.class);
        assertNotNull(findAnnotation(create.getAnnotations(), PostMapping.class), "处方登记必须是 POST");
        assertTrue(Arrays.asList(findAnnotation(create.getAnnotations(), PostMapping.class).value())
                .contains("/create"), "处方登记路径必须为 /create");

        Method page = AppPrescRecordController.class.getMethod("getMyPrescRecords");
        assertNotNull(findAnnotation(page.getAnnotations(), GetMapping.class), "我的处方列表必须是 GET");
        assertTrue(Arrays.asList(findAnnotation(page.getAnnotations(), GetMapping.class).value())
                .contains("/page"), "我的处方列表路径必须为 /page");

        Method get = AppPrescRecordController.class.getMethod("getPrescRecord", Long.class);
        assertNotNull(findAnnotation(get.getAnnotations(), GetMapping.class), "处方详情必须是 GET");
        assertTrue(Arrays.asList(findAnnotation(get.getAnnotations(), GetMapping.class).value())
                .contains("/get"), "处方详情路径必须为 /get");
        assertNotNull(findAnnotation(get.getParameterAnnotations()[0], RequestParam.class), "处方详情必须携带处方编号");
    }

    @Test
    void testPrescRecordController_classMappingKept() {
        RequestMapping mapping = AppPrescRecordController.class.getAnnotation(RequestMapping.class);
        assertNotNull(mapping);
        assertTrue(Arrays.asList(mapping.value()).contains("/member/prescription"),
                "处方控制器类级路由必须为 /member/prescription");
    }

    @Test
    void testInventoryController_hasAvailableEndpoint() throws NoSuchMethodException {
        Method method = AppInventoryController.class.getMethod("getAvailableQty", Long.class, java.util.List.class);
        GetMapping mapping = findAnnotation(method.getAnnotations(), GetMapping.class);
        assertNotNull(mapping, "可售库存必须是 GET");
        assertTrue(Arrays.asList(mapping.value()).contains("/available"), "可售库存路径必须为 /available");
        assertEquals(2, method.getParameterCount(), "可售库存必须携带门店与药品编号列表");
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
