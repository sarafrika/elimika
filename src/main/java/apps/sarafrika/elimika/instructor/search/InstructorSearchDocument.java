package apps.sarafrika.elimika.instructor.search;

import apps.sarafrika.elimika.shared.search.SearchDocument;
import apps.sarafrika.elimika.shared.search.SearchGeoPoint;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.UUID;

/**
 * The public discovery profile of an instructor, as stored in the {@code instructors} index.
 * <p>
 * Only what a public listing may show. Contact details, exact coordinates, education, documents,
 * rates and the owning user are deliberately absent: the index is readable by anyone the scope
 * admits, so leaving a field out is the only guarantee it cannot leak.
 * <p>
 * {@code _geo} (schema v3) is the one location field: set only for a verified instructor who opted
 * in to near-me search and has coordinates, rounded to two decimals (about 1 km), and left out of
 * the index's displayed attributes so hits never return it.
 *
 * @param skillLevels parallel to {@code skills}: the proficiency of the skill at the same position
 * @param skillUuids  the distinct skills-taxonomy entries the skills resolve to (schema v2); free-text
 *                    skills that match no curated skill are in {@code skills} only
 * @param active      always true for an indexed instructor - the module has no lifecycle flag, and
 *                    removing a profile deletes its row and with it the document
 * @param ratingAvg   mean review rating rounded to two decimals, null without reviews
 * @param createdAt   profile creation, UTC epoch seconds
 * @param geo         the rounded near-me point, or {@code null} to stay out of every geo query
 */
public record InstructorSearchDocument(
        @JsonProperty("uuid") UUID uuid,
        @JsonProperty("full_name") String fullName,
        @JsonProperty("professional_headline") String professionalHeadline,
        @JsonProperty("bio") String bio,
        @JsonProperty("location_name") String locationName,
        @JsonProperty("skills") List<String> skills,
        @JsonProperty("skill_levels") List<String> skillLevels,
        @JsonProperty("experience_positions") List<String> experiencePositions,
        @JsonProperty("experience_organisations") List<String> experienceOrganisations,
        @JsonProperty("admin_verified") boolean adminVerified,
        @JsonProperty("active") boolean active,
        @JsonProperty("rating_avg") Double ratingAvg,
        @JsonProperty("review_count") long reviewCount,
        @JsonProperty("created_at") Long createdAt,
        @JsonProperty("skill_uuids") List<UUID> skillUuids,
        @JsonProperty("_geo") SearchGeoPoint geo
) implements SearchDocument {
}
