package apps.sarafrika.elimika.course.internal.recommend;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Small value types shared by the recommender's retrieval, scoring and response steps. */
public final class RecommendationTypes {

    /** The scoring version recorded with every impression; bump it when weights or rules change. */
    public static final String MODEL_VERSION = "rules-v2";

    /** Item type recorded with impressions. */
    public static final String ITEM_TYPE = "course";

    private RecommendationTypes() {
    }

    /** Where recommendations are shown. */
    public enum Surface {
        /** The personal "recommended for you" list. */
        FOR_YOU("for_you", "course_for_you"),
        /** Courses that follow on from what the learner has done, including ones with a prerequisite still to finish. */
        NEXT_STEPS("next_steps", "course_next_steps"),
        /** The non-personal "similar courses" rail on a course page. */
        SIMILAR("similar", "course_similar");

        private final String value;
        private final String trackingSurface;

        Surface(String value, String trackingSurface) {
            this.value = value;
            this.trackingSurface = trackingSurface;
        }

        public String value() {
            return value;
        }

        public String trackingSurface() {
            return trackingSurface;
        }

        /** Case-insensitive; {@code null} or blank is {@link #FOR_YOU}. Only the personal surfaces are accepted. */
        public static Surface personal(String value) {
            if (value == null || value.isBlank()) {
                return FOR_YOU;
            }
            String normalised = value.trim().toLowerCase(Locale.ROOT);
            for (Surface surface : List.of(FOR_YOU, NEXT_STEPS)) {
                if (surface.value.equals(normalised)) {
                    return surface;
                }
            }
            throw new IllegalArgumentException("surface must be for_you or next_steps");
        }
    }

    /** Why a course was recommended. Codes are stable; texts are for display. */
    public enum ReasonCode {
        NEXT_STEP,
        PREREQUISITE_PENDING,
        CO_ENROLLED,
        CATEGORY,
        SKILL_GAP,
        AFFILIATION,
        SIMILAR_CONTENT,
        POPULAR
    }

    /**
     * One reason. {@code text} is {@code null} only for {@link ReasonCode#AFFILIATION} until the service
     * resolves the organisation or instructor name.
     *
     * @param relatedUuid the learner's own course, the category, the organisation or instructor, or {@code null}
     */
    public record Reason(ReasonCode code, String text, UUID relatedUuid) {
        public Reason withText(String newText) {
            return new Reason(code, newText, relatedUuid);
        }
    }

    /** A ranked course with its reasons, strongest first. */
    public record ScoredCourse(CandidateCourse course, double score, List<Reason> reasons, boolean exploration) {
        public ScoredCourse {
            reasons = List.copyOf(reasons);
        }

        public ScoredCourse withReasons(List<Reason> newReasons) {
            return new ScoredCourse(course, score, newReasons, exploration);
        }
    }

    /** Who offers an affiliated course to the learner: an organisation or an instructor they learn with. */
    public record AffiliationOffer(UUID applicantUuid, boolean organisation) {
    }

    /**
     * Relational signals around the learner, loaded once per request.
     *
     * @param coLifts        for each of the learner's courses, the lift of each neighbour that cleared the
     *                       privacy threshold
     * @param offers         affiliated courses and who offers them
     * @param textSimilarity "more like this" relevance in [0, 1] per course, from search
     */
    public record LearnerSignals(Map<UUID, Map<UUID, Double>> coLifts, Map<UUID, AffiliationOffer> offers,
                                 Map<UUID, Double> textSimilarity) {
        public LearnerSignals {
            coLifts = coLifts == null ? Map.of() : Map.copyOf(coLifts);
            offers = offers == null ? Map.of() : Map.copyOf(offers);
            textSimilarity = textSimilarity == null ? Map.of() : Map.copyOf(textSimilarity);
        }

        public static LearnerSignals none() {
            return new LearnerSignals(Map.of(), Map.of(), Map.of());
        }
    }
}
