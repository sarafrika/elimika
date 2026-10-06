package apps.sarafrika.elimika.instructor.search;

import apps.sarafrika.elimika.instructor.model.Instructor;
import apps.sarafrika.elimika.instructor.repository.InstructorRepository;
import apps.sarafrika.elimika.instructor.repository.InstructorReviewRepository;
import apps.sarafrika.elimika.profile.spi.ProfessionalProfileService;
import apps.sarafrika.elimika.profile.spi.UserExperienceDTO;
import apps.sarafrika.elimika.profile.spi.UserSkillDTO;
import apps.sarafrika.elimika.instructor.spi.InstructorMatchProfile;
import apps.sarafrika.elimika.instructor.spi.InstructorMatchingService;
import apps.sarafrika.elimika.shared.search.NearMe;
import apps.sarafrika.elimika.shared.search.SearchAvailability;
import apps.sarafrika.elimika.shared.search.SearchFilter;
import apps.sarafrika.elimika.shared.search.SearchGateway;
import apps.sarafrika.elimika.shared.search.SearchRequest;
import apps.sarafrika.elimika.shared.search.SearchResults;
import apps.sarafrika.elimika.shared.search.SearchScope;
import apps.sarafrika.elimika.shared.search.SearchSort;
import apps.sarafrika.elimika.shared.search.SearchUnavailableException;
import apps.sarafrika.elimika.shared.utils.enums.ProficiencyLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Instructor facts for marketplace job matching, and the candidate lookup on the {@code instructors}
 * index. The index only narrows the set; every fact used for scoring is read back from the database.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InstructorMatchingServiceImpl implements InstructorMatchingService {

    /** An engine filter with thousands of UUIDs gets slow; beyond this the set is trimmed. */
    static final int MAX_FILTER_UUIDS = 1000;

    private final InstructorRepository instructorRepository;
    private final ProfessionalProfileService profileService;
    private final InstructorReviewRepository reviewRepository;
    private final SearchAvailability searchAvailability;
    private final SearchGateway searchGateway;

    @Override
    public Map<UUID, InstructorMatchProfile> findMatchProfiles(Collection<UUID> instructorUuids) {
        Set<UUID> requested = distinct(instructorUuids);
        if (requested.isEmpty()) {
            return Map.of();
        }
        List<Instructor> instructors = instructorRepository.findByUuidIn(requested);
        if (instructors.isEmpty()) {
            return Map.of();
        }
        List<UUID> uuids = instructors.stream().map(Instructor::getUuid).toList();
        // Skills and experience live on the owner's shared profile, keyed by user.
        List<UUID> userUuids = instructors.stream().map(Instructor::getUserUuid).filter(Objects::nonNull).toList();

        Map<UUID, Map<UUID, ProficiencyLevel>> skills = new HashMap<>();
        for (UserSkillDTO skill : profileService.skills().listForUsers(userUuids)) {
            if (skill.skillUuid() == null) {
                continue;
            }
            ProficiencyLevel level = skill.proficiencyLevel() == null ? ProficiencyLevel.BEGINNER : skill.proficiencyLevel();
            skills.computeIfAbsent(skill.userUuid(), key -> new HashMap<>())
                    .merge(skill.skillUuid(), level, (a, b) -> a.ordinal() >= b.ordinal() ? a : b);
        }
        Map<UUID, Double> years = new HashMap<>();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        for (UserExperienceDTO experience : profileService.experience().listForUsers(userUuids)) {
            years.merge(experience.userUuid(), yearsOf(experience, today), Double::sum);
        }
        Map<UUID, InstructorRatingAggregate> ratings = new HashMap<>();
        reviewRepository.aggregateRatingsByInstructorUuidIn(uuids)
                .forEach(aggregate -> ratings.put(aggregate.instructorUuid(), aggregate));
        Double platformMean = reviewRepository.findPlatformAverageRating();

        Map<UUID, InstructorMatchProfile> profiles = new LinkedHashMap<>();
        for (Instructor instructor : instructors) {
            InstructorRatingAggregate rating = ratings.get(instructor.getUuid());
            long count = rating == null || rating.count() == null ? 0L : rating.count();
            Double average = rating == null ? null : rating.average();
            profiles.put(instructor.getUuid(), new InstructorMatchProfile(
                    instructor.getUuid(),
                    instructor.getFullName(),
                    instructor.getLocationName(),
                    Boolean.TRUE.equals(instructor.getAdminVerified()),
                    instructor.getUserUuid() == null ? Map.of() : skills.getOrDefault(instructor.getUserUuid(), Map.of()),
                    instructor.getUserUuid() == null ? 0d : years.getOrDefault(instructor.getUserUuid(), 0d),
                    average,
                    count,
                    bayesian(average, count, platformMean),
                    InstructorSearchSource.nearMePoint(instructor)));
        }
        return profiles;
    }

    /** {@code (prior * mean + sum) / (prior + n)}; null only when the platform has no reviews at all. */
    static Double bayesian(Double average, long count, Double platformMean) {
        if (platformMean == null) {
            return average;
        }
        double sum = average == null ? 0d : average * count;
        return (RATING_PRIOR_WEIGHT * platformMean + sum) / (RATING_PRIOR_WEIGHT + count);
    }

    /** The stated years, else the span of the dates (to today for a current role), never negative. */
    static double yearsOf(UserExperienceDTO experience, LocalDate today) {
        BigDecimal stated = experience.yearsOfExperience();
        if (stated != null) {
            return Math.max(0d, stated.doubleValue());
        }
        if (experience.startDate() == null) {
            return 0d;
        }
        LocalDate end = experience.endDate() != null ? experience.endDate() : today;
        long days = ChronoUnit.DAYS.between(experience.startDate(), end);
        return Math.max(0d, days / 365.25d);
    }

    @Override
    public List<UUID> searchVerifiedAmong(Collection<UUID> among, NearMe near, int limit) {
        Set<UUID> candidates = distinct(among);
        if (candidates.isEmpty() || limit < 1) {
            return List.of();
        }
        if (!searchAvailability.isReadEnabled(InstructorSearchSource.INDEX)) {
            throw new SearchUnavailableException("Search is not enabled for " + InstructorSearchSource.INDEX);
        }
        List<UUID> within = candidates.stream().limit(MAX_FILTER_UUIDS).toList();
        int size = Math.clamp(limit, 1, SearchRequest.MAX_SIZE);
        SearchScope verifiedOnly = SearchScope.of(SearchFilter.eq("admin_verified", true), "job-candidates");
        SearchFilter amongFilter = SearchFilter.in("uuid", within);

        LinkedHashSet<UUID> merged = new LinkedHashSet<>();
        if (near != null) {
            // Only opted-in, verified instructors carry _geo, so this pass can only find them.
            merged.addAll(SearchResults.hitUuids(searchGateway.search(new SearchRequest(InstructorSearchSource.INDEX,
                    null, SearchFilter.and(amongFilter, near.filter()), verifiedOnly, List.of(near.sort()),
                    0, size, List.of(), null))));
        }
        if (merged.size() < size) {
            // Everyone else: instructors who never opted in are still candidates, placed neutrally.
            merged.addAll(SearchResults.hitUuids(searchGateway.search(new SearchRequest(InstructorSearchSource.INDEX,
                    null, amongFilter, verifiedOnly, List.of(SearchSort.desc("rating_avg")),
                    0, size, List.of(), null))));
        }
        return merged.stream().filter(Objects::nonNull).limit(size).toList();
    }

    private static Set<UUID> distinct(Collection<UUID> uuids) {
        if (uuids == null) {
            return Set.of();
        }
        Set<UUID> result = new LinkedHashSet<>();
        for (UUID uuid : uuids) {
            if (uuid != null) {
                result.add(uuid);
            }
        }
        return result;
    }
}
