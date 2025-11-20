package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.CourseTrainingApplicationRequest;
import apps.sarafrika.elimika.course.dto.CourseTrainingRateCardDTO;
import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.model.CourseTrainingApplication;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.repository.CourseTrainingApplicationRepository;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicantType;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicationStatus;
import apps.sarafrika.elimika.course.validation.CourseTrainingRateCardValidator;
import apps.sarafrika.elimika.shared.currency.model.PlatformCurrency;
import apps.sarafrika.elimika.shared.currency.service.CurrencyService;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseTrainingApplicationServiceImplTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseTrainingApplicationRepository applicationRepository;

    @Mock
    private GenericSpecificationBuilder<CourseTrainingApplication> specificationBuilder;

    @Mock
    private CurrencyService currencyService;

    @Mock
    private DomainSecurityService domainSecurityService;

    private CourseTrainingRateCardValidator rateCardValidator;

    private CourseTrainingApplicationServiceImpl service;

    @BeforeEach
    void setUp() {
        rateCardValidator = new CourseTrainingRateCardValidator();
        service = new CourseTrainingApplicationServiceImpl(
                courseRepository,
                applicationRepository,
                specificationBuilder,
                currencyService,
                domainSecurityService,
                rateCardValidator
        );
    }

    @Test
    void submitApplicationRejectsRateBelowMinimum() {
        UUID courseUuid = UUID.randomUUID();
        UUID applicantUuid = UUID.randomUUID();

        Course course = new Course();
        course.setMinimumTrainingFee(new BigDecimal("2500.00"));

        when(courseRepository.findByUuid(courseUuid)).thenReturn(Optional.of(course));
        when(domainSecurityService.isInstructorWithUuid(applicantUuid)).thenReturn(true);
        CourseTrainingApplicationRequest request = new CourseTrainingApplicationRequest(
                CourseTrainingApplicantType.INSTRUCTOR,
                applicantUuid,
                rateCard("KES", "2000.00"),
                null
        );

        assertThatThrownBy(() -> service.submitApplication(courseUuid, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("private_online_rate")
                .hasMessageContaining("minimum training fee");
    }

    @Test
    void submitApplicationRejectsOrganisationRateBelowMinimum() {
        UUID courseUuid = UUID.randomUUID();
        UUID organisationUuid = UUID.randomUUID();

        Course course = new Course();
        course.setMinimumTrainingFee(new BigDecimal("3000.00"));

        when(courseRepository.findByUuid(courseUuid)).thenReturn(Optional.of(course));

        CourseTrainingApplicationRequest request = new CourseTrainingApplicationRequest(
                CourseTrainingApplicantType.ORGANISATION,
                organisationUuid,
                rateCard("KES", "3200.00", "2500.00", "3600.00", "4100.00"),
                null
        );

        assertThatThrownBy(() -> service.submitApplication(courseUuid, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("private_inperson_rate")
                .hasMessageContaining("minimum training fee");
    }

    @Test
    void submitApplicationPersistsNormalisedRateAndCurrency() {
        UUID courseUuid = UUID.randomUUID();
        UUID applicantUuid = UUID.randomUUID();

        Course course = new Course();
        course.setMinimumTrainingFee(new BigDecimal("2500.00"));

        when(courseRepository.findByUuid(courseUuid)).thenReturn(Optional.of(course));
        when(applicationRepository.findByCourseUuidAndApplicantTypeAndApplicantUuid(courseUuid,
                CourseTrainingApplicantType.INSTRUCTOR,
                applicantUuid)).thenReturn(Optional.empty());

        lenient().when(applicationRepository.save(org.mockito.ArgumentMatchers.any(CourseTrainingApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(domainSecurityService.isInstructorWithUuid(applicantUuid)).thenReturn(true);

        PlatformCurrency usd = new PlatformCurrency(
                "USD",
                840,
                "US Dollar",
                "$",
                2,
                true,
                false
        );
        when(currencyService.resolveCurrencyOrDefault("usd")).thenReturn(usd);

        CourseTrainingApplicationRequest request = new CourseTrainingApplicationRequest(
                CourseTrainingApplicantType.INSTRUCTOR,
                applicantUuid,
                rateCard("usd", "2800.1254"),
                "Ready to deliver evening cohorts"
        );

        service.submitApplication(courseUuid, request);

        ArgumentCaptor<CourseTrainingApplication> captor = ArgumentCaptor.forClass(CourseTrainingApplication.class);
        verify(applicationRepository).save(captor.capture());

        CourseTrainingApplication saved = captor.getValue();
        assertThat(saved.getPrivateOnlineRate()).isEqualByComparingTo("2800.1254");
        assertThat(saved.getPrivateInpersonRate()).isEqualByComparingTo("2800.1254");
        assertThat(saved.getGroupOnlineRate()).isEqualByComparingTo("2800.1254");
        assertThat(saved.getGroupInpersonRate()).isEqualByComparingTo("2800.1254");
        assertThat(saved.getRateCurrency()).isEqualTo("USD");
        assertThat(saved.getStatus()).isEqualTo(CourseTrainingApplicationStatus.PENDING);
    }

    @Test
    void submitApplicationRejectsInstructorImpersonation() {
        UUID courseUuid = UUID.randomUUID();
        UUID applicantUuid = UUID.randomUUID();

        when(domainSecurityService.isInstructorWithUuid(applicantUuid)).thenReturn(false);
        CourseTrainingApplicationRequest request = new CourseTrainingApplicationRequest(
                CourseTrainingApplicantType.INSTRUCTOR,
                applicantUuid,
                rateCard("KES", "2500.00"),
                null
        );

        assertThatThrownBy(() -> service.submitApplication(courseUuid, request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Instructors may only submit training applications for themselves");
    }

    private CourseTrainingRateCardDTO rateCard(String currency, String amount) {
        BigDecimal normalized = new BigDecimal(amount);
        return new CourseTrainingRateCardDTO(
                currency,
                normalized,
                normalized,
                normalized,
                normalized
        );
    }

    private CourseTrainingRateCardDTO rateCard(String currency,
                                               String privateOnline,
                                               String privateInperson,
                                               String groupOnline,
                                               String groupInperson) {
        return new CourseTrainingRateCardDTO(
                currency,
                new BigDecimal(privateOnline),
                new BigDecimal(privateInperson),
                new BigDecimal(groupOnline),
                new BigDecimal(groupInperson)
        );
    }

    private PlatformCurrency defaultKes() {
        return new PlatformCurrency(
                "KES",
                404,
                "Kenyan Shilling",
                "KSh",
                2,
                true,
                false
        );
    }
}
