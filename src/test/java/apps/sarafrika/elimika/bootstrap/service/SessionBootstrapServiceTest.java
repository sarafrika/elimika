package apps.sarafrika.elimika.bootstrap.service;

import apps.sarafrika.elimika.bootstrap.dto.SessionBootstrapDTO;
import apps.sarafrika.elimika.coursecreator.spi.CourseCreatorLookupService;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.notifications.spi.DomainNotificationCounts;
import apps.sarafrika.elimika.notifications.spi.NotificationCountsLookupService;
import apps.sarafrika.elimika.notifications.spi.UnreadNotificationSummary;
import apps.sarafrika.elimika.student.spi.StudentLookupService;
import apps.sarafrika.elimika.tenancy.dto.UserDTO;
import apps.sarafrika.elimika.tenancy.dto.UserOrganisationAffiliationDTO;
import apps.sarafrika.elimika.tenancy.spi.UserProfileLookupService;
import apps.sarafrika.elimika.wallet.service.WalletBalanceSummary;
import apps.sarafrika.elimika.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SessionBootstrapServiceTest {

    private static final UUID USER = UUID.randomUUID();

    private final UserProfileLookupService users = Mockito.mock(UserProfileLookupService.class);
    private final StudentLookupService students = Mockito.mock(StudentLookupService.class);
    private final InstructorLookupService instructors = Mockito.mock(InstructorLookupService.class);
    private final CourseCreatorLookupService creators = Mockito.mock(CourseCreatorLookupService.class);
    private final WalletService wallets = Mockito.mock(WalletService.class);
    private final NotificationCountsLookupService notifications = Mockito.mock(NotificationCountsLookupService.class);
    private final SessionBootstrapService service =
            new SessionBootstrapService(users, students, instructors, creators, wallets, notifications);

    @BeforeEach
    void setUp() {
        when(students.findStudentUuidByUserUuid(USER)).thenReturn(Optional.empty());
        when(instructors.findInstructorUuidByUserUuid(USER)).thenReturn(Optional.empty());
        when(creators.findCourseCreatorUuidByUserUuid(USER)).thenReturn(Optional.empty());
    }

    @Test
    void composesEverySectionWithOneReadEach() {
        UUID student = UUID.randomUUID();
        UUID instructor = UUID.randomUUID();
        when(users.getUserProfile(USER)).thenReturn(user(List.of("student", "instructor"), List.of()));
        when(students.findStudentUuidByUserUuid(USER)).thenReturn(Optional.of(student));
        when(instructors.findInstructorUuidByUserUuid(USER)).thenReturn(Optional.of(instructor));
        WalletBalanceSummary wallet = new WalletBalanceSummary(UUID.randomUUID(), "KES", new BigDecimal("150.00"));
        when(wallets.getBalanceSummary(USER, null)).thenReturn(wallet);
        UnreadNotificationSummary counts = new UnreadNotificationSummary(3, 1,
                Map.of("student", new DomainNotificationCounts(2, 1)));
        when(notifications.summarizeUnread(USER, List.of("student", "instructor"))).thenReturn(counts);

        SessionBootstrapDTO result = service.bootstrap(USER);

        assertThat(result.user().uuid()).isEqualTo(USER);
        assertThat(result.profiles().studentUuid()).isEqualTo(student);
        assertThat(result.profiles().instructorUuid()).isEqualTo(instructor);
        assertThat(result.profiles().courseCreatorUuid()).isNull();
        assertThat(result.activeOrganisation()).isNull();
        assertThat(result.wallet()).isEqualTo(wallet);
        assertThat(result.notifications()).isEqualTo(counts);
        verify(notifications, times(1)).summarizeUnread(any(), anyCollection());
        verify(wallets, times(1)).getBalanceSummary(any(), any());
    }

    @Test
    void organisationUserGetsTheFirstActiveAffiliation() {
        UUID inactiveOrg = UUID.randomUUID();
        UUID activeOrg = UUID.randomUUID();
        when(users.getUserProfile(USER)).thenReturn(user(List.of("organisation_user"), List.of(
                affiliation(inactiveOrg, "Old Org", false),
                affiliation(activeOrg, "Current Org", true))));

        SessionBootstrapDTO result = service.bootstrap(USER);

        assertThat(result.activeOrganisation()).isNotNull();
        assertThat(result.activeOrganisation().organisationUuid()).isEqualTo(activeOrg);
        assertThat(result.activeOrganisation().organisationName()).isEqualTo("Current Org");
        assertThat(result.activeOrganisation().active()).isTrue();
    }

    @Test
    void nonOrganisationUserHasNoActiveOrganisationEvenWhenAffiliated() {
        when(users.getUserProfile(USER)).thenReturn(user(List.of("student"),
                List.of(affiliation(UUID.randomUUID(), "School", true))));

        assertThat(service.bootstrap(USER).activeOrganisation()).isNull();
    }

    @Test
    void failingOptionalSectionsDegradeToNull() {
        when(users.getUserProfile(USER)).thenReturn(user(List.of("student"), List.of()));
        when(wallets.getBalanceSummary(any(), any())).thenThrow(new IllegalStateException("db down"));
        when(notifications.summarizeUnread(any(), anyCollection())).thenThrow(new IllegalStateException("db down"));

        SessionBootstrapDTO result = service.bootstrap(USER);

        assertThat(result.user()).isNotNull();
        assertThat(result.wallet()).isNull();
        assertThat(result.notifications()).isNull();
    }

    static UserDTO user(List<String> domains, List<UserOrganisationAffiliationDTO> affiliations) {
        return new UserDTO(USER, "U-1", "Jane", null, "Doe", "jane@example.com", "jane", null, null,
                null, true, "kc-1", null, null, null, null, null, domains, affiliations);
    }

    static UserOrganisationAffiliationDTO affiliation(UUID organisationUuid, String name, boolean active) {
        return new UserOrganisationAffiliationDTO(organisationUuid, name, "admin", null, null,
                null, null, active, null);
    }
}
