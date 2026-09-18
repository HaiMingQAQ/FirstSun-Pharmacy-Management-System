package cn.iocoder.yudao.module.ai.service.pharmacy;

import cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService;
import cn.iocoder.yudao.module.ai.dal.dataobject.pharmacy.PharmacyAiCommandDO;
import cn.iocoder.yudao.module.ai.dal.mysql.pharmacy.PharmacyAiCommandMapper;
import cn.iocoder.yudao.module.pharmacy.service.base.DrugService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PharmacyAiCommandServiceTest {
    private final PharmacyAiCommandMapper mapper = mock(PharmacyAiCommandMapper.class);
    private final DrugService drugs = mock(DrugService.class);
    private final PharmacyAiContextService contexts = mock(PharmacyAiContextService.class);
    private final SecurityFrameworkService security = mock(SecurityFrameworkService.class);
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    private PharmacyAiCommandService service;

    @BeforeEach
    void setUp() {
        service = new PharmacyAiCommandService(mapper, drugs, contexts, security, json,
                Validation.buildDefaultValidatorFactory().getValidator());
        when(contexts.requireContext(false)).thenReturn(new PharmacyAiContextService.Context(1L, 11L, 21L, 31L));
        when(security.hasPermission(any())).thenReturn(true);
    }

    @Test
    void prepareCreatePersistsSnapshotButDoesNotWriteDrug() throws Exception {
        when(mapper.insert(any(PharmacyAiCommandDO.class))).thenAnswer(invocation -> {
            PharmacyAiCommandDO command = invocation.getArgument(0);
            command.setId(100L);
            return 1;
        });
        var args = json.readTree("""
                {"drug":{"drugCode":"AI001","categoryId":1,"genericName":"原型药品","specification":"10mg*10片",
                "drugType":1,"isRx":0,"isSpecial":0,"isPseudoephedrine":0,"isColdChain":0,"unit":"盒",
                "retailPrice":12.50,"taxRate":13,"insuranceType":0,"minStock":1,"maxStock":10,
                "storageCond":0,"needExpiry":1,"saleableOnline":0,"status":1}}
                """);

        var preview = service.prepare("prepareCreateDrug", args, 9L, "m-1");

        assertEquals(100L, preview.commandId());
        assertEquals("PENDING", preview.status());
        assertNotNull(preview.confirmationToken());
        verify(mapper).insert(any(PharmacyAiCommandDO.class));
        verifyNoInteractions(drugs);
    }

    @Test
    void prepareCreateNormalizesBooleanFlagsToIntegerEnums() throws Exception {
        when(mapper.insert(any(PharmacyAiCommandDO.class))).thenAnswer(invocation -> {
            PharmacyAiCommandDO command = invocation.getArgument(0);
            command.setId(102L);
            return 1;
        });
        var args = json.readTree("""
                {"drug":{"drugCode":"AI004","categoryId":1,"genericName":"布尔字段药品","specification":"1片",
                "drugType":8,"isRx":false,"isSpecial":false,"isPseudoephedrine":false,"isColdChain":false,"unit":"盒",
                "retailPrice":1,"taxRate":0,"insuranceType":0,"minStock":0,"maxStock":10,
                "storageCond":0,"needExpiry":false,"saleableOnline":false,"status":true}}
                """);

        var preview = service.prepare("prepareCreateDrug", args, 9L, "m-4");

        assertEquals(0, preview.after().get("isRx").asInt());
        assertEquals(0, preview.after().get("isColdChain").asInt());
        assertEquals(0, preview.after().get("needExpiry").asInt());
        assertEquals(1, preview.after().get("status").asInt());
    }

    @Test
    void confirmationIsAtMostOnce() throws Exception {
        AtomicReference<PharmacyAiCommandDO> stored = new AtomicReference<>();
        when(mapper.insert(any(PharmacyAiCommandDO.class))).thenAnswer(invocation -> {
            PharmacyAiCommandDO command = invocation.getArgument(0);
            command.setId(101L);
            stored.set(command);
            return 1;
        });
        when(mapper.selectById(101L)).thenAnswer(invocation -> stored.get());
        when(mapper.claimPending(eq(101L), any())).thenReturn(1);
        when(drugs.createDrug(any())).thenReturn(501L);
        var args = json.readTree("""
                {"drug":{"drugCode":"AI002","categoryId":1,"genericName":"幂等药品","specification":"1片",
                "drugType":1,"isRx":0,"isSpecial":0,"isPseudoephedrine":0,"isColdChain":0,"unit":"盒",
                "retailPrice":1,"taxRate":13,"insuranceType":0,"minStock":0,"maxStock":0,
                "storageCond":0,"needExpiry":1,"saleableOnline":0,"status":1}}
        """);
        var preview = service.prepare("prepareCreateDrug", args, 9L, "m-2");
        stored.get().setRequestJson(json.writerWithDefaultPrettyPrinter()
                .writeValueAsString(json.readTree(stored.get().getRequestJson())));

        assertEquals("EXECUTED", service.confirm(101L, preview.confirmationToken()).get("status"));
        assertThrows(IllegalStateException.class, () -> service.confirm(101L, preview.confirmationToken()));
        verify(drugs, times(1)).createDrug(any());
    }

    @Test
    void forbiddenServerFieldIsRejected() throws Exception {
        var args = json.readTree("{\"drug\":{\"drugCode\":\"AI003\",\"tenantId\":99}}");
        assertThrows(IllegalArgumentException.class,
                () -> service.prepare("prepareCreateDrug", args, 9L, "m-3"));
        verify(mapper, never()).insert(any(PharmacyAiCommandDO.class));
        verifyNoInteractions(drugs);
    }
}
