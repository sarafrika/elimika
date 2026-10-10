package apps.sarafrika.elimika.classes.service;

import apps.sarafrika.elimika.classes.dto.StudentCourseOverviewItemDTO;
import apps.sarafrika.elimika.classes.model.ClassAssignmentSchedule;
import apps.sarafrika.elimika.classes.model.ClassDefinition;
import apps.sarafrika.elimika.classes.model.ClassQuizSchedule;
import apps.sarafrika.elimika.classes.repository.ClassAssignmentScheduleRepository;
import apps.sarafrika.elimika.classes.repository.ClassDefinitionRepository;
import apps.sarafrika.elimika.classes.repository.ClassQuizScheduleRepository;
import apps.sarafrika.elimika.classes.service.impl.StudentCourseOverviewServiceImpl;
import apps.sarafrika.elimika.course.spi.CourseInfoService;
import apps.sarafrika.elimika.course.spi.LearnerCourseProgressView;
import apps.sarafrika.elimika.course.spi.LearnerProgressLookupService;
import apps.sarafrika.elimika.instructor.spi.InstructorDirectoryEntry;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.timetabling.spi.EnrollmentStatus;
import apps.sarafrika.elimika.timetabling.spi.ScheduledInstanceDTO;
import apps.sarafrika.elimika.timetabling.spi.SchedulingStatus;
import apps.sarafrika.elimika.timetabling.spi.StudentClassEnrollmentSummaryDTO;
import apps.sarafrika.elimika.timetabling.spi.TimetableService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentCourseOverviewServiceImplTest {

    private static final UUID STUDENT = UUID.randomUUID();

    @Mock private TimetableService timetableService;
    @Mock private ClassDefinitionRepository classDefinitionRepository;
    @Mock private ClassAssignmentScheduleRepository assignmentScheduleRepository;
    @Mock private ClassQuizScheduleRepository quizScheduleRepository;
    @Mock private CourseInfoService courseInfoService;
    @Mock private LearnerProgressLookupService learnerProgressLookupService;
    @Mock private InstructorLookupService instructorLookupService;

    private StudentCourseOverviewServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new StudentCourseOverviewServiceImpl(timetableService, classDefinitionRepository,
                assignmentScheduleRepository, quizScheduleRepository, courseInfoService,
                learnerProgressLookupService, instructorLookupService);
    }

    @Test
    void composesEachClassWithCourseInstructorSessionProgressAndPendingWork() {
        UUID piano = UUID.randomUUID();
        UUID theory = UUID.randomUUID();
        UUID course = UUID.randomUUID();
        UUID defaultInstructor = UUID.randomUUID();
        UUID sessionInstructor = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        when(timetableService.getClassEnrollmentsForStudent(STUDENT, pageable)).thenReturn(new PageImpl<>(List.of(
                summary(piano, "Piano"), summary(theory, "Theory")), pageable, 2));
        when(classDefinitionRepository.findByUuidIn(anyCollection())).thenReturn(List.of(
                definition(piano, "Grade 5 Piano", course, defaultInstructor),
                definition(theory, "Theory", null, null)));
        when(courseInfoService.getCourseNames(Set.of(course))).thenReturn(Map.of(course, "Beginner Piano"));
        UUID courseEnrolment = UUID.randomUUID();
        when(learnerProgressLookupService.findCourseProgressByCourse(STUDENT, Set.of(course))).thenReturn(Map.of(
                course, new LearnerCourseProgressView(courseEnrolment, course, "Beginner Piano", "ACTIVE",
                        new BigDecimal("42.50"), now)));
        UUID nextSession = UUID.randomUUID();
        when(timetableService.getNextSessionsForStudent(eq(STUDENT), anyCollection())).thenReturn(Map.of(
                theory, session(nextSession, theory, sessionInstructor, now.plusDays(1))));
        when(instructorLookupService.findInstructorDirectoryEntries(Set.of(defaultInstructor, sessionInstructor)))
                .thenReturn(Map.of(defaultInstructor, new InstructorDirectoryEntry(defaultInstructor, "Ms Wanjiru", null, true)));

        UUID handedIn = UUID.randomUUID();
        UUID outstanding = UUID.randomUUID();
        UUID unreleased = UUID.randomUUID();
        when(assignmentScheduleRepository.findByClassDefinitionUuidIn(anyCollection())).thenReturn(List.of(
                assignment(piano, handedIn, null),
                assignment(piano, outstanding, now.minusDays(1)),
                assignment(piano, unreleased, now.plusDays(3))));
        UUID quiz = UUID.randomUUID();
        when(quizScheduleRepository.findByClassDefinitionUuidIn(anyCollection())).thenReturn(List.of(quiz(theory, quiz)));
        when(learnerProgressLookupService.findSubmittedAssignmentUuids(STUDENT, Set.of(handedIn, outstanding)))
                .thenReturn(Set.of(handedIn));
        when(learnerProgressLookupService.findSubmittedQuizUuids(STUDENT, Set.of(quiz))).thenReturn(Set.of());

        Page<StudentCourseOverviewItemDTO> page = service.getCourseOverview(STUDENT, pageable);

        assertThat(page.getTotalElements()).isEqualTo(2);
        StudentCourseOverviewItemDTO pianoItem = page.getContent().get(0);
        assertThat(pianoItem.classDefinitionUuid()).isEqualTo(piano);
        assertThat(pianoItem.classTitle()).isEqualTo("Grade 5 Piano");
        assertThat(pianoItem.courseName()).isEqualTo("Beginner Piano");
        assertThat(pianoItem.instructorUuid()).isEqualTo(defaultInstructor);
        assertThat(pianoItem.instructorName()).isEqualTo("Ms Wanjiru");
        assertThat(pianoItem.courseEnrollmentUuid()).isEqualTo(courseEnrolment);
        assertThat(pianoItem.progressPercentage()).isEqualByComparingTo("42.50");
        assertThat(pianoItem.nextSession()).isNull();
        assertThat(pianoItem.pendingAssignmentCount()).isEqualTo(1);
        assertThat(pianoItem.pendingQuizCount()).isZero();

        StudentCourseOverviewItemDTO theoryItem = page.getContent().get(1);
        assertThat(theoryItem.courseUuid()).isNull();
        assertThat(theoryItem.progressPercentage()).isNull();
        assertThat(theoryItem.instructorUuid()).isEqualTo(sessionInstructor);
        assertThat(theoryItem.nextSession().scheduledInstanceUuid()).isEqualTo(nextSession);
        assertThat(theoryItem.pendingQuizCount()).isEqualTo(1);
    }

    @Test
    void noEnrolmentsShortCircuitsBeforeAnyLookup() {
        when(timetableService.getClassEnrollmentsForStudent(eq(STUDENT), any(Pageable.class))).thenReturn(Page.empty());

        assertThat(service.getCourseOverview(STUDENT, PageRequest.of(0, 20))).isEmpty();

        verifyNoInteractions(classDefinitionRepository, courseInfoService, learnerProgressLookupService,
                instructorLookupService, assignmentScheduleRepository, quizScheduleRepository);
    }

    @Test
    void capsOversizedPages() {
        when(timetableService.getClassEnrollmentsForStudent(eq(STUDENT), any(Pageable.class))).thenReturn(Page.empty());

        service.getCourseOverview(STUDENT, PageRequest.of(2, 1000));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(timetableService).getClassEnrollmentsForStudent(eq(STUDENT), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(50);
        assertThat(captor.getValue().getPageNumber()).isEqualTo(2);
    }

    private static StudentClassEnrollmentSummaryDTO summary(UUID classUuid, String title) {
        return new StudentClassEnrollmentSummaryDTO(classUuid, title, UUID.randomUUID(), EnrollmentStatus.ENROLLED,
                4, null, null);
    }

    private static ClassDefinition definition(UUID uuid, String title, UUID course, UUID instructor) {
        ClassDefinition definition = new ClassDefinition();
        definition.setUuid(uuid);
        definition.setTitle(title);
        definition.setCourseUuid(course);
        definition.setDefaultInstructorUuid(instructor);
        return definition;
    }

    private static ScheduledInstanceDTO session(UUID uuid, UUID classUuid, UUID instructor, LocalDateTime start) {
        return new ScheduledInstanceDTO(uuid, classUuid, instructor, start, start.plusHours(1), "UTC", "Lesson 3",
                "ONLINE", null, null, null, 20, SchedulingStatus.SCHEDULED, null, null, null, null, null);
    }

    private static ClassAssignmentSchedule assignment(UUID classUuid, UUID assignmentUuid, LocalDateTime visibleAt) {
        ClassAssignmentSchedule schedule = new ClassAssignmentSchedule();
        schedule.setClassDefinitionUuid(classUuid);
        schedule.setAssignmentUuid(assignmentUuid);
        schedule.setVisibleAt(visibleAt);
        return schedule;
    }

    private static ClassQuizSchedule quiz(UUID classUuid, UUID quizUuid) {
        ClassQuizSchedule schedule = new ClassQuizSchedule();
        schedule.setClassDefinitionUuid(classUuid);
        schedule.setQuizUuid(quizUuid);
        return schedule;
    }
}
