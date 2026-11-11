package apps.sarafrika.elimika.wallet.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import apps.sarafrika.elimika.shared.spi.payout.MobileMoneyPayoutGateway;
import apps.sarafrika.elimika.wallet.enums.WithdrawalStatus;
import apps.sarafrika.elimika.wallet.repository.WithdrawalRepository;
import apps.sarafrika.elimika.wallet.service.PayoutBatchService;
import apps.sarafrika.elimika.wallet.service.PayoutSettingsService;
import apps.sarafrika.elimika.wallet.service.WithdrawalService;
import apps.sarafrika.elimika.wallet.service.WithdrawalWalletChecks;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class AdminPayoutControllerTest {

    @Mock
    private WithdrawalService withdrawalService;
    @Mock
    private WithdrawalRepository withdrawalRepository;
    @Mock
    private PayoutSettingsService settingsService;
    @Mock
    private PayoutBatchService batchService;
    @Mock
    private MobileMoneyPayoutGateway gateway;
    @Mock
    private WithdrawalWalletChecks walletChecks;

    @InjectMocks
    private AdminPayoutController controller;

    @Test
    void summaryUsesTheWorkingAccountAsPayoutFloat() {
        OffsetDateTime reportedAt = OffsetDateTime.now(ZoneOffset.UTC);
        when(gateway.currentFloat()).thenReturn(Optional.of(new MobileMoneyPayoutGateway.PayoutFloat(
                BigDecimal.ZERO, new BigDecimal("500"), reportedAt)));
        when(gateway.isConfigured()).thenReturn(true);
        when(withdrawalRepository.countByStatus(any(WithdrawalStatus.class))).thenReturn(0L);
        when(withdrawalRepository.sumNetCompletedSince(any(), any())).thenReturn(BigDecimal.ZERO);
        when(withdrawalService.queue(anyList(), any(Pageable.class))).thenReturn(Page.empty());

        var response = controller.summary().getBody();

        assertThat(response).isNotNull();
        assertThat(response.data().floatAmount()).isEqualByComparingTo("500");
        assertThat(response.data().floatReportedAt()).isEqualTo(reportedAt);
    }
}
