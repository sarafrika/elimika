package apps.sarafrika.elimika.course.internal.training;

import apps.sarafrika.elimika.course.dto.LearnerGroupDTO;
import apps.sarafrika.elimika.course.dto.LearnerGroupRequest;
import apps.sarafrika.elimika.course.dto.LessonHoursDTO;
import apps.sarafrika.elimika.course.dto.LessonHoursRequest;
import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.model.Lesson;
import apps.sarafrika.elimika.course.model.ProgramCourse;
import apps.sarafrika.elimika.course.model.TrainingApplicationLearnerGroup;
import apps.sarafrika.elimika.course.model.TrainingApplicationLessonHours;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.repository.LessonRepository;
import apps.sarafrika.elimika.course.repository.ProgramCourseRepository;
import apps.sarafrika.elimika.course.repository.TrainingApplicationLearnerGroupRepository;
import apps.sarafrika.elimika.course.repository.TrainingApplicationLessonHoursRepository;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicantType;
import apps.sarafrika.elimika.course.util.enums.TrainingApplicationType;
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
 * An instructor applicant's learner groups: named, non-overlapping age bands inside the course's (or
 * program's) age range, each with hours for every active lesson. Organisations never send them.
 */
@Component
@RequiredArgsConstructor
public class TrainingApplicationLearnerGroups {

    static final int MAX_GROUPS = 10;

    private final TrainingApplicationLearnerGroupRepository groupRepository;
    private final TrainingApplicationLessonHoursRepository hoursRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final ProgramCourseRepository programCourseRepository;

    /** The ages and lessons a lesson plan must fit; a null bound is open. */
    public record LessonPlanScope(Integer minAge, Integer maxAge, List<UUID> lessonUuids, String label) {
    }

    public LessonPlanScope forCourse(Course course) {
        return new LessonPlanScope(course.getAgeLowerLimit(), course.getAgeUpperLimit(),
                activeLessons(List.of(course.getUuid())), "this course");
    }

    /** A learner takes every course of a program, so its age range is where all the courses' ranges overlap. */
    public LessonPlanScope forProgram(UUID programUuid) {
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
        return new LessonPlanScope(lower, upper, activeLessons(courseUuids), "this program");
    }

    /** Null leaves stored groups alone; otherwise the whole list is checked against {@code scope}. */
    public void validate(CourseTrainingApplicantType applicantType, LessonPlanScope scope, List<LearnerGroupRequest> groups) {
        if (groups == null) {
            return;
        }
        if (!CourseTrainingApplicantType.INSTRUCTOR.equals(applicantType)) {
            if (!groups.isEmpty()) {
                throw new IllegalArgumentException("Only instructor applicants can define learner groups");
            }
            return;
        }
        if (groups.isEmpty()) {
            throw new IllegalArgumentException("At least one learner group is required");
        }
        if (groups.size() > MAX_GROUPS) {
            throw new IllegalArgumentException(String.format("At most %d learner groups are allowed", MAX_GROUPS));
        }
        if (scope.minAge() != null && scope.maxAge() != null && scope.minAge() > scope.maxAge()) {
            throw new IllegalArgumentException(String.format(
                    "The courses in %s share no common age range, so no learner group can fit", scope.label()));
        }
        Set<String> names = new HashSet<>();
        for (LearnerGroupRequest group : groups) {
            validateGroup(group, scope, names);
        }
        List<LearnerGroupRequest> byAge = groups.stream().sorted(Comparator.comparing(LearnerGroupRequest::minAge)).toList();
        for (int i = 1; i < byAge.size(); i++) {
            LearnerGroupRequest previous = byAge.get(i - 1);
            LearnerGroupRequest current = byAge.get(i);
            if (current.minAge() <= previous.maxAge()) {
                throw new IllegalArgumentException(String.format(
                        "Learner groups '%s' and '%s' overlap; groups may leave gaps but not overlap",
                        previous.name().trim(), current.name().trim()));
            }
        }
    }

    private void validateGroup(LearnerGroupRequest group, LessonPlanScope scope, Set<String> names) {
        if (group == null || group.name() == null || group.name().isBlank()
                || group.minAge() == null || group.maxAge() == null || group.lessonHours() == null) {
            throw new IllegalArgumentException("Each learner group needs a name, min_age, max_age and lesson_hours");
        }
        String name = group.name().trim();
        if (!names.add(name.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException(String.format("Learner group name '%s' is used more than once", name));
        }
        if (group.minAge() > group.maxAge()) {
            throw new IllegalArgumentException(String.format("Learner group '%s' has min_age above max_age", name));
        }
        if ((scope.minAge() != null && group.minAge() < scope.minAge())
                || (scope.maxAge() != null && group.maxAge() > scope.maxAge())) {
            throw new IllegalArgumentException(String.format(
                    "Learner group '%s' must stay within the ages of %s (%s to %s)", name, scope.label(),
                    scope.minAge() == null ? "any" : scope.minAge(), scope.maxAge() == null ? "any" : scope.maxAge()));
        }
        Set<UUID> allowed = new HashSet<>(scope.lessonUuids());
        Set<UUID> covered = new HashSet<>();
        for (LessonHoursRequest hours : group.lessonHours()) {
            if (hours == null || hours.lessonUuid() == null || hours.hours() == null) {
                throw new IllegalArgumentException(String.format(
                        "Every lesson in learner group '%s' needs a lesson_uuid and hours", name));
            }
            if (!allowed.contains(hours.lessonUuid())) {
                throw new IllegalArgumentException(String.format(
                        "Lesson %s is not an active lesson of %s", hours.lessonUuid(), scope.label()));
            }
            if (!covered.add(hours.lessonUuid())) {
                throw new IllegalArgumentException(String.format(
                        "Lesson %s appears more than once in learner group '%s'", hours.lessonUuid(), name));
            }
            if (hours.hours().signum() <= 0 || hours.hours().compareTo(BigDecimal.valueOf(24)) > 0) {
                throw new IllegalArgumentException(String.format(
                        "Hours in learner group '%s' must be above zero and at most 24", name));
            }
        }
        if (!covered.containsAll(allowed)) {
            throw new IllegalArgumentException(String.format(
                    "Learner group '%s' needs hours for every lesson of %s", name, scope.label()));
        }
    }

    /** Replaces the stored groups and their lesson hours; null leaves them as they are. */
    public void replace(TrainingApplicationType type, UUID applicationUuid, List<LearnerGroupRequest> groups) {
        if (groups == null) {
            return;
        }
        groupRepository.deleteForApplication(type, applicationUuid);
        if (groups.isEmpty()) {
            return;
        }
        List<TrainingApplicationLearnerGroup> rows = new ArrayList<>();
        for (int i = 0; i < groups.size(); i++) {
            LearnerGroupRequest request = groups.get(i);
            TrainingApplicationLearnerGroup row = new TrainingApplicationLearnerGroup();
            row.setApplicationType(type);
            row.setApplicationUuid(applicationUuid);
            row.setName(request.name().trim());
            row.setMinAge(request.minAge());
            row.setMaxAge(request.maxAge());
            row.setPosition(i);
            rows.add(row);
        }
        List<TrainingApplicationLearnerGroup> saved = groupRepository.saveAllAndFlush(rows);

        List<TrainingApplicationLessonHours> hours = new ArrayList<>();
        for (int i = 0; i < saved.size(); i++) {
            UUID groupUuid = saved.get(i).getUuid();
            for (LessonHoursRequest request : groups.get(i).lessonHours()) {
                TrainingApplicationLessonHours row = new TrainingApplicationLessonHours();
                row.setLearnerGroupUuid(groupUuid);
                row.setLessonUuid(request.lessonUuid());
                row.setHours(request.hours());
                hours.add(row);
            }
        }
        hoursRepository.saveAll(hours);
    }

    public void deleteFor(TrainingApplicationType type, UUID applicationUuid) {
        groupRepository.deleteForApplication(type, applicationUuid);
    }

    /** Groups per application in the applicant's order, each lesson plan in the order it was sent. */
    public Map<UUID, List<LearnerGroupDTO>> groups(TrainingApplicationType type, Collection<UUID> applicationUuids) {
        List<TrainingApplicationLearnerGroup> groups =
                groupRepository.findByApplicationTypeAndApplicationUuidInOrderByPositionAscIdAsc(type, applicationUuids);
        if (groups.isEmpty()) {
            return Map.of();
        }
        Map<UUID, List<TrainingApplicationLessonHours>> hoursByGroup = hoursRepository
                .findByLearnerGroupUuidIn(groups.stream().map(TrainingApplicationLearnerGroup::getUuid).toList()).stream()
                .sorted(Comparator.comparing(TrainingApplicationLessonHours::getId))
                .collect(Collectors.groupingBy(TrainingApplicationLessonHours::getLearnerGroupUuid));
        Set<UUID> lessonUuids = hoursByGroup.values().stream()
                .flatMap(List::stream)
                .map(TrainingApplicationLessonHours::getLessonUuid)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<UUID, Lesson> lessons = lessonUuids.isEmpty() ? Map.of() : lessonRepository.findByUuidIn(lessonUuids).stream()
                .collect(Collectors.toMap(Lesson::getUuid, Function.identity(), (first, second) -> first));

        Map<UUID, List<LearnerGroupDTO>> result = new HashMap<>();
        for (TrainingApplicationLearnerGroup group : groups) {
            List<LessonHoursDTO> plan = hoursByGroup.getOrDefault(group.getUuid(), List.of()).stream()
                    .map(row -> {
                        Lesson lesson = lessons.get(row.getLessonUuid());
                        return new LessonHoursDTO(row.getLessonUuid(), lesson == null ? null : lesson.getCourseUuid(),
                                lesson == null ? null : lesson.getTitle(), lesson == null ? null : lesson.getLessonNumber(),
                                row.getHours());
                    })
                    .toList();
            BigDecimal total = plan.stream().map(LessonHoursDTO::hours).reduce(BigDecimal.ZERO, BigDecimal::add);
            result.computeIfAbsent(group.getApplicationUuid(), uuid -> new ArrayList<>()).add(new LearnerGroupDTO(
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
