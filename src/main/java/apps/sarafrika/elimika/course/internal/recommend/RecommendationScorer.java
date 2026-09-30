package apps.sarafrika.elimika.course.internal.recommend;

import apps.sarafrika.elimika.course.internal.recommend.CandidateCourse.CategoryRef;
import apps.sarafrika.elimika.course.internal.recommend.CandidateCourse.PrerequisiteRef;
import apps.sarafrika.elimika.course.internal.recommend.LearnerProfile.Enrolment;
import apps.sarafrika.elimika.course.internal.recommend.RecommendationTypes.AffiliationOffer;
import apps.sarafrika.elimika.course.internal.recommend.RecommendationTypes.LearnerSignals;
import apps.sarafrika.elimika.course.internal.recommend.RecommendationTypes.Reason;
import apps.sarafrika.elimika.course.internal.recommend.RecommendationTypes.ReasonCode;
import apps.sarafrika.elimika.course.internal.recommend.RecommendationTypes.ScoredCourse;
import apps.sarafrika.elimika.course.internal.recommend.RecommendationTypes.Surface;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The hand-weighted "rules-v2" ranking (roadmap §10), pure Java over rows already loaded, so the online
 * service and the offline evaluator rank identically.
 *
 * <pre>
 * score = 3.0·next_step + 2.5·co_enrol + 2.0·category_affinity + 3.0·skill_gap
 *       + 1.5·affiliation + 1.0·quality + 0.5·popularity
 * </pre>
 * Penalties: a jump of more than one difficulty level ×0.5; a category the learner mostly dropped ×0.6;
 * an unmet mandatory prerequisite keeps the course off "for you" and moves it to "next steps" with
 * "Complete X first". Diversity: at most 2 per category in the top 6, the last of which is an exploration
 * slot for a category the learner has not touched.
 */
public final class RecommendationScorer {

    static final double W_NEXT_STEP = 3.0;
    static final double W_CO_ENROL = 2.5;
    static final double W_CATEGORY = 2.0;
    static final double W_SKILL_GAP = 3.0;
    static final double W_AFFILIATION = 1.5;
    static final double W_QUALITY = 1.0;
    static final double W_POPULARITY = 0.5;

    static final double LEVEL_JUMP_PENALTY = 0.5;
    static final double DROPPED_CATEGORY_PENALTY = 0.6;

    /** Similar-courses weights: co-enrolment, shared categories, text similarity, quality, popularity. */
    static final double W_SIMILAR_CO = 2.5;
    static final double W_SIMILAR_CATEGORY = 2.0;
    static final double W_SIMILAR_TEXT = 1.0;

    public static final int DIVERSITY_WINDOW = 6;
    public static final int PER_CATEGORY_CAP = 2;

    /** Neutral quality when no rating exists yet (no reviews anywhere). */
    private static final double DEFAULT_QUALITY = 0.5;
    private static final double MAX_RATING = 5.0;
    /** "More like this" hits below this relevance are not worth a reason. */
    private static final double TEXT_REASON_THRESHOLD = 0.5;

    private RecommendationScorer() {
    }

    // ================================================================== personal

    /**
     * Ranks candidates for a learner on one surface. Candidates must already exclude the learner's own
     * courses and anything outside their audience; this only scores, filters by surface and diversifies.
     */
    public static List<ScoredCourse> rank(LearnerProfile profile, List<CandidateCourse> candidates,
                                          LearnerSignals signals, Surface surface, int limit) {
        if (candidates.isEmpty() || limit <= 0) {
            return List.of();
        }
        double maxPopularity = candidates.stream().mapToLong(CandidateCourse::popularity30d).max().orElse(0);
        if (!profile.hasSignals()) {
            if (surface == Surface.NEXT_STEPS) {
                return List.of();
            }
            List<ScoredCourse> popular = candidates.stream()
                    .map(c -> new ScoredCourse(c, round(popularity(c, maxPopularity) + 0.5 * quality(c)),
                            List.of(popularReason(c)), false))
                    .sorted(byScore())
                    .toList();
            return diversify(popular, Set.of(), limit);
        }

        Features features = new Features(profile);
        List<ScoredCourse> scored = new ArrayList<>();
        for (CandidateCourse candidate : candidates) {
            ScoredCourse item = score(candidate, profile, features, signals, surface, maxPopularity);
            if (item != null) {
                scored.add(item);
            }
        }
        scored.sort(byScore());
        return diversify(scored, profile.categoryUuids(), limit);
    }

    private static ScoredCourse score(CandidateCourse candidate, LearnerProfile profile, Features features,
                                      LearnerSignals signals, Surface surface, double maxPopularity) {
        List<Weighted> reasons = new ArrayList<>();

        // Prerequisites: a completed prior course makes this the next step; an unmet mandatory one blocks "for you".
        Set<UUID> completed = features.completed;
        PrerequisiteRef completedPrerequisite = null;
        PrerequisiteRef unmet = null;
        boolean onPath = false;
        for (PrerequisiteRef prerequisite : candidate.prerequisites()) {
            if (completed.contains(prerequisite.courseUuid())) {
                if (completedPrerequisite == null) {
                    completedPrerequisite = prerequisite;
                }
            } else if (prerequisite.mandatory() && unmet == null) {
                unmet = prerequisite;
            }
            if (features.enrolled.contains(prerequisite.courseUuid())) {
                onPath = true;
            }
        }
        if (unmet != null && surface == Surface.FOR_YOU) {
            return null;
        }

        double nextStep = 0;
        if (completedPrerequisite != null) {
            nextStep = 1;
            reasons.add(new Weighted(W_NEXT_STEP, new Reason(ReasonCode.NEXT_STEP,
                    "Next step after " + features.courseName(completedPrerequisite.courseUuid(), completedPrerequisite.name()),
                    completedPrerequisite.courseUuid())));
        } else if (candidate.levelOrder() != null) {
            for (CategoryRef category : candidate.categories()) {
                Enrolment best = features.bestCompletedByCategory.get(category.uuid());
                if (best != null && best.levelOrder() != null && candidate.levelOrder() == best.levelOrder() + 1) {
                    nextStep = 1;
                    reasons.add(new Weighted(W_NEXT_STEP, new Reason(ReasonCode.NEXT_STEP,
                            "Next step after " + best.courseName(), best.courseUuid())));
                    break;
                }
            }
        }

        if (surface == Surface.NEXT_STEPS && nextStep == 0 && !(unmet != null && onPath)) {
            return null;
        }

        // Co-enrolment: the strongest lift from a course the learner took, weighted by how far they got.
        double coEnrol = 0;
        Enrolment coSource = null;
        for (Enrolment enrolment : profile.enrolments()) {
            if (enrolment.dropped()) {
                continue;
            }
            Double lift = signals.coLifts().getOrDefault(enrolment.courseUuid(), Map.of()).get(candidate.uuid());
            if (lift == null || lift <= 0) {
                continue;
            }
            double strength = enrolment.weight() * lift / (1.0 + lift);
            if (strength > coEnrol) {
                coEnrol = strength;
                coSource = enrolment;
            }
        }
        if (coSource != null) {
            reasons.add(new Weighted(W_CO_ENROL * coEnrol, new Reason(ReasonCode.CO_ENROLLED,
                    "Often taken after " + coSource.courseName(), coSource.courseUuid())));
        }

        // Category affinity, parent or child categories counting half.
        Affinity affinity = features.affinity(candidate);
        if (affinity.value > 0 && affinity.enrolment != null) {
            String verb = affinity.enrolment.completed() ? "Because you completed " : "Because you're taking ";
            reasons.add(new Weighted(W_CATEGORY * affinity.value, new Reason(ReasonCode.CATEGORY,
                    verb + affinity.enrolment.courseName() + " in " + affinity.categoryName, affinity.categoryUuid)));
        }

        // Skill gap: the share of the learner's missing goal skills this course teaches.
        double skillGap = 0;
        if (!features.skillGap.isEmpty()) {
            long taught = candidate.skillUuids().stream().filter(features.skillGap::contains).count();
            if (taught > 0) {
                skillGap = (double) taught / features.skillGap.size();
                String text = features.skillGap.size() == 1
                        ? "Teaches a skill you want to learn"
                        : "Teaches " + taught + " of " + features.skillGap.size() + " skills you want to learn";
                reasons.add(new Weighted(W_SKILL_GAP * skillGap, new Reason(ReasonCode.SKILL_GAP, text, null)));
            }
        }

        double affiliation = 0;
        AffiliationOffer offer = signals.offers().get(candidate.uuid());
        if (offer != null) {
            affiliation = 1;
            reasons.add(new Weighted(W_AFFILIATION, new Reason(ReasonCode.AFFILIATION, null, offer.applicantUuid())));
        }

        double score = W_NEXT_STEP * nextStep
                + W_CO_ENROL * coEnrol
                + W_CATEGORY * affinity.value
                + W_SKILL_GAP * skillGap
                + W_AFFILIATION * affiliation
                + W_QUALITY * quality(candidate)
                + W_POPULARITY * popularity(candidate, maxPopularity);

        if (features.isLevelJump(candidate)) {
            score *= LEVEL_JUMP_PENALTY;
        }
        if (features.inMostlyDroppedCategory(candidate)) {
            score *= DROPPED_CATEGORY_PENALTY;
        }

        reasons.sort(Comparator.comparingDouble((Weighted w) -> w.weight).reversed());
        List<Reason> ordered = new ArrayList<>();
        if (unmet != null) {
            ordered.add(new Reason(ReasonCode.PREREQUISITE_PENDING,
                    "Complete " + features.courseName(unmet.courseUuid(), unmet.name()) + " first", unmet.courseUuid()));
        }
        reasons.forEach(w -> ordered.add(w.reason));
        if (ordered.isEmpty()) {
            ordered.add(popularReason(candidate));
        }
        return new ScoredCourse(candidate, round(score), ordered, false);
    }

    // ================================================================== similar

    /**
     * Ranks courses similar to {@code anchor} for anyone: co-enrolment neighbours, shared categories and
     * "more like this" text relevance, plus quality and popularity. Nothing personal is used.
     */
    public static List<ScoredCourse> rankSimilar(CandidateCourse anchor, List<CandidateCourse> candidates,
                                                 Map<UUID, Double> neighbourLifts, Map<UUID, Double> textSimilarity,
                                                 int limit) {
        double maxPopularity = candidates.stream().mapToLong(CandidateCourse::popularity30d).max().orElse(0);
        Set<UUID> anchorCategories = new HashSet<>(anchor.categoryUuids());
        List<ScoredCourse> scored = new ArrayList<>();
        for (CandidateCourse candidate : candidates) {
            if (candidate.uuid().equals(anchor.uuid())) {
                continue;
            }
            List<Weighted> reasons = new ArrayList<>();
            Double lift = neighbourLifts.get(candidate.uuid());
            double co = lift == null || lift <= 0 ? 0 : lift / (1.0 + lift);
            if (co > 0) {
                reasons.add(new Weighted(W_SIMILAR_CO * co, new Reason(ReasonCode.CO_ENROLLED,
                        "Often taken with " + anchor.name(), anchor.uuid())));
            }
            CategoryRef shared = candidate.categories().stream()
                    .filter(c -> anchorCategories.contains(c.uuid())).findFirst().orElse(null);
            long sharedCount = candidate.categories().stream().filter(c -> anchorCategories.contains(c.uuid())).count();
            double category = anchorCategories.isEmpty() ? 0 : (double) sharedCount / anchorCategories.size();
            if (shared != null) {
                reasons.add(new Weighted(W_SIMILAR_CATEGORY * category, new Reason(ReasonCode.CATEGORY,
                        "Also in " + shared.name(), shared.uuid())));
            }
            double text = textSimilarity.getOrDefault(candidate.uuid(), 0.0);
            if (text >= TEXT_REASON_THRESHOLD) {
                reasons.add(new Weighted(W_SIMILAR_TEXT * text, new Reason(ReasonCode.SIMILAR_CONTENT,
                        "Covers similar topics to " + anchor.name(), anchor.uuid())));
            }
            double score = W_SIMILAR_CO * co + W_SIMILAR_CATEGORY * category + W_SIMILAR_TEXT * text
                    + W_QUALITY * quality(candidate) + W_POPULARITY * popularity(candidate, maxPopularity);
            reasons.sort(Comparator.comparingDouble((Weighted w) -> w.weight).reversed());
            List<Reason> ordered = new ArrayList<>(reasons.stream().map(w -> w.reason).toList());
            if (ordered.isEmpty()) {
                ordered.add(popularReason(candidate));
            }
            scored.add(new ScoredCourse(candidate, round(score), ordered, false));
        }
        scored.sort(byScore());
        return scored.stream().limit(limit).toList();
    }

    // ================================================================== diversity

    /**
     * At most {@value #PER_CATEGORY_CAP} courses per category among the first {@value #DIVERSITY_WINDOW}; when
     * the learner has categories, the last slot of that window goes to the best course outside all of them.
     * If the cap leaves the window short, it is filled in score order. Beyond the window, plain score order.
     */
    static List<ScoredCourse> diversify(List<ScoredCourse> ranked, Set<UUID> learnerCategories, int limit) {
        int window = Math.min(DIVERSITY_WINDOW, limit);
        boolean explore = window >= 2 && !learnerCategories.isEmpty();
        int regularSlots = explore ? window - 1 : window;

        List<ScoredCourse> chosen = new ArrayList<>();
        Set<UUID> taken = new HashSet<>();
        Map<UUID, Integer> perCategory = new HashMap<>();

        // The exploration pick is reserved first, so the regular slots cannot use it up.
        ScoredCourse exploration = !explore ? null : ranked.stream()
                .filter(item -> item.course().categoryUuids().stream().noneMatch(learnerCategories::contains))
                .findFirst()
                .orElse(null);
        if (exploration == null) {
            regularSlots = window;
        } else {
            taken.add(exploration.course().uuid());
            new LinkedHashSet<>(exploration.course().categoryUuids()).forEach(c -> perCategory.merge(c, 1, Integer::sum));
        }

        for (ScoredCourse item : ranked) {
            if (chosen.size() >= regularSlots) {
                break;
            }
            if (!taken.contains(item.course().uuid()) && fitsCap(item, perCategory)) {
                add(item, chosen, taken, perCategory);
            }
        }
        for (ScoredCourse item : ranked) {
            if (chosen.size() >= regularSlots) {
                break;
            }
            if (!taken.contains(item.course().uuid())) {
                add(item, chosen, taken, perCategory);
            }
        }
        if (exploration != null) {
            chosen.add(new ScoredCourse(exploration.course(), exploration.score(), exploration.reasons(), true));
        }
        for (ScoredCourse item : ranked) {
            if (chosen.size() >= window) {
                break;
            }
            if (!taken.contains(item.course().uuid())) {
                add(item, chosen, taken, perCategory);
            }
        }
        for (ScoredCourse item : ranked) {
            if (chosen.size() >= limit) {
                break;
            }
            if (!taken.contains(item.course().uuid())) {
                add(item, chosen, taken, perCategory);
            }
        }
        return chosen;
    }

    private static boolean fitsCap(ScoredCourse item, Map<UUID, Integer> perCategory) {
        return item.course().categoryUuids().stream()
                .allMatch(category -> perCategory.getOrDefault(category, 0) < PER_CATEGORY_CAP);
    }

    private static void add(ScoredCourse item, List<ScoredCourse> chosen, Set<UUID> taken, Map<UUID, Integer> perCategory) {
        chosen.add(item);
        taken.add(item.course().uuid());
        new LinkedHashSet<>(item.course().categoryUuids()).forEach(c -> perCategory.merge(c, 1, Integer::sum));
    }

    // ================================================================== shared terms

    static double quality(CandidateCourse course) {
        return course.ratingBayes() == null ? DEFAULT_QUALITY : Math.max(0, Math.min(1, course.ratingBayes() / MAX_RATING));
    }

    static double popularity(CandidateCourse course, double maxPopularity) {
        if (maxPopularity <= 0 || course.popularity30d() <= 0) {
            return 0;
        }
        return Math.log1p(course.popularity30d()) / Math.log1p(maxPopularity);
    }

    static Reason popularReason(CandidateCourse course) {
        if (course.categories().isEmpty()) {
            return new Reason(ReasonCode.POPULAR, "Popular right now", null);
        }
        CategoryRef category = course.categories().getFirst();
        return new Reason(ReasonCode.POPULAR, "Popular in " + category.name(), category.uuid());
    }

    private static Comparator<ScoredCourse> byScore() {
        return Comparator.comparingDouble(ScoredCourse::score).reversed()
                .thenComparing(item -> item.course().popularity30d(), Comparator.reverseOrder())
                .thenComparing(item -> item.course().uuid());
    }

    private static double round(double value) {
        return Math.round(value * 10_000d) / 10_000d;
    }

    private record Weighted(double weight, Reason reason) {
    }

    private record Affinity(double value, Enrolment enrolment, UUID categoryUuid, String categoryName) {
        static final Affinity NONE = new Affinity(0, null, null, null);
    }

    /** Everything derived from the learner's rows once per request. */
    private static final class Features {
        final Set<UUID> completed;
        final Set<UUID> enrolled;
        final Set<UUID> skillGap;
        final Map<UUID, Enrolment> bestCompletedByCategory = new HashMap<>();
        final Map<UUID, Double> categoryWeight = new HashMap<>();
        final Map<UUID, Enrolment> categoryRepresentative = new HashMap<>();
        final Map<UUID, String> categoryNames = new HashMap<>();
        final Map<UUID, UUID> learnerCategoryParents;
        final Map<UUID, int[]> droppedVsKept = new HashMap<>();
        final Map<UUID, String> courseNames = new HashMap<>();
        final Integer bestCompletedLevel;
        double maxCategoryWeight;

        Features(LearnerProfile profile) {
            completed = profile.completedCourseUuids();
            enrolled = profile.enrolledCourseUuids();
            skillGap = profile.skillGap();
            learnerCategoryParents = profile.categoryParents();
            Integer bestLevel = null;
            for (Enrolment enrolment : profile.enrolments()) {
                courseNames.put(enrolment.courseUuid(), enrolment.courseName());
                if (enrolment.completed() && enrolment.levelOrder() != null
                        && (bestLevel == null || enrolment.levelOrder() > bestLevel)) {
                    bestLevel = enrolment.levelOrder();
                }
                for (CategoryRef category : enrolment.categories()) {
                    categoryNames.put(category.uuid(), category.name());
                    int[] counts = droppedVsKept.computeIfAbsent(category.uuid(), key -> new int[2]);
                    counts[enrolment.dropped() ? 0 : 1]++;
                    double weight = enrolment.weight();
                    if (weight > 0) {
                        categoryWeight.merge(category.uuid(), weight, Double::sum);
                        Enrolment current = categoryRepresentative.get(category.uuid());
                        if (current == null || weight > current.weight()) {
                            categoryRepresentative.put(category.uuid(), enrolment);
                        }
                    }
                    if (enrolment.completed() && enrolment.levelOrder() != null) {
                        Enrolment best = bestCompletedByCategory.get(category.uuid());
                        if (best == null || best.levelOrder() == null || enrolment.levelOrder() > best.levelOrder()) {
                            bestCompletedByCategory.put(category.uuid(), enrolment);
                        }
                    }
                }
            }
            bestCompletedLevel = bestLevel;
            maxCategoryWeight = categoryWeight.values().stream().mapToDouble(Double::doubleValue).max().orElse(0);
        }

        String courseName(UUID courseUuid, String fallback) {
            String name = courseNames.get(courseUuid);
            return name != null ? name : fallback == null || fallback.isBlank() ? "the prerequisite course" : fallback;
        }

        double normalised(UUID category) {
            return maxCategoryWeight <= 0 ? 0 : categoryWeight.getOrDefault(category, 0.0) / maxCategoryWeight;
        }

        Affinity affinity(CandidateCourse candidate) {
            Affinity best = Affinity.NONE;
            for (CategoryRef category : candidate.categories()) {
                best = better(best, category.uuid(), normalised(category.uuid()));
                if (category.parentUuid() != null) {
                    best = better(best, category.parentUuid(), 0.5 * normalised(category.parentUuid()));
                }
                for (Map.Entry<UUID, UUID> child : learnerCategoryParents.entrySet()) {
                    if (category.uuid().equals(child.getValue())) {
                        best = better(best, child.getKey(), 0.5 * normalised(child.getKey()));
                    }
                }
            }
            return best;
        }

        private Affinity better(Affinity current, UUID learnerCategory, double value) {
            if (value <= current.value) {
                return current;
            }
            Enrolment representative = categoryRepresentative.get(learnerCategory);
            if (representative == null) {
                return current;
            }
            return new Affinity(value, representative, learnerCategory, categoryNames.getOrDefault(learnerCategory, "this topic"));
        }

        /** More than one level above the learner's best completed level in a shared category (or overall). */
        boolean isLevelJump(CandidateCourse candidate) {
            if (candidate.levelOrder() == null) {
                return false;
            }
            Integer reference = null;
            for (UUID category : candidate.categoryUuids()) {
                Enrolment best = bestCompletedByCategory.get(category);
                if (best != null && best.levelOrder() != null && (reference == null || best.levelOrder() > reference)) {
                    reference = best.levelOrder();
                }
            }
            if (reference == null) {
                reference = bestCompletedLevel;
            }
            return reference != null && candidate.levelOrder() - reference > 1;
        }

        boolean inMostlyDroppedCategory(CandidateCourse candidate) {
            for (UUID category : candidate.categoryUuids()) {
                int[] counts = droppedVsKept.get(category);
                if (counts != null && counts[0] > counts[1]) {
                    return true;
                }
            }
            return false;
        }
    }
}
