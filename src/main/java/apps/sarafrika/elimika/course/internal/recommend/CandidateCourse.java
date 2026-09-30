package apps.sarafrika.elimika.course.internal.recommend;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * A course as the recommender sees it, loaded fresh from SQL: display fields plus the relational
 * features scoring needs.
 *
 * @param publiclyListed published, admin-approved, active root course (the public catalogue)
 * @param ratingBayes    the nightly Bayesian rating, or {@code null} before the first run
 * @param popularity30d  real enrolments in the last 30 days, from the nightly stats
 */
public record CandidateCourse(
        UUID uuid,
        String name,
        String description,
        String thumbnailUrl,
        LocalDateTime createdDate,
        Integer levelOrder,
        List<CategoryRef> categories,
        List<UUID> skillUuids,
        List<PrerequisiteRef> prerequisites,
        Double ratingBayes,
        long popularity30d,
        boolean publiclyListed
) {

    public CandidateCourse {
        categories = categories == null ? List.of() : List.copyOf(categories);
        skillUuids = skillUuids == null ? List.of() : List.copyOf(skillUuids);
        prerequisites = prerequisites == null ? List.of() : List.copyOf(prerequisites);
    }

    /** A category the course is filed under, with its parent for half-weight affinity. */
    public record CategoryRef(UUID uuid, UUID parentUuid, String name) {
    }

    /** A declared prior course; {@code mandatory = false} is only recommended. */
    public record PrerequisiteRef(UUID courseUuid, String name, boolean mandatory) {
    }

    public List<UUID> categoryUuids() {
        return categories.stream().map(CategoryRef::uuid).toList();
    }
}
