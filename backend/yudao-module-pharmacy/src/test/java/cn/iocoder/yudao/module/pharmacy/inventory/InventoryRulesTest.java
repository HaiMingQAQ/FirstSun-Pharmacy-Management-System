package cn.iocoder.yudao.module.pharmacy.inventory;

import org.junit.jupiter.api.Test;

/** One JUnit test invokes the existing 50 independent rule groups; no MySQL assertions. */
class InventoryRulesTest {
    @Test
    void verifyRuleGroups() {
        InventoryRulesCheck.main(new String[0]);
    }
}
