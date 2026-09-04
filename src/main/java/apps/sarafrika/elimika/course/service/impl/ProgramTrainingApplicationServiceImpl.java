package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.CourseTrainingRateCardDTO;
import apps.sarafrika.elimika.course.dto.ProgramTrainingApplicationDTO;
import apps.sarafrika.elimika.course.dto.ProgramTrainingApplicationDecisionRequest;
import apps.sarafrika.elimika.course.dto.ProgramTrainingApplicationRequest;
import apps.sarafrika.elimika.course.dto.ProgramTrainingApplicationUpdateRequest;
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
import apps.sarafrika.elimika.coursecreator.spi.CourseCreatorLookupService;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.shared.currency.model.PlatformCurrency;
import apps.sarafrika.elimika.shared.currency.service.CurrencyService;
import apps.sarafrika.elimika.shared.event.notification.NotificationRequestedEvent;
import apps.sarafrika.elimika.shared.exceptions.DuplicateResourceException;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
public class ProgramTrainingApplicationServiceImpl implements ProgramTrainingApplicationService {

    private static final String PROGRAM_NOT_FOUND_TEMPLATE = "Training program with UUID %s not found";
    private static final String APPLICATION_NOT_FOUND_TEMPLATE = "Training application %s not found for program %s";
    private static final String SYSTEM_USER = "SYSTEM";

    /**
     * Organisation-scoped roles that let someone act for the organisation rather than merely belong
     * to it. Membership alone is too broad to hand over the organisation's negotiated rate cards or
     * the right to apply in its name: the same mapping table holds its students, their guardians and
     * the instructors it has engaged. These are the two domains
     * {@code OrganisationSecurityService#canManageOrganisation} admits, and this gate deliberately
     * matches it so "may act for the organisation" means one thing across the platform.
     */
    private static final List<UserDomain> ORGANISATION_STAFF_DOMAINS =
            List.of(UserDomain.organisation_user, UserDomain.admin);

    /**
     * The fields {@link #toNonPartyDTO(ProgramTrainingApplication)} withholds, normalised the way
     * {@link #searchField(String)} normalises an incoming key. Masking a field in the response is
     * worthless while the same field can still be filtered or sorted on, so a search that mentions
     * one of these is answered without the rows that would have been masked.
     */
    private static final Set<String> CONFIDENTIAL_FIELDS = Set.of(
            "ratecurrency",
            "privateonlinehourlyrate", "privateinpersonhourlyrate",
            "grouponlinehourlyrate", "groupinpersonhourlyrate",
            "privateonlinesessionrate", "privateinpersonsessionrate",
            "grouponlinesessionrate", "groupinpersonsessionrate",
            "privateonlinedailyrate", "privateinpersondailyrate",
            "grouponlinedailyrate", "groupinpersondailyrate",
            "applicationnotes", "reviewnotes", "reviewedby",
            "createdby", "lastmodifiedby", "updatedby");

    /**
     * Mirrors the operation suffixes {@code GenericSpecificationBuilder} recognises, so a key is
     * reduced to its field the same way the builder reduces it.
     */
    private static final Set<String> SEARCH_OPERATIONS = Set.of(
            "eq", "gt", "lt", "gte", "lte", "like", "startswith", "endswith",
            "in", "notin", "noteq", "between", "notingroup");

    private final TrainingProgramRepository trainingProgramRepository;
    private final ProgramTrainingApplicationRepository applicationRepository;
    private final ProgramCourseRepository programCourseRepository;
    private final CourseRepository courseRepository;
    private final CourseTrainingApplicationRepository courseTrainingApplicationRepository;
    private final GenericSpecificationBuilder<ProgramTrainingApplication> specificationBuilder;
    private final CurrencyService currencyService;
    private final DomainSecurityService domainSecurityService;
    private final CourseTrainingRateCardValidator rateCardValidator;
    private final CourseCreatorLookupService courseCreatorLookupService;
    private final InstructorLookupService instructorLookupService;
    private final UserLookupService userLookupService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public ProgramTrainingApplicationDTO submitApplication(UUID programUuid, ProgramTrainingApplicationRequest request) {
        log.debug("Submitting training application for program {} by {} {}", programUuid, request.applicantType(), request.applicantUuid());

        ensureSubmitApplicantOwnedByCurrentUser(request.applicantType(), request.applicantUuid());

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
            publishProgramTrainingApplicationSubmitted(program, saved);
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
    public ProgramTrainingApplicationDTO updateApplication(UUID programUuid,
                                                           UUID applicationUuid,
                                                           ProgramTrainingApplicationUpdateRequest request) {
        log.debug("Updating training application {} for program {}", applicationUuid, programUuid);

        ProgramTrainingApplication application = findApplication(programUuid, applicationUuid);
        ensureApplicantOwnedByCurrentUser(application.getApplicantType(), application.getApplicantUuid());

        if (application.getStatus() != CourseTrainingApplicationStatus.PENDING) {
            throw new IllegalStateException("Only pending applications can be updated.");
        }

        List<UUID> courseUuids = resolveProgramCourseUuids(programUuid);
        if (courseUuids.isEmpty()) {
            throw new IllegalStateException("Training program has no courses. Add courses before updating applications.");
        }

        CourseTrainingRateCardDTO rateCardRequest = request.rateCard();
        if (rateCardRequest == null) {
            throw new IllegalArgumentException("Rate card is required");
        }
        BigDecimal minimumTrainingFee = resolveProgramMinimumTrainingFee(courseUuids);
        rateCardValidator.validateAgainstMinimum(rateCardRequest, minimumTrainingFee);

        PlatformCurrency resolvedCurrency = currencyService.resolveCurrencyOrDefault(rateCardRequest.currency());
        String rateCurrency = resolvedCurrency.getCode();

        application.setApplicationNotes(request.applicationNotes());
        applyRateCard(application, rateCardRequest, rateCurrency);

        ProgramTrainingApplication saved = applicationRepository.save(application);
        return ProgramTrainingApplicationFactory.toDTO(saved);
    }

    @Override
    public void withdrawApplication(UUID programUuid, UUID applicationUuid) {
        log.debug("Withdrawing training application {} for program {}", applicationUuid, programUuid);

        ProgramTrainingApplication application = findApplication(programUuid, applicationUuid);
        ensureApplicantOwnedByCurrentUser(application.getApplicantType(), application.getApplicantUuid());

        if (application.getStatus() != CourseTrainingApplicationStatus.PENDING) {
            throw new IllegalStateException("Only pending applications can be withdrawn.");
        }

        applicationRepository.delete(application);
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
        publishProgramTrainingApplicationDecision(saved, CourseTrainingApplicationStatus.APPROVED, decisionRequest.reviewNotes());
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
        publishProgramTrainingApplicationDecision(saved, CourseTrainingApplicationStatus.REJECTED, decisionRequest.reviewNotes());
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
        publishProgramTrainingApplicationDecision(saved, CourseTrainingApplicationStatus.REVOKED, decisionRequest.reviewNotes());
        return ProgramTrainingApplicationFactory.toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ProgramTrainingApplicationDTO getApplication(UUID programUuid, UUID applicationUuid) {
        log.debug("Fetching training application {} for program {}", applicationUuid, programUuid);
        ProgramTrainingApplication application = findApplication(programUuid, applicationUuid);
        if (!canReadApplication(application)) {
            // Anyone else must not learn that the application exists, so this is the same 404 as a miss.
            throw new ResourceNotFoundException(
                    String.format(APPLICATION_NOT_FOUND_TEMPLATE, applicationUuid, programUuid));
        }
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

        // The application row carries no creator column; a creator filter is answered by naming
        // their programs, exactly as the course search names the creator's courses.
        UUID requestedCourseCreatorUuid = null;
        String courseCreatorParam = extractSearchParam(normalizedParams, "course_creator_uuid", "courseCreatorUuid");
        if (courseCreatorParam != null) {
            try {
                requestedCourseCreatorUuid = UUID.fromString(courseCreatorParam.trim());
                String programUuidsList = programUuidsCreatedBy(requestedCourseCreatorUuid).stream()
                        .map(UUID::toString)
                        .collect(Collectors.joining(","));

                if (programUuidsList.isEmpty()) {
                    return Page.empty(pageable);
                }

                normalizedParams.put("programUuid_in", programUuidsList);
            } catch (IllegalArgumentException ex) {
                log.warn("Invalid course_creator_uuid value: {}", courseCreatorParam);
            }
        }

        CallerScope scope = resolveCallerScope(
                requestedCourseCreatorUuid, mentionsConfidentialField(normalizedParams, pageable));

        Specification<ProgramTrainingApplication> specification =
                specificationBuilder.buildSpecification(ProgramTrainingApplication.class, normalizedParams);
        if (scope != null) {
            Specification<ProgramTrainingApplication> visible = scope.toSpecification();
            specification = specification == null ? visible : specification.and(visible);
        }

        Page<ProgramTrainingApplication> page = specification != null
                ? applicationRepository.findAll(specification, pageable)
                : applicationRepository.findAll(pageable);

        return page.map(application -> scope == null || scope.isParty(application)
                ? ProgramTrainingApplicationFactory.toDTO(application)
                : toNonPartyDTO(application));
    }

    /**
     * What the caller may see of other people's applications, or {@code null} when nothing needs
     * hiding: a platform admin, or a course creator whose search already names themselves and is
     * therefore confined to their own programs.
     * <p>
     * Everyone else is a party to an application only as its applicant (their instructor profile,
     * or an organisation they are staff of) or as the creator of the program it targets; those rows
     * come back whole. An organisation additionally needs to know which instructors are approved on
     * the programs it is itself approved to train, so it can staff its classes - those rows come back
     * without the rate card and notes, which were negotiated with the program creator, not with the
     * organisation. Nothing else is visible.
     *
     * @param requestedCourseCreatorUuid the creator the search names, if any
     * @param confidentialFieldQueried whether the request filters or sorts on a field the masked view
     *                                 withholds; when it does, the masked rows are left out of the
     *                                 result entirely rather than being ordered or sifted by a value
     *                                 the caller is not allowed to read
     */
    private CallerScope resolveCallerScope(UUID requestedCourseCreatorUuid, boolean confidentialFieldQueried) {
        if (domainSecurityService.isPlatformAdmin()) {
            return null;
        }
        UUID callerUuid = domainSecurityService.getCurrentUserUuid();
        if (callerUuid == null) {
            return CallerScope.NONE;
        }

        Set<UUID> creatorIdentities = programCreatorIdentities(callerUuid);
        if (requestedCourseCreatorUuid != null && creatorIdentities.contains(requestedCourseCreatorUuid)) {
            return null;
        }

        UUID instructorUuid = instructorLookupService.findInstructorUuidByUserUuid(callerUuid).orElse(null);
        Set<UUID> organisationUuids = staffedOrganisations(callerUuid);
        Set<UUID> ownedProgramUuids = programUuidsCreatedBy(creatorIdentities);
        return new CallerScope(instructorUuid, organisationUuids, ownedProgramUuids, !confidentialFieldQueried);
    }

    /**
     * The organisations the caller searches on behalf of: those they hold an active organisation
     * scoped staff role in. Plain membership is not enough - the same mapping table records an
     * organisation's students and parents, and an organisation's negotiated rates are not theirs.
     */
    private Set<UUID> staffedOrganisations(UUID callerUuid) {
        return userLookupService.getUserOrganizations(callerUuid).stream()
                .filter(organisationUuid -> isOrganisationStaff(callerUuid, organisationUuid))
                .collect(Collectors.toSet());
    }

    /**
     * Whether the request filters or sorts on a field the masked view withholds.
     */
    private boolean mentionsConfidentialField(Map<String, String> searchParams, Pageable pageable) {
        boolean filtered = searchParams.keySet().stream()
                .anyMatch(key -> CONFIDENTIAL_FIELDS.contains(searchField(key)));
        boolean sorted = pageable != null && pageable.getSort().stream()
                .anyMatch(order -> CONFIDENTIAL_FIELDS.contains(searchField(order.getProperty())));
        return filtered || sorted;
    }

    /**
     * Reduces a search or sort key to the field it addresses: drops a trailing operation suffix, then
     * folds case and underscores so {@code private_online_hourly_rate_gt} and
     * {@code privateOnlineHourlyRate} land on the same name, exactly as the specification builder's
     * field map does.
     */
    private static String searchField(String key) {
        if (key == null) {
            return "";
        }
        String field = key;
        int lastUnderscore = field.lastIndexOf('_');
        if (lastUnderscore > 0
                && SEARCH_OPERATIONS.contains(field.substring(lastUnderscore + 1).toLowerCase(Locale.ROOT))) {
            field = field.substring(0, lastUnderscore);
        }
        return field.toLowerCase(Locale.ROOT).replace("_", "");
    }

    /**
     * The profile UUIDs a program's {@code course_creator_uuid} may legitimately hold for this user.
     * The course-creator dashboard stamps that column with their course-creator profile while the
     * instructor dashboard's program builder stamps it with their instructor profile, so a caller
     * authors programs under either identity and both must count as theirs.
     */
    private Set<UUID> programCreatorIdentities(UUID userUuid) {
        Set<UUID> identities = new HashSet<>();
        courseCreatorLookupService.findCourseCreatorUuidByUserUuid(userUuid).ifPresent(identities::add);
        instructorLookupService.findInstructorUuidByUserUuid(userUuid).ifPresent(identities::add);
        return identities;
    }

    private Set<UUID> programUuidsCreatedBy(UUID courseCreatorUuid) {
        return programUuidsCreatedBy(Set.of(courseCreatorUuid));
    }

    private Set<UUID> programUuidsCreatedBy(Set<UUID> creatorUuids) {
        return creatorUuids.stream()
                .map(trainingProgramRepository::findByCourseCreatorUuid)
                .flatMap(List::stream)
                .map(TrainingProgram::getUuid)
                .collect(Collectors.toSet());
    }

    /**
     * The identities a caller searches as. Turned into a predicate so scoping happens in the query
     * rather than by filtering a page after the fact, which would break paging.
     */
    private record CallerScope(UUID instructorUuid,
                               Set<UUID> organisationUuids,
                               Set<UUID> ownedProgramUuids,
                               boolean includeApprovedProgramPeers) {

        private static final CallerScope NONE = new CallerScope(null, Set.of(), Set.of(), false);

        /** True when the caller is the applicant or the program creator, i.e. may see everything on the row. */
        boolean isParty(ProgramTrainingApplication application) {
            if (ownedProgramUuids.contains(application.getProgramUuid())) {
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

        Specification<ProgramTrainingApplication> toSpecification() {
            return (root, query, cb) -> {
                List<Predicate> visible = new ArrayList<>();
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
                if (!organisationUuids.isEmpty() && includeApprovedProgramPeers) {
                    Subquery<UUID> programsApprovedForOrganisation = query.subquery(UUID.class);
                    Root<ProgramTrainingApplication> organisationApplication =
                            programsApprovedForOrganisation.from(ProgramTrainingApplication.class);
                    programsApprovedForOrganisation.select(organisationApplication.get("programUuid")).where(
                            cb.equal(organisationApplication.get("applicantType"), CourseTrainingApplicantType.ORGANISATION),
                            organisationApplication.get("applicantUuid").in(organisationUuids),
                            cb.equal(organisationApplication.get("status"), CourseTrainingApplicationStatus.APPROVED));
                    visible.add(cb.and(
                            cb.equal(root.get("applicantType"), CourseTrainingApplicantType.INSTRUCTOR),
                            cb.equal(root.get("status"), CourseTrainingApplicationStatus.APPROVED),
                            root.get("programUuid").in(programsApprovedForOrganisation)));
                }
                if (!ownedProgramUuids.isEmpty()) {
                    visible.add(root.get("programUuid").in(ownedProgramUuids));
                }
                return visible.isEmpty() ? cb.disjunction() : cb.or(visible.toArray(Predicate[]::new));
            };
        }
    }

    /**
     * The view a non-party gets: who is approved to deliver which program, and nothing the applicant
     * negotiated with the program creator.
     */
    private static ProgramTrainingApplicationDTO toNonPartyDTO(ProgramTrainingApplication application) {
        return new ProgramTrainingApplicationDTO(
                application.getUuid(),
                application.getProgramUuid(),
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
        target.setPrivateOnlineHourlyRate(rateCard.privateOnlineHourlyRate());
        target.setPrivateInpersonHourlyRate(rateCard.privateInpersonHourlyRate());
        target.setGroupOnlineHourlyRate(rateCard.groupOnlineHourlyRate());
        target.setGroupInpersonHourlyRate(rateCard.groupInpersonHourlyRate());
        target.setPrivateOnlineSessionRate(rateCard.privateOnlineSessionRate());
        target.setPrivateInpersonSessionRate(rateCard.privateInpersonSessionRate());
        target.setGroupOnlineSessionRate(rateCard.groupOnlineSessionRate());
        target.setGroupInpersonSessionRate(rateCard.groupInpersonSessionRate());
        target.setPrivateOnlineDailyRate(rateCard.privateOnlineDailyRate());
        target.setPrivateInpersonDailyRate(rateCard.privateInpersonDailyRate());
        target.setGroupOnlineDailyRate(rateCard.groupOnlineDailyRate());
        target.setGroupInpersonDailyRate(rateCard.groupInpersonDailyRate());
    }

    /**
     * Whether the caller may read this application in full: a platform admin, the creator of the
     * program it targets, or the applicant.
     */
    private boolean canReadApplication(ProgramTrainingApplication application) {
        return domainSecurityService.isPlatformAdmin()
                || isProgramOwnedByCurrentUser(application.getProgramUuid())
                || isApplicantOwnedByCurrentUser(application.getApplicantType(), application.getApplicantUuid());
    }

    private boolean isProgramOwnedByCurrentUser(UUID programUuid) {
        UUID callerUuid = domainSecurityService.getCurrentUserUuid();
        if (callerUuid == null) {
            return false;
        }
        Set<UUID> creatorIdentities = programCreatorIdentities(callerUuid);
        if (creatorIdentities.isEmpty()) {
            return false;
        }
        return trainingProgramRepository.findByUuid(programUuid)
                .map(program -> program.getCourseCreatorUuid() != null
                        && creatorIdentities.contains(program.getCourseCreatorUuid()))
                .orElse(false);
    }

    /**
     * Whether the caller is the applicant: their own instructor profile, or an organisation they
     * hold a staff role in. Bare membership of the organisation is not enough - the same mapping
     * table records its students and their guardians, and applying to deliver a program on the
     * organisation's terms is not theirs to do.
     */
    private boolean isApplicantOwnedByCurrentUser(CourseTrainingApplicantType applicantType, UUID applicantUuid) {
        if (CourseTrainingApplicantType.INSTRUCTOR.equals(applicantType)) {
            return domainSecurityService.isInstructorWithUuid(applicantUuid);
        }

        if (CourseTrainingApplicantType.ORGANISATION.equals(applicantType)) {
            UUID currentUserUuid = domainSecurityService.getCurrentUserUuid();
            return currentUserUuid != null && isOrganisationStaff(currentUserUuid, applicantUuid);
        }

        return false;
    }

    /**
     * Whether the user holds an active organisation-scoped staff role in the organisation.
     */
    private boolean isOrganisationStaff(UUID userUuid, UUID organisationUuid) {
        return ORGANISATION_STAFF_DOMAINS.stream()
                .anyMatch(domain -> userLookupService.userBelongsToOrganizationWithDomain(
                        userUuid, organisationUuid, domain));
    }

    private void ensureApplicantOwnedByCurrentUser(CourseTrainingApplicantType applicantType, UUID applicantUuid) {
        if (isApplicantOwnedByCurrentUser(applicantType, applicantUuid)) {
            return;
        }
        if (CourseTrainingApplicantType.ORGANISATION.equals(applicantType)) {
            throw new AccessDeniedException("You may only manage training applications for your organisation.");
        }
        throw new AccessDeniedException("You may only manage your own training applications.");
    }

    /**
     * Guards the submit path. The applicant named in the body is the party the rate card and any
     * resulting approval belong to, so the caller has to be that party: their own instructor profile,
     * or an organisation they hold a staff role in. Without this an applicant type of
     * {@code organisation} let any authenticated user file an application - and a rate card - in a
     * stranger's organisation's name.
     */
    private void ensureSubmitApplicantOwnedByCurrentUser(CourseTrainingApplicantType applicantType, UUID applicantUuid) {
        if (isApplicantOwnedByCurrentUser(applicantType, applicantUuid)) {
            return;
        }
        if (CourseTrainingApplicantType.INSTRUCTOR.equals(applicantType)) {
            throw new AccessDeniedException("Instructors may only submit training applications for themselves.");
        }
        if (CourseTrainingApplicantType.ORGANISATION.equals(applicantType)) {
            throw new AccessDeniedException(
                    "Organisations may only submit training applications for organisations they are staff of.");
        }
        throw new AccessDeniedException("You may only submit your own training applications.");
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

    private void publishProgramTrainingApplicationSubmitted(TrainingProgram program, ProgramTrainingApplication application) {
        if (program.getCourseCreatorUuid() == null) {
            return;
        }
        UUID recipientUserUuid = courseCreatorLookupService.getCourseCreatorUserUuid(program.getCourseCreatorUuid())
                .orElse(null);
        if (recipientUserUuid == null) {
            return;
        }

        String programTitle = program.getTitle() == null ? "your program" : program.getTitle();
        eventPublisher.publishEvent(NotificationRequestedEvent.inApp(
                recipientUserUuid,
                "PROGRAM_TRAINING_APPLICATION_SUBMITTED",
                "INBOX",
                "Program training application received",
                "An instructor applied to train " + programTitle + ".",
                "/dashboard/programs/" + program.getUuid() + "?tab=applications",
                Map.of(
                        "program_uuid", program.getUuid(),
                        "program_title", programTitle,
                        "application_uuid", application.getUuid(),
                        "applicant_type", application.getApplicantType().getValue(),
                        "applicant_uuid", application.getApplicantUuid()
                ),
                "program-training-application-submitted:" + application.getUuid() + ":" + application.getStatus().getValue()
        ));
    }

    /** Where an approved instructor lands from the notification. */
    private static final String INSTRUCTOR_APPLICATIONS_URL = "/dashboard/instructor/opportunities/my-applications";
    /** Where an approved organisation lands from the notification. */
    private static final String ORGANISATION_APPLICATIONS_URL = "/dashboard/organisation/my-applications";

    /** The user who submitted an organisation's application, resolved from the audit trail. */
    private UUID resolveApplicationSubmitter(ProgramTrainingApplication application) {
        String submittedBy = application.getCreatedBy();
        if (submittedBy == null || submittedBy.isBlank()) {
            return null;
        }
        return userLookupService.findUserUuidByEmail(submittedBy).orElse(null);
    }

    private void publishProgramTrainingApplicationDecision(ProgramTrainingApplication application,
                                                           CourseTrainingApplicationStatus status,
                                                           String reviewNotes) {
        if (application.getApplicantUuid() == null) {
            return;
        }

        boolean organisationApplicant =
                CourseTrainingApplicantType.ORGANISATION.equals(application.getApplicantType());

        // Same reasoning as the course flow: an organisation is notified through the user who
        // applied for it, since the organisation itself has no account to deliver to.
        UUID recipientUserUuid = organisationApplicant
                ? resolveApplicationSubmitter(application)
                : instructorLookupService.getInstructorUserUuid(application.getApplicantUuid()).orElse(null);
        if (recipientUserUuid == null) {
            return;
        }

        TrainingProgram program = trainingProgramRepository.findByUuid(application.getProgramUuid()).orElse(null);
        String programTitle = program == null || program.getTitle() == null ? "the program" : program.getTitle();
        String type = switch (status) {
            case APPROVED -> "PROGRAM_TRAINING_APPLICATION_APPROVED";
            case REJECTED -> "PROGRAM_TRAINING_APPLICATION_REJECTED";
            case REVOKED -> "PROGRAM_TRAINING_APPLICATION_REVOKED";
            default -> null;
        };
        if (type == null) {
            return;
        }

        String title = switch (status) {
            case APPROVED -> "Program training approved";
            case REJECTED -> "Program training rejected";
            case REVOKED -> "Program training approval revoked";
            default -> "Program training application updated";
        };
        String body = switch (status) {
            case APPROVED -> "You have been approved to train " + programTitle + ".";
            case REJECTED -> "Your application to train " + programTitle + " was rejected.";
            case REVOKED -> "Your approval to train " + programTitle + " was revoked.";
            default -> "Your training application for " + programTitle + " was updated.";
        };

        eventPublisher.publishEvent(NotificationRequestedEvent.inApp(
                recipientUserUuid,
                type,
                "INBOX",
                title,
                body,
                organisationApplicant ? ORGANISATION_APPLICATIONS_URL : INSTRUCTOR_APPLICATIONS_URL,
                Map.of(
                        "program_uuid", application.getProgramUuid(),
                        "program_title", programTitle,
                        "application_uuid", application.getUuid(),
                        "review_notes", reviewNotes == null ? "" : reviewNotes
                ),
                "program-training-application-decision:" + application.getUuid() + ":" + type,
                organisationApplicant ? "organisation_user" : null
        ));

        if (status == CourseTrainingApplicationStatus.REJECTED) {
            emailUnsuccessfulApplicant(recipientUserUuid, programTitle, reviewNotes);
        }
    }

    /**
     * Sends the rejected instructor an email in addition to the in-app notice. Delivery
     * problems are swallowed so they never break the review workflow.
     */
    private void emailUnsuccessfulApplicant(UUID recipientUserUuid, String programTitle, String reviewNotes) {
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
                    "PROGRAM_TRAINING_APPLICATION_REJECTED",
                    Map.of(
                            "recipientName", recipientName,
                            "contextType", "programme",
                            "contextName", programTitle,
                            "statusLabel", "was not successful",
                            "reviewNotes", reviewNotes == null ? "" : reviewNotes
                    )
            ));
        } catch (Exception e) {
            log.warn("Failed to email unsuccessful applicant {}: {}", recipientUserUuid, e.getMessage());
        }
    }
}
