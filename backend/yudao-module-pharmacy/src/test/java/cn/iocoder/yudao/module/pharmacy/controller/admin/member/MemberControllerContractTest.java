package cn.iocoder.yudao.module.pharmacy.controller.admin.member;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * F 会员模块 Controller 参数契约测试。
 *
 * <p>背景：管理端页面此前按 JSON Body 调用取消 / 核销，而后端要求 Query 参数；会员启用 / 停用又调用了
 * 不存在的接口；取消 / 核销复用了 update 权限。这些都属于「前后端契约漂移」，编译期无法发现。
 * 本测试把 URL、请求方式、参数名、参数类型、权限标识全部锁死，任一侧改动都会让测试失败。
 */
class MemberControllerContractTest {

    private static final Pattern PERMISSION_PATTERN = Pattern.compile("hasPermission\\('([^']+)'\\)");

    // ========== 1. 会员启用 / 停用 ==========

    /** 会员状态接口：PUT /pharmacy/member/user/update-status，Query 参数 id + status，权限 user:update */
    @Test
    void testMemberUserUpdateStatusContract() {
        Method method = method(MemberUserController.class, "updateMemberUserStatus");

        assertEquals("/pharmacy/member/user", classPath(MemberUserController.class));
        assertEquals("/update-status", putPath(method));
        assertEquals(List.of("id", "status"), requestParamNames(method));
        assertEquals("pharmacy:member:user:update", permission(method));
        assertEquals(Long.class, method.getParameterTypes()[0]);
        assertEquals(Integer.class, method.getParameterTypes()[1]);
    }

    /** 会员新增 / 修改：Body 提交 SaveReqVO，权限分别为 create / update */
    @Test
    void testMemberUserSaveContract() {
        Method create = method(MemberUserController.class, "createMemberUser");
        assertEquals("/create", postPath(create));
        assertEquals("pharmacy:member:user:create", permission(create));
        assertTrue(hasRequestBody(create), "新增会员必须接收 JSON Body");

        Method update = method(MemberUserController.class, "updateMemberUser");
        assertEquals("/update", putPath(update));
        assertEquals("pharmacy:member:user:update", permission(update));
        assertTrue(hasRequestBody(update), "修改会员必须接收 JSON Body");
    }

    /** 会员查询 / 删除权限独立，避免前端按钮与后端校验不一致 */
    @Test
    void testMemberUserQueryAndDeleteContract() {
        assertEquals("pharmacy:member:user:query", permission(method(MemberUserController.class, "getMemberUserPage")));
        assertEquals("pharmacy:member:user:query", permission(method(MemberUserController.class, "getMemberUser")));
        assertEquals("pharmacy:member:user:delete", permission(method(MemberUserController.class, "deleteMemberUser")));
        assertEquals("/page", getPath(method(MemberUserController.class, "getMemberUserPage")));
    }

    // ========== 2. 订单取消 / 核销 ==========

    /** 取消订单：PUT /pharmacy/member/order/cancel，Query 参数 id + cancelReason，权限 order:cancel */
    @Test
    void testOrderCancelContract() {
        Method method = method(WxOrderController.class, "cancelWxOrder");

        assertEquals("/pharmacy/member/order", classPath(WxOrderController.class));
        assertEquals("/cancel", putPath(method));
        assertEquals(List.of("id", "cancelReason"), requestParamNames(method));
        assertEquals("pharmacy:member:order:cancel", permission(method));
        assertFalse(hasRequestBody(method), "取消订单必须走 Query 参数，不能改成 JSON Body");
    }

    /** 核销订单：PUT /pharmacy/member/order/verify，Query 参数 id + pickupCode + verifyBy，权限 order:verify */
    @Test
    void testOrderVerifyContract() {
        Method method = method(WxOrderController.class, "verifyWxOrder");

        assertEquals("/verify", putPath(method));
        assertEquals(List.of("id", "pickupCode", "verifyBy"), requestParamNames(method));
        assertEquals("pharmacy:member:order:verify", permission(method));
        // 类型锁死：id / verifyBy 为 Long，pickupCode 为 String
        assertEquals(Long.class, method.getParameterTypes()[0]);
        assertEquals(String.class, method.getParameterTypes()[1]);
        assertEquals(Long.class, method.getParameterTypes()[2]);
    }

    /** 订单查询仍使用 query 权限 */
    @Test
    void testOrderQueryContract() {
        assertEquals("/page", getPath(method(WxOrderController.class, "getWxOrderPage")));
        assertEquals("pharmacy:member:order:query", permission(method(WxOrderController.class, "getWxOrderPage")));
    }

    /** 后端不存在 /update-status（前端已删除该废弃 API，避免以后误调用） */
    @Test
    void testOrderControllerHasNoUpdateStatusEndpoint() {
        boolean exists = Arrays.stream(WxOrderController.class.getDeclaredMethods())
                .map(m -> m.getAnnotation(PutMapping.class))
                .filter(mapping -> mapping != null)
                .flatMap(mapping -> Arrays.stream(mapping.value()))
                .anyMatch(path -> path.contains("update-status"));

        assertFalse(exists, "后端没有 /pharmacy/member/order/update-status，前端不应保留该调用");
    }

    // ========== 3. 收货地址新增 / 编辑 ==========

    /** 地址新增 / 编辑：Body 提交 SaveReqVO，权限分别为 create / update */
    @Test
    void testMemberAddressSaveContract() {
        assertEquals("/pharmacy/member/address", classPath(MemberAddressController.class));

        Method create = method(MemberAddressController.class, "createMemberAddress");
        assertEquals("/create", postPath(create));
        assertEquals("pharmacy:member:address:create", permission(create));
        assertTrue(hasRequestBody(create), "新增地址必须接收 JSON Body");

        Method update = method(MemberAddressController.class, "updateMemberAddress");
        assertEquals("/update", putPath(update));
        assertEquals("pharmacy:member:address:update", permission(update));
        assertTrue(hasRequestBody(update), "编辑地址必须接收 JSON Body");
    }

    /** 地址查询 / 删除权限独立 */
    @Test
    void testMemberAddressQueryAndDeleteContract() {
        assertEquals("pharmacy:member:address:query", permission(method(MemberAddressController.class, "getMemberAddressPage")));
        assertEquals("pharmacy:member:address:delete", permission(method(MemberAddressController.class, "deleteMemberAddress")));
    }

    // ========== 测试辅助 ==========

    private Method method(Class<?> clazz, String name) {
        return Arrays.stream(clazz.getDeclaredMethods())
                .filter(m -> m.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError(clazz.getSimpleName() + " 缺少方法 " + name));
    }

    private String classPath(Class<?> clazz) {
        RequestMapping mapping = clazz.getAnnotation(RequestMapping.class);
        assertNotNull(mapping, clazz.getSimpleName() + " 缺少 @RequestMapping");
        return mapping.value()[0];
    }

    private String putPath(Method method) {
        PutMapping mapping = method.getAnnotation(PutMapping.class);
        assertNotNull(mapping, method.getName() + " 缺少 @PutMapping");
        return mapping.value()[0];
    }

    private String postPath(Method method) {
        PostMapping mapping = method.getAnnotation(PostMapping.class);
        assertNotNull(mapping, method.getName() + " 缺少 @PostMapping");
        return mapping.value()[0];
    }

    private String getPath(Method method) {
        GetMapping mapping = method.getAnnotation(GetMapping.class);
        assertNotNull(mapping, method.getName() + " 缺少 @GetMapping");
        return mapping.value()[0];
    }

    private List<String> requestParamNames(Method method) {
        List<String> names = new ArrayList<>();
        for (Parameter parameter : method.getParameters()) {
            RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
            if (requestParam != null && !requestParam.value().isBlank()) {
                names.add(requestParam.value());
            }
        }
        return names;
    }

    private boolean hasRequestBody(Method method) {
        return Stream.of(method.getParameterAnnotations())
                .flatMap(Arrays::stream)
                .anyMatch(annotation -> annotation instanceof RequestBody);
    }

    private String permission(Method method) {
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, method.getName() + " 缺少 @PreAuthorize");
        Matcher matcher = PERMISSION_PATTERN.matcher(preAuthorize.value());
        assertTrue(matcher.find(), "无法解析权限标识：" + preAuthorize.value());
        return matcher.group(1);
    }

}
