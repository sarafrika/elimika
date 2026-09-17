package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.internal.training.TrainingApplicantNames;
import apps.sarafrika.elimika.course.internal.training.TrainingApplicationAccess;
import apps.sarafrika.elimika.course.internal.training.TrainingFeeFloors;
import apps.sarafrika.elimika.course.internal.training.TrainingRateUpdateNotifier;
import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.model.CourseTrainingApplication;
import apps.sarafrika.elimika.course.model.CourseTrainingRateUpdate;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.repository.CourseTrainingApplicationRepository;
import apps.sarafrika.elimika.course.repository.CourseTrainingRateUpdateRepository;
import apps.sarafrika.elimika.course.service.CourseTrainingRateUpdateService;
import apps.sarafrika.elimika.course.util.enums.TrainingApplicationType;
import apps.sarafrika.elimika.course.util.enums.TrainingRateUpdateStatus;
import apps.sarafrika.elimika.course.validation.CourseTrainingRateCardValidator;
import apps.sarafrika.elimika.coursecreator.spi.CourseCreatorLookupService;
import apps.sarafrika.elimika.shared.currency.service.CurrencyService;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
public class CourseTrainingRateUpdateServiceImpl
        extends AbstractTrainingRateUpdateService<CourseTrainingApplication, CourseTrainingRateUpdate>
        implements CourseTrainingRateUpdateService {

    private final CourseRepository courseRepository;
    private final CourseTrainingApplicationRepository applicationRepository;
    private final CourseTrainingRateUpdateRepository rateUpdateRepository;
    private final CourseCreatorLookupService courseCreatorLookupService;
    private final TrainingApplicationAccess access;
    private final TrainingFeeFloors feeFloors;

    public CourseTrainingRateUpdateServiceImpl(CourseRepository courseRepository,
                                               CourseTrainingApplicationRepository applicationRepository,
                                               CourseTrainingRateUpdateRepository rateUpdateRepository,
                                               CourseCreatorLookupService courseCreatorLookupService,
                                               CourseTrainingRateCardValidator rateCardValidator,
                                               CurrencyService currencyService,
                                               TrainingApplicationAccess access,
                                               TrainingFeeFloors feeFloors,
                                               TrainingApplicantNames applicantNames,
                                               TrainingRateUpdateNotifier notifier) {
        super(rateUpdateRepository, rateCardValidator, currencyService, access, applicantNames, notifier);
        this.courseRepository = courseRepository;
        this.applicationRepository = applicationRepository;
        this.rateUpdateRepository = rateUpdateRepository;
        this.courseCreatorLookupService = courseCreatorLookupService;
        this.access = access;
        this.feeFloors = feeFloors;
    }

    @Override
    protected TrainingApplicationType applicationType() {
        return TrainingApplicationType.COURSE;
    }

    @Override
    protected CourseTrainingApplication findApplication(UUID courseUuid, UUID applicationUuid) {
        return applicationRepository.findByUuid(applicationUuid)
                .filter(application -> courseUuid.equals(application.getCourseUuid()))
                .orElseThrow(() -> applicationNotFound(courseUuid, applicationUuid));
    }

    @Override
    protected List<CourseTrainingApplication> findApplications(Collection<UUID> applicationUuids) {
        return applicationUuids.isEmpty() ? List.of() : applicationRepository.findByUuidIn(applicationUuids);
    }

    @Override
    protected void saveApplication(CourseTrainingApplication application) {
        applicationRepository.save(application);
    }

    @Override
    protected boolean ownsParent(UUID courseUuid) {
        return access.ownsCourse(courseUuid);
    }

    @Override
    protected String ownerLabel() {
        return "course creator";
    }

    @Override
    protected BigDecimal minimumTrainingFee(UUID courseUuid) {
        return feeFloors.forCourse(courseUuid);
    }

    @Override
    protected TrainingRateUpdateNotifier.Subject subject(UUID courseUuid) {
        Course course = courseRepository.findByUuid(courseUuid).orElse(null);
        String name = course == null || course.getName() == null ? "your course" : course.getName();
        UUID ownerUserUuid = course == null || course.getCourseCreatorUuid() == null
                ? null
                : courseCreatorLookupService.getCourseCreatorUserUuid(course.getCourseCreatorUuid()).orElse(null);
        return new TrainingRateUpdateNotifier.Subject(TrainingApplicationType.COURSE, courseUuid, name, ownerUserUuid);
    }

    @Override
    protected Page<CourseTrainingRateUpdate> findUpdatesForParent(UUID courseUuid, TrainingRateUpdateStatus status,
                                                                  Pageable pageable) {
        return status == null
                ? rateUpdateRepository.findForCourse(courseUuid, pageable)
                : rateUpdateRepository.findForCourseWithStatus(courseUuid, status, pageable);
    }

    @Override
    protected CourseTrainingRateUpdate newUpdate() {
        return new CourseTrainingRateUpdate();
    }

    @Override
    protected ResourceNotFoundException applicationNotFound(UUID courseUuid, UUID applicationUuid) {
        return new ResourceNotFoundException(String.format(
                "Training application %s not found for course %s", applicationUuid, courseUuid));
    }
}
