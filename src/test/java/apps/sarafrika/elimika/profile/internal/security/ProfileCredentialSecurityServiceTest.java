package apps.sarafrika.elimika.profile.internal.security;

import apps.sarafrika.elimika.profile.spi.ProfileAccessGrant;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("One credential-read rule for every domain")
class ProfileCredentialSecurityServiceTest {

    private final DomainSecurityService security = mock(DomainSecurityService.class);
    private final ProfileAccessGrant reviewerGrant = mock(ProfileAccessGrant.class);
    private final UUID subject = UUID.randomUUID();
    private final UUID caller = UUID.randomUUID();

    private ProfileCredentialSecurityService service(ProfileAccessGrant... grants) {
        @SuppressWarnings("unchecked")
        ObjectProvider<ProfileAccessGrant> provider = mock(ObjectProvider.class);
        when(provider.stream()).thenAnswer(invocation -> Stream.of(grants));
        return new ProfileCredentialSecurityService(security, provider);
    }

    @Test
    @DisplayName("the owner and a platform admin may read")
    void ownerAndAdmin() {
        when(security.getCurrentUserUuid()).thenReturn(subject);
        assertThat(service().canReadCredentials(subject)).isTrue();

        when(security.getCurrentUserUuid()).thenReturn(caller);
        when(security.isPlatformAdmin()).thenReturn(true);
        assertThat(service().canReadCredentials(subject)).isTrue();
    }

    @Test
    @DisplayName("staff of an organisation the user belongs to may read")
    void organisationStaff() {
        when(security.getCurrentUserUuid()).thenReturn(caller);
        when(security.staffsOrganisationOf(subject)).thenReturn(true);
        assertThat(service().canReadCredentials(subject)).isTrue();
    }

    @Test
    @DisplayName("a reviewer of the user's application may read through a grant")
    void reviewerGrant() {
        when(security.getCurrentUserUuid()).thenReturn(caller);
        when(reviewerGrant.grantsCredentialRead(subject, caller)).thenReturn(true);
        assertThat(service(reviewerGrant).canReadCredentials(subject)).isTrue();
    }

    @Test
    @DisplayName("strangers, anonymous callers and a failing grant are refused")
    void everyoneElseIsRefused() {
        when(security.getCurrentUserUuid()).thenReturn(caller);
        assertThat(service(reviewerGrant).canReadCredentials(subject)).isFalse();

        ProfileAccessGrant broken = mock(ProfileAccessGrant.class);
        when(broken.grantsCredentialRead(any(), any())).thenThrow(new IllegalStateException("boom"));
        assertThat(service(broken).canReadCredentials(subject)).isFalse();

        when(security.getCurrentUserUuid()).thenReturn(null);
        assertThat(service(reviewerGrant).canReadCredentials(subject)).isFalse();
        assertThat(service().canReadCredentials(null)).isFalse();
    }

    @Test
    @DisplayName("only the owner or a platform admin may edit")
    void editIsOwnerOrAdmin() {
        when(security.getCurrentUserUuid()).thenReturn(caller);
        when(security.staffsOrganisationOf(subject)).thenReturn(true);
        assertThat(service().canEdit(subject)).isFalse();
        assertThat(service().canEdit(caller)).isTrue();
    }
}
