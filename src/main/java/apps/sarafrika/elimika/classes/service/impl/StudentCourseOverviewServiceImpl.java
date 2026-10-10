package apps.sarafrika.elimika.classes.service.impl;

import apps.sarafrika.elimika.classes.dto.StudentCourseOverviewItemDTO;
import apps.sarafrika.elimika.classes.dto.StudentCourseOverviewSessionDTO;
import apps.sarafrika.elimika.classes.model.ClassAssignmentSchedule;
import apps.sarafrika.elimika.classes.model.ClassDefinition;
import apps.sarafrika.elimika.classes.model.ClassQuizSchedule;
import apps.sarafrika.elimika.classes.repository.ClassAssignmentScheduleRepository;
import apps.sarafrika.elimika.classes.repository.ClassDefinitionRepository;
import apps.sarafrika.elimika.classes.repository.ClassQuizScheduleRepository;
import apps.sarafrika.elimika.classes.service.StudentCourseOverviewService;
import apps.sarafrika.elimika.course.spi.CourseInfoService;
import apps.sarafrika.elimika.course.spi.LearnerCourseProgressView;
import apps.sarafrika.elimika.course.spi.LearnerProgressLookupService;
import apps.sarafrika.elimika.instructor.spi.InstructorDirectoryEntry;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.timetabling.spi.ScheduledInstanceDTO;
import apps.sarafrika.elimika.timetabling.spi.StudentClassEnrollmentSummaryDTO;
import apps.sarafrika.elimika.timetabling.spi.TimetableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StudentCourseOverviewServiceImpl implements StudentCourseOverviewService {

    static final int MAX_PAGE_SIZE = 50;

    private final TimetableService timetableService;
    private final ClassDefinitionRepository classDefinitionRepository;
    private final ClassAssignmentScheduleRepository assignmentScheduleRepository;
    private final ClassQuizScheduleRepository quizScheduleRepository;
    private final CourseInfoService courseInfoService;
    private final LearnerProgressLookupService learnerProgressLookupService;
    private final InstructorLookupService instructorLookupService;
    private final Clock clock = Clock.systemUTC();

    @Override
    public Page<StudentCourseOverviewItemDTO> getCourseOverview(UUID studentUuid, Pageable pageable) {
        if (studentUuid == null) {
            throw new IllegalArgumentException("Student UUID cannot be null");
        }
        Pageable page = capPageSize(pageable);
        Page<StudentClassEnrollmentSummaryDTO> classes = timetableService.getClassEnrollmentsForStudent(studentUuid, page);
        if (classes.isEmpty()) {
            return Page.empty(page);
        }

        List<UUID> classUuids = classes.getContent().stream()
                .map(StudentClassEnrollmentSummaryDTO::class_definition_uuid)
                .toList();
        Map<UUID, ClassDefinition> definitions = classDefinitionRepository.findByUuidIn(classUuids).stream()
                .collect(Collectors.toMap(ClassDefinition::getUuid, Function.identity(), (a, b) -> a));
        Set<UUID> courseUuids = collect(definitions.values(), ClassDefinition::getCourseUuid);

        Map<UUID, String> courseNames = courseInfoService.getCourseNames(courseUuids);
        Map<UUID, LearnerCourseProgressView> progress =
                learnerProgressLookupService.findCourseProgressByCourse(studentUuid, courseUuids);
        Map<UUID, ScheduledInstanceDTO> nextSessions = timetableService.getNextSessionsForStudent(studentUuid, classUuids);

        Map<UUID, UUID> instructorByClass = new HashMap<>();
        for (UUID classUuid : classUuids) {
            ClassDefinition definition = definitions.get(classUuid);
            ScheduledInstanceDTO next = nextSessions.get(classUuid);
            UUID instructor = definition != null && definition.getDefaultInstructorUuid() != null
                    ? definition.getDefaultInstructorUuid()
                    : next != null ? next.instructorUuid() : null;
            if (instructor != null) {
                instructorByClass.put(classUuid, instructor);
            }
        }
        Map<UUID, InstructorDirectoryEntry> instructors =
                instructorLookupService.findInstructorDirectoryEntries(new HashSet<>(instructorByClass.values()));

        LocalDateTime now = LocalDateTime.now(clock);
        Map<UUID, Set<UUID>> assignmentsByClass = releasedByClass(
                assignmentScheduleRepository.findByClassDefinitionUuidIn(classUuids), now,
                ClassAssignmentSchedule::getClassDefinitionUuid, ClassAssignmentSchedule::getAssignmentUuid,
                ClassAssignmentSchedule::getVisibleAt);
        Map<UUID, Set<UUID>> quizzesByClass = releasedByClass(
                quizScheduleRepository.findByClassDefinitionUuidIn(classUuids), now,
                ClassQuizSchedule::getClassDefinitionUuid, ClassQuizSchedule::getQuizUuid,
                ClassQuizSchedule::getVisibleAt);
        Set<UUID> submittedAssignments = learnerProgressLookupService.findSubmittedAssignmentUuids(
                studentUuid, flatten(assignmentsByClass));
        Set<UUID> submittedQuizzes = learnerProgressLookupService.findSubmittedQuizUuids(
                studentUuid, flatten(quizzesByClass));

        List<StudentCourseOverviewItemDTO> items = classes.getContent().stream().map(summary -> {
            UUID classUuid = summary.class_definition_uuid();
            ClassDefinition definition = definitions.get(classUuid);
            UUID courseUuid = definition != null ? definition.getCourseUuid() : null;
            LearnerCourseProgressView courseProgress = courseUuid != null ? progress.get(courseUuid) : null;
            UUID instructorUuid = instructorByClass.get(classUuid);
            InstructorDirectoryEntry instructor = instructorUuid != null ? instructors.get(instructorUuid) : null;
            return new StudentCourseOverviewItemDTO(
                    classUuid,
                    definition != null ? definition.getTitle() : summary.class_title(),
                    definition != null ? definition.getThumbnailUrl() : null,
                    definition != null ? definition.getOrganisationUuid() : null,
                    summary.latest_enrollment_uuid(),
                    summary.latest_enrollment_status(),
                    summary.scheduled_instance_count(),
                    courseUuid,
                    courseUuid != null ? courseNames.get(courseUuid) : null,
                    definition != null ? definition.getProgramUuid() : null,
                    instructorUuid,
                    instructor != null ? instructor.displayName() : null,
                    toSession(nextSessions.get(classUuid)),
                    courseProgress != null ? courseProgress.enrollmentUuid() : null,
                    courseProgress != null ? courseProgress.status() : null,
                    courseProgress != null ? courseProgress.progressPercentage() : null,
                    countPending(assignmentsByClass.get(classUuid), submittedAssignments),
                    countPending(quizzesByClass.get(classUuid), submittedQuizzes),
                    summary.latest_activity_date());
        }).toList();
        return new PageImpl<>(items, page, classes.getTotalElements());
    }

    private static Pageable capPageSize(Pageable pageable) {
        if (pageable == null || pageable.isUnpaged()) {
            return PageRequest.of(0, MAX_PAGE_SIZE);
        }
        if (pageable.getPageSize() <= MAX_PAGE_SIZE) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), MAX_PAGE_SIZE, pageable.getSort());
    }

    /**
     * Assessment UUIDs per class, keeping only schedules already released to learners.
     */
    private static <T> Map<UUID, Set<UUID>> releasedByClass(List<T> schedules,
                                                            LocalDateTime now,
                                                            Function<T, UUID> classOf,
                                                            Function<T, UUID> assessmentOf,
                                                            Function<T, LocalDateTime> visibleAtOf) {
        Map<UUID, Set<UUID>> byClass = new HashMap<>();
        for (T schedule : schedules) {
            UUID assessment = assessmentOf.apply(schedule);
            LocalDateTime visibleAt = visibleAtOf.apply(schedule);
            if (assessment == null || (visibleAt != null && visibleAt.isAfter(now))) {
                continue;
            }
            byClass.computeIfAbsent(classOf.apply(schedule), key -> new HashSet<>()).add(assessment);
        }
        return byClass;
    }

    private static Set<UUID> flatten(Map<UUID, Set<UUID>> byClass) {
        return byClass.values().stream().flatMap(Set::stream).collect(Collectors.toSet());
    }

    private static int countPending(Set<UUID> scheduled, Set<UUID> submitted) {
        if (scheduled == null) {
            return 0;
        }
        return (int) scheduled.stream().filter(uuid -> !submitted.contains(uuid)).count();
    }

    private static <T> Set<UUID> collect(Collection<T> items, Function<T, UUID> extractor) {
        return items.stream().map(extractor).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private static StudentCourseOverviewSessionDTO toSession(ScheduledInstanceDTO instance) {
        if (instance == null) {
            return null;
        }
        return new StudentCourseOverviewSessionDTO(
                instance.uuid(),
                instance.title(),
                instance.startTime(),
                instance.endTime(),
                instance.timezone(),
                instance.locationType(),
                instance.locationName(),
                instance.instructorUuid());
    }
}
