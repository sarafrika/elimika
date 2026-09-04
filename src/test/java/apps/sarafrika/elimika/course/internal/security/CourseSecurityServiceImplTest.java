package apps.sarafrika.elimika.course.internal.security;

import apps.sarafrika.elimika.course.model.Certificate;
import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.model.TrainingProgram;
import apps.sarafrika.elimika.course.repository.CertificateRepository;
import apps.sarafrika.elimika.course.repository.CourseEnrollmentRepository;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.repository.CourseTrainingApplicationRepository;
import apps.sarafrika.elimika.course.repository.ProgramCourseRepository;
import apps.sarafrika.elimika.course.repository.ProgramRequirementRepository;
import apps.sarafrika.elimika.course.repository.TrainingProgramRepository;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicantType;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicationStatus;
import apps.sarafrika.elimika.course.util.enums.EnrollmentStatus;
import apps.sarafrika.elimika.coursecreator.spi.CourseCreatorLookupService;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.security.RequestScopedCache;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
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
    @Mock private CourseEnrollmentRepository courseEnrollmentRepository;
    @Mock private CertificateRepository certificateRepository;
    @Mock private TrainingProgramRepository trainingProgramRepository;
    @Mock private CourseCreatorLookupService courseCreatorLookupService;
    @Mock private UserLookupService userLookupService;
    @Mock private DomainSecurityService domainSecurityService;
    @Mock private ProgramCourseRepository programCourseRepository;
    @Mock private ProgramRequirementRepository programRequirementRepository;

    private CourseSecurityServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CourseSecurityServiceImpl(
                courseRepository, trainingApplicationRepository, courseEnrollmentRepository,
                certificateRepository, courseCreatorLookupService, userLookupService,
                domainSecurityService, new RequestScopedCache(), trainingProgramRepository,
                programCourseRepository, programRequirementRepository);

        authenticateAsJwtUser();
        when(userLookupService.findUserUuidByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(USER_UUID));
        when(userLookupService.getUserOrganizations(USER_UUID)).thenReturn(List.of());
        when(courseCreatorLookupService.findCourseCreatorUuidByUserUuid(USER_UUID)).thenReturn(Optional.empty());
        when(domainSecurityService.getCurrentInstructorUuid()).thenReturn(null);
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
        when(domainSecurityService.getCurrentInstructorUuid()).thenReturn(instructorUuid);
        when(trainingApplicationRepository.existsByCourseUuidAndApplicantTypeAndApplicantUuidAndStatus(
                COURSE_UUID, CourseTrainingApplicantType.INSTRUCTOR, instructorUuid,
                CourseTrainingApplicationStatus.APPROVED)).thenReturn(true);

        assertThat(service.canManageCourseGradebook(COURSE_UUID)).isTrue();
    }

    @Test
    void anInstructorWithNoApprovalForThisCourseIsRefused() {
        UUID instructorUuid = UUID.randomUUID();
        when(domainSecurityService.getCurrentInstructorUuid()).thenReturn(instructorUuid);
        when(trainingApplicationRepository.existsByCourseUuidAndApplicantTypeAndApplicantUuidAndStatus(
                any(), any(), any(), any())).thenReturn(false);

        // The whole point of the change: holding the instructor domain is not enough.
        assertThat(service.canManageCourseGradebook(COURSE_UUID)).isFalse();
    }

    @Test
    void anInstructorApprovedForADifferentCourseIsRefused() {
        UUID instructorUuid = UUID.randomUUID();
        UUID otherCourse = UUID.randomUUID();
        when(domainSecurityService.getCurrentInstructorUuid()).thenReturn(instructorUuid);
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

    // ===== ENROLLED LEARNER ACCESS =====

    @Test
    void anActiveEnrolmentMakesTheCallerAnEnrolledLearner() {
        enrolledIn(COURSE_UUID, EnrollmentStatus.ACTIVE);

        assertThat(service.isEnrolledLearner(COURSE_UUID)).isTrue();
        assertThat(service.canReadCourseAsLearner(COURSE_UUID)).isTrue();
    }

    @Test
    void aCompletedEnrolmentStillGrantsAccessToTheMaterial() {
        // Finishing a course must not lock the learner out of what they studied.
        enrolledIn(COURSE_UUID, EnrollmentStatus.COMPLETED);

        assertThat(service.isEnrolledLearner(COURSE_UUID)).isTrue();
    }

    @Test
    void aDroppedEnrolmentDoesNotGrantAccess() {
        enrolledIn(COURSE_UUID, EnrollmentStatus.DROPPED);

        assertThat(service.isEnrolledLearner(COURSE_UUID)).isFalse();
    }

    @Test
    void aSuspendedEnrolmentDoesNotGrantAccess() {
        enrolledIn(COURSE_UUID, EnrollmentStatus.SUSPENDED);

        assertThat(service.isEnrolledLearner(COURSE_UUID)).isFalse();
    }

    @Test
    void anEnrolmentInADifferentCourseDoesNotGrantAccessToThisOne() {
        enrolledIn(UUID.randomUUID(), EnrollmentStatus.ACTIVE);

        assertThat(service.isEnrolledLearner(COURSE_UUID)).isFalse();
    }

    @Test
    void aCallerWithNoStudentProfileIsNotAnEnrolledLearner() {
        when(domainSecurityService.getCurrentStudentUuid()).thenReturn(null);

        assertThat(service.isEnrolledLearner(COURSE_UUID)).isFalse();
    }

    @Test
    void anEnrolmentLookupFailureDeniesRatherThanGrants() {
        when(domainSecurityService.getCurrentStudentUuid()).thenThrow(new IllegalStateException("boom"));

        assertThat(service.isEnrolledLearner(COURSE_UUID)).isFalse();
    }

    @Test
    void readingAsALearnerStillGrantsTheCourseOwner() {
        // canReadCourseAsLearner is a widening of canReadCourseContent, never a replacement.
        UUID creatorUuid = UUID.randomUUID();
        when(courseCreatorLookupService.findCourseCreatorUuidByUserUuid(USER_UUID))
                .thenReturn(Optional.of(creatorUuid));
        Course course = new Course();
        course.setCourseCreatorUuid(creatorUuid);
        when(courseRepository.findByUuid(COURSE_UUID)).thenReturn(Optional.of(course));
        when(domainSecurityService.getCurrentStudentUuid()).thenReturn(null);

        assertThat(service.canReadCourseAsLearner(COURSE_UUID)).isTrue();
    }

    @Test
    void enrolledCoursesAreLoadedOnceAndAnsweredBySetMembership() {
        UUID otherCourse = UUID.randomUUID();
        UUID studentUuid = UUID.randomUUID();
        when(domainSecurityService.getCurrentStudentUuid()).thenReturn(studentUuid);
        when(courseEnrollmentRepository.findCourseUuidsByStudentUuidAndStatusIn(
                studentUuid, EnrollmentStatus.ACCESS_ALLOWING))
                .thenReturn(List.of(COURSE_UUID, otherCourse));

        assertThat(service.enrolledCourseUuids()).containsExactlyInAnyOrder(COURSE_UUID, otherCourse);
        assertThat(service.isEnrolledLearner(COURSE_UUID)).isTrue();
        assertThat(service.isEnrolledLearner(otherCourse)).isTrue();
        assertThat(service.isEnrolledLearner(UUID.randomUUID())).isFalse();
    }

    // ===== CERTIFICATE ISSUANCE =====

    @Test
    void anInstructorApprovedToTrainTheCourseMayAwardItsCertificates() {
        UUID instructorUuid = UUID.randomUUID();
        when(domainSecurityService.getCurrentInstructorUuid()).thenReturn(instructorUuid);
        when(trainingApplicationRepository.existsByCourseUuidAndApplicantTypeAndApplicantUuidAndStatus(
                COURSE_UUID, CourseTrainingApplicantType.INSTRUCTOR, instructorUuid,
                CourseTrainingApplicationStatus.APPROVED)).thenReturn(true);

        assertThat(service.canAwardCertificate(COURSE_UUID, null)).isTrue();
    }

    @Test
    void aLearnerOfAnApprovedOrganisationMayNotAwardItsCertificates() {
        // An organisation enrols its students as members too, so membership alone cannot carry
        // the organisation's teaching rights through to the person.
        UUID organisationUuid = UUID.randomUUID();
        when(userLookupService.getUserOrganizations(USER_UUID)).thenReturn(List.of(organisationUuid));
        when(userLookupService.userBelongsToOrganizationWithDomain(any(), any(), any())).thenReturn(false);
        when(trainingApplicationRepository.existsByCourseUuidAndApplicantTypeAndApplicantUuidAndStatus(
                COURSE_UUID, CourseTrainingApplicantType.ORGANISATION, organisationUuid,
                CourseTrainingApplicationStatus.APPROVED)).thenReturn(true);

        assertThat(service.canAwardCertificate(COURSE_UUID, null)).isFalse();
    }

    @Test
    void anInstructorEmployedByAnApprovedOrganisationMayAwardItsCertificates() {
        UUID organisationUuid = UUID.randomUUID();
        when(userLookupService.getUserOrganizations(USER_UUID)).thenReturn(List.of(organisationUuid));
        when(userLookupService.userBelongsToOrganizationWithDomain(
                USER_UUID, organisationUuid, UserDomain.instructor)).thenReturn(true);
        when(trainingApplicationRepository.existsByCourseUuidAndApplicantTypeAndApplicantUuidAndStatus(
                COURSE_UUID, CourseTrainingApplicantType.ORGANISATION, organisationUuid,
                CourseTrainingApplicationStatus.APPROVED)).thenReturn(true);

        assertThat(service.canAwardCertificate(COURSE_UUID, null)).isTrue();
    }

    @Test
    void aPayloadNamingBothACourseAndAProgramIsRefused() {
        // Only one of the two can be authorised, so naming both would smuggle the other one past.
        UUID creatorUuid = UUID.randomUUID();
        when(courseCreatorLookupService.findCourseCreatorUuidByUserUuid(USER_UUID))
                .thenReturn(Optional.of(creatorUuid));
        Course course = new Course();
        course.setCourseCreatorUuid(creatorUuid);
        when(courseRepository.findByUuid(COURSE_UUID)).thenReturn(Optional.of(course));

        assertThat(service.canAwardCertificate(COURSE_UUID, UUID.randomUUID())).isFalse();
    }

    @Test
    void aPayloadNamingNeitherACourseNorAProgramIsRefused() {
        assertThat(service.canAwardCertificate(null, null)).isFalse();
    }

    @Test
    void theProgramAuthorMayAwardAndListItsCertificates() {
        UUID creatorUuid = UUID.randomUUID();
        UUID programUuid = UUID.randomUUID();
        when(courseCreatorLookupService.findCourseCreatorUuidByUserUuid(USER_UUID))
                .thenReturn(Optional.of(creatorUuid));
        TrainingProgram program = new TrainingProgram();
        program.setCourseCreatorUuid(creatorUuid);
        when(trainingProgramRepository.findByUuid(programUuid)).thenReturn(Optional.of(program));

        assertThat(service.canAwardCertificate(null, programUuid)).isTrue();
        assertThat(service.canReadProgramCertificates(programUuid)).isTrue();
    }

    @Test
    void somebodyElsesProgramCertificatesAreRefused() {
        UUID programUuid = UUID.randomUUID();
        when(courseCreatorLookupService.findCourseCreatorUuidByUserUuid(USER_UUID))
                .thenReturn(Optional.of(UUID.randomUUID()));
        TrainingProgram program = new TrainingProgram();
        program.setCourseCreatorUuid(UUID.randomUUID());
        when(trainingProgramRepository.findByUuid(programUuid)).thenReturn(Optional.of(program));

        assertThat(service.canReadProgramCertificates(programUuid)).isFalse();
        assertThat(service.canAwardCertificate(null, programUuid)).isFalse();
    }

    // ===== CERTIFICATE READS =====

    @Test
    void theLearnerNamedOnACertificateMayReadIt() {
        UUID certificateUuid = UUID.randomUUID();
        UUID studentUuid = UUID.randomUUID();
        Certificate certificate = new Certificate();
        certificate.setStudentUuid(studentUuid);
        certificate.setCourseUuid(COURSE_UUID);
        when(certificateRepository.findByUuid(certificateUuid)).thenReturn(Optional.of(certificate));
        when(domainSecurityService.isStudentWithUuid(studentUuid)).thenReturn(true);

        assertThat(service.canReadCertificate(certificateUuid)).isTrue();
    }

    @Test
    void anUnrelatedCallerMayNotReadACertificate() {
        UUID certificateUuid = UUID.randomUUID();
        Certificate certificate = new Certificate();
        certificate.setStudentUuid(UUID.randomUUID());
        certificate.setCourseUuid(COURSE_UUID);
        when(certificateRepository.findByUuid(certificateUuid)).thenReturn(Optional.of(certificate));

        assertThat(service.canReadCertificate(certificateUuid)).isFalse();
    }

    @Test
    void anOrganisationManagerMayListTheirMembersCertificates() {
        UUID studentUuid = UUID.randomUUID();
        when(domainSecurityService.managesOrganisationOfStudent(studentUuid)).thenReturn(true);

        assertThat(service.canReadStudentCertificates(studentUuid)).isTrue();
    }

    @Test
    void aCallerWhoTeachesNothingTheLearnerTakesIsRefusedTheirCertificates() {
        UUID studentUuid = UUID.randomUUID();

        assertThat(service.canReadStudentCertificates(studentUuid)).isFalse();
    }

    @Test
    void teachingACourseTheLearnerIsEnrolledOnGrantsTheirCertificates() {
        UUID studentUuid = UUID.randomUUID();
        UUID creatorUuid = UUID.randomUUID();
        when(courseCreatorLookupService.findCourseCreatorUuidByUserUuid(USER_UUID))
                .thenReturn(Optional.of(creatorUuid));
        when(courseRepository.findUuidsByCourseCreatorUuid(creatorUuid)).thenReturn(List.of(COURSE_UUID));
        when(courseEnrollmentRepository.findCourseUuidsByStudentUuidAndStatusIn(eq(studentUuid), any()))
                .thenReturn(List.of(COURSE_UUID));

        assertThat(service.canReadStudentCertificates(studentUuid)).isTrue();
    }

    private void enrolledIn(UUID courseUuid, EnrollmentStatus status) {
        UUID studentUuid = UUID.randomUUID();
        when(domainSecurityService.getCurrentStudentUuid()).thenReturn(studentUuid);
        when(courseEnrollmentRepository.findCourseUuidsByStudentUuidAndStatusIn(
                studentUuid, EnrollmentStatus.ACCESS_ALLOWING))
                .thenReturn(status.allowsAccess() ? List.of(courseUuid) : List.of());
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
