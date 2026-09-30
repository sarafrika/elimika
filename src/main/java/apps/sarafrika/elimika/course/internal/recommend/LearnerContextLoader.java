package apps.sarafrika.elimika.course.internal.recommend;

import apps.sarafrika.elimika.course.internal.recommend.CourseCandidateStore.Audience;
import apps.sarafrika.elimika.course.internal.recommend.LearnerProfile.Enrolment;
import apps.sarafrika.elimika.course.internal.recommend.RecommendationTypes.AffiliationOffer;
import apps.sarafrika.elimika.shared.spi.LearnerProfileLookupService;
import apps.sarafrika.elimika.shared.spi.enrollment.LearnerAffiliationLookup;
import apps.sarafrika.elimika.shared.spi.enrollment.LearnerAffiliations;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Loads one learner's own rows for a request: enrolments with progress (course module), skill goals and
 * age (student module, age computed in tenancy), affiliations (timetabling), then the relational signals
 * around them (co-enrolment neighbours, approved offers). Nothing is stored or copied.
 */
@Component
@RequiredArgsConstructor
public class LearnerContextLoader {

    private final CourseCandidateStore store;
    private final LearnerProfileLookupService learnerProfileLookupService;
    private final LearnerAffiliationLookup learnerAffiliationLookup;
    private final Clock clock = Clock.systemUTC();

    /** A learner's profile, what they may see, and the signals around them. */
    public record LearnerContext(LearnerProfile profile, Audience audience,
                                 Map<UUID, Map<UUID, Double>> coLifts, Map<UUID, AffiliationOffer> offers) {

        public static LearnerContext anonymous() {
            return new LearnerContext(LearnerProfile.anonymous(), Audience.anyone(), Map.of(), Map.of());
        }
    }

    public LearnerContext load(UUID studentUuid) {
        if (studentUuid == null) {
            return LearnerContext.anonymous();
        }
        List<Enrolment> enrolments = LearnerProfile.collapse(store.loadEnrolments(studentUuid));
        List<UUID> goals = learnerProfileLookupService.findSkillGoalUuids(studentUuid);
        LearnerAffiliations affiliations = java.util.Objects.requireNonNullElse(
                learnerAffiliationLookup.findAffiliations(studentUuid), LearnerAffiliations.none());
        OptionalInt age = learnerProfileLookupService.findLearnerAge(studentUuid, LocalDate.now(clock));

        LearnerProfile profile = new LearnerProfile(studentUuid, enrolments, goals, affiliations);
        Set<UUID> takenCourses = enrolments.stream().filter(e -> !e.dropped())
                .map(Enrolment::courseUuid).collect(Collectors.toSet());
        Map<UUID, Map<UUID, Double>> coLifts = store.findNeighbourLifts(takenCourses);
        Map<UUID, AffiliationOffer> offers = store.findApprovedOffers(affiliations.organisationUuids(),
                affiliations.instructorUuids());
        return new LearnerContext(profile, age.isPresent() ? Audience.ofAge(age.getAsInt()) : Audience.unknownAge(),
                coLifts, offers);
    }
}
