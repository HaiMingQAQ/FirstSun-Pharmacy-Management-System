package cn.iocoder.yudao.module.ai.service.pharmacy;

import cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService;
import cn.iocoder.yudao.module.ai.dal.dataobject.pharmacy.PharmacyAiCommandDO;
import cn.iocoder.yudao.module.ai.dal.mysql.pharmacy.PharmacyAiCommandMapper;
import cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.drug.DrugSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.base.DrugDO;
import cn.iocoder.yudao.module.pharmacy.service.base.DrugService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.Map;
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
        reset(mapper, drugs, contexts, security); // 隔离跨测试的 stub，避免权限 stub 泄漏
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

    // ==================== 更新药品（prepareUpdateDrug）场景 ====================

    /** 构造一个字段齐全的药品快照（retailPrice=20.00），供 update 预览与确认使用。 */
    private DrugDO sampleDrug(long id) {
        DrugDO drug = new DrugDO();
        drug.setId(id);
        drug.setDrugCode("DRG-" + id);
        drug.setCategoryId(1L);
        drug.setGenericName("测试药品" + id);
        drug.setSpecification("10mg*10片");
        drug.setDrugType(1);
        drug.setIsRx(0);
        drug.setIsSpecial(0);
        drug.setIsPseudoephedrine(0);
        drug.setIsColdChain(0);
        drug.setUnit("盒");
        drug.setRetailPrice(new BigDecimal("20.00"));
        drug.setTaxRate(new BigDecimal("13.00"));
        drug.setInsuranceType(0);
        drug.setMinStock(1);
        drug.setMaxStock(10);
        drug.setStorageCond(0);
        drug.setNeedExpiry(1);
        drug.setSaleableOnline(0);
        drug.setStatus(1);
        return drug;
    }

    private void stubUpdateCommandPersistence(AtomicReference<PharmacyAiCommandDO> stored, long commandId) {
        DrugDO drug = sampleDrug(163101L);
        when(drugs.validateDrugExists(163101L)).thenReturn(drug);
        when(drugs.getDrug(163101L)).thenReturn(drug);
        when(mapper.insert(any(PharmacyAiCommandDO.class))).thenAnswer(invocation -> {
            PharmacyAiCommandDO command = invocation.getArgument(0);
            command.setId(commandId);
            stored.set(command);
            return 1;
        });
        when(mapper.selectById(commandId)).thenAnswer(invocation -> stored.get());
        when(mapper.claimPending(eq(commandId), any())).thenReturn(1);
    }

    @Test
    void prepareUpdatePersistsSnapshotButDoesNotWriteDrug() throws Exception {
        AtomicReference<PharmacyAiCommandDO> stored = new AtomicReference<>();
        stubUpdateCommandPersistence(stored, 200L);
        var args = json.readTree("""
                {"id":163101,"changes":{"retailPrice":25.00}}
                """);

        var preview = service.prepare("prepareUpdateDrug", args, 9L, "m-up-1");

        assertEquals(200L, preview.commandId());
        assertEquals("PENDING", preview.status());
        assertNotNull(preview.confirmationToken());
        assertEquals(163101, preview.before().get("id").asLong());
        assertEquals(0, new BigDecimal("20.00").compareTo(
                new BigDecimal(preview.before().get("retailPrice").asText())));
        assertEquals(25.00, preview.after().get("retailPrice").asDouble());
        verify(drugs, never()).updateDrug(any());
        verify(mapper).insert(any(PharmacyAiCommandDO.class));
    }

    @Test
    void updateMissingIdIsRejected() throws Exception {
        var args = json.readTree("""
                {"changes":{"retailPrice":25.00}}
                """);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.prepare("prepareUpdateDrug", args, 9L, "m-up-2"));
        assertTrue(ex.getMessage().contains("缺少药品 id"), ex.getMessage());
        verify(mapper, never()).insert(any(PharmacyAiCommandDO.class));
        verifyNoInteractions(drugs);
    }

    @Test
    void updateIllegalPriceIsRejected() throws Exception {
        DrugDO drug = sampleDrug(163101L);
        when(drugs.validateDrugExists(163101L)).thenReturn(drug);
        var args = json.readTree("""
                {"id":163101,"changes":{"retailPrice":-5}}
                """);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.prepare("prepareUpdateDrug", args, 9L, "m-up-3"));
        assertTrue(ex.getMessage().contains("零售价不能小于 0"), ex.getMessage());
        verify(mapper, never()).insert(any(PharmacyAiCommandDO.class));
        verify(drugs, never()).updateDrug(any());
    }

    @Test
    void updateWithoutPermissionIsRejected() throws Exception {
        when(security.hasPermission("pharmacy:base:drug:update")).thenReturn(false);
        var args = json.readTree("""
                {"id":163101,"changes":{"retailPrice":25.00}}
                """);
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> service.prepare("prepareUpdateDrug", args, 9L, "m-up-4"));
        assertTrue(ex.getMessage().contains("pharmacy:base:drug:update"), ex.getMessage());
        verify(mapper, never()).insert(any(PharmacyAiCommandDO.class));
        verifyNoInteractions(drugs);
    }

    @Test
    void confirmUpdateOnlyChangesTargetDrugPrice() throws Exception {
        AtomicReference<PharmacyAiCommandDO> stored = new AtomicReference<>();
        stubUpdateCommandPersistence(stored, 201L);
        var args = json.readTree("""
                {"id":163101,"changes":{"retailPrice":25.00}}
                """);
        var preview = service.prepare("prepareUpdateDrug", args, 9L, "m-up-5");

        Map<String, Object> result = service.confirm(201L, preview.confirmationToken());

        assertEquals("EXECUTED", result.get("status"));
        ArgumentCaptor<DrugSaveReqVO> captor = ArgumentCaptor.forClass(DrugSaveReqVO.class);
        verify(drugs, times(1)).updateDrug(captor.capture());
        DrugSaveReqVO executed = captor.getValue();
        assertEquals(163101L, executed.getId());
        assertEquals(0, new BigDecimal("25.00").compareTo(executed.getRetailPrice()));
        assertEquals("EXECUTED", stored.get().getStatus());
        assertNotNull(stored.get().getExecutedAt());
    }

    @Test
    void confirmUpdateIsAtMostOnce() throws Exception {
        AtomicReference<PharmacyAiCommandDO> stored = new AtomicReference<>();
        stubUpdateCommandPersistence(stored, 202L);
        var args = json.readTree("""
                {"id":163101,"changes":{"retailPrice":25.00}}
                """);
        var preview = service.prepare("prepareUpdateDrug", args, 9L, "m-up-6");

        assertEquals("EXECUTED", service.confirm(202L, preview.confirmationToken()).get("status"));
        assertThrows(IllegalStateException.class, () -> service.confirm(202L, preview.confirmationToken()));
        verify(drugs, times(1)).updateDrug(any());
    }

    @Test
    void cancelUpdateDoesNotWriteDrug() throws Exception {
        AtomicReference<PharmacyAiCommandDO> stored = new AtomicReference<>();
        stubUpdateCommandPersistence(stored, 203L);
        when(mapper.cancelPending(203L, 11L)).thenReturn(1);
        var args = json.readTree("""
                {"id":163101,"changes":{"retailPrice":25.00}}
                """);
        var preview = service.prepare("prepareUpdateDrug", args, 9L, "m-up-7");

        service.cancel(203L);

        assertEquals("PENDING", preview.status());
        verify(mapper).cancelPending(203L, 11L);
        verify(drugs, never()).updateDrug(any());
    }

    // ==================== 并发确认检测（verifyTargetUnchanged）序列化差异 ====================

    /** 指定零售价的药品快照。 */
    private DrugDO sampleDrug(long id, BigDecimal retailPrice) {
        DrugDO drug = sampleDrug(id);
        drug.setRetailPrice(retailPrice);
        return drug;
    }

    @Test
    void snapshotsEqualTreatsEquivalentNumbersSame() throws Exception {
        // 19.80、19.8、1.98E+1 视为相同数值
        assertTrue(service.snapshotsEqual(json.readTree("{\"retailPrice\":19.80}"),
                json.readTree("{\"retailPrice\":19.8}")));
        assertTrue(service.snapshotsEqual(json.readTree("{\"retailPrice\":1.98E+1}"),
                json.readTree("{\"retailPrice\":19.8}")));
        assertTrue(service.snapshotsEqual(json.readTree("{\"retailPrice\":1.98E+1}"),
                json.readTree("{\"retailPrice\":19.80}")));
        assertTrue(service.snapshotsEqual(json.readTree("{\"status\":1}"),
                json.readTree("{\"status\":1}")));
    }

    @Test
    void snapshotsEqualIgnoresFieldOrder() throws Exception {
        assertTrue(service.snapshotsEqual(
                json.readTree("{\"a\":1,\"b\":2,\"c\":{\"x\":true},\"d\":[1,2]}"),
                json.readTree("{\"d\":[1,2],\"c\":{\"x\":true},\"b\":2,\"a\":1}")));
    }

    @Test
    void snapshotsEqualDetectsRealChanges() throws Exception {
        // 真正修改业务字段必须拒绝
        assertFalse(service.snapshotsEqual(json.readTree("{\"retailPrice\":19.80}"),
                json.readTree("{\"retailPrice\":25.00}")));
        assertFalse(service.snapshotsEqual(json.readTree("{\"status\":1}"),
                json.readTree("{\"status\":0}")));
        assertFalse(service.snapshotsEqual(json.readTree("{\"genericName\":\"甲\"}"),
                json.readTree("{\"genericName\":\"乙\"}")));
    }

    @Test
    void confirmUpdatePassesWhenNumericRepresentationDiffers() throws Exception {
        // prepare 快照 19.80（scale=2），confirm 时数据库返回 19.8（scale=1），视为未修改
        AtomicReference<PharmacyAiCommandDO> stored = new AtomicReference<>();
        when(drugs.validateDrugExists(163101L)).thenReturn(sampleDrug(163101L, new BigDecimal("19.80")));
        when(drugs.getDrug(163101L)).thenReturn(sampleDrug(163101L, new BigDecimal("19.8")));
        when(mapper.insert(any(PharmacyAiCommandDO.class))).thenAnswer(invocation -> {
            PharmacyAiCommandDO command = invocation.getArgument(0);
            command.setId(204L);
            stored.set(command);
            return 1;
        });
        when(mapper.selectById(204L)).thenAnswer(invocation -> stored.get());
        when(mapper.claimPending(eq(204L), any())).thenReturn(1);
        var args = json.readTree("""
                {"id":163101,"changes":{"retailPrice":20.80}}
                """);

        var preview = service.prepare("prepareUpdateDrug", args, 9L, "m-up-num");

        assertEquals("PENDING", preview.status());
        assertEquals("EXECUTED", service.confirm(204L, preview.confirmationToken()).get("status"));
        verify(drugs, times(1)).updateDrug(any());
    }

    @Test
    void confirmUpdateRejectsRealDrugChange() throws Exception {
        // 预览后目标药品零售价确实被其他请求修改（19.80 -> 25.00），确认必须拒绝
        AtomicReference<PharmacyAiCommandDO> stored = new AtomicReference<>();
        when(drugs.validateDrugExists(163101L)).thenReturn(sampleDrug(163101L, new BigDecimal("19.80")));
        when(drugs.getDrug(163101L)).thenReturn(sampleDrug(163101L, new BigDecimal("25.00")));
        when(mapper.insert(any(PharmacyAiCommandDO.class))).thenAnswer(invocation -> {
            PharmacyAiCommandDO command = invocation.getArgument(0);
            command.setId(205L);
            stored.set(command);
            return 1;
        });
        when(mapper.selectById(205L)).thenAnswer(invocation -> stored.get());
        when(mapper.claimPending(eq(205L), any())).thenReturn(1);
        var args = json.readTree("""
                {"id":163101,"changes":{"retailPrice":20.80}}
                """);
        var preview = service.prepare("prepareUpdateDrug", args, 9L, "m-up-changed");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.confirm(205L, preview.confirmationToken()));
        assertTrue(ex.getMessage().contains("目标药品已发生变化"), ex.getMessage());
        verify(drugs, never()).updateDrug(any());
    }
}
