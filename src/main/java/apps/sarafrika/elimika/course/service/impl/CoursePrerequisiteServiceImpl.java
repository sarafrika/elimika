package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.CoursePrerequisiteDTO;
import apps.sarafrika.elimika.course.dto.CoursePrerequisitesRequest;
import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.model.CoursePrerequisite;
import apps.sarafrika.elimika.course.repository.CoursePrerequisiteRepository;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.service.CourseDraftService;
import apps.sarafrika.elimika.course.service.CoursePendingEditService;
import apps.sarafrika.elimika.course.service.CoursePrerequisiteService;
import apps.sarafrika.elimika.course.service.CourseService;
import apps.sarafrika.elimika.course.util.enums.ContentStatus;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Prerequisites follow the draft-over-live model: a write to a live, approved course lands on its
 * shadow draft (opened on demand) and is promoted with the rest of the edit. The draft's rows point at
 * the draft course; promotion reconciles them onto the live course by prerequisite uuid.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CoursePrerequisiteServiceImpl implements CoursePrerequisiteService {

    private final CoursePrerequisiteRepository prerequisiteRepository;
    private final CourseRepository courseRepository;
    private final CourseService courseService;
    private final CourseDraftService courseDraftService;
    private final CoursePendingEditService coursePendingEditService;

    @Override
    @Transactional(readOnly = true)
    public List<CoursePrerequisiteDTO> getPrerequisites(UUID courseUuid) {
        // Same visibility as GET /courses/{uuid}: not found for a caller who may not read the course.
        courseService.getVisibleCourseByUuid(courseUuid);
        return toDtos(prerequisiteRepository.findByCourseUuidOrderByIdAsc(courseUuid));
    }

    @Override
    public List<CoursePrerequisiteDTO> replacePrerequisites(UUID courseUuid, CoursePrerequisitesRequest request) {
        Course course = findCourse(courseUuid);
        // Writing through a draft's own uuid still validates against the live course it shadows.
        UUID liveCourseUuid = course.getParentCourseUuid() != null ? course.getParentCourseUuid() : courseUuid;
        Map<UUID, Boolean> desired = validate(liveCourseUuid, course.getCourseCreatorUuid(), request);

        UUID targetCourseUuid = courseDraftService.resolveEditableCourseUuid(courseUuid);
        boolean changed = apply(targetCourseUuid, desired);

        if (changed && !targetCourseUuid.equals(courseUuid) && course.getParentCourseUuid() == null) {
            coursePendingEditService.submitOrRefresh(liveCourseUuid, targetCourseUuid);
            log.info("Prerequisite change to live course {} routed to draft {} for review",
                    liveCourseUuid, targetCourseUuid);
        }
        return toDtos(prerequisiteRepository.findByCourseUuidOrderByIdAsc(targetCourseUuid));
    }

    private Map<UUID, Boolean> validate(UUID liveCourseUuid, UUID authorCreatorUuid, CoursePrerequisitesRequest request) {
        Map<UUID, Boolean> desired = new LinkedHashMap<>();
        for (CoursePrerequisitesRequest.Item item : request.prerequisites()) {
            UUID prerequisiteUuid = item.prerequisiteCourseUuid();
            if (liveCourseUuid.equals(prerequisiteUuid)) {
                throw new IllegalArgumentException("A course cannot be its own prerequisite");
            }
            if (desired.put(prerequisiteUuid, item.mandatory()) != null) {
                throw new IllegalArgumentException("Prerequisite " + prerequisiteUuid + " is listed more than once");
            }
        }
        if (desired.isEmpty()) {
            return desired;
        }

        Map<UUID, Course> prerequisites = courseRepository.findByUuidIn(List.copyOf(desired.keySet())).stream()
                .collect(Collectors.toMap(Course::getUuid, Function.identity()));
        for (UUID prerequisiteUuid : desired.keySet()) {
            Course prerequisite = prerequisites.get(prerequisiteUuid);
            if (prerequisite == null || prerequisite.getParentCourseUuid() != null) {
                throw new IllegalArgumentException("Prerequisite course " + prerequisiteUuid + " does not exist");
            }
            boolean published = prerequisite.getStatus() == ContentStatus.PUBLISHED;
            boolean sameAuthor = authorCreatorUuid != null
                    && authorCreatorUuid.equals(prerequisite.getCourseCreatorUuid());
            if (!published && !sameAuthor) {
                throw new IllegalArgumentException("Prerequisite course " + prerequisiteUuid
                        + " must be published or one of your own courses");
            }
        }

        if (prerequisiteRepository.reaches(desired.keySet(), liveCourseUuid)) {
            throw new IllegalArgumentException(
                    "These prerequisites would create a cycle: a chosen course already requires this course");
        }
        return desired;
    }

    /**
     * Reconciles the course's rows onto {@code desired}: kept pairs keep their uuid, changed flags are
     * updated, removed pairs are deleted (they carry no learner data) and new ones inserted.
     *
     * @return whether anything changed
     */
    private boolean apply(UUID courseUuid, Map<UUID, Boolean> desired) {
        boolean changed = false;
        Map<UUID, CoursePrerequisite> current = prerequisiteRepository.findByCourseUuidOrderByIdAsc(courseUuid).stream()
                .collect(Collectors.toMap(CoursePrerequisite::getPrerequisiteCourseUuid, Function.identity(),
                        (a, b) -> a, LinkedHashMap::new));

        for (CoursePrerequisite row : current.values()) {
            if (!desired.containsKey(row.getPrerequisiteCourseUuid())) {
                prerequisiteRepository.delete(row);
                changed = true;
            }
        }
        // Deletes first, so a re-added pair never meets its old row on the unique constraint.
        prerequisiteRepository.flush();

        for (Map.Entry<UUID, Boolean> entry : desired.entrySet()) {
            CoursePrerequisite row = current.get(entry.getKey());
            if (row == null) {
                prerequisiteRepository.save(new CoursePrerequisite(courseUuid, entry.getKey(), entry.getValue()));
                changed = true;
            } else if (!Objects.equals(row.getIsMandatory(), entry.getValue())) {
                row.setIsMandatory(entry.getValue());
                prerequisiteRepository.save(row);
                changed = true;
            }
        }
        return changed;
    }

    private List<CoursePrerequisiteDTO> toDtos(List<CoursePrerequisite> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<UUID, String> names = courseRepository.findByUuidIn(
                        rows.stream().map(CoursePrerequisite::getPrerequisiteCourseUuid).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Course::getUuid, c -> c.getName() == null ? "" : c.getName()));
        return rows.stream()
                .map(row -> new CoursePrerequisiteDTO(
                        row.getUuid(),
                        row.getCourseUuid(),
                        row.getPrerequisiteCourseUuid(),
                        names.get(row.getPrerequisiteCourseUuid()),
                        Boolean.TRUE.equals(row.getIsMandatory())))
                .toList();
    }

    private Course findCourse(UUID courseUuid) {
        return courseRepository.findByUuid(courseUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with UUID: " + courseUuid));
    }
}
