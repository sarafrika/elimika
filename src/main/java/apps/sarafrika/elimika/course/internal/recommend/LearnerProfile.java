package apps.sarafrika.elimika.course.internal.recommend;

import apps.sarafrika.elimika.course.internal.recommend.CandidateCourse.CategoryRef;
import apps.sarafrika.elimika.shared.spi.enrollment.LearnerAffiliations;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * One learner's own rows, loaded per request and never stored: enrolments with progress, declared skill
 * goals and affiliations. Everything the scorer derives (category affinity, best level, skill gap) is
 * computed here from those rows.
 *
 * @param studentUuid  the learner, or {@code null} for a caller without a student profile
 * @param enrolments   one entry per course, the most advanced enrolment when there are several
 * @param skillGoals   declared goal skills
 * @param affiliations organisations, instructors and courses the learner is linked to
 */
public record LearnerProfile(
        UUID studentUuid,
        List<Enrolment> enrolments,
        List<UUID> skillGoals,
        LearnerAffiliations affiliations
) {

    public static final String COMPLETED = "completed";
    public static final String DROPPED = "dropped";

    public LearnerProfile {
        enrolments = enrolments == null ? List.of() : List.copyOf(enrolments);
        skillGoals = skillGoals == null ? List.of() : List.copyOf(skillGoals);
        affiliations = affiliations == null ? LearnerAffiliations.none() : affiliations;
    }

    public static LearnerProfile anonymous() {
        return new LearnerProfile(null, List.of(), List.of(), LearnerAffiliations.none());
    }

    /**
     * One course enrolment.
     *
     * @param status   lower-case: active, completed, dropped or suspended
     * @param progress 0-100
     */
    public record Enrolment(
            UUID courseUuid,
            String courseName,
            String status,
            double progress,
            LocalDateTime enrolledAt,
            LocalDateTime completedAt,
            Integer levelOrder,
            List<CategoryRef> categories,
            List<UUID> skillUuids
    ) {
        public Enrolment {
            categories = categories == null ? List.of() : List.copyOf(categories);
            skillUuids = skillUuids == null ? List.of() : List.copyOf(skillUuids);
        }

        public boolean completed() {
            return COMPLETED.equals(status);
        }

        public boolean dropped() {
            return DROPPED.equals(status);
        }

        /** How much this enrolment says about the learner's interests: completion counts fully. */
        public double weight() {
            if (completed()) {
                return 1.0;
            }
            if (dropped()) {
                return 0.0;
            }
            return Math.max(0.25, Math.min(1.0, progress / 100.0));
        }

        private int rank() {
            return completed() ? 3 : dropped() ? 0 : "active".equals(status) ? 2 : 1;
        }
    }

    /** Keeps one enrolment per course: completed over active over suspended over dropped, then latest. */
    public static List<Enrolment> collapse(List<Enrolment> rows) {
        Map<UUID, Enrolment> best = new LinkedHashMap<>();
        for (Enrolment row : rows) {
            best.merge(row.courseUuid(), row, (a, b) -> {
                if (a.rank() != b.rank()) {
                    return a.rank() > b.rank() ? a : b;
                }
                return a.progress() >= b.progress() ? a : b;
            });
        }
        return List.copyOf(best.values());
    }

    public boolean hasHistory() {
        return !enrolments.isEmpty();
    }

    /** Whether anything personal can drive ranking; without it the learner gets popular courses. */
    public boolean hasSignals() {
        return hasHistory() || !skillGoals.isEmpty() || !affiliations.isEmpty();
    }

    public Set<UUID> enrolledCourseUuids() {
        Set<UUID> uuids = new HashSet<>();
        enrolments.forEach(e -> uuids.add(e.courseUuid()));
        return uuids;
    }

    public Set<UUID> completedCourseUuids() {
        Set<UUID> uuids = new HashSet<>();
        enrolments.stream().filter(Enrolment::completed).forEach(e -> uuids.add(e.courseUuid()));
        return uuids;
    }

    public Optional<Enrolment> enrolment(UUID courseUuid) {
        return enrolments.stream().filter(e -> e.courseUuid().equals(courseUuid)).findFirst();
    }

    /** Categories of the learner's non-dropped enrolments. */
    public Set<UUID> categoryUuids() {
        Set<UUID> uuids = new HashSet<>();
        enrolments.stream().filter(e -> !e.dropped())
                .forEach(e -> e.categories().forEach(c -> uuids.add(c.uuid())));
        return uuids;
    }

    /** The course to base "more like this" on: the latest completed, else the latest enrolment. */
    public Optional<Enrolment> anchorCourse() {
        Comparator<Enrolment> byCompletion = Comparator.comparing(Enrolment::completedAt,
                Comparator.nullsFirst(Comparator.naturalOrder()));
        Optional<Enrolment> completed = enrolments.stream().filter(Enrolment::completed).max(byCompletion);
        if (completed.isPresent()) {
            return completed;
        }
        return enrolments.stream().filter(e -> !e.dropped())
                .max(Comparator.comparing(Enrolment::enrolledAt, Comparator.nullsFirst(Comparator.naturalOrder())));
    }

    /** Goal skills not already taught by a completed course. */
    public Set<UUID> skillGap() {
        Set<UUID> gap = new HashSet<>(skillGoals);
        enrolments.stream().filter(Enrolment::completed).forEach(e -> e.skillUuids().forEach(gap::remove));
        return gap;
    }

    /** Parent of each category the learner has touched, from their own enrolment rows. */
    Map<UUID, UUID> categoryParents() {
        Map<UUID, UUID> parents = new HashMap<>();
        enrolments.forEach(e -> e.categories().forEach(c -> {
            if (c.parentUuid() != null) {
                parents.put(c.uuid(), c.parentUuid());
            }
        }));
        return parents;
    }
}
