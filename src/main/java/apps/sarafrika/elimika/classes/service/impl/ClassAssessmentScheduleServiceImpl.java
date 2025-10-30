package apps.sarafrika.elimika.classes.service.impl;

import apps.sarafrika.elimika.classes.dto.ClassAssignmentScheduleDTO;
import apps.sarafrika.elimika.classes.dto.ClassQuizScheduleDTO;
import apps.sarafrika.elimika.classes.factory.ClassAssignmentScheduleFactory;
import apps.sarafrika.elimika.classes.factory.ClassQuizScheduleFactory;
import apps.sarafrika.elimika.classes.model.ClassAssignmentSchedule;
import apps.sarafrika.elimika.classes.model.ClassLessonPlan;
import apps.sarafrika.elimika.classes.model.ClassQuizSchedule;
import apps.sarafrika.elimika.classes.repository.ClassAssignmentScheduleRepository;
import apps.sarafrika.elimika.classes.repository.ClassDefinitionRepository;
import apps.sarafrika.elimika.classes.repository.ClassLessonPlanRepository;
import apps.sarafrika.elimika.classes.repository.ClassQuizScheduleRepository;
import apps.sarafrika.elimika.classes.service.ClassAssessmentScheduleService;
import apps.sarafrika.elimika.course.spi.CourseAssessmentLookupService;
import apps.sarafrika.elimika.course.spi.CourseAssessmentLookupService.CourseAssignmentSummary;
import apps.sarafrika.elimika.course.spi.CourseAssessmentLookupService.CourseQuizSummary;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.shared.event.classes.ClassAssessmentScheduleChangeType;
import apps.sarafrika.elimika.shared.event.classes.ClassAssignmentScheduleChangedEventDTO;
import apps.sarafrika.elimika.shared.event.classes.ClassQuizScheduleChangedEventDTO;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ClassAssessmentScheduleServiceImpl implements ClassAssessmentScheduleService {

    private static final String CLASS_NOT_FOUND_TEMPLATE = "Class definition with UUID %s not found";
    private static final String ASSIGNMENT_SCHEDULE_NOT_FOUND_TEMPLATE = "Assignment schedule with UUID %s not found";
    private static final String QUIZ_SCHEDULE_NOT_FOUND_TEMPLATE = "Quiz schedule with UUID %s not found";
    private static final String LESSON_PLAN_NOT_FOUND_TEMPLATE = "Lesson plan entry with UUID %s not found";
    private static final String ASSIGNMENT_NOT_FOUND_TEMPLATE = "Assignment with UUID %s not found";
    private static final String QUIZ_NOT_FOUND_TEMPLATE = "Quiz with UUID %s not found";
    private static final String INSTRUCTOR_REQUIRED_MESSAGE = "Instructor UUID is required for class assessment schedules";

    private final ClassDefinitionRepository classDefinitionRepository;
    private final ClassLessonPlanRepository classLessonPlanRepository;
    private final ClassAssignmentScheduleRepository classAssignmentScheduleRepository;
    private final ClassQuizScheduleRepository classQuizScheduleRepository;
    private final CourseAssessmentLookupService courseAssessmentLookupService;
    private final InstructorLookupService instructorLookupService;
    private final UserLookupService userLookupService;
    private final ApplicationEventPublisher eventPublisher;

    // Assignment schedules
    @Override
    @Transactional(readOnly = true)
    public List<ClassAssignmentScheduleDTO> getAssignmentSchedules(UUID classDefinitionUuid) {
        ensureClassExists(classDefinitionUuid);
        return classAssignmentScheduleRepository.findByClassDefinitionUuid(classDefinitionUuid)
                .stream()
                .map(ClassAssignmentScheduleFactory::toDTO)
                .toList();
    }

    @Override
    public ClassAssignmentScheduleDTO createAssignmentSchedule(UUID classDefinitionUuid, ClassAssignmentScheduleDTO request) {
        ensureClassExists(classDefinitionUuid);
        CourseAssignmentSummary assignment = fetchAssignmentSummary(request.assignmentUuid());
        UUID resolvedLessonUuid = resolveLessonUuid(request.lessonUuid(), assignment.lessonUuid());
        validateLessonPlanOwnership(classDefinitionUuid, request.classLessonPlanUuid(), resolvedLessonUuid);
        validateInstructor(request.instructorUuid());

        ClassAssignmentSchedule entity = ClassAssignmentScheduleFactory.toEntity(request);
        entity.setClassDefinitionUuid(classDefinitionUuid);
        entity.setLessonUuid(resolvedLessonUuid);
        entity.setAssignmentUuid(assignment.assignmentUuid());

        ClassAssignmentSchedule saved = classAssignmentScheduleRepository.save(entity);
        log.debug("Created assignment schedule {} for class {}", saved.getUuid(), classDefinitionUuid);
        publishAssignmentScheduleEvent(ClassAssessmentScheduleChangeType.CREATED, saved, assignment.title());
        return ClassAssignmentScheduleFactory.toDTO(saved);
    }

    @Override
    public ClassAssignmentScheduleDTO updateAssignmentSchedule(UUID classDefinitionUuid, UUID scheduleUuid, ClassAssignmentScheduleDTO request) {
        ClassAssignmentSchedule existing = classAssignmentScheduleRepository.findByUuid(scheduleUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ASSIGNMENT_SCHEDULE_NOT_FOUND_TEMPLATE, scheduleUuid)));

        ensureScheduleBelongsToClass(existing.getClassDefinitionUuid(), classDefinitionUuid, scheduleUuid);

        ClassAssignmentScheduleFactory.updateEntityFromDTO(existing, request);

        UUID assignmentUuid = request.assignmentUuid() != null ? request.assignmentUuid() : existing.getAssignmentUuid();
        CourseAssignmentSummary assignment = fetchAssignmentSummary(assignmentUuid);
        existing.setAssignmentUuid(assignment.assignmentUuid());

        UUID resolvedLessonUuid = resolveLessonUuid(
                request.lessonUuid() != null ? request.lessonUuid() : existing.getLessonUuid(),
                assignment.lessonUuid());
        existing.setLessonUuid(resolvedLessonUuid);

        if (existing.getClassLessonPlanUuid() != null || request.classLessonPlanUuid() != null) {
            UUID planUuidToValidate = request.classLessonPlanUuid() != null
                    ? request.classLessonPlanUuid()
                    : existing.getClassLessonPlanUuid();
            validateLessonPlanOwnership(classDefinitionUuid, planUuidToValidate, resolvedLessonUuid);
            existing.setClassLessonPlanUuid(planUuidToValidate);
        }

        existing.setClassDefinitionUuid(classDefinitionUuid);
        validateInstructor(existing.getInstructorUuid());

        ClassAssignmentSchedule saved = classAssignmentScheduleRepository.save(existing);
        log.debug("Updated assignment schedule {} for class {}", scheduleUuid, classDefinitionUuid);
        publishAssignmentScheduleEvent(ClassAssessmentScheduleChangeType.UPDATED, saved, assignment.title());
        return ClassAssignmentScheduleFactory.toDTO(saved);
    }

    @Override
    public void deleteAssignmentSchedule(UUID classDefinitionUuid, UUID scheduleUuid) {
        ClassAssignmentSchedule existing = classAssignmentScheduleRepository.findByUuid(scheduleUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ASSIGNMENT_SCHEDULE_NOT_FOUND_TEMPLATE, scheduleUuid)));

        ensureScheduleBelongsToClass(existing.getClassDefinitionUuid(), classDefinitionUuid, scheduleUuid);
        String assignmentTitle = courseAssessmentLookupService.getAssignmentSummary(existing.getAssignmentUuid())
                .map(CourseAssignmentSummary::title)
                .orElse(null);
        publishAssignmentScheduleEvent(ClassAssessmentScheduleChangeType.DELETED, existing, assignmentTitle);
        classAssignmentScheduleRepository.delete(existing);
        log.debug("Deleted assignment schedule {} for class {}", scheduleUuid, classDefinitionUuid);
    }

    // Quiz schedules
    @Override
    @Transactional(readOnly = true)
    public List<ClassQuizScheduleDTO> getQuizSchedules(UUID classDefinitionUuid) {
        ensureClassExists(classDefinitionUuid);
        return classQuizScheduleRepository.findByClassDefinitionUuid(classDefinitionUuid)
                .stream()
                .map(ClassQuizScheduleFactory::toDTO)
                .toList();
    }

    @Override
    public ClassQuizScheduleDTO createQuizSchedule(UUID classDefinitionUuid, ClassQuizScheduleDTO request) {
        ensureClassExists(classDefinitionUuid);
        CourseQuizSummary quiz = fetchQuizSummary(request.quizUuid());
        UUID resolvedLessonUuid = resolveLessonUuid(request.lessonUuid(), quiz.lessonUuid());
        validateLessonPlanOwnership(classDefinitionUuid, request.classLessonPlanUuid(), resolvedLessonUuid);
        validateInstructor(request.instructorUuid());

        ClassQuizSchedule entity = ClassQuizScheduleFactory.toEntity(request);
        entity.setClassDefinitionUuid(classDefinitionUuid);
        entity.setLessonUuid(resolvedLessonUuid);
        entity.setQuizUuid(quiz.quizUuid());

        ClassQuizSchedule saved = classQuizScheduleRepository.save(entity);
        log.debug("Created quiz schedule {} for class {}", saved.getUuid(), classDefinitionUuid);
        publishQuizScheduleEvent(ClassAssessmentScheduleChangeType.CREATED, saved, quiz.title());
        return ClassQuizScheduleFactory.toDTO(saved);
    }

    @Override
    public ClassQuizScheduleDTO updateQuizSchedule(UUID classDefinitionUuid, UUID scheduleUuid, ClassQuizScheduleDTO request) {
        ClassQuizSchedule existing = classQuizScheduleRepository.findByUuid(scheduleUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(QUIZ_SCHEDULE_NOT_FOUND_TEMPLATE, scheduleUuid)));

        ensureScheduleBelongsToClass(existing.getClassDefinitionUuid(), classDefinitionUuid, scheduleUuid);

        ClassQuizScheduleFactory.updateEntityFromDTO(existing, request);

        UUID quizUuid = request.quizUuid() != null ? request.quizUuid() : existing.getQuizUuid();
        CourseQuizSummary quiz = fetchQuizSummary(quizUuid);
        existing.setQuizUuid(quiz.quizUuid());

        UUID resolvedLessonUuid = resolveLessonUuid(
                request.lessonUuid() != null ? request.lessonUuid() : existing.getLessonUuid(),
                quiz.lessonUuid());
        existing.setLessonUuid(resolvedLessonUuid);

        if (existing.getClassLessonPlanUuid() != null || request.classLessonPlanUuid() != null) {
            UUID planUuidToValidate = request.classLessonPlanUuid() != null
                    ? request.classLessonPlanUuid()
                    : existing.getClassLessonPlanUuid();
            validateLessonPlanOwnership(classDefinitionUuid, planUuidToValidate, resolvedLessonUuid);
            existing.setClassLessonPlanUuid(planUuidToValidate);
        }

        existing.setClassDefinitionUuid(classDefinitionUuid);
        validateInstructor(existing.getInstructorUuid());

        ClassQuizSchedule saved = classQuizScheduleRepository.save(existing);
        log.debug("Updated quiz schedule {} for class {}", scheduleUuid, classDefinitionUuid);
        publishQuizScheduleEvent(ClassAssessmentScheduleChangeType.UPDATED, saved, quiz.title());
        return ClassQuizScheduleFactory.toDTO(saved);
    }

    @Override
    public void deleteQuizSchedule(UUID classDefinitionUuid, UUID scheduleUuid) {
        ClassQuizSchedule existing = classQuizScheduleRepository.findByUuid(scheduleUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(QUIZ_SCHEDULE_NOT_FOUND_TEMPLATE, scheduleUuid)));

        ensureScheduleBelongsToClass(existing.getClassDefinitionUuid(), classDefinitionUuid, scheduleUuid);
        String quizTitle = courseAssessmentLookupService.getQuizSummary(existing.getQuizUuid())
                .map(CourseQuizSummary::title)
                .orElse(null);
        publishQuizScheduleEvent(ClassAssessmentScheduleChangeType.DELETED, existing, quizTitle);
        classQuizScheduleRepository.delete(existing);
        log.debug("Deleted quiz schedule {} for class {}", scheduleUuid, classDefinitionUuid);
    }

    // Helpers
    private void ensureClassExists(UUID classDefinitionUuid) {
        classDefinitionRepository.findByUuid(classDefinitionUuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(CLASS_NOT_FOUND_TEMPLATE, classDefinitionUuid)));
    }

    private void ensureScheduleBelongsToClass(UUID actualClassUuid, UUID expectedClassUuid, UUID scheduleUuid) {
        if (!actualClassUuid.equals(expectedClassUuid)) {
            throw new ResourceNotFoundException(
                    String.format("Schedule %s does not belong to class %s", scheduleUuid, expectedClassUuid));
        }
    }

    private CourseAssignmentSummary fetchAssignmentSummary(UUID assignmentUuid) {
        if (assignmentUuid == null) {
            throw new ValidationException("Assignment UUID is required");
        }
        return courseAssessmentLookupService.getAssignmentSummary(assignmentUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ASSIGNMENT_NOT_FOUND_TEMPLATE, assignmentUuid)));
    }

    private CourseQuizSummary fetchQuizSummary(UUID quizUuid) {
        if (quizUuid == null) {
            throw new ValidationException("Quiz UUID is required");
        }
        return courseAssessmentLookupService.getQuizSummary(quizUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(QUIZ_NOT_FOUND_TEMPLATE, quizUuid)));
    }

    private UUID resolveLessonUuid(UUID requestedLessonUuid, UUID templateLessonUuid) {
        UUID lessonUuid = requestedLessonUuid != null ? requestedLessonUuid : templateLessonUuid;
        if (lessonUuid == null) {
            throw new ValidationException("Lesson UUID could not be resolved from the assessment template");
        }
        if (templateLessonUuid != null && !templateLessonUuid.equals(lessonUuid)) {
            throw new ValidationException("Lesson UUID does not match the assessment template lesson");
        }
        return lessonUuid;
    }

    private void validateLessonPlanOwnership(UUID classDefinitionUuid, UUID planUuid, UUID lessonUuid) {
        if (planUuid == null) {
            return;
        }
        ClassLessonPlan plan = classLessonPlanRepository.findByUuid(planUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(LESSON_PLAN_NOT_FOUND_TEMPLATE, planUuid)));

        if (!plan.getClassDefinitionUuid().equals(classDefinitionUuid)) {
            throw new ValidationException("Lesson plan entry does not belong to the specified class");
        }
        if (lessonUuid != null && plan.getLessonUuid() != null && !plan.getLessonUuid().equals(lessonUuid)) {
            throw new ValidationException("Lesson plan entry lesson does not match the schedule lesson");
        }
    }

    private void validateInstructor(UUID instructorUuid) {
        if (instructorUuid == null) {
            throw new ValidationException(INSTRUCTOR_REQUIRED_MESSAGE);
        }
    }

    private void publishAssignmentScheduleEvent(ClassAssessmentScheduleChangeType changeType,
                                                ClassAssignmentSchedule schedule,
                                                String assignmentTitle) {
        ClassAssignmentScheduleChangedEventDTO event = new ClassAssignmentScheduleChangedEventDTO(
                changeType,
                schedule.getUuid(),
                schedule.getClassDefinitionUuid(),
                schedule.getLessonUuid(),
                schedule.getAssignmentUuid(),
                assignmentTitle,
                schedule.getClassLessonPlanUuid(),
                schedule.getVisibleAt(),
                schedule.getDueAt(),
                schedule.getGradingDueAt(),
                schedule.getTimezone(),
                schedule.getReleaseStrategy() != null ? schedule.getReleaseStrategy().getValue() : null,
                schedule.getMaxAttempts(),
                schedule.getInstructorUuid(),
                schedule.getNotes(),
                resolveChangedBy(schedule.getLastModifiedBy(), schedule.getCreatedBy()),
                resolveChangedAt(schedule.getLastModifiedDate(), schedule.getCreatedDate())
        );
        eventPublisher.publishEvent(event);
    }

    private void publishQuizScheduleEvent(ClassAssessmentScheduleChangeType changeType,
                                          ClassQuizSchedule schedule,
                                          String quizTitle) {
        ClassQuizScheduleChangedEventDTO event = new ClassQuizScheduleChangedEventDTO(
                changeType,
                schedule.getUuid(),
                schedule.getClassDefinitionUuid(),
                schedule.getLessonUuid(),
                schedule.getQuizUuid(),
                quizTitle,
                schedule.getClassLessonPlanUuid(),
                schedule.getVisibleAt(),
                schedule.getDueAt(),
                schedule.getTimezone(),
                schedule.getReleaseStrategy() != null ? schedule.getReleaseStrategy().getValue() : null,
                schedule.getTimeLimitOverride(),
                schedule.getAttemptLimitOverride(),
                schedule.getPassingScoreOverride(),
                schedule.getInstructorUuid(),
                schedule.getNotes(),
                resolveChangedBy(schedule.getLastModifiedBy(), schedule.getCreatedBy()),
                resolveChangedAt(schedule.getLastModifiedDate(), schedule.getCreatedDate())
        );
        eventPublisher.publishEvent(event);
    }

    private String resolveChangedBy(String lastModifiedBy, String createdBy) {
        return lastModifiedBy != null ? lastModifiedBy : createdBy;
    }

    private LocalDateTime resolveChangedAt(LocalDateTime lastModifiedDate, LocalDateTime createdDate) {
        return lastModifiedDate != null ? lastModifiedDate : createdDate;
    }

}
