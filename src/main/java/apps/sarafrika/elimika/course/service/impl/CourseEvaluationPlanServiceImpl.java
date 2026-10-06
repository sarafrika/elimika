package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.CourseAssessmentLineItemDTO;
import apps.sarafrika.elimika.course.dto.CourseEvaluationPlanCellRequest;
import apps.sarafrika.elimika.course.dto.CourseEvaluationPlanDTO;
import apps.sarafrika.elimika.course.factory.CourseAssessmentLineItemFactory;
import apps.sarafrika.elimika.course.internal.CourseResultService;
import apps.sarafrika.elimika.course.model.CourseAssessment;
import apps.sarafrika.elimika.course.model.CourseAssessmentLineItem;
import apps.sarafrika.elimika.course.model.Lesson;
import apps.sarafrika.elimika.course.repository.CourseAssessmentLineItemRepository;
import apps.sarafrika.elimika.course.repository.CourseAssessmentRepository;
import apps.sarafrika.elimika.course.repository.LessonRepository;
import apps.sarafrika.elimika.course.service.CourseEvaluationPlanService;
import apps.sarafrika.elimika.course.service.CourseGradeBookService;
import apps.sarafrika.elimika.course.util.enums.CourseAssessmentAggregationStrategy;
import apps.sarafrika.elimika.course.util.enums.CourseAssessmentLineItemType;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CourseEvaluationPlanServiceImpl implements CourseEvaluationPlanService {

    private final CourseAssessmentRepository assessmentRepository;
    private final CourseAssessmentLineItemRepository lineItemRepository;
    private final LessonRepository lessonRepository;
    private final CourseGradeBookService gradeBookService;
    private final CourseResultService courseResultService;

    @Override
    @Transactional(readOnly = true)
    public CourseEvaluationPlanDTO getPlan(UUID courseUuid) {
        List<CourseAssessment> components = perLessonComponents(courseUuid);
        List<Lesson> lessons = lessonRepository.findByCourseUuidOrderByLessonNumberAsc(courseUuid).stream()
                .filter(lesson -> !Boolean.FALSE.equals(lesson.getActive()))
                .toList();
        Map<String, CourseAssessmentLineItem> cells = components.isEmpty() ? Map.of()
                : lineItemRepository.findByCourseAssessmentUuidInAndLessonUuidIsNotNull(
                                components.stream().map(CourseAssessment::getUuid).toList()).stream()
                        .filter(item -> !Boolean.FALSE.equals(item.getActive()))
                        .collect(Collectors.toMap(item -> key(item.getCourseAssessmentUuid(), item.getLessonUuid()),
                                Function.identity(), (first, second) -> first));

        List<CourseEvaluationPlanDTO.LessonRow> rows = new ArrayList<>();
        for (Lesson lesson : lessons) {
            List<CourseAssessmentLineItemDTO> row = new ArrayList<>();
            for (CourseAssessment component : components) {
                CourseAssessmentLineItem cell = cells.get(key(component.getUuid(), lesson.getUuid()));
                row.add(cell == null ? null : CourseAssessmentLineItemFactory.toDTO(cell));
            }
            rows.add(new CourseEvaluationPlanDTO.LessonRow(lesson.getUuid(), lesson.getLessonNumber(), lesson.getTitle(), row));
        }
        return new CourseEvaluationPlanDTO(courseUuid, courseResultService.passMarkOf(courseUuid),
                components.stream().map(component -> new CourseEvaluationPlanDTO.Component(component.getUuid(),
                        component.getTitle(), component.getAssessmentType(), component.getWeightPercentage(),
                        component.getRubricUuid(), component.getSyncClassAttendance())).toList(),
                rows);
    }

    @Override
    public CourseEvaluationPlanDTO updatePlan(UUID courseUuid, List<CourseEvaluationPlanCellRequest> cells) {
        Map<UUID, CourseAssessment> components = perLessonComponents(courseUuid).stream()
                .collect(Collectors.toMap(CourseAssessment::getUuid, Function.identity()));
        for (CourseEvaluationPlanCellRequest request : cells) {
            CourseAssessment component = components.get(request.assessmentUuid());
            if (component == null) {
                throw new IllegalArgumentException("Assessment " + request.assessmentUuid()
                        + " is not a per-lesson component of this course");
            }
            Lesson lesson = lessonRepository.findByUuid(request.lessonUuid())
                    .filter(found -> courseUuid.equals(found.getCourseUuid()))
                    .orElseThrow(() -> new ResourceNotFoundException("Lesson " + request.lessonUuid() + " is not in this course"));
            applyCell(courseUuid, component, lesson, request);
        }
        return getPlan(courseUuid);
    }

    private void applyCell(UUID courseUuid, CourseAssessment component, Lesson lesson, CourseEvaluationPlanCellRequest request) {
        Optional<CourseAssessmentLineItem> existing =
                lineItemRepository.findByCourseAssessmentUuidAndLessonUuid(component.getUuid(), lesson.getUuid());
        if (!request.enabled()) {
            // Scores may reference the cell, so "None" deactivates it rather than deleting it.
            existing.filter(cell -> !Boolean.FALSE.equals(cell.getActive()))
                    .ifPresent(cell -> gradeBookService.updateLineItem(courseUuid, component.getUuid(), cell.getUuid(),
                            cellDto(component, lesson, request, false, cell.getDisplayOrder())));
            return;
        }
        if (existing.isPresent()) {
            gradeBookService.updateLineItem(courseUuid, component.getUuid(), existing.get().getUuid(),
                    cellDto(component, lesson, request, true, existing.get().getDisplayOrder()));
        } else {
            gradeBookService.createLineItem(courseUuid, component.getUuid(),
                    cellDto(component, lesson, request, true, lesson.getLessonNumber()));
        }
    }

    private static CourseAssessmentLineItemDTO cellDto(CourseAssessment component, Lesson lesson,
                                                       CourseEvaluationPlanCellRequest request, boolean active,
                                                       Integer displayOrder) {
        boolean attendance = Boolean.TRUE.equals(component.getSyncClassAttendance());
        boolean weighted = component.getAggregationStrategy() == CourseAssessmentAggregationStrategy.WEIGHTED_AVERAGE;
        UUID rubricUuid = request.rubricUuid() != null ? request.rubricUuid() : component.getRubricUuid();
        return new CourseAssessmentLineItemDTO(
                null,
                component.getUuid(),
                "Lesson " + lesson.getLessonNumber() + " - " + component.getTitle(),
                lesson.getTitle(),
                itemType(attendance, request),
                attendance ? null : request.assignmentUuid(),
                attendance ? null : request.quizUuid(),
                rubricUuid,
                null,
                attendance && rubricUuid == null ? BigDecimal.ONE : null,
                weighted ? BigDecimal.ONE : null,
                displayOrder == null || displayOrder < 1 ? 1 : displayOrder,
                active,
                null,
                null,
                null,
                null,
                null,
                lesson.getUuid()
        );
    }

    private static CourseAssessmentLineItemType itemType(boolean attendance, CourseEvaluationPlanCellRequest request) {
        if (attendance) {
            return CourseAssessmentLineItemType.ATTENDANCE;
        }
        if (request.quizUuid() != null) {
            return CourseAssessmentLineItemType.QUIZ;
        }
        if (request.assignmentUuid() != null) {
            return CourseAssessmentLineItemType.ASSIGNMENT;
        }
        return CourseAssessmentLineItemType.MANUAL;
    }

    private List<CourseAssessment> perLessonComponents(UUID courseUuid) {
        return assessmentRepository.findByCourseUuidOrderByCreatedDateAsc(courseUuid).stream()
                .filter(assessment -> !Boolean.FALSE.equals(assessment.getActive()))
                .filter(assessment -> Boolean.TRUE.equals(assessment.getPerLesson()))
                .toList();
    }

    private static String key(UUID assessmentUuid, UUID lessonUuid) {
        return assessmentUuid + ":" + lessonUuid;
    }
}
