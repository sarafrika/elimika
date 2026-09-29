package apps.sarafrika.elimika.instructor.search;

import apps.sarafrika.elimika.shared.search.SearchDocument;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.UUID;

/**
 * The public discovery profile of an instructor, as stored in the {@code instructors} index.
 * <p>
 * Only what a public listing may show. Contact details, coordinates, education, documents, rates
 * and the owning user are deliberately absent: the index is readable by anyone the scope admits, so
 * leaving a field out is the only guarantee it cannot leak.
 *
 * @param skillLevels parallel to {@code skills}: the proficiency of the skill at the same position
 * @param active      always true for an indexed instructor - the module has no lifecycle flag, and
 *                    removing a profile deletes its row and with it the document
 * @param ratingAvg   mean review rating rounded to two decimals, null without reviews
 * @param createdAt   profile creation, UTC epoch seconds
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
        @JsonProperty("created_at") Long createdAt
) implements SearchDocument {
}
