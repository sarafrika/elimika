package apps.sarafrika.elimika.wallet.security;

import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Guards the money endpoints on the wallet.
 * <p>
 * The hole these tests pin down is that {@code isOrganizationAdmin()} never looked at whose wallet
 * was being touched: an administrator of one organisation satisfied it against every user on the
 * platform, so they could credit an arbitrary wallet. Authority over a wallet now has to be
 * authority over its holder.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WalletSecurityServiceTest {

    private static final UUID CALLER_UUID = UUID.randomUUID();
    private static final UUID OTHER_USER_UUID = UUID.randomUUID();

    @Mock private DomainSecurityService domainSecurityService;

    private WalletSecurityService service;

    @BeforeEach
    void setUp() {
        service = new WalletSecurityService(domainSecurityService);

        when(domainSecurityService.getCurrentUserUuid()).thenReturn(CALLER_UUID);
        when(domainSecurityService.isPlatformAdmin()).thenReturn(false);
        when(domainSecurityService.administersOrganisationOf(any())).thenReturn(false);
    }

    @Test
    void theHolderKeepsAccessToTheirOwnWallet() {
        assertThat(service.canAccessWallet(CALLER_UUID)).isTrue();
        assertThat(service.canTransferFrom(CALLER_UUID)).isTrue();
    }

    @Test
    void theHolderIsAnsweredWithoutConsultingAnyRoleLookup() {
        // Self-access must never depend on the caller holding a domain anywhere.
        assertThat(service.canAccessWallet(CALLER_UUID)).isTrue();

        verify(domainSecurityService, never()).isPlatformAdmin();
        verify(domainSecurityService, never()).administersOrganisationOf(any());
    }

    @Test
    void anOrdinaryUserCannotReachSomeoneElsesWallet() {
        assertThat(service.canAccessWallet(OTHER_USER_UUID)).isFalse();
        assertThat(service.canTransferFrom(OTHER_USER_UUID)).isFalse();
        assertThat(service.canCreditWallet(OTHER_USER_UUID)).isFalse();
    }

    @Test
    void anAdminOfAnOrganisationTheHolderDoesNotBelongToIsRefused() {
        // The actual hole: administering *something* used to be enough to credit *anyone*.
        when(domainSecurityService.administersOrganisationOf(OTHER_USER_UUID)).thenReturn(false);

        assertThat(service.canCreditWallet(OTHER_USER_UUID)).isFalse();
        assertThat(service.canAccessWallet(OTHER_USER_UUID)).isFalse();
    }

    @Test
    void anAdminOfAnOrganisationTheHolderBelongsToMayReadAndCredit() {
        when(domainSecurityService.administersOrganisationOf(OTHER_USER_UUID)).thenReturn(true);

        assertThat(service.canAccessWallet(OTHER_USER_UUID)).isTrue();
        assertThat(service.canCreditWallet(OTHER_USER_UUID)).isTrue();
        assertThat(service.canTransferFrom(OTHER_USER_UUID)).isTrue();
    }

    @Test
    void aPlatformAdminMayReachAnyWalletForSupport() {
        when(domainSecurityService.isPlatformAdmin()).thenReturn(true);

        assertThat(service.canAccessWallet(OTHER_USER_UUID)).isTrue();
        assertThat(service.canCreditWallet(OTHER_USER_UUID)).isTrue();
    }

    @Test
    void beingTheHolderIsNotOnItsOwnEnoughToCredit() {
        // Reading your own balance needs nothing else; funding it does. (An org admin still reaches
        // their own wallet through administersOrganisationOf, exactly as before this change.)
        assertThat(service.canAccessWallet(CALLER_UUID)).isTrue();
        assertThat(service.canCreditWallet(CALLER_UUID)).isFalse();
    }

    @Test
    void aNullWalletHolderIsRefused() {
        when(domainSecurityService.isPlatformAdmin()).thenReturn(true);

        assertThat(service.canAccessWallet(null)).isFalse();
        assertThat(service.canCreditWallet(null)).isFalse();
        assertThat(service.canTransferFrom(null)).isFalse();
    }

    @Test
    void anUnauthenticatedCallerIsRefused() {
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(null);

        assertThat(service.canAccessWallet(CALLER_UUID)).isFalse();
        assertThat(service.canCreditWallet(CALLER_UUID)).isFalse();
    }
}
