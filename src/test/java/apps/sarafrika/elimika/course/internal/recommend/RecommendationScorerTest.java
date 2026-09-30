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
import apps.sarafrika.elimika.shared.spi.enrollment.LearnerAffiliations;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Recommendation scorer (rules-v2)")
class RecommendationScorerTest {

    private static final CategoryRef PROGRAMMING = new CategoryRef(UUID.randomUUID(), null, "Programming");
    private static final CategoryRef DESIGN = new CategoryRef(UUID.randomUUID(), null, "Design");
    private static final CategoryRef MUSIC = new CategoryRef(UUID.randomUUID(), null, "Music");
    private static final CategoryRef COOKING = new CategoryRef(UUID.randomUUID(), null, "Cooking");

    private final UUID basics = UUID.randomUUID();

    @Test
    @DisplayName("next step: one level above the best completed level in the category, with the reason text")
    void nextStepByLevel() {
        LearnerProfile profile = learner(completed(basics, "Python Basics", 1, PROGRAMMING));
        CandidateCourse intermediate = course("Python Intermediate", 2, PROGRAMMING);
        CandidateCourse design = course("Colour Theory", 1, DESIGN);

        List<ScoredCourse> ranked = RecommendationScorer.rank(profile, List.of(design, intermediate),
                LearnerSignals.none(), Surface.FOR_YOU, 6);

        assertThat(ranked.getFirst().course()).isEqualTo(intermediate);
        Reason first = ranked.getFirst().reasons().getFirst();
        assertThat(first.code()).isEqualTo(ReasonCode.NEXT_STEP);
        assertThat(first.text()).isEqualTo("Next step after Python Basics");
        assertThat(first.relatedUuid()).isEqualTo(basics);
        assertThat(ranked).allSatisfy(item -> assertThat(item.reasons()).isNotEmpty());
    }

    @Test
    @DisplayName("an unmet mandatory prerequisite keeps a course off for_you and puts it on next_steps")
    void unmetPrerequisite() {
        UUID active = UUID.randomUUID();
        LearnerProfile profile = learner(new Enrolment(active, "SQL 101", "active", 40, LocalDateTime.now(), null, 1,
                List.of(PROGRAMMING), List.of()));
        CandidateCourse advanced = new CandidateCourse(UUID.randomUUID(), "SQL Tuning", null, null, null, 2,
                List.of(PROGRAMMING), List.of(), List.of(new PrerequisiteRef(active, "SQL 101", true)), 4.0, 3, true);

        assertThat(RecommendationScorer.rank(profile, List.of(advanced), LearnerSignals.none(), Surface.FOR_YOU, 6))
                .isEmpty();
        List<ScoredCourse> next = RecommendationScorer.rank(profile, List.of(advanced), LearnerSignals.none(),
                Surface.NEXT_STEPS, 6);
        assertThat(next).hasSize(1);
        assertThat(next.getFirst().reasons().getFirst())
                .isEqualTo(new Reason(ReasonCode.PREREQUISITE_PENDING, "Complete SQL 101 first", active));
    }

    @Test
    @DisplayName("co-enrolment, skill gap and affiliation reasons, strongest first")
    void signalReasons() {
        UUID skill = UUID.randomUUID();
        UUID organisation = UUID.randomUUID();
        LearnerProfile profile = new LearnerProfile(UUID.randomUUID(),
                List.of(completed(basics, "Python Basics", 1, PROGRAMMING)), List.of(skill, UUID.randomUUID()),
                LearnerAffiliations.none());
        CandidateCourse viz = new CandidateCourse(UUID.randomUUID(), "Data Viz", null, null, null, null,
                List.of(DESIGN), List.of(skill), List.of(), 4.5, 10, true);
        LearnerSignals signals = new LearnerSignals(Map.of(basics, Map.of(viz.uuid(), 3.0)),
                Map.of(viz.uuid(), new AffiliationOffer(organisation, true)), Map.of());

        ScoredCourse item = RecommendationScorer.rank(profile, List.of(viz), signals, Surface.FOR_YOU, 6).getFirst();

        assertThat(item.reasons()).extracting(Reason::code)
                .containsExactly(ReasonCode.CO_ENROLLED, ReasonCode.SKILL_GAP, ReasonCode.AFFILIATION);
        assertThat(item.reasons().getFirst().text()).isEqualTo("Often taken after Python Basics");
        assertThat(item.reasons().get(1).text()).isEqualTo("Teaches 1 of 2 skills you want to learn");
        assertThat(item.reasons().get(2).relatedUuid()).isEqualTo(organisation);
    }

    @Test
    @DisplayName("penalties: a two-level jump halves the score, a mostly dropped category scales it by 0.6")
    void penalties() {
        LearnerProfile profile = learner(completed(basics, "Python Basics", 1, PROGRAMMING),
                new Enrolment(UUID.randomUUID(), "Knife Skills", "dropped", 5, LocalDateTime.now(), null, 1,
                        List.of(COOKING), List.of()));
        CandidateCourse level2 = course("Python II", 2, PROGRAMMING);
        CandidateCourse level3 = new CandidateCourse(UUID.randomUUID(), "Python III", null, null, null, 3,
                List.of(PROGRAMMING), List.of(), List.of(), 4.0, 5, true);
        CandidateCourse cooking = course("Baking", 1, COOKING);
        CandidateCourse cookingControl = course("Harmony", 1, MUSIC);

        Map<UUID, Double> scores = new HashMap<>();
        RecommendationScorer.rank(profile, List.of(level2, level3, cooking, cookingControl), LearnerSignals.none(),
                Surface.FOR_YOU, 6).forEach(item -> scores.put(item.course().uuid(), item.score()));

        // level 3 has no next-step term: (2.0 affinity + 0.8 quality + 0.5 popularity) × 0.5
        assertThat(scores.get(level3.uuid())).isEqualTo(1.65);
        assertThat(scores.get(cooking.uuid())).isEqualTo(round(scores.get(cookingControl.uuid()) * 0.6));
    }

    @Test
    @DisplayName("diversity: at most 2 per category in the top 6, the last slot for an untouched category")
    void diversity() {
        LearnerProfile profile = learner(completed(basics, "Python Basics", 1, PROGRAMMING),
                completed(UUID.randomUUID(), "Sketching", 1, DESIGN));
        List<CandidateCourse> candidates = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            candidates.add(course("Programming " + i, 1, PROGRAMMING));
            candidates.add(course("Design " + i, 1, DESIGN));
        }
        candidates.add(course("Harmony", 1, MUSIC));
        candidates.add(course("Baking", 1, COOKING));

        List<ScoredCourse> ranked = RecommendationScorer.rank(profile, candidates, LearnerSignals.none(),
                Surface.FOR_YOU, 8);

        List<ScoredCourse> top = ranked.subList(0, RecommendationScorer.DIVERSITY_WINDOW);
        Map<UUID, Integer> perCategory = new HashMap<>();
        top.forEach(item -> item.course().categoryUuids().forEach(c -> perCategory.merge(c, 1, Integer::sum)));
        assertThat(perCategory.values()).allMatch(count -> count <= RecommendationScorer.PER_CATEGORY_CAP);
        assertThat(perCategory.get(PROGRAMMING.uuid())).isEqualTo(2);
        assertThat(perCategory.get(DESIGN.uuid())).isEqualTo(2);
        assertThat(top.getLast().exploration()).isTrue();
        assertThat(top.getLast().course().categoryUuids()).containsAnyOf(MUSIC.uuid(), COOKING.uuid());
        assertThat(top).filteredOn(ScoredCourse::exploration).hasSize(1);
        // Beyond the window, plain score order: the strongest remaining matches come back.
        assertThat(ranked).hasSize(8);
        assertThat(ranked.get(6).course().categoryUuids()).containsAnyOf(PROGRAMMING.uuid(), DESIGN.uuid());
    }

    @Test
    @DisplayName("cold start: popular courses by 30-day enrolments, each with a POPULAR reason; no next steps")
    void coldStart() {
        CandidateCourse quiet = new CandidateCourse(UUID.randomUUID(), "Quiet", null, null, LocalDateTime.now(), null,
                List.of(MUSIC), List.of(), List.of(), 4.9, 0, true);
        CandidateCourse busy = new CandidateCourse(UUID.randomUUID(), "Busy", null, null, LocalDateTime.now().minusYears(1),
                null, List.of(COOKING), List.of(), List.of(), 3.0, 40, true);

        List<ScoredCourse> ranked = RecommendationScorer.rank(LearnerProfile.anonymous(), List.of(quiet, busy),
                LearnerSignals.none(), Surface.FOR_YOU, 6);

        assertThat(ranked.getFirst().course()).isEqualTo(busy);
        assertThat(ranked.getFirst().reasons()).containsExactly(
                new Reason(ReasonCode.POPULAR, "Popular in Cooking", COOKING.uuid()));
        assertThat(RecommendationScorer.rank(LearnerProfile.anonymous(), List.of(quiet, busy), LearnerSignals.none(),
                Surface.NEXT_STEPS, 6)).isEmpty();
    }

    @Test
    @DisplayName("similar: neighbours and shared categories outrank unrelated courses; the anchor never appears")
    void similar() {
        CandidateCourse anchor = course("Python Basics", 1, PROGRAMMING);
        CandidateCourse sibling = course("Python Testing", 1, PROGRAMMING);
        CandidateCourse neighbour = course("Data Viz", 1, DESIGN);
        CandidateCourse unrelated = course("Harmony", 1, MUSIC);

        List<ScoredCourse> ranked = RecommendationScorer.rankSimilar(anchor, List.of(anchor, unrelated, neighbour, sibling),
                Map.of(neighbour.uuid(), 4.0), Map.of(), 6);

        assertThat(ranked).extracting(ScoredCourse::course).doesNotContain(anchor);
        assertThat(ranked.getLast().course()).isEqualTo(unrelated);
        assertThat(ranked.getFirst().reasons().getFirst().code()).isIn(ReasonCode.CATEGORY, ReasonCode.CO_ENROLLED);
        assertThat(Set.of(ranked.get(0).course(), ranked.get(1).course())).containsExactlyInAnyOrder(sibling, neighbour);
    }

    // ----------------------------------------------------------------- helpers

    private static double round(double value) {
        return Math.round(value * 10_000d) / 10_000d;
    }

    private static LearnerProfile learner(Enrolment... enrolments) {
        return new LearnerProfile(UUID.randomUUID(), List.of(enrolments), List.of(), LearnerAffiliations.none());
    }

    private static Enrolment completed(UUID course, String name, int level, CategoryRef category) {
        return new Enrolment(course, name, "completed", 100, LocalDateTime.now().minusDays(30), LocalDateTime.now(),
                level, List.of(category), List.of());
    }

    private static CandidateCourse course(String name, int level, CategoryRef category) {
        return new CandidateCourse(UUID.randomUUID(), name, null, null, null, level, List.of(category), List.of(),
                List.of(), 4.0, 5, true);
    }
}
