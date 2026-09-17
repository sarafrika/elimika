package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.internal.training.TrainingApplicantNames;
import apps.sarafrika.elimika.course.internal.training.TrainingApplicationAccess;
import apps.sarafrika.elimika.course.internal.training.TrainingFeeFloors;
import apps.sarafrika.elimika.course.internal.training.TrainingRateUpdateNotifier;
import apps.sarafrika.elimika.course.model.ProgramTrainingApplication;
import apps.sarafrika.elimika.course.model.ProgramTrainingRateUpdate;
import apps.sarafrika.elimika.course.model.TrainingProgram;
import apps.sarafrika.elimika.course.repository.ProgramTrainingApplicationRepository;
import apps.sarafrika.elimika.course.repository.ProgramTrainingRateUpdateRepository;
import apps.sarafrika.elimika.course.repository.TrainingProgramRepository;
import apps.sarafrika.elimika.course.service.ProgramTrainingRateUpdateService;
import apps.sarafrika.elimika.course.util.enums.TrainingApplicationType;
import apps.sarafrika.elimika.course.util.enums.TrainingRateUpdateStatus;
import apps.sarafrika.elimika.course.validation.CourseTrainingRateCardValidator;
import apps.sarafrika.elimika.coursecreator.spi.CourseCreatorLookupService;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
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
public class ProgramTrainingRateUpdateServiceImpl
        extends AbstractTrainingRateUpdateService<ProgramTrainingApplication, ProgramTrainingRateUpdate>
        implements ProgramTrainingRateUpdateService {

    private final TrainingProgramRepository programRepository;
    private final ProgramTrainingApplicationRepository applicationRepository;
    private final ProgramTrainingRateUpdateRepository rateUpdateRepository;
    private final CourseCreatorLookupService courseCreatorLookupService;
    private final InstructorLookupService instructorLookupService;
    private final TrainingApplicationAccess access;
    private final TrainingFeeFloors feeFloors;

    public ProgramTrainingRateUpdateServiceImpl(TrainingProgramRepository programRepository,
                                                ProgramTrainingApplicationRepository applicationRepository,
                                                ProgramTrainingRateUpdateRepository rateUpdateRepository,
                                                CourseCreatorLookupService courseCreatorLookupService,
                                                InstructorLookupService instructorLookupService,
                                                CourseTrainingRateCardValidator rateCardValidator,
                                                CurrencyService currencyService,
                                                TrainingApplicationAccess access,
                                                TrainingFeeFloors feeFloors,
                                                TrainingApplicantNames applicantNames,
                                                TrainingRateUpdateNotifier notifier) {
        super(rateUpdateRepository, rateCardValidator, currencyService, access, applicantNames, notifier);
        this.programRepository = programRepository;
        this.applicationRepository = applicationRepository;
        this.rateUpdateRepository = rateUpdateRepository;
        this.courseCreatorLookupService = courseCreatorLookupService;
        this.instructorLookupService = instructorLookupService;
        this.access = access;
        this.feeFloors = feeFloors;
    }

    @Override
    protected TrainingApplicationType applicationType() {
        return TrainingApplicationType.PROGRAM;
    }

    @Override
    protected ProgramTrainingApplication findApplication(UUID programUuid, UUID applicationUuid) {
        return applicationRepository.findByUuid(applicationUuid)
                .filter(application -> programUuid.equals(application.getProgramUuid()))
                .orElseThrow(() -> applicationNotFound(programUuid, applicationUuid));
    }

    @Override
    protected List<ProgramTrainingApplication> findApplications(Collection<UUID> applicationUuids) {
        return applicationUuids.isEmpty() ? List.of() : applicationRepository.findByUuidIn(applicationUuids);
    }

    @Override
    protected void saveApplication(ProgramTrainingApplication application) {
        applicationRepository.save(application);
    }

    @Override
    protected boolean ownsParent(UUID programUuid) {
        return access.ownsProgram(programUuid);
    }

    @Override
    protected String ownerLabel() {
        return "program creator";
    }

    @Override
    protected BigDecimal minimumTrainingFee(UUID programUuid) {
        return feeFloors.forProgram(programUuid);
    }

    /** A program's creator column may hold a course-creator or an instructor profile. */
    @Override
    protected TrainingRateUpdateNotifier.Subject subject(UUID programUuid) {
        TrainingProgram program = programRepository.findByUuid(programUuid).orElse(null);
        String title = program == null || program.getTitle() == null ? "your program" : program.getTitle();
        UUID creator = program == null ? null : program.getCourseCreatorUuid();
        UUID ownerUserUuid = creator == null
                ? null
                : courseCreatorLookupService.getCourseCreatorUserUuid(creator)
                        .or(() -> instructorLookupService.getInstructorUserUuid(creator))
                        .orElse(null);
        return new TrainingRateUpdateNotifier.Subject(TrainingApplicationType.PROGRAM, programUuid, title, ownerUserUuid);
    }

    @Override
    protected Page<ProgramTrainingRateUpdate> findUpdatesForParent(UUID programUuid, TrainingRateUpdateStatus status,
                                                                   Pageable pageable) {
        return status == null
                ? rateUpdateRepository.findForProgram(programUuid, pageable)
                : rateUpdateRepository.findForProgramWithStatus(programUuid, status, pageable);
    }

    @Override
    protected ProgramTrainingRateUpdate newUpdate() {
        return new ProgramTrainingRateUpdate();
    }

    @Override
    protected ResourceNotFoundException applicationNotFound(UUID programUuid, UUID applicationUuid) {
        return new ResourceNotFoundException(String.format(
                "Training application %s not found for program %s", applicationUuid, programUuid));
    }
}
