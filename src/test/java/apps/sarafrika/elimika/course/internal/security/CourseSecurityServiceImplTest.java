package apps.sarafrika.elimika.course.internal.security;

import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.repository.CourseTrainingApplicationRepository;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicantType;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicationStatus;
import apps.sarafrika.elimika.coursecreator.spi.CourseCreatorLookupService;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Guards the rule that marking somebody's work requires a real relationship to the
 * course. Before this predicate existed, any instructor could reach any gradebook.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CourseSecurityServiceImplTest {

    private static final String KEYCLOAK_ID = "keycloak-subject";
    private static final UUID COURSE_UUID = UUID.randomUUID();
    private static final UUID USER_UUID = UUID.randomUUID();

    @Mock private CourseRepository courseRepository;
    @Mock private CourseTrainingApplicationRepository trainingApplicationRepository;
    @Mock private CourseCreatorLookupService courseCreatorLookupService;
    @Mock private InstructorLookupService instructorLookupService;
    @Mock private UserLookupService userLookupService;

    private CourseSecurityServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CourseSecurityServiceImpl(
                courseRepository, trainingApplicationRepository, courseCreatorLookupService,
                instructorLookupService, userLookupService);

        authenticateAsJwtUser();
        when(userLookupService.findUserUuidByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(USER_UUID));
        when(userLookupService.getUserOrganizations(USER_UUID)).thenReturn(List.of());
        when(courseCreatorLookupService.findCourseCreatorUuidByUserUuid(USER_UUID)).thenReturn(Optional.empty());
        when(instructorLookupService.findInstructorUuidByUserUuid(USER_UUID)).thenReturn(Optional.empty());
        when(courseRepository.findByUuid(COURSE_UUID)).thenReturn(Optional.of(new Course()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void theCourseOwnerMayManageTheGradebook() {
        UUID creatorUuid = UUID.randomUUID();
        when(courseCreatorLookupService.findCourseCreatorUuidByUserUuid(USER_UUID))
                .thenReturn(Optional.of(creatorUuid));
        Course course = new Course();
        course.setCourseCreatorUuid(creatorUuid);
        when(courseRepository.findByUuid(COURSE_UUID)).thenReturn(Optional.of(course));

        assertThat(service.canManageCourseGradebook(COURSE_UUID)).isTrue();
    }

    @Test
    void anInstructorApprovedToTrainTheCourseMayManageTheGradebook() {
        UUID instructorUuid = UUID.randomUUID();
        when(instructorLookupService.findInstructorUuidByUserUuid(USER_UUID))
                .thenReturn(Optional.of(instructorUuid));
        when(trainingApplicationRepository.existsByCourseUuidAndApplicantTypeAndApplicantUuidAndStatus(
                COURSE_UUID, CourseTrainingApplicantType.INSTRUCTOR, instructorUuid,
                CourseTrainingApplicationStatus.APPROVED)).thenReturn(true);

        assertThat(service.canManageCourseGradebook(COURSE_UUID)).isTrue();
    }

    @Test
    void anInstructorWithNoApprovalForThisCourseIsRefused() {
        UUID instructorUuid = UUID.randomUUID();
        when(instructorLookupService.findInstructorUuidByUserUuid(USER_UUID))
                .thenReturn(Optional.of(instructorUuid));
        when(trainingApplicationRepository.existsByCourseUuidAndApplicantTypeAndApplicantUuidAndStatus(
                any(), any(), any(), any())).thenReturn(false);

        // The whole point of the change: holding the instructor domain is not enough.
        assertThat(service.canManageCourseGradebook(COURSE_UUID)).isFalse();
    }

    @Test
    void anInstructorApprovedForADifferentCourseIsRefused() {
        UUID instructorUuid = UUID.randomUUID();
        UUID otherCourse = UUID.randomUUID();
        when(instructorLookupService.findInstructorUuidByUserUuid(USER_UUID))
                .thenReturn(Optional.of(instructorUuid));
        when(trainingApplicationRepository.existsByCourseUuidAndApplicantTypeAndApplicantUuidAndStatus(
                eq(otherCourse), any(), any(), any())).thenReturn(true);
        when(trainingApplicationRepository.existsByCourseUuidAndApplicantTypeAndApplicantUuidAndStatus(
                eq(COURSE_UUID), any(), any(), any())).thenReturn(false);

        assertThat(service.canManageCourseGradebook(COURSE_UUID)).isFalse();
    }

    @Test
    void aMemberOfAnOrganisationApprovedToTrainTheCourseMayManageTheGradebook() {
        UUID organisationUuid = UUID.randomUUID();
        when(userLookupService.getUserOrganizations(USER_UUID)).thenReturn(List.of(organisationUuid));
        when(trainingApplicationRepository.existsByCourseUuidAndApplicantTypeAndApplicantUuidAndStatus(
                COURSE_UUID, CourseTrainingApplicantType.ORGANISATION, organisationUuid,
                CourseTrainingApplicationStatus.APPROVED)).thenReturn(true);

        assertThat(service.canManageCourseGradebook(COURSE_UUID)).isTrue();
    }

    @Test
    void aMemberOfAnUnapprovedOrganisationIsRefused() {
        when(userLookupService.getUserOrganizations(USER_UUID)).thenReturn(List.of(UUID.randomUUID()));
        when(trainingApplicationRepository.existsByCourseUuidAndApplicantTypeAndApplicantUuidAndStatus(
                any(), any(), any(), any())).thenReturn(false);

        assertThat(service.canManageCourseGradebook(COURSE_UUID)).isFalse();
    }

    @Test
    void anUnauthenticatedCallerIsRefused() {
        SecurityContextHolder.clearContext();

        assertThat(service.canManageCourseGradebook(COURSE_UUID)).isFalse();
    }

    @Test
    void aLookupFailureDeniesRatherThanGrants() {
        when(userLookupService.getUserOrganizations(USER_UUID))
                .thenThrow(new IllegalStateException("lookup exploded"));

        assertThat(service.canManageCourseGradebook(COURSE_UUID)).isFalse();
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
