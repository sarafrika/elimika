package apps.sarafrika.elimika.instructor.search;

import apps.sarafrika.elimika.instructor.model.Instructor;
import apps.sarafrika.elimika.instructor.model.InstructorExperience;
import apps.sarafrika.elimika.instructor.model.InstructorReview;
import apps.sarafrika.elimika.instructor.model.InstructorSkill;
import apps.sarafrika.elimika.instructor.repository.InstructorExperienceRepository;
import apps.sarafrika.elimika.instructor.repository.InstructorRepository;
import apps.sarafrika.elimika.instructor.repository.InstructorReviewRepository;
import apps.sarafrika.elimika.instructor.repository.InstructorSkillRepository;
import apps.sarafrika.elimika.shared.search.SearchBatch;
import apps.sarafrika.elimika.shared.search.SearchDocumentSource;
import apps.sarafrika.elimika.shared.search.SearchIndexDefinition;
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
 */
@Component
@RequiredArgsConstructor
public class InstructorSearchSource implements SearchDocumentSource<InstructorSearchDocument> {

    public static final String INDEX = "instructors";

    public static final SearchIndexDefinition DEFINITION = SearchIndexDefinition.of(INDEX, 2,
            List.of("full_name", "professional_headline", "skills", "experience_positions",
                    "experience_organisations", "location_name", "bio"),
            List.of("admin_verified", "active", "skills", "skill_levels", "skill_uuids", "location_name", "uuid",
                    "created_at"),
            List.of("full_name", "rating_avg", "review_count", "created_at"));

    static final int BIO_MAX_LENGTH = 1500;

    private final InstructorRepository instructorRepository;
    private final InstructorSkillRepository skillRepository;
    private final InstructorExperienceRepository experienceRepository;
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
                SearchIndexTrigger.direct(InstructorSkill.class, InstructorSkill::getInstructorUuid),
                SearchIndexTrigger.direct(InstructorExperience.class, InstructorExperience::getInstructorUuid),
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
        Map<UUID, List<InstructorSkill>> skills = skillRepository.findByInstructorUuidInOrderByIdAsc(uuids).stream()
                .collect(Collectors.groupingBy(InstructorSkill::getInstructorUuid));
        Map<UUID, List<InstructorExperience>> experience = experienceRepository
                .findByInstructorUuidInOrderByIdAsc(uuids).stream()
                .collect(Collectors.groupingBy(InstructorExperience::getInstructorUuid));
        Map<UUID, InstructorRatingAggregate> ratings = new HashMap<>();
        reviewRepository.aggregateRatingsByInstructorUuidIn(uuids)
                .forEach(aggregate -> ratings.put(aggregate.instructorUuid(), aggregate));

        return instructors.stream()
                .map(instructor -> toDocument(instructor,
                        skills.getOrDefault(instructor.getUuid(), List.of()),
                        experience.getOrDefault(instructor.getUuid(), List.of()),
                        ratings.get(instructor.getUuid())))
                .toList();
    }

    private static InstructorSearchDocument toDocument(
            Instructor instructor,
            List<InstructorSkill> skills,
            List<InstructorExperience> experience,
            InstructorRatingAggregate rating
    ) {
        List<String> skillNames = new ArrayList<>();
        List<String> skillLevels = new ArrayList<>();
        List<UUID> skillUuids = new ArrayList<>();
        for (InstructorSkill skill : skills) {
            if (skill.getSkillUuid() != null && !skillUuids.contains(skill.getSkillUuid())) {
                skillUuids.add(skill.getSkillUuid());
            }
            if (skill.getSkillName() == null || skill.getSkillName().isBlank()) {
                continue;
            }
            skillNames.add(skill.getSkillName().trim());
            skillLevels.add(skill.getProficiencyLevel() == null ? "" : skill.getProficiencyLevel().name());
        }
        return new InstructorSearchDocument(
                instructor.getUuid(),
                instructor.getFullName(),
                instructor.getProfessionalHeadline(),
                truncate(instructor.getBio()),
                instructor.getLocationName(),
                skillNames,
                skillLevels,
                distinctNonBlank(experience, InstructorExperience::getPosition),
                distinctNonBlank(experience, InstructorExperience::getOrganizationName),
                Boolean.TRUE.equals(instructor.getAdminVerified()),
                true,
                rating == null || rating.average() == null ? null
                        : BigDecimal.valueOf(rating.average()).setScale(2, RoundingMode.HALF_UP).doubleValue(),
                rating == null || rating.count() == null ? 0L : rating.count(),
                instructor.getCreatedDate() == null ? null : instructor.getCreatedDate().toEpochSecond(ZoneOffset.UTC),
                skillUuids);
    }

    private static List<String> distinctNonBlank(List<InstructorExperience> rows,
                                                 Function<InstructorExperience, String> field) {
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
