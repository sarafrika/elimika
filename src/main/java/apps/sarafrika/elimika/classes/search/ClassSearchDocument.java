package apps.sarafrika.elimika.classes.search;

import apps.sarafrika.elimika.shared.search.SearchDocument;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * One class definition as the {@code classes} search index stores it.
 * <p>
 * Carries every attribute the listing scope needs ({@code is_active}, {@code class_visibility},
 * {@code organisation_uuid}, {@code default_instructor_uuid}, {@code uuid}) and nothing the listing
 * withholds: no instructor pay, meeting link or coordinates. Instants are UTC epoch seconds.
 */
public record ClassSearchDocument(
        @JsonProperty("uuid") UUID uuid,
        @JsonProperty("title") String title,
        @JsonProperty("description") String description,
        @JsonProperty("location_name") String locationName,
        @JsonProperty("course_uuid") UUID courseUuid,
        @JsonProperty("course_name") String courseName,
        @JsonProperty("program_uuid") UUID programUuid,
        @JsonProperty("program_title") String programTitle,
        @JsonProperty("organisation_uuid") UUID organisationUuid,
        @JsonProperty("organisation_name") String organisationName,
        @JsonProperty("branch_uuid") UUID branchUuid,
        @JsonProperty("branch_name") String branchName,
        @JsonProperty("default_instructor_uuid") UUID defaultInstructorUuid,
        @JsonProperty("instructor_name") String instructorName,
        @JsonProperty("category_uuid") UUID categoryUuid,
        @JsonProperty("is_active") boolean isActive,
        @JsonProperty("class_visibility") String classVisibility,
        @JsonProperty("content_approved") boolean contentApproved,
        @JsonProperty("location_type") String locationType,
        @JsonProperty("session_format") String sessionFormat,
        @JsonProperty("starts_at") Long startsAt,
        @JsonProperty("registration_closes_at") Long registrationClosesAt,
        @JsonProperty("sale_price") BigDecimal salePrice,
        @JsonProperty("created_at") Long createdAt
) implements SearchDocument {
}
