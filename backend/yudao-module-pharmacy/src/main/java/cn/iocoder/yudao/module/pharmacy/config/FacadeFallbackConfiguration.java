package cn.iocoder.yudao.module.pharmacy.config;

import cn.iocoder.yudao.module.pharmacy.api.inventory.InventoryFacade;
import cn.iocoder.yudao.module.pharmacy.api.inventory.InventoryFacadeImpl;
import cn.iocoder.yudao.module.pharmacy.api.member.MemberPointFacade;
import cn.iocoder.yudao.module.pharmacy.api.member.MemberPointFacadeImpl;
import cn.iocoder.yudao.module.pharmacy.api.payment.PaymentFacade;
import cn.iocoder.yudao.module.pharmacy.api.payment.PaymentFacadeImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * POS 域跨域门面【临时降级注册】。
 * <p>
 * C（库存）/ E（支付）/ F（积分）实现对应 Facade 后，@ConditionalOnMissingBean
 * 自动让位，本配置不再生效；在此之前，容器可正常启动，调用方收到
 * UnsupportedOperationException 并由 Service 层转为业务错误、事务整体回滚。
 */
@Configuration
public class FacadeFallbackConfiguration {

    @Bean
    @ConditionalOnMissingBean(InventoryFacade.class)
    public InventoryFacade inventoryFacade() {
        return new InventoryFacadeImpl();
    }

    @Bean
    @ConditionalOnMissingBean(PaymentFacade.class)
    public PaymentFacade paymentFacade() {
        return new PaymentFacadeImpl();
    }

    @Bean
    @ConditionalOnMissingBean(MemberPointFacade.class)
    public MemberPointFacade memberPointFacade() {
        return new MemberPointFacadeImpl();
    }

}
