package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.RecommendationEvaluationDTO;
import apps.sarafrika.elimika.course.dto.RecommendationReasonDTO;
import apps.sarafrika.elimika.course.dto.RecommendedCourseDTO;
import apps.sarafrika.elimika.course.internal.recommend.CandidateCourse;
import apps.sarafrika.elimika.course.internal.recommend.CourseCandidateRetriever;
import apps.sarafrika.elimika.course.internal.recommend.CourseCandidateRetriever.CandidateQuery;
import apps.sarafrika.elimika.course.internal.recommend.CourseCandidateRetriever.Retrieval;
import apps.sarafrika.elimika.course.internal.recommend.CourseCandidateStore;
import apps.sarafrika.elimika.course.internal.recommend.CourseCandidateStore.Audience;
import apps.sarafrika.elimika.course.internal.recommend.LearnerContextLoader;
import apps.sarafrika.elimika.course.internal.recommend.LearnerContextLoader.LearnerContext;
import apps.sarafrika.elimika.course.internal.recommend.LearnerProfile;
import apps.sarafrika.elimika.course.internal.recommend.RecommendationEvaluator;
import apps.sarafrika.elimika.course.internal.recommend.RecommendationScorer;
import apps.sarafrika.elimika.course.internal.recommend.RecommendationTypes;
import apps.sarafrika.elimika.course.internal.recommend.RecommendationTypes.AffiliationOffer;
import apps.sarafrika.elimika.course.internal.recommend.RecommendationTypes.LearnerSignals;
import apps.sarafrika.elimika.course.internal.recommend.RecommendationTypes.Reason;
import apps.sarafrika.elimika.course.internal.recommend.RecommendationTypes.ReasonCode;
import apps.sarafrika.elimika.course.internal.recommend.RecommendationTypes.ScoredCourse;
import apps.sarafrika.elimika.course.internal.recommend.RecommendationTypes.Surface;
import apps.sarafrika.elimika.course.service.CourseRecommendationService;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.spi.LearnerProfileLookupService;
import apps.sarafrika.elimika.shared.tracking.discovery.DiscoveryImpression;
import apps.sarafrika.elimika.shared.tracking.discovery.DiscoveryTracker;
import apps.sarafrika.elimika.tenancy.spi.OrganisationLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Course recommendations "rules-v2" (roadmap §03, §09, §10): SQL decides, Meilisearch narrows, Java ranks.
 * <ol>
 *   <li>The learner's own rows from SQL ({@link LearnerContextLoader}).</li>
 *   <li>Candidates from one multi-search over public courses in the learner's age band, excluding their
 *       courses; SQL over the same filters when search is off ({@link CourseCandidateRetriever}).</li>
 *   <li>Scoring, diversity and reasons in Java ({@link RecommendationScorer}).</li>
 *   <li>Every candidate is loaded through the public-catalogue SQL, so stale hits drop out; impressions
 *       are recorded with a fresh {@code recommendation_id}.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseRecommendationServiceImpl implements CourseRecommendationService {

    static final int DEFAULT_LIMIT = 6;
    static final int MAX_LIMIT = 50;

    private final LearnerContextLoader learnerContextLoader;
    private final CourseCandidateRetriever candidateRetriever;
    private final CourseCandidateStore candidateStore;
    private final RecommendationEvaluator recommendationEvaluator;
    private final LearnerProfileLookupService learnerProfileLookupService;
    private final DomainSecurityService domainSecurityService;
    private final DiscoveryTracker discoveryTracker;
    private final OrganisationLookupService organisationLookupService;
    private final InstructorLookupService instructorLookupService;

    @Override
    public List<RecommendedCourseDTO> recommendForCaller(UUID requestedUserUuid, UUID studentUuid, String surface, int limit) {
        Surface resolvedSurface = Surface.personal(surface);
        UUID callerUuid = domainSecurityService.getCurrentUserUuid();
        if (callerUuid == null) {
            throw new AccessDeniedException("Sign in to get recommendations.");
        }
        if (studentUuid != null && requestedUserUuid != null) {
            throw new IllegalArgumentException("Pass either user_uuid or student_uuid, not both");
        }

        UUID learner;
        if (studentUuid != null) {
            boolean allowed = studentUuid.equals(domainSecurityService.getCurrentStudentUuid())
                    || domainSecurityService.isPlatformAdmin()
                    || learnerProfileLookupService.guardianCanViewAcademics(callerUuid, studentUuid);
            if (!allowed) {
                throw new AccessDeniedException("You can only request recommendations for yourself or a learner you guard "
                        + "with a FULL or ACADEMICS share.");
            }
            learner = studentUuid;
        } else if (requestedUserUuid == null || requestedUserUuid.equals(callerUuid)) {
            learner = domainSecurityService.getCurrentStudentUuid();
        } else if (domainSecurityService.isPlatformAdmin()) {
            learner = learnerProfileLookupService.findStudentUuidByUserUuid(requestedUserUuid).orElse(null);
        } else {
            throw new AccessDeniedException("You can only request recommendations for yourself.");
        }

        return recommend(learner, resolvedSurface, cap(limit), callerUuid);
    }

    private List<RecommendedCourseDTO> recommend(UUID studentUuid, Surface surface, int limit, UUID viewerUuid) {
        LearnerContext context = learnerContextLoader.load(studentUuid);
        LearnerProfile profile = context.profile();
        if (surface == Surface.NEXT_STEPS && !profile.hasHistory()) {
            return List.of();
        }

        Set<UUID> neighbours = new LinkedHashSet<>();
        context.coLifts().values().forEach(lifts -> neighbours.addAll(lifts.keySet()));
        Set<UUID> followOnOf = new HashSet<>(profile.enrolledCourseUuids());
        CandidateQuery query = new CandidateQuery(
                profile.categoryUuids(),
                neighbours,
                context.offers().keySet(),
                profile.anchorCourse().map(LearnerProfile.Enrolment::courseName).orElse(null),
                profile.skillGap(),
                followOnOf);
        Retrieval retrieval = candidateRetriever.retrieve(query, context.audience(), profile.enrolledCourseUuids());

        LearnerSignals signals = new LearnerSignals(context.coLifts(), context.offers(), retrieval.textSimilarity());
        List<ScoredCourse> ranked = RecommendationScorer.rank(profile, retrieval.candidates(), signals, surface, limit);
        return respond(withAffiliationNames(ranked, context.offers()), surface, viewerUuid);
    }

    @Override
    public List<RecommendedCourseDTO> findSimilar(UUID courseUuid, int limit) {
        CandidateCourse anchor = candidateStore.loadPublic(List.of(courseUuid), Audience.anyone(), List.of()).stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Course with UUID " + courseUuid + " not found"));
        Map<UUID, Double> lifts = candidateStore.findNeighbourLifts(List.of(courseUuid)).getOrDefault(courseUuid, Map.of());
        CandidateQuery query = new CandidateQuery(anchor.categoryUuids(), lifts.keySet(), List.of(), anchor.name(),
                List.of(), List.of());
        Retrieval retrieval = candidateRetriever.retrieve(query, Audience.anyone(), Set.of(courseUuid));
        List<ScoredCourse> ranked = RecommendationScorer.rankSimilar(anchor, retrieval.candidates(), lifts,
                retrieval.textSimilarity(), cap(limit));
        return respond(ranked, Surface.SIMILAR, domainSecurityService.getCurrentUserUuid());
    }

    @Override
    public RecommendationEvaluationDTO evaluate() {
        return recommendationEvaluator.evaluate();
    }

    /** Fills "Offered by {name}" for the affiliation reasons of the final items only (two batched lookups). */
    private List<ScoredCourse> withAffiliationNames(List<ScoredCourse> ranked, Map<UUID, AffiliationOffer> offers) {
        Set<UUID> organisations = new HashSet<>();
        Set<UUID> instructors = new HashSet<>();
        for (ScoredCourse item : ranked) {
            AffiliationOffer offer = offers.get(item.course().uuid());
            if (offer != null && item.reasons().stream().anyMatch(r -> r.code() == ReasonCode.AFFILIATION)) {
                (offer.organisation() ? organisations : instructors).add(offer.applicantUuid());
            }
        }
        if (organisations.isEmpty() && instructors.isEmpty()) {
            return ranked;
        }
        Map<UUID, String> names = new HashMap<>();
        if (!organisations.isEmpty()) {
            names.putAll(organisationLookupService.findOrganisationNames(organisations));
        }
        if (!instructors.isEmpty()) {
            instructorLookupService.findInstructorDirectoryEntries(instructors).forEach((uuid, entry) -> {
                if (entry != null && entry.displayName() != null) {
                    names.put(uuid, entry.displayName());
                }
            });
        }
        List<ScoredCourse> named = new ArrayList<>(ranked.size());
        for (ScoredCourse item : ranked) {
            named.add(item.withReasons(item.reasons().stream()
                    .map(reason -> reason.code() == ReasonCode.AFFILIATION && reason.text() == null
                            ? reason.withText(offeredBy(names.get(reason.relatedUuid())))
                            : reason)
                    .toList()));
        }
        return named;
    }

    private static String offeredBy(String name) {
        return name == null || name.isBlank() ? "Offered where you learn" : "Offered by " + name;
    }

    private List<RecommendedCourseDTO> respond(List<ScoredCourse> ranked, Surface surface, UUID viewerUuid) {
        if (ranked.isEmpty()) {
            return List.of();
        }
        UUID recommendationId = UUID.randomUUID();
        List<RecommendedCourseDTO> response = new ArrayList<>(ranked.size());
        List<DiscoveryImpression> impressions = new ArrayList<>(ranked.size());
        for (int position = 0; position < ranked.size(); position++) {
            ScoredCourse item = ranked.get(position);
            List<RecommendationReasonDTO> reasons = item.reasons().stream()
                    .map(r -> new RecommendationReasonDTO(r.code().name(), r.text(), r.relatedUuid()))
                    .toList();
            CandidateCourse course = item.course();
            response.add(new RecommendedCourseDTO(course.uuid(), course.name(), course.description(),
                    course.thumbnailUrl(), reasons.getFirst().text(), item.score(), reasons, recommendationId,
                    surface.value(), RecommendationTypes.MODEL_VERSION));
            impressions.add(new DiscoveryImpression(RecommendationTypes.ITEM_TYPE, course.uuid(), position,
                    item.reasons().stream().map(Reason::code).map(Enum::name).distinct().toList()));
        }
        if (viewerUuid != null) {
            discoveryTracker.recordImpressions(viewerUuid, surface.trackingSurface(), recommendationId,
                    RecommendationTypes.MODEL_VERSION, impressions);
        }
        return response;
    }

    private static int cap(int limit) {
        return limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
    }
}
