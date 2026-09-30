package apps.sarafrika.elimika.course.internal.search;

import apps.sarafrika.elimika.shared.search.SearchDocument;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * The {@code courses} index document: one root course (never a shadow draft), denormalised with its
 * categories, difficulty, creator name and review/enrolment figures.
 * <p>
 * Deliberately absent: co-enrolment neighbours (they stay in SQL), the minimum training fee,
 * revenue-share fields, rate cards and lesson content.
 * {@code status} is the lower-case value the API serialises; instants are UTC epoch seconds.
 * {@code skill_uuids} (schema v2) are the skills-taxonomy entries the creator tagged the course with.
 */
public record CourseSearchDocument(
        @JsonProperty("uuid") UUID uuid,
        @JsonProperty("name") String name,
        @JsonProperty("description") String description,
        @JsonProperty("objectives") String objectives,
        @JsonProperty("category_uuids") List<UUID> categoryUuids,
        @JsonProperty("category_names") List<String> categoryNames,
        @JsonProperty("difficulty_uuid") UUID difficultyUuid,
        @JsonProperty("difficulty_name") String difficultyName,
        @JsonProperty("course_creator_uuid") UUID courseCreatorUuid,
        @JsonProperty("creator_name") String creatorName,
        @JsonProperty("status") String status,
        @JsonProperty("active") boolean active,
        @JsonProperty("admin_approved") boolean adminApproved,
        @JsonProperty("is_public") boolean isPublic,
        @JsonProperty("is_free") boolean isFree,
        @JsonProperty("price") BigDecimal price,
        @JsonProperty("thumbnail_url") String thumbnailUrl,
        @JsonProperty("rating_avg") Double ratingAvg,
        @JsonProperty("review_count") long reviewCount,
        @JsonProperty("enrolment_count") long enrolmentCount,
        @JsonProperty("created_at") Long createdAt,
        // Nightly aggregates from course_learning_stats (up to a day old; live figures come from SQL).
        @JsonProperty("completion_rate") Double completionRate,
        @JsonProperty("popularity_30d") long popularity30d,
        @JsonProperty("rating_bayes") Double ratingBayes,
        @JsonProperty("level_order") Integer levelOrder,
        @JsonProperty("prerequisite_uuids") List<UUID> prerequisiteUuids,
        @JsonProperty("age_lower_limit") Integer ageLowerLimit,
        @JsonProperty("age_upper_limit") Integer ageUpperLimit,
        @JsonProperty("skill_uuids") List<UUID> skillUuids
) implements SearchDocument {
}
