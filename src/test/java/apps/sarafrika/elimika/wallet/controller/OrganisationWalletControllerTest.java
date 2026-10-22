package apps.sarafrika.elimika.wallet.controller;

import apps.sarafrika.elimika.wallet.entity.UserWallet;
import apps.sarafrika.elimika.wallet.entity.UserWalletTransaction;
import apps.sarafrika.elimika.wallet.enums.WalletTransactionType;
import apps.sarafrika.elimika.wallet.service.WalletBalanceSummary;
import apps.sarafrika.elimika.wallet.security.WalletSecurityService;
import apps.sarafrika.elimika.wallet.service.WalletOwner;
import apps.sarafrika.elimika.wallet.service.WalletStanding;
import apps.sarafrika.elimika.wallet.service.WalletService;
import apps.sarafrika.elimika.wallet.service.WalletTotals;
import apps.sarafrika.elimika.wallet.service.WalletTransactionFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OrganisationWalletControllerTest {

    @Mock
    private WalletService walletService;

    @Mock
    private WalletSecurityService walletSecurityService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new OrganisationWalletController(walletService, walletSecurityService))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void returnsTheOrganisationBalance() throws Exception {
        UUID organisationUuid = UUID.randomUUID();
        when(walletService.balanceOf(WalletOwner.organisation(organisationUuid), null))
                .thenReturn(new WalletBalanceSummary(UUID.randomUUID(), "KES", new BigDecimal("31500.00")));
        when(walletService.totalsOf(WalletOwner.organisation(organisationUuid), null))
                .thenReturn(new WalletTotals(new BigDecimal("35000.00"), new BigDecimal("3500.00"), 1, BigDecimal.ZERO));
        when(walletSecurityService.canWithdrawFromOrganisation(organisationUuid)).thenReturn(true);
        when(walletService.standingOf(WalletOwner.organisation(organisationUuid), null, true))
                .thenReturn(new WalletStanding(BigDecimal.ZERO, new BigDecimal("31500.00"), true));

        mockMvc.perform(get("/api/v1/organisations/{organisationUuid}/wallet", organisationUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currency_code").value("KES"))
                .andExpect(jsonPath("$.data.balance_amount").value(31500.00))
                .andExpect(jsonPath("$.data.shares_total").value(35000.00))
                .andExpect(jsonPath("$.data.platform_fees_total").value(3500.00))
                .andExpect(jsonPath("$.data.sales_count").value(1))
                .andExpect(jsonPath("$.data.held_amount").value(0))
                .andExpect(jsonPath("$.data.available_amount").value(31500.00))
                .andExpect(jsonPath("$.data.can_withdraw").value(true));
    }

    @Test
    void returnsTheStatementWithTheShareAndItsFee() throws Exception {
        UUID organisationUuid = UUID.randomUUID();
        when(walletService.statementOf(eq(WalletOwner.organisation(organisationUuid)), eq(null),
                eq(WalletTransactionFilter.all()), any()))
                .thenReturn(new PageImpl<>(List.of(
                        row(WalletTransactionType.PLATFORM_FEE, "3500.00", "Platform fee - Class sale share"),
                        row(WalletTransactionType.SALE, "35000.00", "Class sale share"))));

        mockMvc.perform(get("/api/v1/organisations/{organisationUuid}/wallet/transactions", organisationUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].transaction_type").value("PLATFORM_FEE"))
                .andExpect(jsonPath("$.data.content[0].amount").value(3500.00))
                .andExpect(jsonPath("$.data.content[1].transaction_type").value("SALE"))
                .andExpect(jsonPath("$.data.content[1].amount").value(35000.00));
    }

    @Test
    void filtersTheStatementByTypeAndDates() throws Exception {
        UUID organisationUuid = UUID.randomUUID();
        WalletTransactionFilter filter = WalletTransactionFilter.of(
                List.of(WalletTransactionType.SALE), LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));
        when(walletService.statementOf(eq(WalletOwner.organisation(organisationUuid)), eq(null), eq(filter), any()))
                .thenReturn(new PageImpl<>(List.of(row(WalletTransactionType.SALE, "35000.00", "Class sale share"))));

        mockMvc.perform(get("/api/v1/organisations/{organisationUuid}/wallet/transactions", organisationUuid)
                        .param("type", "SALE").param("from", "2026-10-01").param("to", "2026-10-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].transaction_type").value("SALE"));
    }

    @Test
    void downloadsEveryMatchingRowAsCsv() throws Exception {
        UUID organisationUuid = UUID.randomUUID();
        when(walletService.exportOf(WalletOwner.organisation(organisationUuid), null, WalletTransactionFilter.all()))
                .thenReturn(List.of(row(WalletTransactionType.SALE, "35000.00", "=HYPERLINK(\"x\")")));

        mockMvc.perform(get("/api/v1/organisations/{organisationUuid}/wallet/transactions.csv", organisationUuid))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"organisation-wallet-transactions.csv\""))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"SALE\",\"35000.00\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"'=HYPERLINK(\"\"x\"\")\"")));
    }

    @Test
    void isReadableOnlyByOrganisationManagers() {
        PreAuthorize rule = OrganisationWalletController.class.getAnnotation(PreAuthorize.class);

        assertThat(rule.value()).isEqualTo("@organisationSecurityService.canManageOrganisation(#organisationUuid)");
    }

    private static UserWalletTransaction row(WalletTransactionType type, String amount, String description) {
        UserWalletTransaction transaction = new UserWalletTransaction();
        transaction.setWallet(new UserWallet());
        transaction.setTransactionType(type);
        transaction.setAmount(new BigDecimal(amount));
        transaction.setCurrencyCode("KES");
        transaction.setBalanceBefore(BigDecimal.ZERO);
        transaction.setBalanceAfter(BigDecimal.ZERO);
        transaction.setDescription(description);
        return transaction;
    }
}
