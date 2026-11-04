package apps.sarafrika.elimika.wallet.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import apps.sarafrika.elimika.commerce.purchase.entity.CommercePurchase;
import apps.sarafrika.elimika.commerce.purchase.entity.CommercePurchaseItem;
import apps.sarafrika.elimika.commerce.purchase.repository.CommercePurchaseItemRepository;
import apps.sarafrika.elimika.commerce.purchase.repository.CommercePurchaseRepository;
import apps.sarafrika.elimika.commerce.purchase.service.impl.CommercePurchaseSettlementRecorder;
import apps.sarafrika.elimika.commerce.purchase.service.impl.CommerceSaleSplitQueryService;
import apps.sarafrika.elimika.course.spi.CourseInfoService;
import apps.sarafrika.elimika.course.spi.CourseInfoService.RevenueShare;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.payout.service.OrderCaptureWalletCreditListener;
import apps.sarafrika.elimika.shared.config.JpaConfig;
import apps.sarafrika.elimika.shared.currency.model.PlatformCurrency;
import apps.sarafrika.elimika.shared.currency.service.CurrencyService;
import apps.sarafrika.elimika.shared.dto.commerce.CartItemResponse;
import apps.sarafrika.elimika.shared.dto.commerce.OrderResponse;
import apps.sarafrika.elimika.shared.event.commerce.OrderCompletedEvent;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService.ClassDefinitionSnapshot;
import apps.sarafrika.elimika.shared.spi.revenue.PurchaseScope;
import apps.sarafrika.elimika.shared.spi.revenue.SaleDeliveryKind;
import apps.sarafrika.elimika.shared.spi.revenue.SaleFeeMode;
import apps.sarafrika.elimika.shared.spi.revenue.SaleLegStatus;
import apps.sarafrika.elimika.shared.spi.revenue.SalePartyType;
import apps.sarafrika.elimika.shared.utils.enums.RateBasis;
import apps.sarafrika.elimika.systemconfig.dto.PlatformFeeConfig;
import apps.sarafrika.elimika.systemconfig.enums.PlatformFeeMode;
import apps.sarafrika.elimika.systemconfig.enums.RuleCategory;
import apps.sarafrika.elimika.systemconfig.model.SystemRule;
import apps.sarafrika.elimika.systemconfig.service.RuleEvaluationService;
import apps.sarafrika.elimika.tenancy.services.impl.PartyNameLookupServiceImpl;
import apps.sarafrika.elimika.tenancy.spi.OrganisationLookupService;
import apps.sarafrika.elimika.testsupport.SharedPostgres;
import apps.sarafrika.elimika.wallet.controller.AdminSaleSplitController;
import apps.sarafrika.elimika.wallet.controller.OrganisationWalletController;
import apps.sarafrika.elimika.wallet.controller.WalletController;
import apps.sarafrika.elimika.wallet.dto.SaleSplitDTO;
import apps.sarafrika.elimika.wallet.ledger.WalletLedgerReconciliationJob;
import apps.sarafrika.elimika.wallet.ledger.impl.LedgerServiceImpl;
import apps.sarafrika.elimika.wallet.security.WalletSecurityService;
import apps.sarafrika.elimika.wallet.service.SaleSplitRole;
import apps.sarafrika.elimika.wallet.service.SaleSplitService;
import apps.sarafrika.elimika.wallet.service.WalletOwner;
import apps.sarafrika.elimika.wallet.service.WalletPurse;
import apps.sarafrika.elimika.wallet.service.WalletPurseBalance;
import apps.sarafrika.elimika.wallet.service.impl.SaleSplitServiceImpl;
import apps.sarafrika.elimika.wallet.service.impl.WalletServiceImpl;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * A captured sale line records its split on the purchase line in the same settlement that credits the
 * wallets, and earners and admins read it back: per leg, with the fee rule, reconciling to the line.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({OrderCaptureWalletCreditListener.class, WalletServiceImpl.class, LedgerServiceImpl.class,
        WalletLedgerReconciliationJob.class, CommercePurchaseSettlementRecorder.class,
        CommerceSaleSplitQueryService.class, SaleSplitServiceImpl.class, PartyNameLookupServiceImpl.class,
        JpaConfig.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("Sale splits")
class SaleSplitIntegrationTest {

    private static final String KES = "KES";
    private static final OffsetDateTime ORDERED_AT = OffsetDateTime.of(2026, 10, 20, 9, 0, 0, 0, ZoneOffset.UTC);
    private static final AtomicInteger USER_NO = new AtomicInteger(700_000_000);
    private static final PlatformFeeConfig TEN_PERCENT =
            new PlatformFeeConfig(PlatformFeeMode.PERCENTAGE, new BigDecimal("10"), KES, null, null);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        SharedPostgres.register(registry);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @Autowired
    private OrderCaptureWalletCreditListener listener;
    @Autowired
    private WalletServiceImpl walletService;
    @Autowired
    private SaleSplitService saleSplits;
    @Autowired
    private CommercePurchaseRepository purchaseRepository;
    @Autowired
    private CommercePurchaseItemRepository purchaseItemRepository;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private WalletLedgerReconciliationJob reconciliation;

    @MockitoBean
    private CurrencyService currencyService;
    @MockitoBean
    private CourseInfoService courseInfoService;
    @MockitoBean
    private InstructorLookupService instructorLookupService;
    @MockitoBean
    private ClassDefinitionLookupService classDefinitionLookupService;
    @MockitoBean
    private RuleEvaluationService ruleEvaluationService;
    @MockitoBean
    private OrganisationLookupService organisationLookupService;
    @MockitoBean
    private WalletSecurityService walletSecurityService;

    private final SystemRule feeRule = new SystemRule();
    private final UUID classUuid = UUID.randomUUID();
    private final UUID courseUuid = UUID.randomUUID();
    private final UUID instructorUuid = UUID.randomUUID();
    private UUID creator;
    private UUID instructor;
    private String orderId;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        when(currencyService.resolveCurrencyOrDefault(any()))
                .thenReturn(new PlatformCurrency(KES, 404, "Kenyan Shilling", "KES", 2, true, true));
        feeRule.setUuid(UUID.randomUUID());
        feeRule.setKey("platform-fee-default");
        when(ruleEvaluationService.resolvePlatformFeeMatch(any()))
                .thenReturn(Optional.of(new RuleEvaluationService.RuleMatch<>(feeRule, TEN_PERCENT, RuleCategory.PLATFORM_FEE)));
        creator = user("Wanjiru");
        instructor = user("Kamau");
        orderId = UUID.randomUUID().toString();
        when(classDefinitionLookupService.findByUuid(classUuid)).thenReturn(Optional.of(new ClassDefinitionSnapshot(
                classUuid, courseUuid, null, "Flower Gardening", null, null, null, RateBasis.PER_HOUR,
                null, null, null, null, null, null, null)));
        when(courseInfoService.getRevenueShare(courseUuid))
                .thenReturn(Optional.of(new RevenueShare(new BigDecimal("30.00"), new BigDecimal("70.00"))));
        when(courseInfoService.getCourseCreatorUserUuid(courseUuid)).thenReturn(Optional.of(creator));
        when(classDefinitionLookupService.findHiringOrganisationUuid(classUuid)).thenReturn(Optional.empty());
        when(classDefinitionLookupService.findDefaultInstructorUuid(classUuid)).thenReturn(Optional.of(instructorUuid));
        when(instructorLookupService.getInstructorUserUuid(instructorUuid)).thenReturn(Optional.of(instructor));
        mvc = MockMvcBuilders.standaloneSetup(
                        new WalletController(walletService, walletSecurityService, saleSplits),
                        new OrganisationWalletController(walletService, walletSecurityService, saleSplits),
                        new AdminSaleSplitController(saleSplits))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("an instructor's class: one split on the purchase line, matching both wallet credits")
    void instructorClass() throws Exception {
        String line = capture("50000.00");

        CommercePurchaseItem item = item(line);
        assertThat(item.getCreatorPercentage()).isEqualByComparingTo("30");
        assertThat(item.getCreatorOwnerType()).isEqualTo(SalePartyType.USER);
        assertThat(item.getCreatorOwnerUuid()).isEqualTo(creator);
        assertThat(item.getCreatorStatus()).isEqualTo(SaleLegStatus.PAID);
        assertThat(item.getCreatorGross()).isEqualByComparingTo("15000.00");
        assertThat(item.getCreatorFee()).isEqualByComparingTo("1500.00");
        assertThat(item.getCreatorCreditedAmount()).isEqualByComparingTo("13500.00");
        assertThat(item.getDeliveryKind()).isEqualTo(SaleDeliveryKind.INSTRUCTOR);
        assertThat(item.getDeliveryOwnerUuid()).isEqualTo(instructor);
        assertThat(item.getDeliveryGross()).isEqualByComparingTo("35000.00");
        assertThat(item.getDeliveryFee()).isEqualByComparingTo("3500.00");
        assertThat(item.getDeliveryNet()).isEqualByComparingTo("31500.00");
        assertThat(item.getPlatformFeeAmount()).isEqualByComparingTo("5000.00");
        assertThat(item.getRetainedAmount()).isEqualByComparingTo("0");
        assertThat(item.getSplitReconciles()).isTrue();
        assertThat(item.isSplitBackfilled()).isFalse();
        assertThat(item.getSettledAt()).isNotNull();
        // Fee rule snapshot as it stood at order time.
        assertThat(item.getFeeRuleUuid()).isEqualTo(feeRule.getUuid());
        assertThat(item.getFeeRuleKey()).isEqualTo("platform-fee-default");
        assertThat(item.getFeeMode()).isEqualTo(SaleFeeMode.PERCENTAGE);
        assertThat(item.getFeeRate()).isEqualByComparingTo("10");
        assertThat(item.getFeeWaived()).isFalse();
        assertThat(item.getFeeEvaluatedAt()).isEqualTo(ORDERED_AT);

        assertThat(walletService.balanceOf(WalletOwner.user(creator), KES).balanceAmount()).isEqualByComparingTo("13500.00");
        assertThat(walletService.balanceOf(WalletOwner.user(instructor), KES).balanceAmount())
                .isEqualByComparingTo("31500.00");

        SaleSplitDTO split = saleSplits.salesOf(WalletOwner.user(creator), PageRequest.of(0, 20)).getContent().get(0);
        assertThat(split.uuid()).isEqualTo(item.getUuid());
        assertReconciles(split);
        assertThat(split.yourRole()).isEqualTo(SaleSplitRole.COURSE_CREATOR);
        assertThat(split.yourNet()).isEqualByComparingTo("13500.00");
        assertThat(split.delivery().name()).isEqualTo("Kamau Tester");
        assertThat(split.transactions()).hasSize(2);

        mvc.perform(get("/api/v1/wallets/{userUuid}/sales", creator))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].uuid").value(item.getUuid().toString()))
                .andExpect(jsonPath("$.data.content[0].order_id").value(orderId))
                .andExpect(jsonPath("$.data.content[0].order_number").value("ORD-1"))
                .andExpect(jsonPath("$.data.content[0].title").value("Flower Gardening"))
                .andExpect(jsonPath("$.data.content[0].line_gross").value(50000.0))
                .andExpect(jsonPath("$.data.content[0].creator_percentage").value(30))
                .andExpect(jsonPath("$.data.content[0].your_role").value("COURSE_CREATOR"))
                .andExpect(jsonPath("$.data.content[0].your_gross").value(15000.0))
                .andExpect(jsonPath("$.data.content[0].your_fee").value(1500.0))
                .andExpect(jsonPath("$.data.content[0].your_net").value(13500.0))
                .andExpect(jsonPath("$.data.content[0].creator.role").value("COURSE_CREATOR"))
                .andExpect(jsonPath("$.data.content[0].creator.name").value("Wanjiru Tester"))
                .andExpect(jsonPath("$.data.content[0].creator.owner_uuid").value(creator.toString()))
                .andExpect(jsonPath("$.data.content[0].delivery.role").value("INSTRUCTOR"))
                .andExpect(jsonPath("$.data.content[0].delivery.name").value("Kamau Tester"))
                .andExpect(jsonPath("$.data.content[0].delivery.owner_uuid").value(nullValue()))
                .andExpect(jsonPath("$.data.content[0].delivery.net").value(31500.0))
                .andExpect(jsonPath("$.data.content[0].platform_fee_total").value(5000.0))
                .andExpect(jsonPath("$.data.content[0].retained").value(0.0))
                .andExpect(jsonPath("$.data.content[0].reconciles").value(true))
                .andExpect(jsonPath("$.data.content[0].fee_rule.description").value("10% of each share"))
                .andExpect(jsonPath("$.data.content[0].fee_rule.rule_uuid").value(feeRule.getUuid().toString()))
                .andExpect(jsonPath("$.data.content[0].transactions", hasSize(2)))
                .andExpect(jsonPath("$.data.content[0].transactions[0].role").value("COURSE_CREATOR"))
                // Names only: no email or phone of either party.
                .andExpect(content().string(not(containsString("@test.local"))));

        mvc.perform(get("/api/v1/wallets/{userUuid}/sales/{splitUuid}", instructor, item.getUuid()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.your_role").value("INSTRUCTOR"))
                .andExpect(jsonPath("$.data.your_net").value(31500.0))
                .andExpect(jsonPath("$.data.creator.name").value("Wanjiru Tester"))
                .andExpect(jsonPath("$.data.creator.owner_uuid").value(nullValue()));

        // Each SALE and PLATFORM_FEE row opens its split.
        mvc.perform(get("/api/v1/wallets/{userUuid}/transactions", instructor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.content[0].transaction_type").value("PLATFORM_FEE"))
                .andExpect(jsonPath("$.data.content[0].sale_split_uuid").value(item.getUuid().toString()))
                .andExpect(jsonPath("$.data.content[1].transaction_type").value("SALE"))
                .andExpect(jsonPath("$.data.content[1].sale_split_uuid").value(item.getUuid().toString()));
    }

    @Test
    @DisplayName("a job class: the hiring organisation is the delivery party and sees the creator by name")
    void jobClass() throws Exception {
        UUID organisation = UUID.randomUUID();
        when(classDefinitionLookupService.findHiringOrganisationUuid(classUuid)).thenReturn(Optional.of(organisation));
        when(organisationLookupService.findOrganisationNames(anyCollection()))
                .thenReturn(Map.of(organisation, "Green Thumb Academy"));

        String line = capture("50000.00");

        CommercePurchaseItem item = item(line);
        assertThat(item.getDeliveryKind()).isEqualTo(SaleDeliveryKind.ORGANISATION);
        assertThat(item.getDeliveryOwnerType()).isEqualTo(SalePartyType.ORGANISATION);
        assertThat(item.getDeliveryOwnerUuid()).isEqualTo(organisation);
        assertThat(item.getDeliveryNet()).isEqualByComparingTo("31500.00");
        assertThat(walletService.balanceOf(WalletOwner.organisation(organisation), KES).balanceAmount())
                .isEqualByComparingTo("31500.00");
        assertThat(saleSplits.salesOf(WalletOwner.user(instructor), PageRequest.of(0, 20)).getContent()).isEmpty();
        // The organisation keeps its one purse; the creator's leg is in the creator's COURSE_CREATOR purse.
        assertThat(walletService.pursesOf(WalletOwner.organisation(organisation), KES))
                .extracting(WalletPurseBalance::purse).containsExactly(WalletPurse.EARNINGS);
        assertThat(walletService.balanceOf(WalletOwner.user(creator), WalletPurse.COURSE_CREATOR, KES).balanceAmount())
                .isEqualByComparingTo("13500.00");
        mvc.perform(get("/api/v1/organisations/{organisationUuid}/wallet", organisation))
                .andExpect(jsonPath("$.data.balance_amount").value(31500.0))
                .andExpect(jsonPath("$.data.purses", hasSize(1)))
                .andExpect(jsonPath("$.data.purses[0].purse").value("EARNINGS"))
                .andExpect(jsonPath("$.data.purses[0].balance_amount").value(31500.0));

        mvc.perform(get("/api/v1/organisations/{organisationUuid}/wallet/sales", organisation))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].your_role").value("ORGANISATION"))
                .andExpect(jsonPath("$.data.content[0].your_purse").value("EARNINGS"))
                .andExpect(jsonPath("$.data.content[0].your_net").value(31500.0))
                .andExpect(jsonPath("$.data.content[0].delivery.name").value("Green Thumb Academy"))
                .andExpect(jsonPath("$.data.content[0].creator.name").value("Wanjiru Tester"))
                .andExpect(jsonPath("$.data.content[0].creator.owner_uuid").value(nullValue()))
                .andExpect(content().string(not(containsString("@test.local"))));
        mvc.perform(get("/api/v1/organisations/{organisationUuid}/wallet/sales/{splitUuid}", organisation, item.getUuid()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.delivery.role").value("ORGANISATION"));
        mvc.perform(get("/api/v1/organisations/{organisationUuid}/wallet/transactions", organisation))
                .andExpect(jsonPath("$.data.content[1].sale_split_uuid").value(item.getUuid().toString()));
    }

    @Test
    @DisplayName("one person who created the course and teaches the class has two purses, each net of its own fee")
    void creatorWhoTeachesKeepsTwoPurses() throws Exception {
        when(instructorLookupService.getInstructorUserUuid(instructorUuid)).thenReturn(Optional.of(creator));

        String line = capture("50000.00");
        UUID splitUuid = item(line).getUuid();
        WalletOwner person = WalletOwner.user(creator);

        assertThat(walletService.balanceOf(person, WalletPurse.COURSE_CREATOR, KES).balanceAmount())
                .isEqualByComparingTo("13500.00");
        assertThat(walletService.balanceOf(person, WalletPurse.INSTRUCTOR, KES).balanceAmount())
                .isEqualByComparingTo("31500.00");
        assertThat(walletService.balanceOf(person, WalletPurse.STUDENT, KES).balanceAmount()).isEqualByComparingTo("0");
        assertThat(walletService.balanceOf(person, KES).balanceAmount()).isEqualByComparingTo("45000.00");
        // Each purse's rows run their own balance: share, then the fee on that share.
        assertThat(jdbc.queryForList("select w.purse || ' ' || t.transaction_type || ' ' || t.balance_after::numeric(18,2) "
                        + "from user_wallet_transactions t join user_wallets w on w.id = t.wallet_id "
                        + "where w.user_uuid = ? order by w.purse, t.id", String.class, creator))
                .containsExactly("COURSE_CREATOR SALE 15000.00", "COURSE_CREATOR PLATFORM_FEE 13500.00",
                        "INSTRUCTOR SALE 35000.00", "INSTRUCTOR PLATFORM_FEE 31500.00");

        mvc.perform(get("/api/v1/wallets/{userUuid}", creator))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.balance_amount").value(45000.0))
                .andExpect(jsonPath("$.data.account_balance").value(45000.0))
                .andExpect(jsonPath("$.data.purse").value(nullValue()))
                .andExpect(jsonPath("$.data.purses", hasSize(3)))
                .andExpect(jsonPath("$.data.purses[0].purse").value("COURSE_CREATOR"))
                .andExpect(jsonPath("$.data.purses[0].balance_amount").value(13500.0))
                .andExpect(jsonPath("$.data.purses[0].available_amount").value(13500.0))
                .andExpect(jsonPath("$.data.purses[0].held_amount").value(0))
                .andExpect(jsonPath("$.data.purses[0].platform_fees_total").value(1500.0))
                .andExpect(jsonPath("$.data.purses[1].purse").value("INSTRUCTOR"))
                .andExpect(jsonPath("$.data.purses[1].account_balance").value(31500.0))
                .andExpect(jsonPath("$.data.purses[1].shares_total").value(35000.0))
                .andExpect(jsonPath("$.data.purses[2].purse").value("STUDENT"))
                .andExpect(jsonPath("$.data.purses[2].wallet_uuid").value(nullValue()))
                .andExpect(jsonPath("$.data.purses[2].balance_amount").value(0));
        mvc.perform(get("/api/v1/wallets/{userUuid}/transactions", creator).param("purse", "INSTRUCTOR"))
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.content[0].purse").value("INSTRUCTOR"))
                .andExpect(jsonPath("$.data.content[1].purse").value("INSTRUCTOR"));
        mvc.perform(get("/api/v1/wallets/{userUuid}/statements", creator).param("purse", "COURSE_CREATOR"))
                .andExpect(jsonPath("$.data[0].closing_balance").value(13500.0));

        // The sales read through a purse names that purse's leg only.
        mvc.perform(get("/api/v1/wallets/{userUuid}/sales", creator).param("purse", "INSTRUCTOR"))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].your_role").value("INSTRUCTOR"))
                .andExpect(jsonPath("$.data.content[0].your_purse").value("INSTRUCTOR"))
                .andExpect(jsonPath("$.data.content[0].your_net").value(31500.0))
                .andExpect(jsonPath("$.data.content[0].transactions", hasSize(2)))
                .andExpect(jsonPath("$.data.content[0].transactions[0].purse").value("INSTRUCTOR"));
        mvc.perform(get("/api/v1/wallets/{userUuid}/sales/{splitUuid}", creator, splitUuid)
                        .param("purse", "COURSE_CREATOR"))
                .andExpect(jsonPath("$.data.your_role").value("COURSE_CREATOR"))
                .andExpect(jsonPath("$.data.your_purse").value("COURSE_CREATOR"))
                .andExpect(jsonPath("$.data.your_net").value(13500.0));
        mvc.perform(get("/api/v1/wallets/{userUuid}/sales", creator))
                .andExpect(jsonPath("$.data.content[0].your_purse").value(nullValue()))
                .andExpect(jsonPath("$.data.content[0].your_net").value(45000.0))
                .andExpect(jsonPath("$.data.content[0].transactions", hasSize(4)));
        assertThat(saleSplits.salesOf(person, WalletPurse.STUDENT, PageRequest.of(0, 20)).getContent()).isEmpty();
        assertThat(reconciliation.runOnce().isClean()).isTrue();
    }

    @Test
    @DisplayName("a reversed share comes off the purse it was paid into, net of its fee, and the ledger follows")
    void reversalDebitsThePurseOfTheCredit() {
        when(instructorLookupService.getInstructorUserUuid(instructorUuid)).thenReturn(Optional.of(creator));
        String line = capture("50000.00");
        WalletOwner person = WalletOwner.user(creator);

        walletService.reverseSaleShare(orderId + ":" + line + ":creator", "Refunded to the learner");

        assertThat(walletService.balanceOf(person, WalletPurse.COURSE_CREATOR, KES).balanceAmount())
                .isEqualByComparingTo("0");
        assertThat(walletService.balanceOf(person, WalletPurse.INSTRUCTOR, KES).balanceAmount())
                .isEqualByComparingTo("31500.00");
        assertThat(walletService.reverseSaleShare(orderId + ":" + line + ":creator", "Again")).isNull();
        assertThat(jdbc.queryForObject("select w.purse from user_wallet_transactions t join user_wallets w "
                + "on w.id = t.wallet_id where t.transaction_type = 'SALE_REVERSAL' and t.reference = ?", String.class,
                orderId + ":" + line + ":creator:reversal")).isEqualTo("COURSE_CREATOR");
        assertThat(reconciliation.runOnce().isClean()).isTrue();
    }

    @Test
    @DisplayName("a retried settlement leaves one split and one credit per leg, dated to the first settlement")
    void retryDoesNotDuplicate() {
        String line = capture("10000.00");
        OffsetDateTime firstSettledAt = item(line).getSettledAt();

        listener.handleOrderCompleted(new OrderCompletedEvent(order(line, "10000.00"), null));

        assertThat(jdbc.queryForObject("select count(*) from user_wallet_transactions where reference like ?",
                Long.class, orderId + ":" + line + ":%")).isEqualTo(4);
        assertThat(walletService.balanceOf(WalletOwner.user(creator), KES).balanceAmount()).isEqualByComparingTo("2700.00");
        assertThat(saleSplits.splitsOfOrder(orderId)).hasSize(1);
        assertThat(item(line).getSettledAt()).isEqualTo(firstSettledAt);
        assertReconciles(saleSplits.splitsOfOrder(orderId).get(0));
    }

    @Test
    @DisplayName("a leg with nobody to pay is retained, says why, and still reconciles")
    void unpaidLegIsRetained() {
        when(classDefinitionLookupService.findDefaultInstructorUuid(classUuid)).thenReturn(Optional.empty());

        String line = capture("50000.00");

        SaleSplitDTO split = saleSplits.salesOf(WalletOwner.user(creator), PageRequest.of(0, 20)).getContent().get(0);
        assertThat(split.delivery().status()).isEqualTo(SaleLegStatus.NO_EARNER);
        assertThat(split.delivery().net()).isEqualByComparingTo("0");
        assertThat(split.retained()).isEqualByComparingTo("35000.00");
        assertThat(split.platformFeeTotal()).isEqualByComparingTo("1500.00");
        assertReconciles(split);
        assertThat(item(line).getSplitReconciles()).isTrue();
    }

    @Test
    @DisplayName("an earner reads only splits they were paid on")
    void earnersSeeOnlyTheirOwn() {
        capture("1000.00");
        UUID stranger = user("Otieno");
        UUID splitUuid = saleSplits.splitsOfOrder(orderId).get(0).uuid();

        assertThat(saleSplits.salesOf(WalletOwner.user(stranger), PageRequest.of(0, 20)).getContent()).isEmpty();
        assertThatThrownBy(() -> saleSplits.saleOf(WalletOwner.user(stranger), splitUuid))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> saleSplits.saleOf(WalletOwner.organisation(creator), splitUuid))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("admins see every split of an order, both parties identified, with all four wallet rows")
    void adminRead() throws Exception {
        String line = capture("50000.00");
        UUID splitUuid = item(line).getUuid();

        mvc.perform(get("/api/v1/admin/sales/orders/{orderId}/splits", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].your_role").value(nullValue()))
                .andExpect(jsonPath("$.data[0].creator.owner_uuid").value(creator.toString()))
                .andExpect(jsonPath("$.data[0].delivery.owner_uuid").value(instructor.toString()))
                .andExpect(jsonPath("$.data[0].transactions", hasSize(4)))
                .andExpect(jsonPath("$.data[0].platform_fee_total").value(5000.0));
        mvc.perform(get("/api/v1/admin/sales/splits/{splitUuid}", splitUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.delivery.name").value("Kamau Tester"));
    }

    @Test
    @DisplayName("the backfill rebuilds legs of an older line from its wallet rows, without percentage or rule")
    void backfill() throws Exception {
        String line = "line-" + UUID.randomUUID();
        purchase(line, "50000.00");
        walletService.settleSaleShare(WalletOwner.user(creator), WalletPurse.creatorLegOf(WalletOwner.user(creator).type()), new BigDecimal("15000.00"), new BigDecimal("1500.00"),
                KES, orderId + ":" + line + ":creator", "Class sale share - Flower Gardening");
        walletService.settleSaleShare(WalletOwner.user(instructor), WalletPurse.deliveryLegOf(WalletOwner.user(instructor).type()), new BigDecimal("35000.00"),
                new BigDecimal("3500.00"), KES, orderId + ":" + line + ":delivery", "Class sale share - Flower Gardening");
        jdbc.update("update commerce_purchase_item set platform_fee_amount = 5000, credited_amount = 45000, "
                + "retained_amount = 0, creator_credited_amount = 13500 where line_item_id = ?", line);

        String migration = Files.readString(Path.of(
                "src/main/resources/db/migration/V202611042151__record_sale_split_on_purchase_lines.sql"));
        String backfill = migration.substring(migration.indexOf("WITH legs AS"));
        jdbc.execute(backfill);
        jdbc.execute(backfill);

        CommercePurchaseItem item = item(line);
        assertThat(item.isSplitBackfilled()).isTrue();
        assertThat(item.getSplitReconciles()).isTrue();
        assertThat(item.getCreatorPercentage()).isNull();
        assertThat(item.getFeeRuleUuid()).isNull();
        assertThat(item.getCreatorGross()).isEqualByComparingTo("15000.00");
        assertThat(item.getCreatorFee()).isEqualByComparingTo("1500.00");
        assertThat(item.getDeliveryKind()).isEqualTo(SaleDeliveryKind.INSTRUCTOR);
        assertThat(item.getDeliveryNet()).isEqualByComparingTo("31500.00");
        assertThat(item.getSettledAt()).isNotNull();

        SaleSplitDTO split = saleSplits.saleOf(WalletOwner.user(instructor), item.getUuid());
        assertReconciles(split);
        assertThat(split.backfilled()).isTrue();
        assertThat(split.feeRule().recorded()).isFalse();
        mvc.perform(get("/api/v1/wallets/{userUuid}/transactions", instructor))
                .andExpect(jsonPath("$.data.content[0].sale_split_uuid").value(item.getUuid().toString()));
    }

    private static void assertReconciles(SaleSplitDTO split) {
        assertThat(split.reconciles()).isTrue();
        assertThat(split.creator().net().add(split.delivery().net()).add(split.platformFeeTotal()).add(split.retained()))
                .isEqualByComparingTo(split.lineGross());
    }

    private String capture(String total) {
        String line = "line-" + UUID.randomUUID();
        purchase(line, total);
        listener.handleOrderCompleted(new OrderCompletedEvent(order(line, total), null));
        return line;
    }

    private OrderResponse order(String line, String total) {
        CartItemResponse item = CartItemResponse.builder().id(line).total(new BigDecimal(total))
                .metadata(Map.of("class_definition_uuid", classUuid.toString(), "course_uuid", courseUuid.toString()))
                .build();
        return OrderResponse.builder().id(orderId).displayId("ORD-1").paymentStatus("CAPTURED").currencyCode(KES)
                .total(new BigDecimal(total)).createdAt(ORDERED_AT).items(List.of(item)).build();
    }

    private void purchase(String line, String total) {
        CommercePurchase purchase = new CommercePurchase();
        purchase.setOrderId(orderId);
        purchase.setOrderNumber("ORD-1");
        purchase.setPaymentStatus("CAPTURED");
        purchase.setOrderCurrencyCode(KES);
        purchase.setOrderTotalAmount(new BigDecimal(total));
        purchase.setOrderCreatedAt(ORDERED_AT);
        CommercePurchaseItem item = new CommercePurchaseItem();
        item.setPurchase(purchase);
        item.setLineItemId(line);
        item.setTitle("Flower Gardening");
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal(total));
        item.setSubtotal(new BigDecimal(total));
        item.setTotal(new BigDecimal(total));
        item.setScope(PurchaseScope.CLASS);
        item.setClassDefinitionUuid(null);
        purchase.getItems().add(item);
        purchaseRepository.saveAndFlush(purchase);
    }

    private CommercePurchaseItem item(String line) {
        return purchaseItemRepository.findByOrderIdAndLineItemId(orderId, line).orElseThrow();
    }

    private UUID user(String name) {
        UUID uuid = UUID.randomUUID();
        String userNo = String.valueOf(USER_NO.incrementAndGet());
        jdbc.update("insert into users (uuid, first_name, last_name, email, user_no, created_by) "
                        + "values (?, ?, 'Tester', ?, ?, 'TEST')",
                uuid, name, name + "-" + userNo + "@test.local", userNo);
        return uuid;
    }
}
