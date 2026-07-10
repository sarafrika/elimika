package apps.sarafrika.elimika.wallet.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import apps.sarafrika.elimika.shared.currency.model.PlatformCurrency;
import apps.sarafrika.elimika.shared.currency.service.CurrencyService;
import apps.sarafrika.elimika.wallet.entity.UserWallet;
import apps.sarafrika.elimika.wallet.entity.UserWalletTransaction;
import apps.sarafrika.elimika.wallet.repository.UserWalletRepository;
import apps.sarafrika.elimika.wallet.repository.UserWalletTransactionRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplIdempotencyTest {

    @Mock
    private UserWalletRepository userWalletRepository;
    @Mock
    private UserWalletTransactionRepository transactionRepository;
    @Mock
    private CurrencyService currencyService;

    @InjectMocks
    private WalletServiceImpl walletService;

    private final UUID userUuid = UUID.randomUUID();
    private final String reference = "order-1:line-1";

    @Test
    void creditsWhenReferenceNotSeen() {
        PlatformCurrency kes = new PlatformCurrency();
        kes.setCode("KES");
        when(currencyService.resolveCurrencyOrDefault(any())).thenReturn(kes);
        when(transactionRepository.existsByReference(reference)).thenReturn(false);
        UserWallet wallet = new UserWallet();
        wallet.setUserUuid(userUuid);
        wallet.setCurrencyCode("KES");
        wallet.setBalanceAmount(BigDecimal.ZERO);
        when(userWalletRepository.findLockedByUserUuidAndCurrencyCode(userUuid, "KES"))
                .thenReturn(Optional.of(wallet));

        boolean credited = walletService.creditSaleIdempotent(
                userUuid, new BigDecimal("700.00"), "KES", reference, "desc");

        assertThat(credited).isTrue();
        verify(transactionRepository).save(any(UserWalletTransaction.class));
        assertThat(wallet.getBalanceAmount()).isEqualByComparingTo("700.00");
    }

    @Test
    void skipsWhenReferenceAlreadyExists() {
        when(transactionRepository.existsByReference(reference)).thenReturn(true);

        boolean credited = walletService.creditSaleIdempotent(
                userUuid, new BigDecimal("700.00"), "KES", reference, "desc");

        assertThat(credited).isFalse();
        verify(transactionRepository, never()).save(any(UserWalletTransaction.class));
        verify(userWalletRepository, never()).save(any(UserWallet.class));
    }
}
