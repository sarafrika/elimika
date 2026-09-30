package apps.sarafrika.elimika.instructor.spi;

import apps.sarafrika.elimika.shared.search.NearMe;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Instructor facts and index lookups for marketplace job matching. */
public interface InstructorMatchingService {

    /** A rating counts as this many reviews at the platform mean. */
    int RATING_PRIOR_WEIGHT = 5;

    /** Match profiles for the given instructors, in one batch of queries; unknown UUIDs are left out. */
    Map<UUID, InstructorMatchProfile> findMatchProfiles(Collection<UUID> instructorUuids);

    /**
     * Verified instructors among {@code among}, from the {@code instructors} index: first those within
     * {@code near} (only opted-in instructors carry a point), then the rest, without repeats, at most
     * {@code limit}. {@code near} may be null.
     *
     * @throws apps.sarafrika.elimika.shared.search.SearchUnavailableException when the index cannot answer
     */
    List<UUID> searchVerifiedAmong(Collection<UUID> among, NearMe near, int limit);
}
