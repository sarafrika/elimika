package apps.sarafrika.elimika.classes.internal;

import apps.sarafrika.elimika.classes.model.ClassDefinition;
import apps.sarafrika.elimika.shared.enums.ClassVisibility;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import apps.sarafrika.elimika.timetabling.spi.TimetableService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassListingVisibilityTest {

    @Mock
    private DomainSecurityService domainSecurityService;
    @Mock
    private UserLookupService userLookupService;
    @Mock
    private ObjectProvider<TimetableService> timetableServiceProvider;

    @Test
    void outsidersSeeOnlyActivePublicClasses() {
        ClassListingVisibility.Scope scope = new ClassListingVisibility.Scope(false, Set.of(), null, Set.of());

        assertThat(scope.admits(classOf(UUID.randomUUID(), ClassVisibility.PUBLIC, true))).isTrue();
        assertThat(scope.admits(classOf(UUID.randomUUID(), ClassVisibility.PRIVATE, true))).isFalse();
        assertThat(scope.admits(classOf(UUID.randomUUID(), ClassVisibility.PUBLIC, false))).isFalse();
    }

    @Test
    void staffTeachersAndLearnersSeeTheirOwnHiddenClasses() {
        UUID organisationUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();
        ClassDefinition orgClass = classOf(organisationUuid, ClassVisibility.PRIVATE, false);
        ClassDefinition taught = classOf(UUID.randomUUID(), ClassVisibility.PRIVATE, false);
        taught.setDefaultInstructorUuid(instructorUuid);
        ClassDefinition studied = classOf(UUID.randomUUID(), ClassVisibility.PRIVATE, true);
        studied.setUuid(UUID.randomUUID());

        ClassListingVisibility.Scope scope = new ClassListingVisibility.Scope(
                false, Set.of(organisationUuid), instructorUuid, Set.of(studied.getUuid()));

        assertThat(scope.admits(orgClass)).isTrue();
        assertThat(scope.admits(taught)).isTrue();
        assertThat(scope.admits(studied)).isTrue();
        assertThat(scope.admits(classOf(UUID.randomUUID(), ClassVisibility.PRIVATE, true))).isFalse();
    }

    @Test
    void scopeKeepsOnlyOrganisationsTheCallerStaffs() {
        UUID userUuid = UUID.randomUUID();
        UUID staffed = UUID.randomUUID();
        UUID learnerOnly = UUID.randomUUID();
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(userUuid);
        when(userLookupService.getActiveUserOrganizations(userUuid)).thenReturn(List.of(staffed, learnerOnly));
        when(domainSecurityService.staffsOrganisation(staffed)).thenReturn(true);

        ClassListingVisibility.Scope scope = new ClassListingVisibility(
                domainSecurityService, userLookupService, timetableServiceProvider).forCurrentCaller();

        assertThat(scope.everything()).isFalse();
        assertThat(scope.staffedOrganisations()).containsExactly(staffed);
    }

    @Test
    void platformAdminsSeeEverything() {
        when(domainSecurityService.isPlatformAdmin()).thenReturn(true);

        ClassListingVisibility.Scope scope = new ClassListingVisibility(
                domainSecurityService, userLookupService, timetableServiceProvider).forCurrentCaller();

        assertThat(scope.admits(classOf(UUID.randomUUID(), ClassVisibility.PRIVATE, false))).isTrue();
    }

    private ClassDefinition classOf(UUID organisationUuid, ClassVisibility visibility, boolean active) {
        ClassDefinition definition = new ClassDefinition();
        definition.setUuid(UUID.randomUUID());
        definition.setOrganisationUuid(organisationUuid);
        definition.setDefaultInstructorUuid(UUID.randomUUID());
        definition.setClassVisibility(visibility);
        definition.setIsActive(active);
        return definition;
    }
}
