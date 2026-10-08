package apps.sarafrika.elimika.course.internal.agegroup;

import apps.sarafrika.elimika.course.dto.AgeGroupDTO;
import apps.sarafrika.elimika.course.dto.AgeGroupRequest;
import apps.sarafrika.elimika.course.dto.LessonHoursDTO;
import apps.sarafrika.elimika.course.dto.LessonHoursRequest;
import apps.sarafrika.elimika.course.model.AgeGroup;
import apps.sarafrika.elimika.course.model.AgeGroupLessonHours;
import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.model.Lesson;
import apps.sarafrika.elimika.course.model.ProgramCourse;
import apps.sarafrika.elimika.course.repository.AgeGroupLessonHoursRepository;
import apps.sarafrika.elimika.course.repository.AgeGroupRepository;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.repository.LessonRepository;
import apps.sarafrika.elimika.course.repository.ProgramCourseRepository;
import apps.sarafrika.elimika.course.util.enums.AgeGroupOwnerType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Age groups with a lesson plan: named, non-overlapping age bands inside a course's (or program's)
 * age range, each with hours for every active lesson. Owner-agnostic, so jobs and classes can reuse it.
 */
@Component
@RequiredArgsConstructor
public class AgeGroups {

    static final int MAX_GROUPS = 10;

    private final AgeGroupRepository groupRepository;
    private final AgeGroupLessonHoursRepository hoursRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final ProgramCourseRepository programCourseRepository;

    /** The ages and lessons a lesson plan must fit; a null bound is open. */
    public record AgeGroupScope(Integer minAge, Integer maxAge, List<UUID> lessonUuids, String label) {
    }

    public AgeGroupScope forCourse(Course course) {
        return new AgeGroupScope(course.getAgeLowerLimit(), course.getAgeUpperLimit(),
                activeLessons(List.of(course.getUuid())), "this course");
    }

    /** A learner takes every course of a program, so its age range is where all the courses' ranges overlap. */
    public AgeGroupScope forProgram(UUID programUuid) {
        List<UUID> courseUuids = programCourseRepository.findByProgramUuidOrderBySequenceOrderAsc(programUuid).stream()
                .map(ProgramCourse::getCourseUuid)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Integer lower = null;
        Integer upper = null;
        for (Course course : courseRepository.findByUuidIn(courseUuids)) {
            if (course.getAgeLowerLimit() != null) {
                lower = lower == null ? course.getAgeLowerLimit() : Math.max(lower, course.getAgeLowerLimit());
            }
            if (course.getAgeUpperLimit() != null) {
                upper = upper == null ? course.getAgeUpperLimit() : Math.min(upper, course.getAgeUpperLimit());
            }
        }
        return new AgeGroupScope(lower, upper, activeLessons(courseUuids), "this program");
    }

    /** The whole list is checked against {@code scope}; an empty list is the caller's policy to judge. */
    public void validate(AgeGroupScope scope, List<AgeGroupRequest> groups) {
        if (groups == null || groups.isEmpty()) {
            return;
        }
        if (groups.size() > MAX_GROUPS) {
            throw new IllegalArgumentException(String.format("At most %d age groups are allowed", MAX_GROUPS));
        }
        if (scope.minAge() != null && scope.maxAge() != null && scope.minAge() > scope.maxAge()) {
            throw new IllegalArgumentException(String.format(
                    "The courses in %s share no common age range, so no age group can fit", scope.label()));
        }
        Set<String> names = new HashSet<>();
        for (AgeGroupRequest group : groups) {
            validateGroup(group, scope, names);
        }
        List<AgeGroupRequest> byAge = groups.stream().sorted(Comparator.comparing(AgeGroupRequest::minAge)).toList();
        for (int i = 1; i < byAge.size(); i++) {
            AgeGroupRequest previous = byAge.get(i - 1);
            AgeGroupRequest current = byAge.get(i);
            if (current.minAge() <= previous.maxAge()) {
                throw new IllegalArgumentException(String.format(
                        "Age groups '%s' and '%s' overlap; groups may leave gaps but not overlap",
                        previous.name().trim(), current.name().trim()));
            }
        }
    }

    private void validateGroup(AgeGroupRequest group, AgeGroupScope scope, Set<String> names) {
        if (group == null || group.name() == null || group.name().isBlank()
                || group.minAge() == null || group.maxAge() == null || group.lessonHours() == null) {
            throw new IllegalArgumentException("Each age group needs a name, min_age, max_age and lesson_hours");
        }
        String name = group.name().trim();
        if (!names.add(name.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException(String.format("Age group name '%s' is used more than once", name));
        }
        if (group.minAge() > group.maxAge()) {
            throw new IllegalArgumentException(String.format("Age group '%s' has min_age above max_age", name));
        }
        if ((scope.minAge() != null && group.minAge() < scope.minAge())
                || (scope.maxAge() != null && group.maxAge() > scope.maxAge())) {
            throw new IllegalArgumentException(String.format(
                    "Age group '%s' must stay within the ages of %s (%s to %s)", name, scope.label(),
                    scope.minAge() == null ? "any" : scope.minAge(), scope.maxAge() == null ? "any" : scope.maxAge()));
        }
        Set<UUID> allowed = new HashSet<>(scope.lessonUuids());
        Set<UUID> covered = new HashSet<>();
        for (LessonHoursRequest hours : group.lessonHours()) {
            if (hours == null || hours.lessonUuid() == null || hours.hours() == null) {
                throw new IllegalArgumentException(String.format(
                        "Every lesson in age group '%s' needs a lesson_uuid and hours", name));
            }
            if (!allowed.contains(hours.lessonUuid())) {
                throw new IllegalArgumentException(String.format(
                        "Lesson %s is not an active lesson of %s", hours.lessonUuid(), scope.label()));
            }
            if (!covered.add(hours.lessonUuid())) {
                throw new IllegalArgumentException(String.format(
                        "Lesson %s appears more than once in age group '%s'", hours.lessonUuid(), name));
            }
            if (hours.hours().signum() <= 0 || hours.hours().compareTo(BigDecimal.valueOf(24)) > 0) {
                throw new IllegalArgumentException(String.format(
                        "Hours in age group '%s' must be above zero and at most 24", name));
            }
        }
        if (!covered.containsAll(allowed)) {
            throw new IllegalArgumentException(String.format(
                    "Age group '%s' needs hours for every lesson of %s", name, scope.label()));
        }
    }

    /** Replaces the owner's groups and their lesson hours; null leaves them as they are. */
    public void replace(AgeGroupOwnerType ownerType, UUID ownerUuid, List<AgeGroupRequest> groups) {
        if (groups == null) {
            return;
        }
        groupRepository.deleteForOwner(ownerType, ownerUuid);
        if (groups.isEmpty()) {
            return;
        }
        List<AgeGroup> rows = new ArrayList<>();
        for (int i = 0; i < groups.size(); i++) {
            AgeGroupRequest request = groups.get(i);
            AgeGroup row = new AgeGroup();
            row.setOwnerType(ownerType);
            row.setOwnerUuid(ownerUuid);
            row.setName(request.name().trim());
            row.setMinAge(request.minAge());
            row.setMaxAge(request.maxAge());
            row.setPosition(i);
            rows.add(row);
        }
        List<AgeGroup> saved = groupRepository.saveAllAndFlush(rows);

        List<AgeGroupLessonHours> hours = new ArrayList<>();
        for (int i = 0; i < saved.size(); i++) {
            UUID groupUuid = saved.get(i).getUuid();
            for (LessonHoursRequest request : groups.get(i).lessonHours()) {
                AgeGroupLessonHours row = new AgeGroupLessonHours();
                row.setAgeGroupUuid(groupUuid);
                row.setLessonUuid(request.lessonUuid());
                row.setHours(request.hours());
                hours.add(row);
            }
        }
        hoursRepository.saveAll(hours);
    }

    public void deleteFor(AgeGroupOwnerType ownerType, UUID ownerUuid) {
        groupRepository.deleteForOwner(ownerType, ownerUuid);
    }

    /** Groups per owner in the owner's order, each lesson plan in the order it was sent. */
    public Map<UUID, List<AgeGroupDTO>> groupsFor(AgeGroupOwnerType ownerType, Collection<UUID> ownerUuids) {
        List<AgeGroup> groups = groupRepository.findByOwnerTypeAndOwnerUuidInOrderByPositionAscIdAsc(ownerType, ownerUuids);
        if (groups.isEmpty()) {
            return Map.of();
        }
        Map<UUID, List<AgeGroupLessonHours>> hoursByGroup = hoursRepository
                .findByAgeGroupUuidIn(groups.stream().map(AgeGroup::getUuid).toList()).stream()
                .sorted(Comparator.comparing(AgeGroupLessonHours::getId))
                .collect(Collectors.groupingBy(AgeGroupLessonHours::getAgeGroupUuid));
        Set<UUID> lessonUuids = hoursByGroup.values().stream()
                .flatMap(List::stream)
                .map(AgeGroupLessonHours::getLessonUuid)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<UUID, Lesson> lessons = lessonUuids.isEmpty() ? Map.of() : lessonRepository.findByUuidIn(lessonUuids).stream()
                .collect(Collectors.toMap(Lesson::getUuid, Function.identity(), (first, second) -> first));

        Map<UUID, List<AgeGroupDTO>> result = new HashMap<>();
        for (AgeGroup group : groups) {
            List<LessonHoursDTO> plan = hoursByGroup.getOrDefault(group.getUuid(), List.of()).stream()
                    .map(row -> {
                        Lesson lesson = lessons.get(row.getLessonUuid());
                        return new LessonHoursDTO(row.getLessonUuid(), lesson == null ? null : lesson.getCourseUuid(),
                                lesson == null ? null : lesson.getTitle(), lesson == null ? null : lesson.getLessonNumber(),
                                row.getHours());
                    })
                    .toList();
            BigDecimal total = plan.stream().map(LessonHoursDTO::hours).reduce(BigDecimal.ZERO, BigDecimal::add);
            result.computeIfAbsent(group.getOwnerUuid(), uuid -> new ArrayList<>()).add(new AgeGroupDTO(
                    group.getUuid(), group.getName(), group.getMinAge(), group.getMaxAge(), total, plan));
        }
        return result;
    }

    private List<UUID> activeLessons(List<UUID> courseUuids) {
        List<UUID> lessons = new ArrayList<>();
        for (UUID courseUuid : courseUuids) {
            lessonRepository.findByCourseUuidOrderByLessonNumberAsc(courseUuid).stream()
                    .filter(lesson -> Boolean.TRUE.equals(lesson.getActive()))
                    .map(Lesson::getUuid)
                    .forEach(lessons::add);
        }
        return lessons;
    }
}
