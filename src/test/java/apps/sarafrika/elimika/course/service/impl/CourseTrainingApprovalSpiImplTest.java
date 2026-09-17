package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.model.CourseTrainingApplication;
import apps.sarafrika.elimika.course.model.ProgramTrainingApplication;
import apps.sarafrika.elimika.course.repository.CourseTrainingApplicationRepository;
import apps.sarafrika.elimika.course.repository.ProgramTrainingApplicationRepository;
import apps.sarafrika.elimika.course.spi.ApprovedTrainingRate;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicantType;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicationStatus;
import apps.sarafrika.elimika.shared.enums.LocationType;
import apps.sarafrika.elimika.shared.enums.SessionFormat;
import apps.sarafrika.elimika.shared.utils.enums.RateBasis;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseTrainingApprovalSpiImplTest {

    @Mock
    private CourseTrainingApplicationRepository courseApplications;

    @Mock
    private ProgramTrainingApplicationRepository programApplications;

    private CourseTrainingApprovalSpiImpl spi;

    private final UUID courseUuid = UUID.randomUUID();
    private final UUID programUuid = UUID.randomUUID();
    private final UUID instructorUuid = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        spi = new CourseTrainingApprovalSpiImpl(courseApplications, programApplications);
    }

    @Test
    @DisplayName("a method stored as not offered resolves to nothing on every overload")
    void aNullCellResolvesToNothing() {
        CourseTrainingApplication application = new CourseTrainingApplication();
        application.setGroupOnlineHourlyRate(new BigDecimal("2500"));
        when(courseApplications.findByCourseUuidAndApplicantTypeAndApplicantUuidAndStatus(
                courseUuid, CourseTrainingApplicantType.INSTRUCTOR, instructorUuid, CourseTrainingApplicationStatus.APPROVED))
                .thenReturn(Optional.of(application));

        assertThat(spi.resolveInstructorRate(courseUuid, instructorUuid, SessionFormat.GROUP, LocationType.IN_PERSON,
                RateBasis.PER_HOUR)).isEmpty();
        assertThat(spi.resolveInstructorRate(courseUuid, instructorUuid, SessionFormat.GROUP, LocationType.ONLINE,
                RateBasis.PER_SESSION)).isEmpty();
        assertThat(spi.resolveInstructorRate(courseUuid, instructorUuid, SessionFormat.GROUP, LocationType.ONLINE,
                RateBasis.PER_HOUR)).contains(new BigDecimal("2500"));
    }

    @Test
    @DisplayName("a lookup without a basis is refused rather than read as hourly")
    void aMissingBasisIsNotReadAsHourly() {
        CourseTrainingApplication application = new CourseTrainingApplication();
        application.setGroupOnlineHourlyRate(new BigDecimal("2500"));
        when(courseApplications.findByCourseUuidAndApplicantTypeAndApplicantUuidAndStatus(
                courseUuid, CourseTrainingApplicantType.INSTRUCTOR, instructorUuid, CourseTrainingApplicationStatus.APPROVED))
                .thenReturn(Optional.of(application));

        assertThatThrownBy(() -> spi.resolveInstructorRate(
                courseUuid, instructorUuid, SessionFormat.GROUP, LocationType.ONLINE, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("rate basis");
    }

    @Test
    @DisplayName("a legacy zero is never returned as a price")
    void aZeroCellResolvesToNothing() {
        ProgramTrainingApplication application = new ProgramTrainingApplication();
        application.setPrivateInpersonDailyRate(BigDecimal.ZERO);
        when(programApplications.findByProgramUuidAndApplicantTypeAndApplicantUuidAndStatus(
                programUuid, CourseTrainingApplicantType.ORGANISATION, instructorUuid, CourseTrainingApplicationStatus.APPROVED))
                .thenReturn(Optional.of(application));

        assertThat(spi.resolveOrganisationProgramRate(programUuid, instructorUuid, SessionFormat.INDIVIDUAL,
                LocationType.HYBRID, RateBasis.PER_DAY)).isEmpty();
    }

    @Test
    @DisplayName("a charged lookup carries the card's currency alongside the rate")
    void theChargedLookupCarriesTheCardCurrency() {
        CourseTrainingApplication application = new CourseTrainingApplication();
        application.setRateCurrency("KES");
        application.setGroupInpersonDailyRate(new BigDecimal("9000"));
        when(courseApplications.findByCourseUuidAndApplicantTypeAndApplicantUuidAndStatus(
                courseUuid, CourseTrainingApplicantType.INSTRUCTOR, instructorUuid, CourseTrainingApplicationStatus.APPROVED))
                .thenReturn(Optional.of(application));

        assertThat(spi.resolveInstructorRateWithCurrency(courseUuid, instructorUuid, SessionFormat.GROUP,
                LocationType.HYBRID, RateBasis.PER_DAY)).contains(new ApprovedTrainingRate(new BigDecimal("9000"), "KES"));
        assertThat(spi.resolveInstructorRateWithCurrency(courseUuid, instructorUuid, SessionFormat.GROUP,
                LocationType.ONLINE, RateBasis.PER_DAY)).isEmpty();
    }

    @Test
    @DisplayName("hybrid and in-person delivery read the in-person cell")
    void hybridReadsInPerson() {
        CourseTrainingApplication application = new CourseTrainingApplication();
        application.setPrivateOnlineSessionRate(new BigDecimal("1000"));
        application.setPrivateInpersonSessionRate(new BigDecimal("4000"));
        when(courseApplications.findByCourseUuidAndApplicantTypeAndApplicantUuidAndStatus(
                courseUuid, CourseTrainingApplicantType.ORGANISATION, instructorUuid, CourseTrainingApplicationStatus.APPROVED))
                .thenReturn(Optional.of(application));

        assertThat(spi.resolveOrganisationRate(courseUuid, instructorUuid, SessionFormat.INDIVIDUAL, LocationType.HYBRID,
                RateBasis.PER_SESSION)).contains(new BigDecimal("4000"));
        assertThat(spi.resolveOrganisationRate(courseUuid, instructorUuid, SessionFormat.INDIVIDUAL, null,
                RateBasis.PER_SESSION)).contains(new BigDecimal("1000"));
    }

    @Test
    @DisplayName("an instructor's approvals load once and price cells exactly as the single lookups do")
    void approvalsLoadOnceAndPriceLikeTheSingleLookups() {
        CourseTrainingApplication course = new CourseTrainingApplication();
        course.setCourseUuid(courseUuid);
        course.setGroupOnlineSessionRate(new BigDecimal("3000"));
        course.setGroupInpersonSessionRate(BigDecimal.ZERO);
        ProgramTrainingApplication program = new ProgramTrainingApplication();
        program.setProgramUuid(programUuid);
        program.setPrivateInpersonDailyRate(new BigDecimal("9000"));
        when(courseApplications.findByApplicantTypeAndApplicantUuidAndStatus(
                CourseTrainingApplicantType.INSTRUCTOR, instructorUuid, CourseTrainingApplicationStatus.APPROVED))
                .thenReturn(java.util.List.of(course));
        when(programApplications.findByApplicantTypeAndApplicantUuidAndStatus(
                CourseTrainingApplicantType.INSTRUCTOR, instructorUuid, CourseTrainingApplicationStatus.APPROVED))
                .thenReturn(java.util.List.of(program));

        var approvals = spi.findInstructorApprovals(instructorUuid);

        assertThat(approvals.approvedForCourse(courseUuid)).isTrue();
        assertThat(approvals.approvedForCourse(UUID.randomUUID())).isFalse();
        assertThat(approvals.approvedForProgram(programUuid)).isTrue();
        assertThat(approvals.courseRate(courseUuid, SessionFormat.GROUP, LocationType.ONLINE, RateBasis.PER_SESSION))
                .contains(new BigDecimal("3000"));
        assertThat(approvals.courseRate(courseUuid, SessionFormat.GROUP, LocationType.HYBRID, RateBasis.PER_SESSION))
                .as("a legacy zero is not a price").isEmpty();
        assertThat(approvals.courseRate(courseUuid, null, LocationType.ONLINE, RateBasis.PER_SESSION)).isEmpty();
        assertThat(approvals.programRate(programUuid, SessionFormat.INDIVIDUAL, LocationType.IN_PERSON, RateBasis.PER_DAY))
                .contains(new BigDecimal("9000"));
        assertThat(approvals.programRate(UUID.randomUUID(), SessionFormat.INDIVIDUAL, LocationType.IN_PERSON,
                RateBasis.PER_DAY)).isEmpty();
    }
}
