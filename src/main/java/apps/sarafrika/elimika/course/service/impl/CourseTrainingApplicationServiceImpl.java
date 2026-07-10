package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.CourseTrainingApplicationDTO;
import apps.sarafrika.elimika.course.dto.CourseTrainingApplicationDecisionRequest;
import apps.sarafrika.elimika.course.dto.CourseTrainingApplicationRequest;
import apps.sarafrika.elimika.course.dto.CourseTrainingApplicationUpdateRequest;
import apps.sarafrika.elimika.course.dto.CourseTrainingRateCardDTO;
import apps.sarafrika.elimika.course.factory.CourseTrainingApplicationFactory;
import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.model.CourseTrainingApplication;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.repository.CourseTrainingApplicationRepository;
import apps.sarafrika.elimika.course.service.CourseTrainingApplicationService;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicantType;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicationStatus;
import apps.sarafrika.elimika.course.validation.CourseTrainingRateCardValidator;
import apps.sarafrika.elimika.coursecreator.spi.CourseCreatorLookupService;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.shared.currency.model.PlatformCurrency;
import apps.sarafrika.elimika.shared.currency.service.CurrencyService;
import apps.sarafrika.elimika.shared.event.notification.NotificationRequestedEvent;
import apps.sarafrika.elimika.shared.exceptions.DuplicateResourceException;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.dao.DataIntegrityViolationException;
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
import java.util.stream.Collectors;

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
    private final DomainSecurityService domainSecurityService;
    private final CourseTrainingRateCardValidator rateCardValidator;
    private final CourseCreatorLookupService courseCreatorLookupService;
    private final InstructorLookupService instructorLookupService;
    private final UserLookupService userLookupService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public CourseTrainingApplicationDTO submitApplication(UUID courseUuid, CourseTrainingApplicationRequest request) {
        log.debug("Submitting training application for course {} by {} {}", courseUuid, request.applicantType(), request.applicantUuid());

        if (CourseTrainingApplicantType.INSTRUCTOR.equals(request.applicantType())
                && !domainSecurityService.isInstructorWithUuid(request.applicantUuid())) {
            throw new AccessDeniedException("Instructors may only submit training applications for themselves.");
        }

        Course course = courseRepository.findByUuid(courseUuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(COURSE_NOT_FOUND_TEMPLATE, courseUuid)));

        ensureNoPendingApplication(courseUuid, request.applicantType(), request.applicantUuid());

        BigDecimal minimumTrainingFee = resolveMinimumTrainingFee(course);
        CourseTrainingRateCardDTO rateCardRequest = request.rateCard();
        if (rateCardRequest == null) {
            throw new IllegalArgumentException("Rate card is required");
        }
        rateCardValidator.validateAgainstMinimum(rateCardRequest, minimumTrainingFee);

        PlatformCurrency resolvedCurrency = currencyService.resolveCurrencyOrDefault(rateCardRequest.currency());
        String rateCurrency = resolvedCurrency.getCode();

        CourseTrainingApplication application = applicationRepository
                .findByCourseUuidAndApplicantTypeAndApplicantUuid(courseUuid, request.applicantType(), request.applicantUuid())
                .map(existing -> updateExistingApplication(existing, request, rateCardRequest, rateCurrency))
                .orElseGet(() -> createNewApplication(courseUuid, request, rateCardRequest, rateCurrency));

        try {
            CourseTrainingApplication saved = applicationRepository.save(application);
            publishCourseTrainingApplicationSubmitted(course, saved);
            return CourseTrainingApplicationFactory.toDTO(saved);
        } catch (DataIntegrityViolationException ex) {
            String exceptionMessage = ex.getMessage();
            if (exceptionMessage != null && exceptionMessage.contains("uq_course_training_application")) {
                throw new DuplicateResourceException("You have already submitted an application to deliver this course.");
            }
            throw ex;
        }
    }

    @Override
    public CourseTrainingApplicationDTO updateApplication(UUID courseUuid,
                                                          UUID applicationUuid,
                                                          CourseTrainingApplicationUpdateRequest request) {
        log.debug("Updating training application {} for course {}", applicationUuid, courseUuid);

        CourseTrainingApplication application = findApplication(courseUuid, applicationUuid);
        ensureApplicantOwnedByCurrentUser(application.getApplicantType(), application.getApplicantUuid());

        if (application.getStatus() != CourseTrainingApplicationStatus.PENDING) {
            throw new IllegalStateException("Only pending applications can be updated.");
        }

        Course course = courseRepository.findByUuid(courseUuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(COURSE_NOT_FOUND_TEMPLATE, courseUuid)));

        CourseTrainingRateCardDTO rateCardRequest = request.rateCard();
        if (rateCardRequest == null) {
            throw new IllegalArgumentException("Rate card is required");
        }
        BigDecimal minimumTrainingFee = resolveMinimumTrainingFee(course);
        rateCardValidator.validateAgainstMinimum(rateCardRequest, minimumTrainingFee);

        PlatformCurrency resolvedCurrency = currencyService.resolveCurrencyOrDefault(rateCardRequest.currency());
        String rateCurrency = resolvedCurrency.getCode();

        application.setApplicationNotes(request.applicationNotes());
        applyRateCard(application, rateCardRequest, rateCurrency);

        CourseTrainingApplication saved = applicationRepository.save(application);
        return CourseTrainingApplicationFactory.toDTO(saved);
    }

    @Override
    public void withdrawApplication(UUID courseUuid, UUID applicationUuid) {
        log.debug("Withdrawing training application {} for course {}", applicationUuid, courseUuid);

        CourseTrainingApplication application = findApplication(courseUuid, applicationUuid);
        ensureApplicantOwnedByCurrentUser(application.getApplicantType(), application.getApplicantUuid());

        if (application.getStatus() != CourseTrainingApplicationStatus.PENDING) {
            throw new IllegalStateException("Only pending applications can be withdrawn.");
        }

        applicationRepository.delete(application);
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
        publishCourseTrainingApplicationDecision(saved, CourseTrainingApplicationStatus.APPROVED, decisionRequest.reviewNotes());
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
        publishCourseTrainingApplicationDecision(saved, CourseTrainingApplicationStatus.REJECTED, decisionRequest.reviewNotes());
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
        publishCourseTrainingApplicationDecision(saved, CourseTrainingApplicationStatus.REVOKED, decisionRequest.reviewNotes());
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

        String courseCreatorParam = extractSearchParam(normalizedParams, "course_creator_uuid", "courseCreatorUuid");
        if (courseCreatorParam != null) {
            try {
                UUID courseCreatorUuid = UUID.fromString(courseCreatorParam.trim());
                String courseUuidsList = courseRepository.findUuidsByCourseCreatorUuid(courseCreatorUuid).stream()
                        .map(UUID::toString)
                        .collect(Collectors.joining(","));

                if (courseUuidsList.isEmpty()) {
                    return Page.empty(pageable);
                }

                normalizedParams.put("courseUuid_in", courseUuidsList);
            } catch (IllegalArgumentException ex) {
                log.warn("Invalid course_creator_uuid value: {}", courseCreatorParam);
            }
        }

        Specification<CourseTrainingApplication> specification =
                specificationBuilder.buildSpecification(CourseTrainingApplication.class, normalizedParams);

        Page<CourseTrainingApplication> page = specification != null
                ? applicationRepository.findAll(specification, pageable)
                : applicationRepository.findAll(pageable);

        return page.map(CourseTrainingApplicationFactory::toDTO);
    }

    private String extractSearchParam(Map<String, String> searchParams, String... keys) {
        String extractedValue = null;
        var iterator = searchParams.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            for (String key : keys) {
                if (matchesSearchKey(entry.getKey(), key)) {
                    if (extractedValue == null && entry.getValue() != null && !entry.getValue().isBlank()) {
                        extractedValue = entry.getValue();
                    }
                    iterator.remove();
                    break;
                }
            }
        }
        return extractedValue;
    }

    private boolean matchesSearchKey(String candidate, String key) {
        return candidate.equals(key) || candidate.startsWith(key + "_");
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
                                                                CourseTrainingRateCardDTO rateCard,
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
        applyRateCard(existing, rateCard, rateCurrency);
        return existing;
    }

    private CourseTrainingApplication createNewApplication(UUID courseUuid,
                                                           CourseTrainingApplicationRequest request,
                                                           CourseTrainingRateCardDTO rateCard,
                                                           String rateCurrency) {
        CourseTrainingApplication application = new CourseTrainingApplication();
        application.setCourseUuid(courseUuid);
        application.setApplicantType(request.applicantType());
        application.setApplicantUuid(request.applicantUuid());
        application.setStatus(CourseTrainingApplicationStatus.PENDING);
        application.setApplicationNotes(request.applicationNotes());
        applyRateCard(application, rateCard, rateCurrency);
        return application;
    }

    private void applyRateCard(CourseTrainingApplication target,
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

    private void ensureApplicantOwnedByCurrentUser(CourseTrainingApplicantType applicantType, UUID applicantUuid) {
        if (CourseTrainingApplicantType.INSTRUCTOR.equals(applicantType)) {
            if (!domainSecurityService.isInstructorWithUuid(applicantUuid)) {
                throw new AccessDeniedException("You may only manage your own training applications.");
            }
            return;
        }

        if (CourseTrainingApplicantType.ORGANISATION.equals(applicantType)) {
            UUID currentUserUuid = domainSecurityService.getCurrentUserUuid();
            if (currentUserUuid == null
                    || !userLookupService.userBelongsToOrganization(currentUserUuid, applicantUuid)) {
                throw new AccessDeniedException("You may only manage training applications for your organisation.");
            }
            return;
        }

        throw new AccessDeniedException("You may only manage your own training applications.");
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

    private void ensureNoPendingApplication(UUID courseUuid,
                                            CourseTrainingApplicantType applicantType,
                                            UUID applicantUuid) {
        boolean hasPending = applicationRepository.existsByCourseUuidAndApplicantTypeAndApplicantUuidAndStatus(
                courseUuid,
                applicantType,
                applicantUuid,
                CourseTrainingApplicationStatus.PENDING
        );
        if (hasPending) {
            throw new DuplicateResourceException("An application is already pending review for this course.");
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

    private void publishCourseTrainingApplicationSubmitted(Course course, CourseTrainingApplication application) {
        if (course.getCourseCreatorUuid() == null) {
            return;
        }
        UUID recipientUserUuid = courseCreatorLookupService.getCourseCreatorUserUuid(course.getCourseCreatorUuid())
                .orElse(null);
        if (recipientUserUuid == null) {
            return;
        }

        String courseName = course.getName() == null ? "your course" : course.getName();
        eventPublisher.publishEvent(NotificationRequestedEvent.inApp(
                recipientUserUuid,
                "COURSE_TRAINING_APPLICATION_SUBMITTED",
                "INBOX",
                "Training application received",
                "An instructor applied to train " + courseName + ".",
                "/dashboard/course-management/preview/" + course.getUuid() + "?tab=applications",
                Map.of(
                        "course_uuid", course.getUuid(),
                        "course_name", courseName,
                        "application_uuid", application.getUuid(),
                        "applicant_type", application.getApplicantType().getValue(),
                        "applicant_uuid", application.getApplicantUuid()
                ),
                "course-training-application-submitted:" + application.getUuid() + ":" + application.getStatus().getValue()
        ));
    }

    private void publishCourseTrainingApplicationDecision(CourseTrainingApplication application,
                                                          CourseTrainingApplicationStatus status,
                                                          String reviewNotes) {
        if (!CourseTrainingApplicantType.INSTRUCTOR.equals(application.getApplicantType())
                || application.getApplicantUuid() == null) {
            return;
        }

        UUID recipientUserUuid = instructorLookupService.getInstructorUserUuid(application.getApplicantUuid())
                .orElse(null);
        if (recipientUserUuid == null) {
            return;
        }

        Course course = courseRepository.findByUuid(application.getCourseUuid()).orElse(null);
        String courseName = course == null || course.getName() == null ? "the course" : course.getName();
        String type = switch (status) {
            case APPROVED -> "COURSE_TRAINING_APPLICATION_APPROVED";
            case REJECTED -> "COURSE_TRAINING_APPLICATION_REJECTED";
            case REVOKED -> "COURSE_TRAINING_APPLICATION_REVOKED";
            default -> null;
        };
        if (type == null) {
            return;
        }

        String title = switch (status) {
            case APPROVED -> "Training application approved";
            case REJECTED -> "Training application rejected";
            case REVOKED -> "Training approval revoked";
            default -> "Training application updated";
        };
        String body = switch (status) {
            case APPROVED -> "You have been approved to train " + courseName + ".";
            case REJECTED -> "Your application to train " + courseName + " was rejected.";
            case REVOKED -> "Your approval to train " + courseName + " was revoked.";
            default -> "Your training application for " + courseName + " was updated.";
        };

        eventPublisher.publishEvent(NotificationRequestedEvent.inApp(
                recipientUserUuid,
                type,
                "INBOX",
                title,
                body,
                "/dashboard/instructor/applications",
                Map.of(
                        "course_uuid", application.getCourseUuid(),
                        "course_name", courseName,
                        "application_uuid", application.getUuid(),
                        "review_notes", reviewNotes == null ? "" : reviewNotes
                ),
                "course-training-application-decision:" + application.getUuid() + ":" + type
        ));

        if (status == CourseTrainingApplicationStatus.REJECTED) {
            emailUnsuccessfulApplicant(recipientUserUuid, courseName, "course", reviewNotes);
        }
    }

    /**
     * Sends the rejected instructor an email in addition to the in-app notice. Delivery
     * problems are swallowed so they never break the review workflow.
     */
    private void emailUnsuccessfulApplicant(UUID recipientUserUuid, String contextName,
                                            String contextType, String reviewNotes) {
        try {
            String recipientEmail = userLookupService.getUserEmail(recipientUserUuid).orElse(null);
            if (recipientEmail == null || recipientEmail.isBlank()) {
                return;
            }
            String recipientName = userLookupService.getUserFullName(recipientUserUuid).orElse(recipientEmail);
            eventPublisher.publishEvent(NotificationRequestedEvent.email(
                    recipientUserUuid,
                    recipientEmail,
                    recipientName,
                    "COURSE_TRAINING_APPLICATION_REJECTED",
                    Map.of(
                            "recipientName", recipientName,
                            "contextType", contextType,
                            "contextName", contextName,
                            "statusLabel", "was not successful",
                            "reviewNotes", reviewNotes == null ? "" : reviewNotes
                    )
            ));
        } catch (Exception e) {
            log.warn("Failed to email unsuccessful applicant {}: {}", recipientUserUuid, e.getMessage());
        }
    }
}
