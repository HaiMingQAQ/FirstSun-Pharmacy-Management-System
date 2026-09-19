package cn.iocoder.yudao.module.pharmacy.config;

import cn.iocoder.yudao.module.pharmacy.api.inventory.InventoryFacade;
import cn.iocoder.yudao.module.pharmacy.service.inventory.InventoryFacadeAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 库存门面「重复实现」的 F 侧兼容层（不修改 C / D 的任何文件）。
 *
 * <p>问题：当前 main 上同时存在两个 {@code InventoryFacade} 的 {@code @Service} 实现 ——
 * C 的 {@link InventoryFacadeAdapter}（含冻结 / 释放 / 冻结转出库与门店隔离门禁）与
 * D 的 {@code api.inventory.PharmacyInventoryFacadeImpl}（仅 deduct / returnBack / receive，
 * 且每次 deduct 生成随机 bizNo）。两者都会让容器在注入 {@code InventoryFacade} 时报
 * {@code A component required a single bean, but 2 were found}，后端直接启动失败。
 *
 * <p>F 不能改别人的文件，因此在这里把 C 的实现声明为 {@code @Primary}：
 * 删掉 D 的重复实现后（建议由 D 处理），本类可以整块删除。
 *
 * <p>如需临时关闭本兼容层：{@code yudao.pharmacy.compat.inventory-facade-primary=false}。
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "yudao.pharmacy.compat", name = "inventory-facade-primary",
        havingValue = "true", matchIfMissing = true)
public class InventoryFacadeCompatConfiguration {

    private final InventoryFacadeAdapter inventoryFacadeAdapter;

    /**
     * 优先使用 C 的库存门面：它带有幂等键（{@code uk_flow_event}）与门店隔离门禁，
     * D 的随机 bizNo 实现会让重复销售回调重复扣库存。
     */
    @Bean
    @Primary
    public InventoryFacade primaryInventoryFacade() {
        return inventoryFacadeAdapter;
    }

}
