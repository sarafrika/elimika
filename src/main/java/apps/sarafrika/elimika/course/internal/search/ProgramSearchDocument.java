package apps.sarafrika.elimika.course.internal.search;

import apps.sarafrika.elimika.shared.search.SearchDocument;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * The {@code programs} index document: one training program with its category, creator name and the
 * names of its member courses. {@code is_public} is the live state the program SQL scope keys off
 * (published, active and admin-approved).
 * <p>
 * Schema 2 adds what a public catalogue card and the merged catalogue ranking need:
 * {@code category_uuids}/{@code category_names} (the program's one category, shaped like the
 * course document's lists), {@code thumbnail_url} (the program's own, else its first member course's),
 * {@code course_count}, {@code difficulty_uuids} and the
 * {@code level_min}/{@code level_max} names over the member courses, review figures
 * ({@code rating_avg}, {@code review_count}, {@code rating_bayes}) and {@code popularity_30d}
 * (enrolments in the last 30 days), named like the course document's so the two indexes sort alike.
 */
public record ProgramSearchDocument(
        @JsonProperty("uuid") UUID uuid,
        @JsonProperty("title") String title,
        @JsonProperty("program_code") String programCode,
        @JsonProperty("description") String description,
        @JsonProperty("category_uuid") UUID categoryUuid,
        @JsonProperty("category_name") String categoryName,
        @JsonProperty("course_creator_uuid") UUID courseCreatorUuid,
        @JsonProperty("creator_name") String creatorName,
        @JsonProperty("course_names") List<String> courseNames,
        @JsonProperty("status") String status,
        @JsonProperty("is_published") boolean isPublished,
        @JsonProperty("admin_approved") boolean adminApproved,
        @JsonProperty("active") boolean active,
        @JsonProperty("is_public") boolean isPublic,
        @JsonProperty("is_free") boolean isFree,
        @JsonProperty("price") BigDecimal price,
        @JsonProperty("created_at") Long createdAt,
        @JsonProperty("category_uuids") List<UUID> categoryUuids,
        @JsonProperty("category_names") List<String> categoryNames,
        @JsonProperty("thumbnail_url") String thumbnailUrl,
        @JsonProperty("course_count") int courseCount,
        @JsonProperty("difficulty_uuids") List<UUID> difficultyUuids,
        @JsonProperty("level_min") String levelMin,
        @JsonProperty("level_max") String levelMax,
        @JsonProperty("rating_avg") Double ratingAvg,
        @JsonProperty("review_count") long reviewCount,
        @JsonProperty("rating_bayes") Double ratingBayes,
        @JsonProperty("enrolment_count") long enrolmentCount,
        @JsonProperty("popularity_30d") long popularity30d
) implements SearchDocument {
}
