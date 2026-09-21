package cn.iocoder.yudao.module.pharmacy.controller.app;

import cn.iocoder.yudao.module.pharmacy.controller.app.member.AppWxOrderController;
import cn.iocoder.yudao.module.pharmacy.service.member.WxOrderService;
import cn.iocoder.yudao.module.pay.api.notify.dto.PayOrderNotifyReqDTO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.MediaType;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** MVC DTO/response contract; does not replace full security-filter HTTP acceptance. */
class WxOrderPaymentNotifyControllerTest {
    @Test void acceptsPayModuleContractAndReturnsCommonSuccess() throws Exception {
        var service=mock(WxOrderService.class); var controller=new AppWxOrderController();
        ReflectionTestUtils.setField(controller,"wxOrderService",service);
        var mvc=MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(post("/member/wx-order/payment-notify").contentType(MediaType.APPLICATION_JSON)
                .content("{\"merchantOrderId\":\"WX100\",\"payOrderId\":50}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0)).andExpect(jsonPath("$.data").value(true));
        verify(service).notifyWxOrderPaid("WX100",50L);
        assertTrue(AppWxOrderController.class.getMethod("paymentNotify",PayOrderNotifyReqDTO.class)
                .isAnnotationPresent(jakarta.annotation.security.PermitAll.class));
    }
    @Test void missingPaymentIdCannotReachService() throws Exception {
        var service=mock(WxOrderService.class); var controller=new AppWxOrderController();
        ReflectionTestUtils.setField(controller,"wxOrderService",service);
        MockMvcBuilders.standaloneSetup(controller).build().perform(post("/member/wx-order/payment-notify")
                .contentType(MediaType.APPLICATION_JSON).content("{\"merchantOrderId\":\"WX100\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
