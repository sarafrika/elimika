package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.CourseTrainingRateCardDTO;
import apps.sarafrika.elimika.course.dto.ProgramTrainingApplicationDTO;
import apps.sarafrika.elimika.course.dto.ProgramTrainingApplicationDecisionRequest;
import apps.sarafrika.elimika.course.dto.ProgramTrainingApplicationRequest;
import apps.sarafrika.elimika.course.factory.ProgramTrainingApplicationFactory;
import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.model.ProgramCourse;
import apps.sarafrika.elimika.course.model.ProgramTrainingApplication;
import apps.sarafrika.elimika.course.model.TrainingProgram;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.repository.CourseTrainingApplicationRepository;
import apps.sarafrika.elimika.course.repository.ProgramCourseRepository;
import apps.sarafrika.elimika.course.repository.ProgramTrainingApplicationRepository;
import apps.sarafrika.elimika.course.repository.TrainingProgramRepository;
import apps.sarafrika.elimika.course.service.ProgramTrainingApplicationService;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicantType;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicationStatus;
import apps.sarafrika.elimika.course.validation.CourseTrainingRateCardValidator;
import apps.sarafrika.elimika.shared.currency.model.PlatformCurrency;
import apps.sarafrika.elimika.shared.currency.service.CurrencyService;
import apps.sarafrika.elimika.shared.exceptions.DuplicateResourceException;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProgramTrainingApplicationServiceImpl implements ProgramTrainingApplicationService {

    private static final String PROGRAM_NOT_FOUND_TEMPLATE = "Training program with UUID %s not found";
    private static final String APPLICATION_NOT_FOUND_TEMPLATE = "Training application %s not found for program %s";
    private static final String SYSTEM_USER = "SYSTEM";

    private final TrainingProgramRepository trainingProgramRepository;
    private final ProgramTrainingApplicationRepository applicationRepository;
    private final ProgramCourseRepository programCourseRepository;
    private final CourseRepository courseRepository;
    private final CourseTrainingApplicationRepository courseTrainingApplicationRepository;
    private final GenericSpecificationBuilder<ProgramTrainingApplication> specificationBuilder;
    private final CurrencyService currencyService;
    private final DomainSecurityService domainSecurityService;
    private final CourseTrainingRateCardValidator rateCardValidator;

    @Override
    public ProgramTrainingApplicationDTO submitApplication(UUID programUuid, ProgramTrainingApplicationRequest request) {
        log.debug("Submitting training application for program {} by {} {}", programUuid, request.applicantType(), request.applicantUuid());

        if (CourseTrainingApplicantType.INSTRUCTOR.equals(request.applicantType())
                && !domainSecurityService.isInstructorWithUuid(request.applicantUuid())) {
            throw new AccessDeniedException("Instructors may only submit training applications for themselves.");
        }

        TrainingProgram program = trainingProgramRepository.findByUuid(programUuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(PROGRAM_NOT_FOUND_TEMPLATE, programUuid)));

        List<UUID> courseUuids = resolveProgramCourseUuids(program.getUuid());
        if (courseUuids.isEmpty()) {
            throw new IllegalStateException("Training program has no courses. Add courses before submitting applications.");
        }

        ensureNoPendingApplication(programUuid, request.applicantType(), request.applicantUuid());

        CourseTrainingRateCardDTO rateCardRequest = request.rateCard();
        BigDecimal minimumTrainingFee = resolveProgramMinimumTrainingFee(courseUuids);
        rateCardValidator.validateAgainstMinimum(rateCardRequest, minimumTrainingFee);

        PlatformCurrency resolvedCurrency = currencyService.resolveCurrencyOrDefault(rateCardRequest.currency());
        String rateCurrency = resolvedCurrency.getCode();

        ProgramTrainingApplication application = applicationRepository
                .findByProgramUuidAndApplicantTypeAndApplicantUuid(programUuid, request.applicantType(), request.applicantUuid())
                .map(existing -> updateExistingApplication(existing, request, rateCardRequest, rateCurrency))
                .orElseGet(() -> createNewApplication(programUuid, request, rateCardRequest, rateCurrency));

        try {
            ProgramTrainingApplication saved = applicationRepository.save(application);
            return ProgramTrainingApplicationFactory.toDTO(saved);
        } catch (DataIntegrityViolationException ex) {
            String exceptionMessage = ex.getMessage();
            if (exceptionMessage != null && exceptionMessage.contains("uq_program_training_application")) {
                throw new DuplicateResourceException("You have already submitted an application to deliver this training program.");
            }
            throw ex;
        }
    }

    @Override
    public ProgramTrainingApplicationDTO approveApplication(UUID programUuid,
                                                            UUID applicationUuid,
                                                            ProgramTrainingApplicationDecisionRequest decisionRequest) {
        log.debug("Approving training application {} for program {}", applicationUuid, programUuid);

        ProgramTrainingApplication application = findApplication(programUuid, applicationUuid);
        if (application.getStatus() == CourseTrainingApplicationStatus.APPROVED) {
            throw new IllegalStateException("Application has already been approved.");
        }

        ensureApplicantApprovedForProgramCourses(programUuid, application.getApplicantType(), application.getApplicantUuid());

        application.setStatus(CourseTrainingApplicationStatus.APPROVED);
        application.setReviewNotes(decisionRequest.reviewNotes());
        application.setReviewedBy(resolveCurrentReviewer());
        application.setReviewedAt(LocalDateTime.now(ZoneOffset.UTC));

        ProgramTrainingApplication saved = applicationRepository.save(application);
        return ProgramTrainingApplicationFactory.toDTO(saved);
    }

    @Override
    public ProgramTrainingApplicationDTO rejectApplication(UUID programUuid,
                                                           UUID applicationUuid,
                                                           ProgramTrainingApplicationDecisionRequest decisionRequest) {
        log.debug("Rejecting training application {} for program {}", applicationUuid, programUuid);

        ProgramTrainingApplication application = findApplication(programUuid, applicationUuid);
        if (application.getStatus() == CourseTrainingApplicationStatus.REJECTED) {
            throw new IllegalStateException("Application has already been rejected.");
        }
        if (application.getStatus() == CourseTrainingApplicationStatus.REVOKED) {
            throw new IllegalStateException("Application has already been revoked.");
        }

        application.setStatus(CourseTrainingApplicationStatus.REJECTED);
        application.setReviewNotes(decisionRequest.reviewNotes());
        application.setReviewedBy(resolveCurrentReviewer());
        application.setReviewedAt(LocalDateTime.now(ZoneOffset.UTC));

        ProgramTrainingApplication saved = applicationRepository.save(application);
        return ProgramTrainingApplicationFactory.toDTO(saved);
    }

    @Override
    public ProgramTrainingApplicationDTO revokeApplication(UUID programUuid,
                                                           UUID applicationUuid,
                                                           ProgramTrainingApplicationDecisionRequest decisionRequest) {
        log.debug("Revoking training application {} for program {}", applicationUuid, programUuid);

        ProgramTrainingApplication application = findApplication(programUuid, applicationUuid);
        if (application.getStatus() != CourseTrainingApplicationStatus.APPROVED) {
            throw new IllegalStateException("Only approved applications can be revoked.");
        }

        application.setStatus(CourseTrainingApplicationStatus.REVOKED);
        application.setReviewNotes(decisionRequest.reviewNotes());
        application.setReviewedBy(resolveCurrentReviewer());
        application.setReviewedAt(LocalDateTime.now(ZoneOffset.UTC));

        ProgramTrainingApplication saved = applicationRepository.save(application);
        return ProgramTrainingApplicationFactory.toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ProgramTrainingApplicationDTO getApplication(UUID programUuid, UUID applicationUuid) {
        log.debug("Fetching training application {} for program {}", applicationUuid, programUuid);
        ProgramTrainingApplication application = findApplication(programUuid, applicationUuid);
        return ProgramTrainingApplicationFactory.toDTO(application);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProgramTrainingApplicationDTO> getApplications(UUID programUuid,
                                                               Optional<CourseTrainingApplicationStatus> status,
                                                               Pageable pageable) {
        log.debug("Listing training applications for program {} with status {}", programUuid, status);

        ensureProgramExists(programUuid);

        Map<String, String> filters = new HashMap<>();
        filters.put("programUuid", programUuid.toString());
        status.ifPresent(applicationStatus -> filters.put("status", applicationStatus.getValue()));

        return search(filters, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProgramTrainingApplicationDTO> search(Map<String, String> searchParams, Pageable pageable) {
        Map<String, String> normalizedParams = searchParams == null ? new HashMap<>() : new HashMap<>(searchParams);
        normalizedParams.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue().isBlank());

        Specification<ProgramTrainingApplication> specification =
                specificationBuilder.buildSpecification(ProgramTrainingApplication.class, normalizedParams);

        Page<ProgramTrainingApplication> page = specification != null
                ? applicationRepository.findAll(specification, pageable)
                : applicationRepository.findAll(pageable);

        return page.map(ProgramTrainingApplicationFactory::toDTO);
    }

    private ProgramTrainingApplication updateExistingApplication(ProgramTrainingApplication existing,
                                                                 ProgramTrainingApplicationRequest request,
                                                                 CourseTrainingRateCardDTO rateCard,
                                                                 String rateCurrency) {
        if (existing.getStatus() == CourseTrainingApplicationStatus.APPROVED) {
            throw new IllegalStateException("Applicant is already approved to deliver this training program.");
        }
        if (existing.getStatus() == CourseTrainingApplicationStatus.PENDING) {
            throw new IllegalStateException("An application is already pending review for this training program.");
        }

        existing.setStatus(CourseTrainingApplicationStatus.PENDING);
        existing.setApplicationNotes(request.applicationNotes());
        existing.setReviewNotes(null);
        existing.setReviewedBy(null);
        existing.setReviewedAt(null);
        applyRateCard(existing, rateCard, rateCurrency);
        return existing;
    }

    private ProgramTrainingApplication createNewApplication(UUID programUuid,
                                                            ProgramTrainingApplicationRequest request,
                                                            CourseTrainingRateCardDTO rateCard,
                                                            String rateCurrency) {
        ProgramTrainingApplication application = new ProgramTrainingApplication();
        application.setProgramUuid(programUuid);
        application.setApplicantType(request.applicantType());
        application.setApplicantUuid(request.applicantUuid());
        application.setStatus(CourseTrainingApplicationStatus.PENDING);
        application.setApplicationNotes(request.applicationNotes());
        applyRateCard(application, rateCard, rateCurrency);
        return application;
    }

    private void applyRateCard(ProgramTrainingApplication target,
                               CourseTrainingRateCardDTO rateCard,
                               String rateCurrency) {
        if (rateCard == null) {
            throw new IllegalArgumentException("Rate card is required");
        }
        target.setRateCurrency(rateCurrency);
        target.setPrivateOnlineRate(rateCard.privateOnlineRate());
        target.setPrivateInpersonRate(rateCard.privateInpersonRate());
        target.setGroupOnlineRate(rateCard.groupOnlineRate());
        target.setGroupInpersonRate(rateCard.groupInpersonRate());
    }

    private ProgramTrainingApplication findApplication(UUID programUuid, UUID applicationUuid) {
        return applicationRepository.findByUuid(applicationUuid)
                .filter(application -> programUuid.equals(application.getProgramUuid()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(APPLICATION_NOT_FOUND_TEMPLATE, applicationUuid, programUuid)
                ));
    }

    private void ensureNoPendingApplication(UUID programUuid,
                                            CourseTrainingApplicantType applicantType,
                                            UUID applicantUuid) {
        boolean hasPending = applicationRepository.existsByProgramUuidAndApplicantTypeAndApplicantUuidAndStatus(
                programUuid,
                applicantType,
                applicantUuid,
                CourseTrainingApplicationStatus.PENDING
        );
        if (hasPending) {
            throw new DuplicateResourceException("An application is already pending review for this training program.");
        }
    }

    private void ensureProgramExists(UUID programUuid) {
        if (!trainingProgramRepository.existsByUuid(programUuid)) {
            throw new ResourceNotFoundException(String.format(PROGRAM_NOT_FOUND_TEMPLATE, programUuid));
        }
    }

    private List<UUID> resolveProgramCourseUuids(UUID programUuid) {
        return programCourseRepository.findByProgramUuidOrderBySequenceOrderAsc(programUuid)
                .stream()
                .map(ProgramCourse::getCourseUuid)
                .filter(uuid -> uuid != null)
                .distinct()
                .collect(Collectors.toList());
    }

    private BigDecimal resolveProgramMinimumTrainingFee(List<UUID> courseUuids) {
        if (courseUuids == null || courseUuids.isEmpty()) {
            return BigDecimal.ZERO;
        }

        List<Course> courses = courseRepository.findByUuidIn(courseUuids);
        return courses.stream()
                .map(course -> course.getMinimumTrainingFee() != null ? course.getMinimumTrainingFee() : BigDecimal.ZERO)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    private void ensureApplicantApprovedForProgramCourses(UUID programUuid,
                                                          CourseTrainingApplicantType applicantType,
                                                          UUID applicantUuid) {
        List<UUID> courseUuids = resolveProgramCourseUuids(programUuid);
        if (courseUuids.isEmpty()) {
            throw new IllegalStateException("Training program has no courses. Add courses before approving applications.");
        }

        Set<UUID> missingApprovals = courseUuids.stream()
                .filter(courseUuid -> !courseTrainingApplicationRepository
                        .existsByCourseUuidAndApplicantTypeAndApplicantUuidAndStatus(
                                courseUuid,
                                applicantType,
                                applicantUuid,
                                CourseTrainingApplicationStatus.APPROVED))
                .collect(Collectors.toSet());

        if (!missingApprovals.isEmpty()) {
            String missingList = missingApprovals.stream()
                    .map(UUID::toString)
                    .collect(Collectors.joining(", "));
            throw new IllegalStateException(String.format(
                    "Applicant must be approved to deliver all courses in the program before approval. Missing approvals for courses: %s",
                    missingList));
        }
    }

    private String resolveCurrentReviewer() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return SYSTEM_USER;
        }
        return authentication.getName();
    }
}
