package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.CourseSkillDTO;
import apps.sarafrika.elimika.course.dto.CourseSkillsUpdateRequest;
import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.model.CourseSkill;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.repository.CourseSkillRepository;
import apps.sarafrika.elimika.course.service.CourseService;
import apps.sarafrika.elimika.course.service.CourseSkillService;
import apps.sarafrika.elimika.course.spi.CourseSkillLookupService;
import apps.sarafrika.elimika.course.spi.CourseSkillsChangedEvent;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.utils.enums.ProficiencyLevel;
import apps.sarafrika.elimika.skills.spi.SkillLookupService;
import apps.sarafrika.elimika.skills.spi.SkillSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Course skill tags. Tagging is optional and never gates publishing; tags attach to the live
 * (root) course directly rather than through the shadow-draft review, because they describe the
 * course for matching and discovery and change nothing learners see in the content.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CourseSkillServiceImpl implements CourseSkillService, CourseSkillLookupService {

    static final int DEFAULT_WEIGHT = 1;

    private final CourseSkillRepository courseSkillRepository;
    private final CourseRepository courseRepository;
    private final CourseService courseService;
    private final SkillLookupService skillLookupService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public List<CourseSkillDTO> getCourseSkills(UUID courseUuid) {
        // Throws 404 for a course the caller may not read, exactly as GET /courses/{uuid} does.
        courseService.getVisibleCourseByUuid(courseUuid);
        return toDtos(courseSkillRepository.findByCourseUuidOrderByWeightDescIdAsc(courseUuid));
    }

    @Override
    public List<CourseSkillDTO> replaceCourseSkills(UUID courseUuid, CourseSkillsUpdateRequest request) {
        Course course = courseRepository.findByUuid(courseUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Course with UUID " + courseUuid + " not found"));
        if (course.getParentCourseUuid() != null) {
            throw new IllegalArgumentException("Skills are tagged on the live course, not on a pending draft; use course "
                    + course.getParentCourseUuid());
        }
        List<CourseSkillsUpdateRequest.Item> items = request.skills() == null ? List.of() : request.skills();
        Set<UUID> requested = new HashSet<>();
        for (CourseSkillsUpdateRequest.Item item : items) {
            if (!requested.add(item.skillUuid())) {
                throw new IllegalArgumentException("Skill " + item.skillUuid() + " is listed more than once");
            }
        }

        List<CourseSkill> existing = courseSkillRepository.findByCourseUuidOrderByWeightDescIdAsc(courseUuid);
        Map<UUID, CourseSkill> existingBySkill = existing.stream()
                .collect(Collectors.toMap(CourseSkill::getSkillUuid, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        validateSkills(requested, existingBySkill.keySet());

        List<CourseSkill> removed = existing.stream().filter(tag -> !requested.contains(tag.getSkillUuid())).toList();
        courseSkillRepository.deleteAll(removed);
        courseSkillRepository.flush();

        List<CourseSkill> kept = new ArrayList<>();
        for (CourseSkillsUpdateRequest.Item item : items) {
            ProficiencyLevel level = item.level() == null ? ProficiencyLevel.BEGINNER : item.level();
            int weight = item.weight() == null ? DEFAULT_WEIGHT : item.weight();
            CourseSkill tag = existingBySkill.get(item.skillUuid());
            if (tag == null) {
                tag = new CourseSkill(courseUuid, item.skillUuid(), level, weight);
            } else {
                tag.setLevel(level);
                tag.setWeight(weight);
            }
            kept.add(tag);
        }
        courseSkillRepository.saveAll(kept);
        eventPublisher.publishEvent(new CourseSkillsChangedEvent(courseUuid));
        return toDtos(courseSkillRepository.findByCourseUuidOrderByWeightDescIdAsc(courseUuid));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, List<CourseSkillTag>> findSkillsByCourseUuids(Collection<UUID> courseUuids) {
        if (courseUuids == null || courseUuids.isEmpty()) {
            return Map.of();
        }
        Map<UUID, List<CourseSkillTag>> tags = new LinkedHashMap<>();
        for (CourseSkill tag : courseSkillRepository.findByCourseUuidInOrderByWeightDescIdAsc(courseUuids)) {
            tags.computeIfAbsent(tag.getCourseUuid(), key -> new ArrayList<>())
                    .add(new CourseSkillTag(tag.getSkillUuid(), tag.getLevel(),
                            tag.getWeight() == null ? DEFAULT_WEIGHT : tag.getWeight()));
        }
        return tags;
    }

    /** Every skill must exist; a retired one may stay on the course but may not be newly added. */
    private void validateSkills(Set<UUID> requested, Set<UUID> alreadyTagged) {
        if (requested.isEmpty()) {
            return;
        }
        Map<UUID, SkillSummary> found = skillLookupService.findByUuids(requested).stream()
                .collect(Collectors.toMap(SkillSummary::uuid, Function.identity()));
        List<UUID> unknown = requested.stream().filter(uuid -> !found.containsKey(uuid)).toList();
        if (!unknown.isEmpty()) {
            throw new IllegalArgumentException("Unknown skill(s): " + unknown);
        }
        List<String> retired = requested.stream()
                .filter(uuid -> !found.get(uuid).active() && !alreadyTagged.contains(uuid))
                .map(uuid -> found.get(uuid).name())
                .toList();
        if (!retired.isEmpty()) {
            throw new IllegalArgumentException("Retired skill(s) cannot be added: " + retired);
        }
    }

    private List<CourseSkillDTO> toDtos(List<CourseSkill> tags) {
        if (tags.isEmpty()) {
            return List.of();
        }
        Map<UUID, SkillSummary> skills = skillLookupService.findByUuids(tags.stream().map(CourseSkill::getSkillUuid).toList())
                .stream()
                .collect(Collectors.toMap(SkillSummary::uuid, Function.identity()));
        List<CourseSkillDTO> dtos = new ArrayList<>(tags.size());
        for (CourseSkill tag : tags) {
            SkillSummary skill = skills.get(tag.getSkillUuid());
            if (skill == null) {
                continue;
            }
            dtos.add(new CourseSkillDTO(skill.uuid(), skill.name(), skill.slug(), tag.getLevel(),
                    tag.getWeight() == null ? DEFAULT_WEIGHT : tag.getWeight(), skill.active()));
        }
        return dtos;
    }
}
