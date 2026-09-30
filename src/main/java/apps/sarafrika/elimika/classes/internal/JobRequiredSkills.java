package apps.sarafrika.elimika.classes.internal;

import apps.sarafrika.elimika.classes.model.ClassMarketplaceJob;
import apps.sarafrika.elimika.classes.model.ClassMarketplaceJobRequiredSkill;
import apps.sarafrika.elimika.classes.repository.ClassMarketplaceJobRequiredSkillRepository;
import apps.sarafrika.elimika.course.spi.CourseSkillLookupService;
import apps.sarafrika.elimika.shared.utils.enums.ProficiencyLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * A job's effective required skills, in one place for the API and the search index: its own tags
 * when it has any, otherwise its course's skills (inherited, all mandatory, the course's level as
 * the minimum proficiency).
 */
@Component
@RequiredArgsConstructor
public class JobRequiredSkills {

    private final ClassMarketplaceJobRequiredSkillRepository requiredSkillRepository;
    private final CourseSkillLookupService courseSkillLookupService;

    public record Tag(UUID skillUuid, ProficiencyLevel minProficiency, boolean mandatory) {
    }

    /** @param inheritedFromCourseUuid set only when {@code inherited} */
    public record Effective(boolean inherited, UUID inheritedFromCourseUuid, List<Tag> tags) {
        static final Effective NONE = new Effective(false, null, List.of());
    }

    public Map<UUID, Effective> forJobs(Collection<ClassMarketplaceJob> jobs) {
        if (jobs.isEmpty()) {
            return Map.of();
        }
        Map<UUID, List<Tag>> own = new HashMap<>();
        List<UUID> jobUuids = jobs.stream().map(ClassMarketplaceJob::getUuid).toList();
        for (ClassMarketplaceJobRequiredSkill row : requiredSkillRepository.findByJobUuidInOrderByIdAsc(jobUuids)) {
            own.computeIfAbsent(row.getJobUuid(), key -> new ArrayList<>()).add(new Tag(row.getSkillUuid(),
                    row.getMinProficiency() == null ? ProficiencyLevel.BEGINNER : row.getMinProficiency(),
                    !Boolean.FALSE.equals(row.getIsMandatory())));
        }
        List<UUID> inheritingCourses = jobs.stream()
                .filter(job -> !own.containsKey(job.getUuid()))
                .map(ClassMarketplaceJob::getCourseUuid)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<UUID, List<CourseSkillLookupService.CourseSkillTag>> courseSkills = inheritingCourses.isEmpty()
                ? Map.of() : courseSkillLookupService.findSkillsByCourseUuids(inheritingCourses);

        Map<UUID, Effective> effective = new HashMap<>();
        for (ClassMarketplaceJob job : jobs) {
            List<Tag> tags = own.get(job.getUuid());
            if (tags != null) {
                effective.put(job.getUuid(), new Effective(false, null, List.copyOf(tags)));
                continue;
            }
            List<CourseSkillLookupService.CourseSkillTag> inherited =
                    job.getCourseUuid() == null ? null : courseSkills.get(job.getCourseUuid());
            if (inherited == null || inherited.isEmpty()) {
                effective.put(job.getUuid(), Effective.NONE);
                continue;
            }
            effective.put(job.getUuid(), new Effective(true, job.getCourseUuid(), inherited.stream()
                    .map(tag -> new Tag(tag.skillUuid(),
                            tag.level() == null ? ProficiencyLevel.BEGINNER : tag.level(), true))
                    .toList()));
        }
        return effective;
    }

    public Effective forJob(ClassMarketplaceJob job) {
        return forJobs(List.of(job)).getOrDefault(job.getUuid(), Effective.NONE);
    }
}
