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
 */
public record ProgramSearchDocument(
        @JsonProperty("uuid") UUID uuid,
        @JsonProperty("title") String title,
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
        @JsonProperty("created_at") Long createdAt
) implements SearchDocument {
}
