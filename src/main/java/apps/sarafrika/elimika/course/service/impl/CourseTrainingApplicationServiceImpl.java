package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.CourseTrainingApplicationDTO;
import apps.sarafrika.elimika.course.dto.CourseTrainingApplicationDecisionRequest;
import apps.sarafrika.elimika.course.dto.CourseTrainingApplicationRequest;
import apps.sarafrika.elimika.course.dto.CourseTrainingApplicationUpdateRequest;
import apps.sarafrika.elimika.course.dto.CourseTrainingRateCardDTO;
import apps.sarafrika.elimika.course.dto.TrainingApplicationEventDTO;
import apps.sarafrika.elimika.course.factory.CourseTrainingApplicationFactory;
import apps.sarafrika.elimika.course.factory.TrainingRateCardFactory;
import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.internal.security.CourseFootingCap;
import apps.sarafrika.elimika.course.internal.training.TrainingApplicationAccess;
import apps.sarafrika.elimika.course.internal.training.TrainingApplicationExtras;
import apps.sarafrika.elimika.course.internal.training.TrainingApplicationExtrasResolver;
import apps.sarafrika.elimika.course.internal.training.TrainingApplicationHistory;
import apps.sarafrika.elimika.course.internal.training.TrainingApplicationOffers;
import apps.sarafrika.elimika.course.internal.training.TrainingFeeFloors;
import apps.sarafrika.elimika.course.internal.training.TrainingSubmitters;
import apps.sarafrika.elimika.course.model.CourseTrainingApplication;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.repository.CourseTrainingApplicationRepository;
import apps.sarafrika.elimika.course.service.CourseTrainingApplicationService;
import apps.sarafrika.elimika.course.service.CourseTrainingRateUpdateService;
import apps.sarafrika.elimika.course.util.enums.CourseContentAccess;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicantType;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicationStatus;
import apps.sarafrika.elimika.course.util.enums.TrainingApplicationEventType;
import apps.sarafrika.elimika.course.util.enums.TrainingApplicationType;
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
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
    private final CourseFootingCap courseFootingCap;
    private final CourseTrainingRateCardValidator rateCardValidator;
    private final CourseCreatorLookupService courseCreatorLookupService;
    private final InstructorLookupService instructorLookupService;
    private final UserLookupService userLookupService;
    private final ApplicationEventPublisher eventPublisher;
    private final TrainingApplicationAccess access;
    private final TrainingApplicationExtrasResolver extrasResolver;
    private final CourseTrainingRateUpdateService rateUpdateService;
    private final TrainingFeeFloors feeFloors;
    private final TrainingApplicationHistory history;
    private final TrainingApplicationOffers offers;
    private final TrainingSubmitters submitters;

    @Override
    public CourseTrainingApplicationDTO submitApplication(UUID courseUuid, CourseTrainingApplicationRequest request) {
        log.debug("Submitting training application for course {} by {} {}", courseUuid, request.applicantType(), request.applicantUuid());

        ensureSubmitApplicantOwnedByCurrentUser(request.applicantType(), request.applicantUuid());

        Course course = courseRepository.findByUuid(courseUuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(COURSE_NOT_FOUND_TEMPLATE, courseUuid)));

        ensureNoActiveApplication(courseUuid, request.applicantType(), request.applicantUuid());

        BigDecimal minimumTrainingFee = resolveMinimumTrainingFee(course);
        CourseTrainingRateCardDTO rateCardRequest = request.rateCard();
        if (rateCardRequest == null) {
            throw new IllegalArgumentException("Rate card is required");
        }
        rateCardValidator.validateAgainstMinimum(rateCardRequest, minimumTrainingFee);

        List<UUID> venues = offers.validateVenues(request.applicantType(), request.applicantUuid(), request.offeredVenueUuids());
        offers.validateAnswers(List.of(courseUuid), request.requirementAnswers(), "this course");

        PlatformCurrency resolvedCurrency = currencyService.resolveCurrencyOrDefault(rateCardRequest.currency());
        String rateCurrency = resolvedCurrency.getCode();

        CourseTrainingApplication application = applicationRepository
                .findByCourseUuidAndApplicantTypeAndApplicantUuid(courseUuid, request.applicantType(), request.applicantUuid())
                .map(existing -> updateExistingApplication(existing, request, rateCardRequest, rateCurrency))
                .orElseGet(() -> createNewApplication(courseUuid, request, rateCardRequest, rateCurrency));

        try {
            CourseTrainingApplication saved = applicationRepository.save(application);
            // A submission is the whole application, so a resubmission without venues or answers clears the old ones.
            offers.replaceVenues(TrainingApplicationType.COURSE, saved.getUuid(), venues == null ? List.of() : venues);
            offers.replaceAnswers(TrainingApplicationType.COURSE, saved.getUuid(),
                    request.requirementAnswers() == null ? List.of() : request.requirementAnswers());
            history.record(TrainingApplicationType.COURSE, saved.getUuid(), TrainingApplicationEventType.SUBMITTED,
                    request.applicationNotes());
            publishCourseTrainingApplicationSubmitted(course, saved);
            return toDTO(saved);
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

        List<UUID> venues = offers.validateVenues(
                application.getApplicantType(), application.getApplicantUuid(), request.offeredVenueUuids());
        offers.validateAnswers(List.of(courseUuid), request.requirementAnswers(), "this course");

        application.setApplicationNotes(request.applicationNotes());
        TrainingRateCardFactory.apply(application, rateCardRequest, rateCurrency);

        CourseTrainingApplication saved = applicationRepository.save(application);
        offers.replaceVenues(TrainingApplicationType.COURSE, saved.getUuid(), venues);
        offers.replaceAnswers(TrainingApplicationType.COURSE, saved.getUuid(), request.requirementAnswers());
        history.record(TrainingApplicationType.COURSE, saved.getUuid(), TrainingApplicationEventType.EDITED,
                request.applicationNotes());
        return toDTO(saved);
    }

    @Override
    public void withdrawApplication(UUID courseUuid, UUID applicationUuid) {
        log.debug("Withdrawing training application {} for course {}", applicationUuid, courseUuid);

        CourseTrainingApplication application = findApplication(courseUuid, applicationUuid);
        ensureApplicantOwnedByCurrentUser(application.getApplicantType(), application.getApplicantUuid());

        if (application.getStatus() != CourseTrainingApplicationStatus.PENDING) {
            throw new IllegalStateException("Only pending applications can be withdrawn.");
        }

        history.record(TrainingApplicationType.COURSE, application.getUuid(), TrainingApplicationEventType.WITHDRAWN, null);
        offers.deleteFor(TrainingApplicationType.COURSE, application.getUuid());
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
        history.record(TrainingApplicationType.COURSE, saved.getUuid(), TrainingApplicationEventType.APPROVED,
                decisionRequest.reviewNotes());
        publishCourseTrainingApplicationDecision(saved, CourseTrainingApplicationStatus.APPROVED, decisionRequest.reviewNotes());
        return toDTO(saved);
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
        rateUpdateService.closePendingRateUpdate(saved.getUuid(), "Closed because the training application was rejected.");
        history.record(TrainingApplicationType.COURSE, saved.getUuid(), TrainingApplicationEventType.REJECTED,
                decisionRequest.reviewNotes());
        publishCourseTrainingApplicationDecision(saved, CourseTrainingApplicationStatus.REJECTED, decisionRequest.reviewNotes());
        return toDTO(saved);
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
        rateUpdateService.closePendingRateUpdate(saved.getUuid(), "Closed because the training approval was revoked.");
        history.record(TrainingApplicationType.COURSE, saved.getUuid(), TrainingApplicationEventType.REVOKED,
                decisionRequest.reviewNotes());
        publishCourseTrainingApplicationDecision(saved, CourseTrainingApplicationStatus.REVOKED, decisionRequest.reviewNotes());
        return toDTO(saved);
    }

    @Override
    public CourseTrainingApplicationDTO getApplication(UUID courseUuid, UUID applicationUuid) {
        log.debug("Fetching training application {} for course {}", applicationUuid, courseUuid);
        CourseTrainingApplication application = findApplication(courseUuid, applicationUuid);
        if (!canReadApplication(application)) {
            // Anyone else must not learn that the application exists, so this is the same 404 as a miss.
            throw new ResourceNotFoundException(
                    String.format(APPLICATION_NOT_FOUND_TEMPLATE, applicationUuid, courseUuid));
        }
        boolean owner = access.ownsCourse(application.getCourseUuid());
        if (owner) {
            history.recordOpenedByCreator(TrainingApplicationType.COURSE, application.getUuid());
        }
        return toDTO(application, owner);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrainingApplicationEventDTO> getApplicationHistory(UUID courseUuid, UUID applicationUuid) {
        CourseTrainingApplication application = findApplication(courseUuid, applicationUuid);
        if (!access.ownsCourse(application.getCourseUuid())
                && !access.isApplicant(application.getApplicantType(), application.getApplicantUuid())) {
            throw new ResourceNotFoundException(
                    String.format(APPLICATION_NOT_FOUND_TEMPLATE, applicationUuid, courseUuid));
        }
        return history.history(TrainingApplicationType.COURSE, application.getUuid());
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

        UUID requestedCourseCreatorUuid = null;
        String courseCreatorParam = extractSearchParam(normalizedParams, "course_creator_uuid", "courseCreatorUuid");
        if (courseCreatorParam != null) {
            try {
                requestedCourseCreatorUuid = UUID.fromString(courseCreatorParam.trim());
                String courseUuidsList = courseRepository.findUuidsByCourseCreatorUuid(requestedCourseCreatorUuid).stream()
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

        // Checked against what the client actually sent, before the redaction pass below narrows it.
        specificationBuilder.validateSortProperties(CourseTrainingApplication.class, pageable);

        CallerScope scope = resolveCallerScope(requestedCourseCreatorUuid);

        Pageable effectivePageable = pageable;
        if (scope != null) {
            // Blanking a field in the response is worth nothing while the caller can still filter or
            // order by it: `...&privateOnlineHourlyRate_lte=5000` or `...&sort=privateOnlineHourlyRate`
            // reads the hidden value back one comparison at a time.
            normalizedParams.keySet().removeIf(CourseTrainingApplicationServiceImpl::namesRedactedAttribute);
            effectivePageable = withoutRedactedSort(pageable);
        }

        Specification<CourseTrainingApplication> specification =
                specificationBuilder.buildSpecification(CourseTrainingApplication.class, normalizedParams);
        if (scope != null) {
            Specification<CourseTrainingApplication> visible = scope.toSpecification();
            specification = specification == null ? visible : specification.and(visible);
        }

        Page<CourseTrainingApplication> page = specification != null
                ? applicationRepository.findAll(specification, effectivePageable)
                : applicationRepository.findAll(effectivePageable);

        Map<UUID, TrainingApplicationExtras> extras = extrasResolver.resolve(TrainingApplicationType.COURSE,
                page.map(CourseTrainingApplication::getUuid).toList());
        Set<UUID> ownedCourseUuids = scope != null ? scope.ownedCourseUuids() : callerOwnedCourseUuids();
        Map<UUID, BigDecimal> floors = feeFloors.perCourse(page.stream()
                .map(CourseTrainingApplication::getCourseUuid)
                .filter(ownedCourseUuids::contains)
                .collect(Collectors.toSet()));
        return page.map(application -> {
            if (scope != null && !scope.isParty(application)) {
                return toNonPartyDTO(application);
            }
            TrainingApplicationExtras applicationExtras =
                    extras.getOrDefault(application.getUuid(), TrainingApplicationExtras.NONE);
            if (ownedCourseUuids.contains(application.getCourseUuid())) {
                applicationExtras = applicationExtras.withRateFloorFlags(TrainingRateCardFactory.floorFlags(
                        application, floors.get(application.getCourseUuid())));
            }
            return CourseTrainingApplicationFactory.toDTO(application, applicationExtras);
        });
    }

    /** The courses the caller created, when searching from a dashboard that may act as their creator. */
    private Set<UUID> callerOwnedCourseUuids() {
        UUID callerUuid = domainSecurityService.getCurrentUserUuid();
        if (callerUuid == null || !courseFootingCap.permits(CourseContentAccess.CREATOR)) {
            return Set.of();
        }
        return courseCreatorLookupService.findCourseCreatorUuidByUserUuid(callerUuid)
                .map(creatorUuid -> Set.copyOf(courseRepository.findUuidsByCourseCreatorUuid(creatorUuid)))
                .orElse(Set.of());
    }

    /**
     * What the caller may see of other people's applications, or {@code null} when nothing needs
     * hiding: a platform admin, or a course creator whose search already names themselves and is
     * therefore confined to their own courses.
     * <p>
     * Everyone else is a party to an application only as its applicant (their instructor profile,
     * or an organisation they belong to) or as the creator of the course it targets; those rows come
     * back whole. An <em>approved</em> application is additionally readable by any caller, because
     * "who may deliver this course" is the instructor directory that learners, guardians and
     * organisations all browse - but readable only in the redacted form of {@link #toNonPartyDTO},
     * without the rate card and notes the applicant negotiated with the course creator alone.
     * Applications still under review, rejected or revoked stay between those two parties.
     * <p>
     * Every identity below is capped by the dashboard the request came from, because an unscoped
     * answer here is the negotiated rate card — twelve hourly, session and daily rates, the currency
     * and the reviewer's notes — plus the applications that were rejected or revoked. An account
     * that is both an administrator and a learner was reading all of it from their own learner
     * dashboard. Capped, that same account has no identity left to search as and receives the
     * redacted directory every other learner gets.
     */
    private CallerScope resolveCallerScope(UUID requestedCourseCreatorUuid) {
        if (courseFootingCap.permits(CourseContentAccess.ADMIN) && domainSecurityService.isPlatformAdmin()) {
            return null;
        }
        UUID callerUuid = domainSecurityService.getCurrentUserUuid();
        if (callerUuid == null) {
            return CallerScope.NONE;
        }

        boolean readsAsCreator = courseFootingCap.permits(CourseContentAccess.CREATOR);
        UUID callerCourseCreatorUuid = readsAsCreator
                ? courseCreatorLookupService.findCourseCreatorUuidByUserUuid(callerUuid).orElse(null)
                : null;
        if (callerCourseCreatorUuid != null && callerCourseCreatorUuid.equals(requestedCourseCreatorUuid)) {
            return null;
        }

        UUID instructorUuid = courseFootingCap.permits(CourseContentAccess.INSTRUCTOR)
                ? instructorLookupService.findInstructorUuidByUserUuid(callerUuid).orElse(null)
                : null;
        Set<UUID> organisationUuids = courseFootingCap.permits(CourseContentAccess.ORGANISATION)
                ? Set.copyOf(userLookupService.getUserOrganizations(callerUuid))
                : Set.<UUID>of();
        Set<UUID> ownedCourseUuids = callerCourseCreatorUuid == null
                ? Set.<UUID>of()
                : Set.copyOf(courseRepository.findUuidsByCourseCreatorUuid(callerCourseCreatorUuid));
        return new CallerScope(instructorUuid, organisationUuids, ownedCourseUuids);
    }

    /**
     * The identities a caller searches as. Turned into a predicate so scoping happens in the query
     * rather than by filtering a page after the fact, which would break paging.
     */
    private record CallerScope(UUID instructorUuid, Set<UUID> organisationUuids, Set<UUID> ownedCourseUuids) {

        private static final CallerScope NONE = new CallerScope(null, Set.of(), Set.of());

        /** True when the caller is the applicant or the course creator, i.e. may see everything on the row. */
        boolean isParty(CourseTrainingApplication application) {
            if (ownedCourseUuids.contains(application.getCourseUuid())) {
                return true;
            }
            if (CourseTrainingApplicantType.INSTRUCTOR.equals(application.getApplicantType())) {
                return instructorUuid != null && instructorUuid.equals(application.getApplicantUuid());
            }
            if (CourseTrainingApplicantType.ORGANISATION.equals(application.getApplicantType())) {
                return organisationUuids.contains(application.getApplicantUuid());
            }
            return false;
        }

        Specification<CourseTrainingApplication> toSpecification() {
            return (root, query, cb) -> {
                List<Predicate> visible = new ArrayList<>();
                // The directory every role browses: who is approved to deliver the course. Rows the
                // caller is not a party to are redacted on the way out by toNonPartyDTO.
                visible.add(cb.equal(root.get("status"), CourseTrainingApplicationStatus.APPROVED));
                if (instructorUuid != null) {
                    visible.add(cb.and(
                            cb.equal(root.get("applicantType"), CourseTrainingApplicantType.INSTRUCTOR),
                            cb.equal(root.get("applicantUuid"), instructorUuid)));
                }
                if (!organisationUuids.isEmpty()) {
                    visible.add(cb.and(
                            cb.equal(root.get("applicantType"), CourseTrainingApplicantType.ORGANISATION),
                            root.get("applicantUuid").in(organisationUuids)));
                }
                if (!ownedCourseUuids.isEmpty()) {
                    visible.add(root.get("courseUuid").in(ownedCourseUuids));
                }
                return cb.or(visible.toArray(Predicate[]::new));
            };
        }
    }

    /**
     * The entity attributes {@link #toNonPartyDTO} blanks - the whole rate card, the notes, and who
     * reviewed or raised the application. A caller who is not shown these must not be able to filter
     * or sort by them either.
     */
    private static final Set<String> REDACTED_ATTRIBUTES = Set.of(
            "ratecurrency",
            "privateonlinehourlyrate", "privateinpersonhourlyrate",
            "grouponlinehourlyrate", "groupinpersonhourlyrate",
            "privateonlinesessionrate", "privateinpersonsessionrate",
            "grouponlinesessionrate", "groupinpersonsessionrate",
            "privateonlinedailyrate", "privateinpersondailyrate",
            "grouponlinedailyrate", "groupinpersondailyrate",
            "applicationnotes", "reviewnotes", "reviewedby", "createdby");

    /**
     * True when a search key or sort property names a redacted attribute. Matched on the prefix, and
     * ignoring case and underscores, so that every spelling the search syntax accepts for the field -
     * {@code review_notes}, {@code reviewNotes_like}, {@code privateOnlineHourlyRate_between} - is
     * caught along with the bare name.
     */
    private static boolean namesRedactedAttribute(String key) {
        if (key == null) {
            return false;
        }
        String normalised = key.toLowerCase(Locale.ROOT).replace("_", "");
        return REDACTED_ATTRIBUTES.stream().anyMatch(normalised::startsWith);
    }

    /** The requested page with any ordering by a redacted attribute dropped. */
    private static Pageable withoutRedactedSort(Pageable pageable) {
        if (pageable == null || !pageable.isPaged() || pageable.getSort().isUnsorted()) {
            return pageable;
        }
        List<Sort.Order> permitted = pageable.getSort().stream()
                .filter(order -> !namesRedactedAttribute(order.getProperty()))
                .toList();
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(permitted));
    }

    /**
     * The view a non-party gets: who is approved to deliver which course, and nothing the applicant
     * negotiated with the course creator.
     */
    private static CourseTrainingApplicationDTO toNonPartyDTO(CourseTrainingApplication application) {
        return new CourseTrainingApplicationDTO(
                application.getUuid(),
                application.getCourseUuid(),
                application.getApplicantType(),
                application.getApplicantUuid(),
                application.getStatus(),
                null,
                null,
                null,
                null,
                application.getReviewedAt(),
                application.getCreatedDate(),
                null,
                application.getLastModifiedDate(),
                null,
                null,
                null,
                null,
                null,
                null
        );
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
        TrainingRateCardFactory.apply(existing, rateCard, rateCurrency);
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
        TrainingRateCardFactory.apply(application, rateCard, rateCurrency);
        return application;
    }

    /** A platform admin (on the admin footing), the course's creator, or the applicant reads it in full. */
    private boolean canReadApplication(CourseTrainingApplication application) {
        return (courseFootingCap.permits(CourseContentAccess.ADMIN) && domainSecurityService.isPlatformAdmin())
                || access.ownsCourse(application.getCourseUuid())
                || access.isApplicant(application.getApplicantType(), application.getApplicantUuid());
    }

    private void ensureApplicantOwnedByCurrentUser(CourseTrainingApplicantType applicantType, UUID applicantUuid) {
        if (access.isApplicant(applicantType, applicantUuid)) {
            return;
        }
        if (CourseTrainingApplicantType.ORGANISATION.equals(applicantType)) {
            throw new AccessDeniedException("You may only manage training applications for an organisation you manage.");
        }
        throw new AccessDeniedException("You may only manage your own training applications.");
    }

    private void ensureSubmitApplicantOwnedByCurrentUser(CourseTrainingApplicantType applicantType, UUID applicantUuid) {
        if (access.isApplicant(applicantType, applicantUuid)) {
            return;
        }
        if (CourseTrainingApplicantType.INSTRUCTOR.equals(applicantType)) {
            throw new AccessDeniedException("Instructors may only submit training applications for themselves.");
        }
        if (CourseTrainingApplicantType.ORGANISATION.equals(applicantType)) {
            throw new AccessDeniedException("Organisations may only submit training applications for organisations they manage.");
        }
        throw new AccessDeniedException("You may only submit your own training applications.");
    }

    private CourseTrainingApplicationDTO toDTO(CourseTrainingApplication application) {
        return toDTO(application, access.ownsCourse(application.getCourseUuid()));
    }

    private CourseTrainingApplicationDTO toDTO(CourseTrainingApplication application, boolean owner) {
        TrainingApplicationExtras extras = extrasResolver.resolve(TrainingApplicationType.COURSE, application.getUuid());
        if (owner) {
            extras = extras.withRateFloorFlags(TrainingRateCardFactory.floorFlags(
                    application, feeFloors.forCourse(application.getCourseUuid())));
        }
        return CourseTrainingApplicationFactory.toDTO(application, extras);
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

    private void ensureNoActiveApplication(UUID courseUuid,
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

        boolean hasApproved = applicationRepository.existsByCourseUuidAndApplicantTypeAndApplicantUuidAndStatus(
                courseUuid,
                applicantType,
                applicantUuid,
                CourseTrainingApplicationStatus.APPROVED
        );
        if (hasApproved) {
            throw new DuplicateResourceException("Applicant is already approved to deliver this course.");
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

    /** Where an approved instructor lands from the notification. */
    private static final String INSTRUCTOR_APPLICATIONS_URL = "/dashboard/instructor/opportunities/my-applications";
    /** Where an approved organisation lands from the notification. */
    private static final String ORGANISATION_APPLICATIONS_URL = "/dashboard/organisation/my-applications";

    private void publishCourseTrainingApplicationDecision(CourseTrainingApplication application,
                                                          CourseTrainingApplicationStatus status,
                                                          String reviewNotes) {
        if (application.getApplicantUuid() == null) {
            return;
        }

        boolean organisationApplicant =
                CourseTrainingApplicantType.ORGANISATION.equals(application.getApplicantType());

        // An organisation has no single user account behind it, so the decision goes to whoever
        // submitted the application on its behalf. Without this branch organisations were never
        // told they had been approved - they had to notice the state change in the catalogue.
        UUID recipientUserUuid = organisationApplicant
                ? submitters.userOf(application.getCreatedBy()).orElse(null)
                : instructorLookupService.getInstructorUserUuid(application.getApplicantUuid()).orElse(null);
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
                organisationApplicant ? ORGANISATION_APPLICATIONS_URL : INSTRUCTOR_APPLICATIONS_URL,
                Map.of(
                        "course_uuid", application.getCourseUuid(),
                        "course_name", courseName,
                        "application_uuid", application.getUuid(),
                        "review_notes", reviewNotes == null ? "" : reviewNotes
                ),
                "course-training-application-decision:" + application.getUuid() + ":" + type,
                // The type's default audience is the instructor dashboard; an organisation's copy
                // has to land in the organisation inbox or its recipient never sees it.
                organisationApplicant ? "organisation_user" : null
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
