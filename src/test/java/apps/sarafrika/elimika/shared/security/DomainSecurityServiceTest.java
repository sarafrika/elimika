package apps.sarafrika.elimika.shared.security;

import apps.sarafrika.elimika.coursecreator.spi.CourseCreatorLookupService;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService;
import apps.sarafrika.elimika.shared.spi.enrollment.EnrollmentLookupService;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the predicates here that are scoped to a subject rather than to the caller alone:
 * {@code administersOrganisationOf}, the org-scoped counterpart to {@code isOrganizationAdmin()};
 * {@code isCourseCreatorWithUuid}, which ties a caller to the one course creator profile they own;
 * and the class-scoped {@code canManageClass} / {@code canViewClassSchedule}.
 * <p>
 * {@code administersOrganisationOf} is the org-scoped counterpart to {@code isOrganizationAdmin()}:
 * that method answers a question about the caller alone, so it is satisfied by an administrator of
 * any organisation regardless of who the request is about. Anywhere the subject is another user — a
 * wallet, most obviously — that is not the question worth asking.
 * <p>
 * {@code canManageClass} and {@code canViewClassSchedule} are the same idea applied to a class:
 * holding the instructor domain, or an admin role in some organisation, says nothing about
 * <em>this</em> class, so both resolve the class and ask whether the caller stands in a relation to
 * it.
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

    private static final UUID CLASS_UUID = UUID.randomUUID();
    private static final UUID CALLER_INSTRUCTOR_UUID = UUID.randomUUID();
    private static final UUID OTHER_INSTRUCTOR_UUID = UUID.randomUUID();
    private static final UUID CALLER_STUDENT_UUID = UUID.randomUUID();
    private static final UUID OWNING_ORG_UUID = UUID.randomUUID();

    @Mock private UserLookupService userLookupService;
    @Mock private StudentLookupService studentLookupService;
    @Mock private InstructorLookupService instructorLookupService;
    @Mock private CourseCreatorLookupService courseCreatorLookupService;
    @Mock private ClassDefinitionLookupService classDefinitionLookupService;
    @Mock private EnrollmentLookupService enrollmentLookupService;

    private DomainSecurityService service;

    @BeforeEach
    void setUp() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));

        service = new DomainSecurityService(
                userLookupService, studentLookupService, instructorLookupService, courseCreatorLookupService,
                classDefinitionLookupService, enrollmentLookupService, new RequestScopedCache());

        authenticateAsJwtUser();
        when(userLookupService.findUserUuidByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(CALLER_UUID));
        when(userLookupService.getUserOrganizations(TARGET_UUID)).thenReturn(List.of(SHARED_ORG_UUID));
        when(userLookupService.userBelongsToOrganizationWithDomain(any(), any(), any())).thenReturn(false);
        when(userLookupService.userHasGlobalDomain(any(), any())).thenReturn(false);
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

    // canManageClass — the write side of a class

    @Test
    void theInstructorTheClassIsAssignedToMayManageIt() {
        givenClassRunBy(CALLER_INSTRUCTOR_UUID, null);
        givenCallerIsInstructor(CALLER_INSTRUCTOR_UUID);

        assertThat(service.canManageClass(CLASS_UUID)).isTrue();
    }

    @Test
    void anotherInstructorMayNotManageSomebodyElsesClass() {
        // The hijack this guards: an instructor PUTting their own uuid onto a class that is not theirs.
        givenClassRunBy(OTHER_INSTRUCTOR_UUID, null);
        givenCallerIsInstructor(CALLER_INSTRUCTOR_UUID);

        assertThat(service.canManageClass(CLASS_UUID)).isFalse();
    }

    @Test
    void aManagerOfTheOwningOrganisationMayManageItsClass() {
        givenClassRunBy(OTHER_INSTRUCTOR_UUID, OWNING_ORG_UUID);
        when(userLookupService.userBelongsToOrganizationWithDomain(
                CALLER_UUID, OWNING_ORG_UUID, UserDomain.organisation_user)).thenReturn(true);

        assertThat(service.canManageClass(CLASS_UUID)).isTrue();
    }

    @Test
    void anAdminOfSomeOtherOrganisationMayNot() {
        givenClassRunBy(OTHER_INSTRUCTOR_UUID, OWNING_ORG_UUID);
        when(userLookupService.userBelongsToOrganizationWithDomain(
                CALLER_UUID, OTHER_ORG_UUID, UserDomain.admin)).thenReturn(true);

        assertThat(service.canManageClass(CLASS_UUID)).isFalse();
    }

    @Test
    void anInstructorOwnedClassHasNoOrganisationRouteIn() {
        givenClassRunBy(OTHER_INSTRUCTOR_UUID, null);
        when(userLookupService.userBelongsToOrganizationWithDomain(any(), any(), any())).thenReturn(true);

        assertThat(service.canManageClass(CLASS_UUID)).isFalse();
    }

    @Test
    void thePlatformAdminMayManageAnyClass() {
        givenClassRunBy(OTHER_INSTRUCTOR_UUID, OWNING_ORG_UUID);
        when(userLookupService.userHasGlobalDomain(CALLER_UUID, UserDomain.admin)).thenReturn(true);

        assertThat(service.canManageClass(CLASS_UUID)).isTrue();
    }

    @Test
    void aNullClassIsRefusedWithoutResolvingAnything() {
        assertThat(service.canManageClass(null)).isFalse();

        verify(classDefinitionLookupService, times(0)).findDefaultInstructorUuid(any());
    }

    @Test
    void anUnauthenticatedCallerMayNotManageAClass() {
        SecurityContextHolder.clearContext();
        givenClassRunBy(CALLER_INSTRUCTOR_UUID, null);

        assertThat(service.canManageClass(CLASS_UUID)).isFalse();
    }

    @Test
    void aClassLookupFailureDeniesRatherThanGrants() {
        when(classDefinitionLookupService.findDefaultInstructorUuid(CLASS_UUID))
                .thenThrow(new IllegalStateException("boom"));

        assertThat(service.canManageClass(CLASS_UUID)).isFalse();
    }

    @Test
    void theClassIsResolvedOncePerRequest() {
        // A request that rewrites several schedules of one class should not re-read the class each time.
        givenClassRunBy(CALLER_INSTRUCTOR_UUID, null);
        givenCallerIsInstructor(CALLER_INSTRUCTOR_UUID);

        assertThat(service.canManageClass(CLASS_UUID)).isTrue();
        assertThat(service.canManageClass(CLASS_UUID)).isTrue();

        verify(classDefinitionLookupService, times(1)).findDefaultInstructorUuid(CLASS_UUID);
    }

    // canViewClassSchedule — the read side

    @Test
    void aLearnerEnrolledInTheClassMayReadItsSchedule() {
        givenClassRunBy(OTHER_INSTRUCTOR_UUID, OWNING_ORG_UUID);
        when(studentLookupService.findStudentUuidByUserUuid(CALLER_UUID))
                .thenReturn(Optional.of(CALLER_STUDENT_UUID));
        when(enrollmentLookupService.findMostRecentEnrollmentForClassDefinition(CALLER_STUDENT_UUID, CLASS_UUID))
                .thenReturn(Optional.of(new EnrollmentLookupService.ClassEnrollmentStatusSnapshot(
                        UUID.randomUUID(), "ENROLLED", LocalDateTime.now())));

        assertThat(service.canViewClassSchedule(CLASS_UUID)).isTrue();
    }

    @Test
    void aLearnerEnrolledInSomeOtherClassMayNot() {
        givenClassRunBy(OTHER_INSTRUCTOR_UUID, OWNING_ORG_UUID);
        when(studentLookupService.findStudentUuidByUserUuid(CALLER_UUID))
                .thenReturn(Optional.of(CALLER_STUDENT_UUID));
        when(enrollmentLookupService.findMostRecentEnrollmentForClassDefinition(CALLER_STUDENT_UUID, CLASS_UUID))
                .thenReturn(Optional.empty());

        assertThat(service.canViewClassSchedule(CLASS_UUID)).isFalse();
    }

    @Test
    void whoeverMayManageTheClassMayAlsoReadItsSchedule() {
        givenClassRunBy(CALLER_INSTRUCTOR_UUID, null);
        givenCallerIsInstructor(CALLER_INSTRUCTOR_UUID);

        assertThat(service.canViewClassSchedule(CLASS_UUID)).isTrue();

        verify(enrollmentLookupService, times(0))
                .findMostRecentEnrollmentForClassDefinition(any(), any());
    }

    @Test
    void anEnrolmentLookupFailureDeniesRatherThanGrants() {
        givenClassRunBy(OTHER_INSTRUCTOR_UUID, null);
        when(studentLookupService.findStudentUuidByUserUuid(CALLER_UUID))
                .thenReturn(Optional.of(CALLER_STUDENT_UUID));
        when(enrollmentLookupService.findMostRecentEnrollmentForClassDefinition(CALLER_STUDENT_UUID, CLASS_UUID))
                .thenThrow(new IllegalStateException("boom"));

        assertThat(service.canViewClassSchedule(CLASS_UUID)).isFalse();
    }

    private void givenClassRunBy(UUID instructorUuid, UUID organisationUuid) {
        when(classDefinitionLookupService.findDefaultInstructorUuid(CLASS_UUID))
                .thenReturn(Optional.ofNullable(instructorUuid));
        when(classDefinitionLookupService.findOrganisationUuid(CLASS_UUID))
                .thenReturn(Optional.ofNullable(organisationUuid));
    }

    private void givenCallerIsInstructor(UUID instructorUuid) {
        when(instructorLookupService.findInstructorUuidByUserUuid(CALLER_UUID))
                .thenReturn(Optional.of(instructorUuid));
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
