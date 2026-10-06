package apps.sarafrika.elimika.instructor.search;

import apps.sarafrika.elimika.instructor.model.Instructor;
import apps.sarafrika.elimika.instructor.model.InstructorReview;
import apps.sarafrika.elimika.instructor.repository.InstructorRepository;
import apps.sarafrika.elimika.instructor.repository.InstructorReviewRepository;
import apps.sarafrika.elimika.profile.spi.ProfessionalProfileDTO;
import apps.sarafrika.elimika.profile.spi.ProfessionalProfileService;
import apps.sarafrika.elimika.profile.spi.UserExperienceDTO;
import apps.sarafrika.elimika.profile.spi.UserSkillDTO;
import apps.sarafrika.elimika.shared.search.SearchBatch;
import apps.sarafrika.elimika.shared.search.SearchDocumentAttributes;
import apps.sarafrika.elimika.shared.search.SearchDocumentSource;
import apps.sarafrika.elimika.shared.search.SearchGeoPoint;
import apps.sarafrika.elimika.shared.search.SearchIndexDefinition;
import apps.sarafrika.elimika.shared.search.SearchSynonymSource;
import apps.sarafrika.elimika.shared.search.SearchIndexTrigger;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;

/**
 * Feeds the {@code instructors} index: one public discovery profile per instructor, with their skills,
 * experience and review metrics folded in. Every instructor row is indexable; visibility (verified
 * only, for non-admins) is applied at query time by {@link InstructorSearchScopes}.
 * <p>
 * Schema v3 adds {@code _geo} for near-me search: only for an instructor who opted in, is verified
 * and has coordinates, and always rounded to about 1 km. Verification and the opt-in are columns of
 * the instructor row, so the entity trigger re-indexes the profile when either changes.
 * Skills, experience and basics come from the owner's shared professional profile.
 */
@Component
@RequiredArgsConstructor
public class InstructorSearchSource implements SearchDocumentSource<InstructorSearchDocument> {

    public static final String INDEX = "instructors";

    public static final SearchIndexDefinition DEFINITION = SearchIndexDefinition.of(INDEX, 3,
            List.of("full_name", "professional_headline", "skills", "experience_positions",
                    "experience_organisations", "location_name", "bio"),
            List.of("admin_verified", "active", "skills", "skill_levels", "skill_uuids", "location_name", "uuid",
                    "created_at", SearchIndexDefinition.GEO_ATTRIBUTE),
            List.of("full_name", "rating_avg", "review_count", "created_at", SearchIndexDefinition.GEO_ATTRIBUTE))
            .withDisplayedAttributes(SearchDocumentAttributes.displayedWithoutGeo(InstructorSearchDocument.class))
            .withSynonymSources(List.of(SearchSynonymSource.SKILLS));

    static final int BIO_MAX_LENGTH = 1500;

    private final InstructorRepository instructorRepository;
    private final ProfessionalProfileService profileService;
    private final InstructorReviewRepository reviewRepository;

    @Override
    public SearchIndexDefinition definition() {
        return DEFINITION;
    }

    @Override
    public List<InstructorSearchDocument> loadByUuids(Collection<UUID> uuids) {
        if (uuids == null || uuids.isEmpty()) {
            return List.of();
        }
        return toDocuments(instructorRepository.findByUuidIn(uuids));
    }

    @Override
    public SearchBatch<InstructorSearchDocument> loadAfter(long lastId, int batchSize) {
        List<Instructor> rows = instructorRepository.findByIdGreaterThanOrderByIdAsc(lastId, Limit.of(batchSize));
        if (rows.isEmpty()) {
            return SearchBatch.end(lastId);
        }
        return new SearchBatch<>(toDocuments(rows), rows.getLast().getId());
    }

    @Override
    public List<SearchIndexTrigger<?>> triggers() {
        return List.of(
                SearchIndexTrigger.direct(Instructor.class, Instructor::getUuid),
                SearchIndexTrigger.direct(InstructorReview.class, InstructorReview::getInstructorUuid));
    }

    @Override
    public long countIndexable() {
        return instructorRepository.count();
    }

    private List<InstructorSearchDocument> toDocuments(List<Instructor> instructors) {
        if (instructors.isEmpty()) {
            return List.of();
        }
        List<UUID> uuids = instructors.stream().map(Instructor::getUuid).toList();
        List<UUID> userUuids = instructors.stream().map(Instructor::getUserUuid).filter(Objects::nonNull).toList();
        Map<UUID, List<UserSkillDTO>> skills = profileService.skills().listForUsers(userUuids).stream()
                .collect(Collectors.groupingBy(UserSkillDTO::userUuid));
        Map<UUID, List<UserExperienceDTO>> experience = profileService.experience().listForUsers(userUuids).stream()
                .collect(Collectors.groupingBy(UserExperienceDTO::userUuid));
        Map<UUID, ProfessionalProfileDTO> basics = profileService.getBasics(userUuids);
        Map<UUID, InstructorRatingAggregate> ratings = new HashMap<>();
        reviewRepository.aggregateRatingsByInstructorUuidIn(uuids)
                .forEach(aggregate -> ratings.put(aggregate.instructorUuid(), aggregate));

        return instructors.stream()
                .map(instructor -> toDocument(instructor,
                        basics.get(instructor.getUserUuid()),
                        skills.getOrDefault(instructor.getUserUuid(), List.of()),
                        experience.getOrDefault(instructor.getUserUuid(), List.of()),
                        ratings.get(instructor.getUuid())))
                .toList();
    }

    private static InstructorSearchDocument toDocument(
            Instructor instructor,
            ProfessionalProfileDTO basics,
            List<UserSkillDTO> skills,
            List<UserExperienceDTO> experience,
            InstructorRatingAggregate rating
    ) {
        ProfessionalProfileDTO profile = basics == null ? ProfessionalProfileDTO.empty(instructor.getUserUuid()) : basics;
        List<String> skillNames = new ArrayList<>();
        List<String> skillLevels = new ArrayList<>();
        List<UUID> skillUuids = new ArrayList<>();
        for (UserSkillDTO skill : skills) {
            if (skill.skillUuid() != null && !skillUuids.contains(skill.skillUuid())) {
                skillUuids.add(skill.skillUuid());
            }
            if (skill.skillName() == null || skill.skillName().isBlank()) {
                continue;
            }
            skillNames.add(skill.skillName().trim());
            skillLevels.add(skill.proficiencyLevel() == null ? "" : skill.proficiencyLevel().name());
        }
        return new InstructorSearchDocument(
                instructor.getUuid(),
                instructor.getFullName(),
                profile.professionalHeadline() != null ? profile.professionalHeadline() : instructor.getProfessionalHeadline(),
                truncate(profile.bio() != null ? profile.bio() : instructor.getBio()),
                profile.locationName() != null ? profile.locationName() : instructor.getLocationName(),
                skillNames,
                skillLevels,
                distinctNonBlank(experience, UserExperienceDTO::position),
                distinctNonBlank(experience, UserExperienceDTO::organizationName),
                Boolean.TRUE.equals(instructor.getAdminVerified()),
                true,
                rating == null || rating.average() == null ? null
                        : BigDecimal.valueOf(rating.average()).setScale(2, RoundingMode.HALF_UP).doubleValue(),
                rating == null || rating.count() == null ? 0L : rating.count(),
                instructor.getCreatedDate() == null ? null : instructor.getCreatedDate().toEpochSecond(ZoneOffset.UTC),
                skillUuids,
                nearMePoint(instructor));
    }

    /**
     * The instructor's near-me point: only with the owner's opt-in, verification and both coordinates,
     * and rounded to about 1 km. Anything else stays out of every geo query.
     */
    static SearchGeoPoint nearMePoint(Instructor instructor) {
        return isLocatable(instructor)
                ? SearchGeoPoint.rounded(instructor.getLatitude(), instructor.getLongitude())
                : null;
    }

    /** Opted in, verified and with both coordinates: the only instructors near-me may return. */
    static boolean isLocatable(Instructor instructor) {
        return Boolean.TRUE.equals(instructor.getLocationSearchOptIn())
                && Boolean.TRUE.equals(instructor.getAdminVerified())
                && instructor.getLatitude() != null
                && instructor.getLongitude() != null;
    }

    private static List<String> distinctNonBlank(List<UserExperienceDTO> rows,
                                                 Function<UserExperienceDTO, String> field) {
        return rows.stream()
                .map(field)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .toList();
    }

    /** Caps the bio so one long profile cannot dominate the index; prefers a word boundary. */
    static String truncate(String bio) {
        if (bio == null || bio.length() <= BIO_MAX_LENGTH) {
            return bio;
        }
        int end = BIO_MAX_LENGTH;
        if (Character.isHighSurrogate(bio.charAt(end - 1))) {
            end--;
        }
        int lastSpace = bio.lastIndexOf(' ', end);
        if (lastSpace > BIO_MAX_LENGTH - 100) {
            end = lastSpace;
        }
        return bio.substring(0, end).stripTrailing();
    }
}
