package apps.sarafrika.elimika.course.internal.search;

import apps.sarafrika.elimika.shared.search.SearchDocument;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * The {@code rubrics} index document: one assessment rubric and how many course associations use it.
 */
public record RubricSearchDocument(
        @JsonProperty("uuid") UUID uuid,
        @JsonProperty("title") String title,
        @JsonProperty("description") String description,
        @JsonProperty("rubric_type") String rubricType,
        @JsonProperty("is_public") boolean isPublic,
        @JsonProperty("is_active") boolean isActive,
        @JsonProperty("status") String status,
        @JsonProperty("course_creator_uuid") UUID courseCreatorUuid,
        @JsonProperty("usage_count") long usageCount,
        @JsonProperty("created_at") Long createdAt
) implements SearchDocument {
}
