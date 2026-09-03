package apps.sarafrika.elimika.shared.security;

import apps.sarafrika.elimika.coursecreator.spi.CourseCreatorLookupService;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import apps.sarafrika.elimika.student.spi.StudentLookupService;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the subject-scoped predicates: {@code administersOrganisationOf}, the org-scoped counterpart to
 * {@code isOrganizationAdmin()}, and {@code isCourseCreatorWithUuid}, which ties a caller to the one
 * course creator profile they own.
 * <p>
 * {@code isOrganizationAdmin()} answers a question about the caller alone, so it is satisfied by an
 * administrator of any organisation regardless of who the request is about. Anywhere the subject is
 * another user — a wallet, most obviously — that is not the question worth asking.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DomainSecurityServiceTest {

    private static final String KEYCLOAK_ID = "keycloak-subject";
    private static final UUID CALLER_UUID = UUID.randomUUID();
    private static final UUID TARGET_UUID = UUID.randomUUID();
    private static final UUID SHARED_ORG_UUID = UUID.randomUUID();
    private static final UUID OTHER_ORG_UUID = UUID.randomUUID();
    private static final UUID COURSE_CREATOR_UUID = UUID.randomUUID();
    private static final UUID OTHER_COURSE_CREATOR_UUID = UUID.randomUUID();

    @Mock private UserLookupService userLookupService;
    @Mock private StudentLookupService studentLookupService;
    @Mock private InstructorLookupService instructorLookupService;
    @Mock private CourseCreatorLookupService courseCreatorLookupService;

    private DomainSecurityService service;

    @BeforeEach
    void setUp() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));

        service = new DomainSecurityService(
                userLookupService, studentLookupService, instructorLookupService, courseCreatorLookupService,
                new RequestScopedCache());

        authenticateAsJwtUser();
        when(userLookupService.findUserUuidByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(CALLER_UUID));
        when(userLookupService.getUserOrganizations(TARGET_UUID)).thenReturn(List.of(SHARED_ORG_UUID));
        when(userLookupService.userBelongsToOrganizationWithDomain(any(), any(), any())).thenReturn(false);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void anAdminOfAnOrganisationTheTargetBelongsToReachesThem() {
        when(userLookupService.userBelongsToOrganizationWithDomain(CALLER_UUID, SHARED_ORG_UUID, UserDomain.admin))
                .thenReturn(true);

        assertThat(service.administersOrganisationOf(TARGET_UUID)).isTrue();
    }

    @Test
    void anAdminOfSomeOtherOrganisationDoesNot() {
        when(userLookupService.userBelongsToOrganizationWithDomain(CALLER_UUID, OTHER_ORG_UUID, UserDomain.admin))
                .thenReturn(true);

        assertThat(service.administersOrganisationOf(TARGET_UUID)).isFalse();
    }

    @Test
    void merelySharingAnOrganisationIsNotEnough() {
        // Membership without the admin domain in that organisation grants nothing.
        when(userLookupService.userBelongsToOrganizationWithDomain(
                CALLER_UUID, SHARED_ORG_UUID, UserDomain.organisation_user)).thenReturn(true);

        assertThat(service.administersOrganisationOf(TARGET_UUID)).isFalse();
    }

    @Test
    void aTargetInNoOrganisationIsOutOfEveryonesReach() {
        when(userLookupService.getUserOrganizations(TARGET_UUID)).thenReturn(List.of());

        assertThat(service.administersOrganisationOf(TARGET_UUID)).isFalse();
    }

    @Test
    void aNullTargetIsRefusedWithoutConsultingAnyone() {
        assertThat(service.administersOrganisationOf(null)).isFalse();

        verify(userLookupService, times(0)).getUserOrganizations(any());
    }

    @Test
    void anUnauthenticatedCallerIsRefused() {
        SecurityContextHolder.clearContext();

        assertThat(service.administersOrganisationOf(TARGET_UUID)).isFalse();
    }

    @Test
    void aLookupFailureDeniesRatherThanGrants() {
        when(userLookupService.getUserOrganizations(TARGET_UUID)).thenThrow(new IllegalStateException("boom"));

        assertThat(service.administersOrganisationOf(TARGET_UUID)).isFalse();
    }

    @Test
    void theAnswerIsResolvedOncePerRequestPerTarget() {
        // A wallet credit asks once for the guard; anything else on the same request should be free.
        when(userLookupService.userBelongsToOrganizationWithDomain(CALLER_UUID, SHARED_ORG_UUID, UserDomain.admin))
                .thenReturn(true);

        assertThat(service.administersOrganisationOf(TARGET_UUID)).isTrue();
        assertThat(service.administersOrganisationOf(TARGET_UUID)).isTrue();

        verify(userLookupService, times(1)).getUserOrganizations(TARGET_UUID);
    }

    @Test
    void aCourseCreatorIsRecognisedByTheirOwnProfileUuid() {
        when(courseCreatorLookupService.findCourseCreatorUuidByUserUuid(CALLER_UUID))
                .thenReturn(Optional.of(COURSE_CREATOR_UUID));

        assertThat(service.isCourseCreatorWithUuid(COURSE_CREATOR_UUID)).isTrue();
    }

    @Test
    void aCourseCreatorIsNotRecognisedByAnotherCreatorsProfileUuid() {
        when(courseCreatorLookupService.findCourseCreatorUuidByUserUuid(CALLER_UUID))
                .thenReturn(Optional.of(COURSE_CREATOR_UUID));

        assertThat(service.isCourseCreatorWithUuid(OTHER_COURSE_CREATOR_UUID)).isFalse();
    }

    @Test
    void aCallerWithNoCourseCreatorProfileOwnsNothing() {
        when(courseCreatorLookupService.findCourseCreatorUuidByUserUuid(CALLER_UUID)).thenReturn(Optional.empty());

        assertThat(service.isCourseCreatorWithUuid(COURSE_CREATOR_UUID)).isFalse();
        assertThat(service.getCurrentCourseCreatorUuid()).isNull();
    }

    @Test
    void aNullCourseCreatorUuidIsRefusedWithoutConsultingAnyone() {
        assertThat(service.isCourseCreatorWithUuid(null)).isFalse();

        verify(courseCreatorLookupService, times(0)).findCourseCreatorUuidByUserUuid(any());
    }

    @Test
    void anUnauthenticatedCallerHasNoCourseCreatorProfile() {
        SecurityContextHolder.clearContext();

        assertThat(service.isCourseCreatorWithUuid(COURSE_CREATOR_UUID)).isFalse();
    }

    @Test
    void aCourseCreatorLookupFailureDeniesRatherThanGrants() {
        when(courseCreatorLookupService.findCourseCreatorUuidByUserUuid(CALLER_UUID))
                .thenThrow(new IllegalStateException("boom"));

        assertThat(service.isCourseCreatorWithUuid(COURSE_CREATOR_UUID)).isFalse();
    }

    @Test
    void theCourseCreatorProfileIsResolvedOncePerRequest() {
        when(courseCreatorLookupService.findCourseCreatorUuidByUserUuid(CALLER_UUID))
                .thenReturn(Optional.of(COURSE_CREATOR_UUID));

        assertThat(service.isCourseCreatorWithUuid(COURSE_CREATOR_UUID)).isTrue();
        assertThat(service.isCourseCreatorWithUuid(COURSE_CREATOR_UUID)).isTrue();

        verify(courseCreatorLookupService, times(1)).findCourseCreatorUuidByUserUuid(CALLER_UUID);
    }

    private void authenticateAsJwtUser() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", KEYCLOAK_ID)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(jwt, null, "ROLE_USER");
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
