package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.CourseTrainingApplicationDTO;
import apps.sarafrika.elimika.course.dto.CourseTrainingApplicationDecisionRequest;
import apps.sarafrika.elimika.course.dto.CourseTrainingApplicationRequest;
import apps.sarafrika.elimika.course.factory.CourseTrainingApplicationFactory;
import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.model.CourseTrainingApplication;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.repository.CourseTrainingApplicationRepository;
import apps.sarafrika.elimika.course.service.CourseTrainingApplicationService;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicantType;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicationStatus;
import apps.sarafrika.elimika.shared.currency.model.PlatformCurrency;
import apps.sarafrika.elimika.shared.currency.service.CurrencyService;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CourseTrainingApplicationServiceImpl implements CourseTrainingApplicationService {

    private static final String COURSE_NOT_FOUND_TEMPLATE = "Course with UUID %s not found";
    private static final String APPLICATION_NOT_FOUND_TEMPLATE = "Training application %s not found for course %s";
    private static final String SYSTEM_USER = "SYSTEM";

    private final CourseRepository courseRepository;
    private final CourseTrainingApplicationRepository applicationRepository;
    private final GenericSpecificationBuilder<CourseTrainingApplication> specificationBuilder;
    private final CurrencyService currencyService;

    @Override
    public CourseTrainingApplicationDTO submitApplication(UUID courseUuid, CourseTrainingApplicationRequest request) {
        log.debug("Submitting training application for course {} by {} {}", courseUuid, request.applicantType(), request.applicantUuid());

        Course course = courseRepository.findByUuid(courseUuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(COURSE_NOT_FOUND_TEMPLATE, courseUuid)));

        BigDecimal minimumTrainingFee = resolveMinimumTrainingFee(course);
        BigDecimal proposedRate = request.ratePerHourPerHead();
        if (proposedRate == null) {
            throw new IllegalArgumentException("Rate per hour per head is required");
        }
        validateRateAgainstMinimum(proposedRate, minimumTrainingFee);

        PlatformCurrency resolvedCurrency = currencyService.resolveCurrencyOrDefault(request.rateCurrency());
        String rateCurrency = resolvedCurrency.getCode();

        CourseTrainingApplication application = applicationRepository
                .findByCourseUuidAndApplicantTypeAndApplicantUuid(courseUuid, request.applicantType(), request.applicantUuid())
                .map(existing -> updateExistingApplication(existing, request, proposedRate, rateCurrency))
                .orElseGet(() -> createNewApplication(courseUuid, request, proposedRate, rateCurrency));

        CourseTrainingApplication saved = applicationRepository.save(application);
        return CourseTrainingApplicationFactory.toDTO(saved);
    }

    @Override
    public CourseTrainingApplicationDTO approveApplication(UUID courseUuid,
                                                           UUID applicationUuid,
                                                           CourseTrainingApplicationDecisionRequest decisionRequest) {
        log.debug("Approving training application {} for course {}", applicationUuid, courseUuid);

        CourseTrainingApplication application = findApplication(courseUuid, applicationUuid);
        if (application.getStatus() == CourseTrainingApplicationStatus.APPROVED) {
            throw new IllegalStateException("Application has already been approved.");
        }

        application.setStatus(CourseTrainingApplicationStatus.APPROVED);
        application.setReviewNotes(decisionRequest.reviewNotes());
        application.setReviewedBy(resolveCurrentReviewer());
        application.setReviewedAt(LocalDateTime.now(ZoneOffset.UTC));

        CourseTrainingApplication saved = applicationRepository.save(application);
        return CourseTrainingApplicationFactory.toDTO(saved);
    }

    @Override
    public CourseTrainingApplicationDTO rejectApplication(UUID courseUuid,
                                                          UUID applicationUuid,
                                                          CourseTrainingApplicationDecisionRequest decisionRequest) {
        log.debug("Rejecting training application {} for course {}", applicationUuid, courseUuid);

        CourseTrainingApplication application = findApplication(courseUuid, applicationUuid);
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

        CourseTrainingApplication saved = applicationRepository.save(application);
        return CourseTrainingApplicationFactory.toDTO(saved);
    }

    @Override
    public CourseTrainingApplicationDTO revokeApplication(UUID courseUuid,
                                                          UUID applicationUuid,
                                                          CourseTrainingApplicationDecisionRequest decisionRequest) {
        log.debug("Revoking training application {} for course {}", applicationUuid, courseUuid);

        CourseTrainingApplication application = findApplication(courseUuid, applicationUuid);
        if (application.getStatus() != CourseTrainingApplicationStatus.APPROVED) {
            throw new IllegalStateException("Only approved applications can be revoked.");
        }

        application.setStatus(CourseTrainingApplicationStatus.REVOKED);
        application.setReviewNotes(decisionRequest.reviewNotes());
        application.setReviewedBy(resolveCurrentReviewer());
        application.setReviewedAt(LocalDateTime.now(ZoneOffset.UTC));

        CourseTrainingApplication saved = applicationRepository.save(application);
        return CourseTrainingApplicationFactory.toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseTrainingApplicationDTO getApplication(UUID courseUuid, UUID applicationUuid) {
        log.debug("Fetching training application {} for course {}", applicationUuid, courseUuid);
        CourseTrainingApplication application = findApplication(courseUuid, applicationUuid);
        return CourseTrainingApplicationFactory.toDTO(application);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseTrainingApplicationDTO> getApplications(UUID courseUuid,
                                                              Optional<CourseTrainingApplicationStatus> status,
                                                              Pageable pageable) {
        log.debug("Listing training applications for course {} with status {}", courseUuid, status);

        ensureCourseExists(courseUuid);

        Map<String, String> filters = new HashMap<>();
        filters.put("courseUuid", courseUuid.toString());
        status.ifPresent(applicationStatus -> filters.put("status", applicationStatus.getValue()));

        return search(filters, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseTrainingApplicationDTO> search(Map<String, String> searchParams, Pageable pageable) {
        Map<String, String> normalizedParams = searchParams == null ? new HashMap<>() : new HashMap<>(searchParams);
        normalizedParams.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue().isBlank());

        Specification<CourseTrainingApplication> specification =
                specificationBuilder.buildSpecification(CourseTrainingApplication.class, normalizedParams);

        Page<CourseTrainingApplication> page = specification != null
                ? applicationRepository.findAll(specification, pageable)
                : applicationRepository.findAll(pageable);

        return page.map(CourseTrainingApplicationFactory::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasApprovedApplication(UUID courseUuid,
                                          CourseTrainingApplicantType applicantType,
                                          UUID applicantUuid) {
        return applicationRepository.existsByCourseUuidAndApplicantTypeAndApplicantUuidAndStatus(
                courseUuid,
                applicantType,
                applicantUuid,
                CourseTrainingApplicationStatus.APPROVED
        );
    }

    private CourseTrainingApplication updateExistingApplication(CourseTrainingApplication existing,
                                                                CourseTrainingApplicationRequest request,
                                                                BigDecimal ratePerHourPerHead,
                                                                String rateCurrency) {
        if (existing.getStatus() == CourseTrainingApplicationStatus.APPROVED) {
            throw new IllegalStateException("Applicant is already approved to deliver this course.");
        }
        if (existing.getStatus() == CourseTrainingApplicationStatus.PENDING) {
            throw new IllegalStateException("An application is already pending review for this course.");
        }

        existing.setStatus(CourseTrainingApplicationStatus.PENDING);
        existing.setApplicationNotes(request.applicationNotes());
        existing.setReviewNotes(null);
        existing.setReviewedBy(null);
        existing.setReviewedAt(null);
        existing.setRatePerHourPerHead(ratePerHourPerHead);
        existing.setRateCurrency(rateCurrency);
        return existing;
    }

    private CourseTrainingApplication createNewApplication(UUID courseUuid,
                                                           CourseTrainingApplicationRequest request,
                                                           BigDecimal ratePerHourPerHead,
                                                           String rateCurrency) {
        CourseTrainingApplication application = new CourseTrainingApplication();
        application.setCourseUuid(courseUuid);
        application.setApplicantType(request.applicantType());
        application.setApplicantUuid(request.applicantUuid());
        application.setStatus(CourseTrainingApplicationStatus.PENDING);
        application.setApplicationNotes(request.applicationNotes());
        application.setRatePerHourPerHead(ratePerHourPerHead);
        application.setRateCurrency(rateCurrency);
        return application;
    }

    private CourseTrainingApplication findApplication(UUID courseUuid, UUID applicationUuid) {
        return applicationRepository.findByUuid(applicationUuid)
                .filter(application -> courseUuid.equals(application.getCourseUuid()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(APPLICATION_NOT_FOUND_TEMPLATE, applicationUuid, courseUuid)
                ));
    }

    private BigDecimal resolveMinimumTrainingFee(Course course) {
        return course.getMinimumTrainingFee() != null ? course.getMinimumTrainingFee() : BigDecimal.ZERO;
    }

    private void validateRateAgainstMinimum(BigDecimal proposedRate, BigDecimal minimumTrainingFee) {
        if (proposedRate.compareTo(minimumTrainingFee) < 0) {
            throw new IllegalArgumentException(String.format(
                    "Rate %s cannot be less than the course minimum training fee %s",
                    proposedRate,
                    minimumTrainingFee));
        }
    }

    private void ensureCourseExists(UUID courseUuid) {
        if (!courseRepository.existsByUuid(courseUuid)) {
            throw new ResourceNotFoundException(String.format(COURSE_NOT_FOUND_TEMPLATE, courseUuid));
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
