package apps.sarafrika.elimika.wallet.controller;

import apps.sarafrika.elimika.wallet.entity.UserWallet;
import apps.sarafrika.elimika.wallet.entity.UserWalletTransaction;
import apps.sarafrika.elimika.wallet.enums.WalletTransactionType;
import apps.sarafrika.elimika.wallet.service.WalletBalanceSummary;
import apps.sarafrika.elimika.wallet.service.WalletOwner;
import apps.sarafrika.elimika.wallet.service.WalletService;
import apps.sarafrika.elimika.wallet.service.WalletTotals;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OrganisationWalletControllerTest {

    @Mock
    private WalletService walletService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new OrganisationWalletController(walletService))
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

        mockMvc.perform(get("/api/v1/organisations/{organisationUuid}/wallet", organisationUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currency_code").value("KES"))
                .andExpect(jsonPath("$.data.balance_amount").value(31500.00))
                .andExpect(jsonPath("$.data.shares_total").value(35000.00))
                .andExpect(jsonPath("$.data.platform_fees_total").value(3500.00))
                .andExpect(jsonPath("$.data.sales_count").value(1));
    }

    @Test
    void returnsTheStatementWithTheShareAndItsFee() throws Exception {
        UUID organisationUuid = UUID.randomUUID();
        when(walletService.statementOf(eq(WalletOwner.organisation(organisationUuid)), eq(null), any()))
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
