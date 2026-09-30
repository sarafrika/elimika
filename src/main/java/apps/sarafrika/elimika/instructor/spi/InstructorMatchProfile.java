package apps.sarafrika.elimika.instructor.spi;

import apps.sarafrika.elimika.shared.search.SearchGeoPoint;
import apps.sarafrika.elimika.shared.utils.enums.ProficiencyLevel;

import java.util.Map;
import java.util.UUID;

/**
 * What job matching needs to know about one instructor. Never carries rates (they live with the
 * training approvals in the course module) or exact coordinates.
 *
 * @param instructorUuid    the instructor
 * @param displayName       full name as the instructor gave it
 * @param locationName      the stored place name, or null
 * @param adminVerified     whether an administrator has verified the profile
 * @param skillLevels       taxonomy skill UUID to the highest proficiency the instructor claims for it;
 *                          skills with no taxonomy link are left out
 * @param yearsOfExperience summed from the experience entries (0 when none)
 * @param ratingAverage     plain average of {@code instructor_reviews.rating} (1-5), null without reviews
 * @param reviewCount       number of reviews
 * @param ratingBayes       Bayesian average on the 1-5 scale (prior: the platform mean weighted as
 *                          {@link InstructorMatchingService#RATING_PRIOR_WEIGHT} reviews), null when the
 *                          platform has no reviews at all
 * @param searchPoint       the near-me point (about 1 km), only for an opted-in, verified instructor
 *                          with coordinates; null otherwise
 */
public record InstructorMatchProfile(UUID instructorUuid,
                                     String displayName,
                                     String locationName,
                                     boolean adminVerified,
                                     Map<UUID, ProficiencyLevel> skillLevels,
                                     double yearsOfExperience,
                                     Double ratingAverage,
                                     long reviewCount,
                                     Double ratingBayes,
                                     SearchGeoPoint searchPoint) {

    public InstructorMatchProfile {
        skillLevels = skillLevels == null ? Map.of() : Map.copyOf(skillLevels);
    }

    /** Whether the instructor has opted in to location search (and so may be placed by distance). */
    public boolean locatable() {
        return searchPoint != null;
    }
}
