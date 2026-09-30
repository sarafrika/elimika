package apps.sarafrika.elimika.course.internal.recommend;

import apps.sarafrika.elimika.course.dto.RecommendationEvaluationDTO;
import apps.sarafrika.elimika.course.dto.RecommendationEvaluationDTO.ModelScore;
import apps.sarafrika.elimika.course.internal.recommend.CourseCandidateStore.EnrolmentRow;
import apps.sarafrika.elimika.course.internal.recommend.LearnerProfile.Enrolment;
import apps.sarafrika.elimika.course.internal.recommend.RecommendationTypes.LearnerSignals;
import apps.sarafrika.elimika.course.internal.recommend.RecommendationTypes.ScoredCourse;
import apps.sarafrika.elimika.course.internal.recommend.RecommendationTypes.Surface;
import apps.sarafrika.elimika.shared.spi.MinorLearnerLookupService;
import apps.sarafrika.elimika.shared.spi.enrollment.LearnerAffiliations;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Offline leave-last-out evaluation (roadmap §11). For every learner with at least two enrolments whose
 * latest enrolment is a public course, that enrolment is hidden and each model recommends
 * {@value #K} courses from the learner's earlier rows only. Co-enrolment lifts and popularity are rebuilt
 * in memory from the earlier rows of every learner, with the same privacy thresholds as the nightly job,
 * so the hidden rows never leak into the features.
 * <p>
 * Models: {@code rules-v2} (the live scorer), {@code popularity} (most enrolled, earlier rows) and
 * {@code legacy-newest} (what the old engine gave every student: the newest published courses).
 * Skill goals and affiliations are left out: neither is time-sliced, so they could leak the hidden
 * enrolment. Only aggregates leave this class.
 */
@Component
@RequiredArgsConstructor
public class RecommendationEvaluator {

    static final int K = 6;
    public static final String MODEL_V2 = RecommendationTypes.MODEL_VERSION;
    public static final String MODEL_POPULARITY = "popularity";
    public static final String MODEL_LEGACY = "legacy-newest";

    private final CourseCandidateStore store;
    private final MinorLearnerLookupService minorLearnerLookupService;
    private final Clock clock = Clock.systemUTC();

    public RecommendationEvaluationDTO evaluate() {
        Map<UUID, CandidateCourse> courses = store.loadAllRootCourses().stream()
                .collect(Collectors.toMap(CandidateCourse::uuid, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        List<CandidateCourse> catalogue = courses.values().stream().filter(CandidateCourse::publiclyListed).toList();

        Map<UUID, List<EnrolmentRow>> byLearner = new LinkedHashMap<>();
        for (EnrolmentRow row : store.loadAllEnrolments()) {
            if (courses.containsKey(row.courseUuid())) {
                byLearner.computeIfAbsent(row.studentUuid(), key -> new ArrayList<>()).add(row);
            }
        }

        // Split: each learner's latest enrolment is hidden; everything else is the training history.
        Map<UUID, EnrolmentRow> hidden = new HashMap<>();
        Map<UUID, List<EnrolmentRow>> history = new HashMap<>();
        for (Map.Entry<UUID, List<EnrolmentRow>> entry : byLearner.entrySet()) {
            List<EnrolmentRow> rows = entry.getValue();
            if (rows.size() < 2) {
                history.put(entry.getKey(), rows);
                continue;
            }
            EnrolmentRow last = rows.getLast();
            if (!courses.get(last.courseUuid()).publiclyListed()
                    || rows.subList(0, rows.size() - 1).stream().anyMatch(r -> r.courseUuid().equals(last.courseUuid()))) {
                history.put(entry.getKey(), rows);
                continue;
            }
            hidden.put(entry.getKey(), last);
            history.put(entry.getKey(), rows.subList(0, rows.size() - 1));
        }

        Map<UUID, Map<UUID, Double>> lifts = coEnrolmentLifts(history);
        Map<UUID, Long> popularity = new HashMap<>();
        history.values().forEach(rows -> rows.stream().map(EnrolmentRow::courseUuid).distinct()
                .forEach(course -> popularity.merge(course, 1L, Long::sum)));
        List<CandidateCourse> trainingCatalogue = catalogue.stream()
                .map(c -> new CandidateCourse(c.uuid(), c.name(), c.description(), c.thumbnailUrl(), c.createdDate(),
                        c.levelOrder(), c.categories(), c.skillUuids(), c.prerequisites(), c.ratingBayes(),
                        popularity.getOrDefault(c.uuid(), 0L), true))
                .toList();

        Tally v2 = new Tally();
        Tally popular = new Tally();
        Tally legacy = new Tally();
        List<CandidateCourse> byPopularity = trainingCatalogue.stream()
                .sorted(Comparator.comparingLong(CandidateCourse::popularity30d).reversed()
                        .thenComparing(c -> c.ratingBayes() == null ? 0 : c.ratingBayes(), Comparator.reverseOrder())
                        .thenComparing(CandidateCourse::uuid))
                .toList();
        List<CandidateCourse> byNewest = trainingCatalogue.stream()
                .sorted(Comparator.comparing(CandidateCourse::createdDate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(CandidateCourse::uuid))
                .toList();

        for (Map.Entry<UUID, EnrolmentRow> entry : hidden.entrySet()) {
            UUID learner = entry.getKey();
            UUID target = entry.getValue().courseUuid();
            List<EnrolmentRow> rows = history.get(learner);
            Set<UUID> seen = rows.stream().map(EnrolmentRow::courseUuid).collect(Collectors.toSet());

            List<Enrolment> enrolments = LearnerProfile.collapse(rows.stream().map(row -> {
                CandidateCourse course = courses.get(row.courseUuid());
                return new Enrolment(row.courseUuid(), course.name(), row.status(), row.progress(), row.enrolledAt(),
                        row.completedAt(), course.levelOrder(), course.categories(), course.skillUuids());
            }).toList());
            LearnerProfile profile = new LearnerProfile(learner, enrolments,
                    List.of(), LearnerAffiliations.none());
            List<CandidateCourse> candidates = trainingCatalogue.stream().filter(c -> !seen.contains(c.uuid())).toList();

            v2.add(target, RecommendationScorer.rank(profile, candidates,
                    new LearnerSignals(lifts, Map.of(), Map.of()), Surface.FOR_YOU, K).stream()
                    .map(ScoredCourse::course).map(CandidateCourse::uuid).toList());
            popular.add(target, top(byPopularity, seen));
            legacy.add(target, top(byNewest, seen));
        }

        int size = catalogue.size();
        return new RecommendationEvaluationDTO(K, hidden.size(), size, List.of(
                v2.score(MODEL_V2, size), popular.score(MODEL_POPULARITY, size), legacy.score(MODEL_LEGACY, size)),
                Instant.now(clock));
    }

    private static List<UUID> top(List<CandidateCourse> ordered, Set<UUID> seen) {
        return ordered.stream().map(CandidateCourse::uuid).filter(uuid -> !seen.contains(uuid)).limit(K).toList();
    }

    /**
     * Pair lifts over the training rows (active or completed), both directions, kept only when at least 5
     * learners share the pair, or 10 when any of them is a minor.
     */
    private Map<UUID, Map<UUID, Double>> coEnrolmentLifts(Map<UUID, List<EnrolmentRow>> history) {
        Map<UUID, Set<UUID>> coursesByLearner = new HashMap<>();
        history.forEach((learner, rows) -> rows.stream()
                .filter(r -> "active".equals(r.status()) || LearnerProfile.COMPLETED.equals(r.status()))
                .forEach(r -> coursesByLearner.computeIfAbsent(learner, key -> new HashSet<>()).add(r.courseUuid())));
        if (coursesByLearner.isEmpty()) {
            return Map.of();
        }
        Set<UUID> minors = minorLearnerLookupService.findMinorStudentUuids(coursesByLearner.keySet(), LocalDate.now(clock));
        Map<UUID, Integer> courseSize = new HashMap<>();
        Map<UUID, Map<UUID, int[]>> pairs = new HashMap<>();
        for (Map.Entry<UUID, Set<UUID>> entry : coursesByLearner.entrySet()) {
            boolean minor = minors.contains(entry.getKey());
            for (UUID a : entry.getValue()) {
                courseSize.merge(a, 1, Integer::sum);
                for (UUID b : entry.getValue()) {
                    if (!a.equals(b)) {
                        int[] counts = pairs.computeIfAbsent(a, key -> new HashMap<>()).computeIfAbsent(b, key -> new int[2]);
                        counts[0]++;
                        if (minor) {
                            counts[1]++;
                        }
                    }
                }
            }
        }
        double learners = coursesByLearner.size();
        Map<UUID, Map<UUID, Double>> lifts = new HashMap<>();
        pairs.forEach((a, neighbours) -> neighbours.forEach((b, counts) -> {
            int threshold = counts[1] > 0 ? CourseFeatureRefresher.MIN_SHARED_LEARNERS_WITH_MINORS
                    : CourseFeatureRefresher.MIN_SHARED_LEARNERS;
            if (counts[0] >= threshold) {
                double lift = counts[0] * learners / ((double) courseSize.get(a) * courseSize.get(b));
                lifts.computeIfAbsent(a, key -> new HashMap<>()).put(b, lift);
            }
        }));
        return lifts;
    }

    /** Running recall, nDCG and coverage for one model. */
    private static final class Tally {
        private int learners;
        private int hits;
        private double ndcg;
        private final Set<UUID> recommended = new HashSet<>();

        void add(UUID target, List<UUID> ranked) {
            learners++;
            recommended.addAll(ranked);
            int position = ranked.indexOf(target);
            if (position >= 0) {
                hits++;
                ndcg += 1.0 / (Math.log(position + 2) / Math.log(2));
            }
        }

        ModelScore score(String model, int catalogueSize) {
            return new ModelScore(model,
                    learners == 0 ? 0 : round((double) hits / learners),
                    learners == 0 ? 0 : round(ndcg / learners),
                    catalogueSize == 0 ? 0 : round((double) recommended.size() / catalogueSize));
        }

        private static double round(double value) {
            return Math.round(value * 10_000d) / 10_000d;
        }
    }
}
