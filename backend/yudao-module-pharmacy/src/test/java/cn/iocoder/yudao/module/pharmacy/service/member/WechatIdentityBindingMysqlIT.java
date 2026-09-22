package cn.iocoder.yudao.module.pharmacy.service.member;

import cn.iocoder.yudao.framework.common.biz.system.oauth2.OAuth2TokenCommonApi;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.mybatis.config.YudaoMybatisAutoConfiguration;
import cn.iocoder.yudao.framework.mybatis.core.util.MyBatisUtils;
import cn.iocoder.yudao.framework.tenant.config.TenantProperties;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.db.TenantDatabaseInterceptor;
import cn.iocoder.yudao.module.pharmacy.config.WechatLoginProperties;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.MemberUserDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.member.WechatIdentityDO;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.member.MemberUserMapper;
import cn.iocoder.yudao.module.pharmacy.dal.mysql.member.WechatIdentityMapper;
import cn.iocoder.yudao.module.system.api.logger.LoginLogApi;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Opt-in real MySQL tests for the pharmacy WeChat identity binding.
 *
 * <p>The config must be inside this module's dedicated target directory and carry the
 * firstsun-wechat-it owner marker. The database is created by the caller's unique Compose
 * project; this test never drops or truncates data.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = WechatIdentityBindingMysqlIT.TestApplication.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfSystemProperty(named = "wechat.identity.mysql.config", matches = ".+")
class WechatIdentityBindingMysqlIT {

    private static final long AUTH_TENANT_ID = 1104L;
    private static final String AUTH_APP_ID = "wx-wechat-it-20260922";
    private static final String AUTH_CODE = "synthetic-code-not-a-real-wechat-code";

    @Autowired
    private WechatIdentityBindingService bindingService;
    @Autowired
    private MemberUserService memberUserService;
    @Autowired
    private MemberUserMapper memberUserMapper;
    @Autowired
    private WechatIdentityMapper identityMapper;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private WechatMiniAppCodeVerifier codeVerifier;
    @Autowired
    private OAuth2TokenCommonApi oauth2TokenApi;
    @MockBean
    private WechatMiniAppCodeVerifier wechatMiniAppCodeVerifier;

    @BeforeAll
    static void verifyDedicatedConfig() throws Exception {
        loadConfig();
    }

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) throws Exception {
        Properties properties = loadConfig();
        String url = "jdbc:mysql://127.0.0.1:" + properties.getProperty("port") + "/"
                + properties.getProperty("database")
                + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
                + "&nullCatalogMeansCurrent=true&connectTimeout=5000&socketTimeout=15000";
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.username", () -> properties.getProperty("user"));
        registry.add("spring.datasource.password", () -> properties.getProperty("password"));
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("yudao.info.base-package", () -> "cn.iocoder.yudao.module.pharmacy");
        registry.add("mybatis-plus.configuration.map-underscore-to-camel-case", () -> "true");
        registry.add("mybatis-plus.global-config.db-config.id-type", () -> "auto");
        registry.add("mybatis-plus.mapper-locations", () -> "classpath*:mapper/**/*.xml");
        registry.add("spring.sql.init.mode", () -> "never");
        registry.add("spring.main.web-application-type", () -> "none");
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void concurrentFirstLoginCreatesOneBindingAndOneMember() throws Exception {
        long tenantId = 1101L;
        String appId = "wx-concurrent-20260922";
        String openid = "openid-concurrent-" + token();
        String registerIp = "10.110.1.1";
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<MemberUserDO>> futures = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                futures.add(executor.submit(() -> {
                    TenantContextHolder.setTenantId(tenantId);
                    start.await(5, TimeUnit.SECONDS);
                    try {
                        return bindingService.findOrCreate(appId, openid, tenantId, registerIp);
                    } finally {
                        TenantContextHolder.clear();
                    }
                }));
            }
            start.countDown();
            List<MemberUserDO> successful = new ArrayList<>();
            int duplicateFailures = 0;
            for (Future<MemberUserDO> future : futures) {
                try {
                    successful.add(future.get(15, TimeUnit.SECONDS));
                } catch (java.util.concurrent.ExecutionException ex) {
                    assertInstanceOf(org.springframework.dao.DuplicateKeyException.class, ex.getCause());
                    duplicateFailures++;
                }
            }

            TenantContextHolder.setTenantId(tenantId);
            MemberUserDO winner = bindingService.findMember(appId, openid, tenantId);
            assertNotNull(winner);
            assertEquals(1, jdbc.queryForObject(
                    "SELECT COUNT(*) FROM ph_member_wechat_identity WHERE tenant_id=? AND app_id=? AND openid=?",
                    Integer.class, tenantId, appId, openid));
            assertEquals(1, jdbc.queryForObject(
                    "SELECT COUNT(*) FROM member_user WHERE tenant_id=? AND nickname=? AND register_ip=?",
                    Integer.class, tenantId, "微信会员", registerIp));
            assertTrue(successful.size() == 1 || successful.size() == 2);
            assertTrue(duplicateFailures == 0 || duplicateFailures == 1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void uniqueConflictRollsBackCandidateAndReadsCommittedWinner() throws Exception {
        long tenantId = 1102L;
        String appId = "wx-conflict-20260922";
        String openid = "openid-conflict-" + token();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch loserStarted = new CountDownLatch(1);
        Future<Throwable> loser;
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);

        try {
            TenantContextHolder.setTenantId(tenantId);
            loser = transaction.execute(status -> {
                MemberUserDO winner = memberUserService.createWechatMember(tenantId, "10.110.2.1");
                WechatIdentityDO identity = new WechatIdentityDO();
                identity.setTenantId(tenantId);
                identity.setAppId(appId);
                identity.setOpenid(openid);
                identity.setMemberId(winner.getId());
                identityMapper.insert(identity);

                Future<Throwable> future = executor.submit(() -> {
                    TenantContextHolder.setTenantId(tenantId);
                    loserStarted.countDown();
                    try {
                        bindingService.findOrCreate(appId, openid, tenantId, "10.110.2.2");
                        return null;
                    } catch (Throwable error) {
                        return error;
                    } finally {
                        TenantContextHolder.clear();
                    }
                });
                try {
                    assertTrue(loserStarted.await(5, TimeUnit.SECONDS));
                    Thread.sleep(700);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError("interrupted while coordinating transaction test", interrupted);
                }
                return future;
            });
        } finally {
            TenantContextHolder.clear();
        }

        Throwable error = loser.get(15, TimeUnit.SECONDS);
        assertInstanceOf(org.springframework.dao.DuplicateKeyException.class, error);
        TenantContextHolder.setTenantId(tenantId);
        MemberUserDO committed = bindingService.findMember(appId, openid, tenantId);
        assertNotNull(committed);
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM member_user WHERE tenant_id=? AND register_ip=?",
                Integer.class, tenantId, "10.110.2.1"));
        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM member_user WHERE tenant_id=? AND register_ip=?",
                Integer.class, tenantId, "10.110.2.2"));
        executor.shutdownNow();
    }

    @Test
    void sameOpenidAcrossTenantAndAppIdDoesNotCrossBind() {
        String openid = "openid-shared-" + token();
        MemberUserDO first = findOrCreate(1103L, "wx-isolation-a-20260922", openid, "10.110.3.1");
        MemberUserDO second = findOrCreate(1104L, "wx-isolation-b-20260922", openid, "10.110.4.1");

        assertFalse(first.getId().equals(second.getId()));
        assertEquals(1, countIdentity(1103L, "wx-isolation-a-20260922", openid));
        assertEquals(1, countIdentity(1104L, "wx-isolation-b-20260922", openid));
        TenantContextHolder.setTenantId(1103L);
        assertEquals(1103L, memberUserMapper.selectByIdAndTenantId(first.getId(), 1103L).getTenantId());
        TenantContextHolder.setTenantId(1104L);
        assertEquals(1104L, memberUserMapper.selectByIdAndTenantId(second.getId(), 1104L).getTenantId());
    }

    @Test
    void noPhoneMemberIsReadableAndDisabledMemberCannotLogin() {
        TenantContextHolder.setTenantId(AUTH_TENANT_ID);
        when(codeVerifier.verifyAndGetOpenid(AUTH_CODE)).thenReturn("openid-disabled-" + token());
        MemberUserDO created = bindingService.findOrCreate(AUTH_APP_ID,
                codeVerifier.verifyAndGetOpenid(AUTH_CODE), AUTH_TENANT_ID, "10.110.4.1");

        assertNull(created.getMobile());
        assertNull(memberUserMapper.selectByIdAndTenantId(created.getId(), AUTH_TENANT_ID).getMobile());

        MemberUserDO disabled = new MemberUserDO();
        disabled.setId(created.getId());
        disabled.setTenantId(AUTH_TENANT_ID);
        disabled.setStatus(1);
        memberUserMapper.updateById(disabled);

        assertThrows(ServiceException.class, () ->
                getAuthService().wechatLogin(AUTH_CODE));
        verify(oauth2TokenApi, never()).createAccessToken(org.mockito.ArgumentMatchers.any());
    }

    private MemberUserDO findOrCreate(long tenantId, String appId, String openid, String ip) {
        TenantContextHolder.setTenantId(tenantId);
        return bindingService.findOrCreate(appId, openid, tenantId, ip);
    }

    private int countIdentity(long tenantId, String appId, String openid) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM ph_member_wechat_identity WHERE tenant_id=? AND app_id=? AND openid=?",
                Integer.class, tenantId, appId, openid);
    }

    private static String token() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private MemberAuthService getAuthService() {
        return applicationContext.getBean(MemberAuthService.class);
    }

    @Autowired
    private org.springframework.context.ApplicationContext applicationContext;

    private static Properties loadConfig() throws Exception {
        Path module = Path.of(System.getProperty("basedir")).toRealPath();
        Path root = module.resolve("target/wechat-mysql-it").toRealPath();
        Path configPath = Path.of(System.getProperty("wechat.identity.mysql.config")).toRealPath();
        if (!configPath.startsWith(root)) {
            throw new IllegalArgumentException("微信身份测试配置必须位于专属 target 目录");
        }
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        assertEquals("firstsun-wechat-it-20260922-01", properties.getProperty("owner"));
        assertEquals("firstsun-wechat-it-20260922-01", properties.getProperty("project"));
        assertEquals("firstsun-wechat-it-20260922-01-mysql", properties.getProperty("container"));
        assertEquals("firstsun-wechat-it-20260922-01-mysql-data", properties.getProperty("volume"));
        assertEquals("127.0.0.1", properties.getProperty("host"));
        assertEquals("23317", properties.getProperty("port"));
        if (properties.getProperty("password") == null || properties.getProperty("password").isBlank()) {
            throw new IllegalArgumentException("独立测试配置缺少密码");
        }
        return properties;
    }

    @SpringBootConfiguration
    @EnableTransactionManagement(proxyTargetClass = true)
    @Import({DataSourceAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class,
            YudaoMybatisAutoConfiguration.class, MybatisPlusAutoConfiguration.class,
            TenantTestConfiguration.class, MemberUserServiceImpl.class,
            WechatIdentityBindingService.class, MemberAuthServiceImpl.class, TestDoubles.class})
    static class TestApplication {
    }

    @Configuration(proxyBeanMethods = false)
    static class TenantTestConfiguration {
        @Bean
        TenantLineInnerInterceptor tenantLineInnerInterceptor(MybatisPlusInterceptor interceptor) {
            TenantLineInnerInterceptor tenant = new TenantLineInnerInterceptor(
                    new TenantDatabaseInterceptor(new TenantProperties()));
            MyBatisUtils.addInterceptor(interceptor, tenant, 0);
            return tenant;
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class TestDoubles {
        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }

        @Bean
        WechatLoginProperties wechatLoginProperties() {
            return new WechatLoginProperties().setTenantId(AUTH_TENANT_ID).setAppId(AUTH_APP_ID);
        }

        @Bean
        OAuth2TokenCommonApi oauth2TokenCommonApi() {
            return mock(OAuth2TokenCommonApi.class);
        }

        @Bean
        LoginLogApi loginLogApi() {
            return mock(LoginLogApi.class);
        }

        @Bean
        MemberSmsCodeService memberSmsCodeService() {
            return mock(MemberSmsCodeService.class);
        }
    }

}
