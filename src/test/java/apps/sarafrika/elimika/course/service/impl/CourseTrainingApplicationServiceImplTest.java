package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.CourseTrainingApplicationRequest;
import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.model.CourseTrainingApplication;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.repository.CourseTrainingApplicationRepository;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicantType;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicationStatus;
import apps.sarafrika.elimika.shared.currency.model.PlatformCurrency;
import apps.sarafrika.elimika.shared.currency.service.CurrencyService;
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

    private CourseTrainingApplicationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CourseTrainingApplicationServiceImpl(
                courseRepository,
                applicationRepository,
                specificationBuilder,
                currencyService
        );
    }

    @Test
    void submitApplicationRejectsRateBelowMinimum() {
        UUID courseUuid = UUID.randomUUID();
        UUID applicantUuid = UUID.randomUUID();

        Course course = new Course();
        course.setMinimumTrainingFee(new BigDecimal("2500.00"));

        when(courseRepository.findByUuid(courseUuid)).thenReturn(Optional.of(course));
        when(applicationRepository.findByCourseUuidAndApplicantTypeAndApplicantUuid(courseUuid,
                CourseTrainingApplicantType.INSTRUCTOR,
                applicantUuid)).thenReturn(Optional.empty());

        PlatformCurrency kes = new PlatformCurrency(
                "KES",
                404,
                "Kenyan Shilling",
                "KES",
                2,
                true,
                true
        );
        lenient().when(currencyService.resolveCurrencyOrDefault("KES")).thenReturn(kes);

        CourseTrainingApplicationRequest request = new CourseTrainingApplicationRequest(
                CourseTrainingApplicantType.INSTRUCTOR,
                applicantUuid,
                new BigDecimal("2000.00"),
                "KES",
                null
        );

        assertThatThrownBy(() -> service.submitApplication(courseUuid, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rate")
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
                new BigDecimal("2800.1254"),
                "usd",
                "Ready to deliver evening cohorts"
        );

        service.submitApplication(courseUuid, request);

        ArgumentCaptor<CourseTrainingApplication> captor = ArgumentCaptor.forClass(CourseTrainingApplication.class);
        verify(applicationRepository).save(captor.capture());

        CourseTrainingApplication saved = captor.getValue();
        assertThat(saved.getRatePerHourPerHead()).isEqualByComparingTo("2800.1254");
        assertThat(saved.getRateCurrency()).isEqualTo("USD");
        assertThat(saved.getStatus()).isEqualTo(CourseTrainingApplicationStatus.PENDING);
    }
}
