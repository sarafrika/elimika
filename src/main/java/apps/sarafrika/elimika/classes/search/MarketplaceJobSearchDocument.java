package apps.sarafrika.elimika.classes.search;

import apps.sarafrika.elimika.shared.search.SearchDocument;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

/**
 * One marketplace class job as the {@code marketplace_jobs} search index stores it.
 * <p>
 * Carries {@code status} and {@code organisation_uuid} for the marketplace scope, and nothing a
 * browser of the marketplace may not read: no instructor pay, sale price, branch contact, meeting
 * link or coordinates. Instants are UTC epoch seconds.
 */
public record MarketplaceJobSearchDocument(
        @JsonProperty("uuid") UUID uuid,
        @JsonProperty("title") String title,
        @JsonProperty("description") String description,
        @JsonProperty("location_name") String locationName,
        @JsonProperty("target_groups") List<String> targetGroups,
        @JsonProperty("service_type") String serviceType,
        @JsonProperty("status") String status,
        @JsonProperty("organisation_uuid") UUID organisationUuid,
        @JsonProperty("organisation_name") String organisationName,
        @JsonProperty("branch_uuid") UUID branchUuid,
        @JsonProperty("branch_name") String branchName,
        @JsonProperty("course_uuid") UUID courseUuid,
        @JsonProperty("course_name") String courseName,
        @JsonProperty("program_uuid") UUID programUuid,
        @JsonProperty("program_title") String programTitle,
        @JsonProperty("category_uuid") UUID categoryUuid,
        @JsonProperty("location_type") String locationType,
        @JsonProperty("session_format") String sessionFormat,
        @JsonProperty("class_visibility") String classVisibility,
        @JsonProperty("starts_at") Long startsAt,
        @JsonProperty("registration_closes_at") Long registrationClosesAt,
        @JsonProperty("session_count") int sessionCount,
        @JsonProperty("created_at") Long createdAt,
        @JsonProperty("required_skill_uuids") List<UUID> requiredSkillUuids,
        @JsonProperty("required_skill_names") List<String> requiredSkillNames
) implements SearchDocument {
}
