package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.CourseAssessmentLineItemDTO;
import apps.sarafrika.elimika.course.dto.CourseAssessmentLineItemScoreDTO;
import apps.sarafrika.elimika.course.dto.CourseGradebookDTO;
import apps.sarafrika.elimika.course.factory.CourseAssessmentFactory;
import apps.sarafrika.elimika.course.factory.CourseAssessmentLineItemFactory;
import apps.sarafrika.elimika.course.factory.CourseAssessmentLineItemScoreFactory;
import apps.sarafrika.elimika.course.factory.CourseAssessmentScoreFactory;
import apps.sarafrika.elimika.course.model.Assignment;
import apps.sarafrika.elimika.course.model.CourseAssessment;
import apps.sarafrika.elimika.course.model.CourseAssessmentLineItem;
import apps.sarafrika.elimika.course.model.CourseAssessmentLineItemScore;
import apps.sarafrika.elimika.course.model.CourseAssessmentScore;
import apps.sarafrika.elimika.course.model.CourseEnrollment;
import apps.sarafrika.elimika.course.model.Lesson;
import apps.sarafrika.elimika.course.model.Quiz;
import apps.sarafrika.elimika.course.repository.AssignmentRepository;
import apps.sarafrika.elimika.course.repository.CourseAssessmentLineItemRepository;
import apps.sarafrika.elimika.course.repository.CourseAssessmentLineItemScoreRepository;
import apps.sarafrika.elimika.course.repository.CourseAssessmentRepository;
import apps.sarafrika.elimika.course.repository.CourseAssessmentScoreRepository;
import apps.sarafrika.elimika.course.repository.CourseEnrollmentRepository;
import apps.sarafrika.elimika.course.repository.LessonRepository;
import apps.sarafrika.elimika.course.repository.QuizRepository;
import apps.sarafrika.elimika.course.service.CourseGradebookService;
import apps.sarafrika.elimika.course.util.enums.AttemptStatus;
import apps.sarafrika.elimika.course.util.enums.CourseAssessmentAggregationStrategy;
import apps.sarafrika.elimika.course.util.enums.CourseAssessmentLineItemType;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CourseGradebookServiceImpl implements CourseGradebookService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100.00");
    private static final int DIVISION_SCALE = 4;
    private static final String ASSESSMENT_NOT_FOUND_TEMPLATE = "Course assessment %s was not found in course %s";
    private static final String LINE_ITEM_NOT_FOUND_TEMPLATE = "Gradebook line item %s was not found in assessment %s";
    private static final String ENROLLMENT_NOT_FOUND_TEMPLATE = "Course enrollment %s was not found in course %s";

    private final CourseAssessmentRepository courseAssessmentRepository;
    private final CourseAssessmentLineItemRepository lineItemRepository;
    private final CourseAssessmentLineItemScoreRepository lineItemScoreRepository;
    private final CourseAssessmentScoreRepository courseAssessmentScoreRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final AssignmentRepository assignmentRepository;
    private final QuizRepository quizRepository;
    private final LessonRepository lessonRepository;

    @Override
    public CourseAssessmentLineItemDTO createLineItem(UUID courseUuid, UUID assessmentUuid, CourseAssessmentLineItemDTO lineItemDTO) {
        CourseAssessment assessment = getAssessmentOrThrow(courseUuid, assessmentUuid);
        CourseAssessmentLineItem lineItem = CourseAssessmentLineItemFactory.toEntity(lineItemDTO);
        lineItem.setCourseAssessmentUuid(assessment.getUuid());
        applyCreateDefaults(lineItem);
        validateLineItem(assessment, courseUuid, lineItem, null);

        CourseAssessmentLineItem savedLineItem = lineItemRepository.save(lineItem);
        recalculateAssessmentForCourseEnrollments(courseUuid, assessment.getUuid());
        return CourseAssessmentLineItemFactory.toDTO(savedLineItem);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseAssessmentLineItemDTO> getLineItems(UUID courseUuid, UUID assessmentUuid) {
        getAssessmentOrThrow(courseUuid, assessmentUuid);
        return lineItemRepository.findByCourseAssessmentUuidOrderByDisplayOrderAscCreatedDateAsc(assessmentUuid)
                .stream()
                .map(CourseAssessmentLineItemFactory::toDTO)
                .toList();
    }

    @Override
    public CourseAssessmentLineItemDTO updateLineItem(
            UUID courseUuid,
            UUID assessmentUuid,
            UUID lineItemUuid,
            CourseAssessmentLineItemDTO lineItemDTO
    ) {
        CourseAssessment assessment = getAssessmentOrThrow(courseUuid, assessmentUuid);
        CourseAssessmentLineItem existingLineItem = getLineItemOrThrow(assessmentUuid, lineItemUuid);

        updateLineItemFields(existingLineItem, lineItemDTO);
        if (existingLineItem.getCourseAssessmentUuid() == null) {
            existingLineItem.setCourseAssessmentUuid(assessmentUuid);
        }
        if (existingLineItem.getDisplayOrder() == null) {
            existingLineItem.setDisplayOrder(nextDisplayOrder(assessmentUuid));
        }
        if (existingLineItem.getActive() == null) {
            existingLineItem.setActive(Boolean.TRUE);
        }

        validateLineItem(assessment, courseUuid, existingLineItem, existingLineItem.getUuid());

        CourseAssessmentLineItem updatedLineItem = lineItemRepository.save(existingLineItem);
        recalculateAssessmentForCourseEnrollments(courseUuid, assessment.getUuid());
        return CourseAssessmentLineItemFactory.toDTO(updatedLineItem);
    }

    @Override
    public void deleteLineItem(UUID courseUuid, UUID assessmentUuid, UUID lineItemUuid) {
        getAssessmentOrThrow(courseUuid, assessmentUuid);
        getLineItemOrThrow(assessmentUuid, lineItemUuid);
        lineItemRepository.deleteByUuid(lineItemUuid);
        recalculateAssessmentForCourseEnrollments(courseUuid, assessmentUuid);
    }

    @Override
    public CourseAssessmentLineItemScoreDTO upsertLineItemScore(
            UUID courseUuid,
            UUID assessmentUuid,
            UUID lineItemUuid,
            UUID enrollmentUuid,
            CourseAssessmentLineItemScoreDTO scoreDTO
    ) {
        getAssessmentOrThrow(courseUuid, assessmentUuid);
        CourseAssessmentLineItem lineItem = getLineItemOrThrow(assessmentUuid, lineItemUuid);
        getEnrollmentOrThrow(courseUuid, enrollmentUuid);

        CourseAssessmentLineItemScore score = lineItemScoreRepository.findByLineItemUuidAndEnrollmentUuid(lineItemUuid, enrollmentUuid)
                .orElseGet(CourseAssessmentLineItemScore::new);

        score.setLineItemUuid(lineItemUuid);
        score.setEnrollmentUuid(enrollmentUuid);
        score.setComments(scoreDTO.comments());
        score.setGradedAt(scoreDTO.gradedAt() != null ? scoreDTO.gradedAt() : LocalDateTime.now());
        score.setGradedByUuid(scoreDTO.gradedByUuid());
        applyScoreValues(score, scoreDTO.score(), scoreDTO.maxScore(), lineItem.getMaxScore());

        CourseAssessmentLineItemScore savedScore = lineItemScoreRepository.save(score);
        recalculateAssessmentForEnrollment(enrollmentUuid, getAssessmentOrThrow(courseUuid, assessmentUuid));
        recalculateCourseFinalGrade(courseUuid, enrollmentUuid);
        return CourseAssessmentLineItemScoreFactory.toDTO(savedScore);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseGradebookDTO getEnrollmentGradebook(UUID courseUuid, UUID enrollmentUuid) {
        CourseEnrollment enrollment = getEnrollmentOrThrow(courseUuid, enrollmentUuid);
        List<CourseAssessment> assessments = courseAssessmentRepository.findByCourseUuidOrderByCreatedDateAsc(courseUuid);
        List<UUID> assessmentUuids = assessments.stream()
                .map(CourseAssessment::getUuid)
                .toList();

        List<CourseAssessmentLineItem> lineItems = assessmentUuids.isEmpty()
                ? List.of()
                : lineItemRepository.findByCourseAssessmentUuidInOrderByDisplayOrderAscCreatedDateAsc(assessmentUuids);
        Map<UUID, List<CourseAssessmentLineItem>> lineItemsByAssessment = lineItems.stream()
                .collect(Collectors.groupingBy(
                        CourseAssessmentLineItem::getCourseAssessmentUuid,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<UUID> lineItemUuids = lineItems.stream()
                .map(CourseAssessmentLineItem::getUuid)
                .toList();
        Map<UUID, CourseAssessmentLineItemScore> lineItemScoresByItemUuid = lineItemUuids.isEmpty()
                ? Map.of()
                : lineItemScoreRepository.findByEnrollmentUuidAndLineItemUuidIn(enrollmentUuid, lineItemUuids).stream()
                .collect(Collectors.toMap(CourseAssessmentLineItemScore::getLineItemUuid, Function.identity()));

        Map<UUID, CourseAssessmentScore> componentScoresByAssessmentUuid = assessmentUuids.isEmpty()
                ? Map.of()
                : courseAssessmentScoreRepository.findByEnrollmentUuidAndAssessmentUuidIn(enrollmentUuid, assessmentUuids).stream()
                .collect(Collectors.toMap(CourseAssessmentScore::getAssessmentUuid, Function.identity()));

        List<CourseGradebookDTO.ComponentDTO> components = new ArrayList<>();
        BigDecimal configuredWeightPercentage = BigDecimal.ZERO;
        BigDecimal gradedWeightPercentage = BigDecimal.ZERO;

        for (CourseAssessment assessment : assessments) {
            BigDecimal componentWeight = defaultWeight(assessment.getWeightPercentage());
            configuredWeightPercentage = configuredWeightPercentage.add(componentWeight);

            CourseAssessmentScore aggregateScore = componentScoresByAssessmentUuid.get(assessment.getUuid());
            if (aggregateScore != null && aggregateScore.getPercentage() != null) {
                gradedWeightPercentage = gradedWeightPercentage.add(componentWeight);
            }

            List<CourseGradebookDTO.LineItemEntryDTO> entries = lineItemsByAssessment
                    .getOrDefault(assessment.getUuid(), List.of())
                    .stream()
                    .sorted(Comparator.comparing(CourseAssessmentLineItem::getDisplayOrder, Comparator.nullsLast(Integer::compareTo))
                            .thenComparing(CourseAssessmentLineItem::getCreatedDate, Comparator.nullsLast(LocalDateTime::compareTo)))
                    .map(lineItem -> new CourseGradebookDTO.LineItemEntryDTO(
                            CourseAssessmentLineItemFactory.toDTO(lineItem),
                            CourseAssessmentLineItemScoreFactory.toDTO(lineItemScoresByItemUuid.get(lineItem.getUuid()))
                    ))
                    .toList();

            BigDecimal lineItemWeightTotal = lineItemsByAssessment
                    .getOrDefault(assessment.getUuid(), List.of())
                    .stream()
                    .map(CourseAssessmentLineItem::getWeightPercentage)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            components.add(new CourseGradebookDTO.ComponentDTO(
                    CourseAssessmentFactory.toDTO(assessment),
                    CourseAssessmentScoreFactory.toDTO(aggregateScore),
                    lineItemWeightTotal,
                    entries
            ));
        }

        return new CourseGradebookDTO(
                enrollment.getCourseUuid(),
                enrollment.getUuid(),
                enrollment.getFinalGrade(),
                gradedWeightPercentage,
                configuredWeightPercentage,
                components
        );
    }

    @Override
    public void recalculateCourseAssessment(UUID courseUuid, UUID assessmentUuid) {
        recalculateAssessmentForCourseEnrollments(courseUuid, assessmentUuid);
    }

    @Override
    public void syncAssignmentGrade(
            UUID assignmentUuid,
            UUID enrollmentUuid,
            BigDecimal score,
            BigDecimal maxScore,
            String comments,
            LocalDateTime gradedAt,
            UUID gradedByUuid
    ) {
        if (assignmentUuid == null || enrollmentUuid == null || score == null || maxScore == null) {
            return;
        }

        lineItemRepository.findByAssignmentUuid(assignmentUuid)
                .ifPresent(lineItem -> syncDerivedScore(lineItem, enrollmentUuid, score, maxScore, comments, gradedAt, gradedByUuid));
    }

    @Override
    public void syncQuizAttemptGrade(
            UUID quizUuid,
            UUID enrollmentUuid,
            BigDecimal score,
            BigDecimal maxScore,
            String comments,
            LocalDateTime gradedAt,
            UUID gradedByUuid,
            AttemptStatus status
    ) {
        if (status == null || !status.isGraded() || quizUuid == null || enrollmentUuid == null || score == null || maxScore == null) {
            return;
        }

        lineItemRepository.findByQuizUuid(quizUuid)
                .ifPresent(lineItem -> syncDerivedScore(lineItem, enrollmentUuid, score, maxScore, comments, gradedAt, gradedByUuid));
    }

    private void syncDerivedScore(
            CourseAssessmentLineItem lineItem,
            UUID enrollmentUuid,
            BigDecimal scoreValue,
            BigDecimal maxScoreValue,
            String comments,
            LocalDateTime gradedAt,
            UUID gradedByUuid
    ) {
        CourseAssessmentLineItemScore score = lineItemScoreRepository
                .findByLineItemUuidAndEnrollmentUuid(lineItem.getUuid(), enrollmentUuid)
                .orElseGet(CourseAssessmentLineItemScore::new);

        score.setLineItemUuid(lineItem.getUuid());
        score.setEnrollmentUuid(enrollmentUuid);
        score.setComments(comments);
        score.setGradedAt(gradedAt != null ? gradedAt : LocalDateTime.now());
        score.setGradedByUuid(gradedByUuid);
        applyScoreValues(score, scoreValue, maxScoreValue, lineItem.getMaxScore());
        lineItemScoreRepository.save(score);

        CourseAssessment assessment = courseAssessmentRepository.findByUuid(lineItem.getCourseAssessmentUuid())
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Course assessment with ID %s not found", lineItem.getCourseAssessmentUuid())));
        CourseEnrollment enrollment = courseEnrollmentRepository.findByUuid(enrollmentUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Course enrollment with ID %s not found", enrollmentUuid)));

        recalculateAssessmentForEnrollment(enrollmentUuid, assessment);
        recalculateCourseFinalGrade(enrollment.getCourseUuid(), enrollmentUuid);
    }

    private void applyScoreValues(
            CourseAssessmentLineItemScore score,
            BigDecimal rawScore,
            BigDecimal suppliedMaxScore,
            BigDecimal defaultMaxScore
    ) {
        BigDecimal resolvedMaxScore = suppliedMaxScore != null ? suppliedMaxScore : defaultMaxScore;
        if (rawScore == null || resolvedMaxScore == null) {
            throw new IllegalArgumentException("Both score and max score are required to record a gradebook score");
        }
        if (resolvedMaxScore.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Maximum score must be greater than zero");
        }
        if (rawScore.compareTo(BigDecimal.ZERO) < 0 || rawScore.compareTo(resolvedMaxScore) > 0) {
            throw new IllegalArgumentException("Score must be between 0 and the maximum score");
        }

        BigDecimal percentage = rawScore
                .divide(resolvedMaxScore, DIVISION_SCALE, RoundingMode.HALF_UP)
                .multiply(ONE_HUNDRED)
                .setScale(2, RoundingMode.HALF_UP);

        score.setScore(rawScore);
        score.setMaxScore(resolvedMaxScore);
        score.setPercentage(percentage);
    }

    private void recalculateAssessmentForCourseEnrollments(UUID courseUuid, UUID assessmentUuid) {
        CourseAssessment assessment = getAssessmentOrThrow(courseUuid, assessmentUuid);
        for (CourseEnrollment enrollment : courseEnrollmentRepository.findByCourseUuid(courseUuid)) {
            recalculateAssessmentForEnrollment(enrollment.getUuid(), assessment);
            recalculateCourseFinalGrade(courseUuid, enrollment.getUuid());
        }
    }

    private CourseAssessmentScore recalculateAssessmentForEnrollment(UUID enrollmentUuid, CourseAssessment assessment) {
        List<CourseAssessmentLineItem> lineItems = lineItemRepository
                .findByCourseAssessmentUuidOrderByDisplayOrderAscCreatedDateAsc(assessment.getUuid())
                .stream()
                .filter(lineItem -> !Boolean.FALSE.equals(lineItem.getActive()))
                .toList();

        if (lineItems.isEmpty()) {
            courseAssessmentScoreRepository.deleteByEnrollmentUuidAndAssessmentUuid(enrollmentUuid, assessment.getUuid());
            return null;
        }

        List<UUID> lineItemUuids = lineItems.stream()
                .map(CourseAssessmentLineItem::getUuid)
                .toList();
        Map<UUID, CourseAssessmentLineItemScore> scoresByLineItemUuid = lineItemScoreRepository
                .findByEnrollmentUuidAndLineItemUuidIn(enrollmentUuid, lineItemUuids)
                .stream()
                .filter(score -> score.getPercentage() != null)
                .collect(Collectors.toMap(CourseAssessmentLineItemScore::getLineItemUuid, Function.identity()));

        List<ScoredLineItem> scoredLineItems = lineItems.stream()
                .map(lineItem -> {
                    CourseAssessmentLineItemScore lineItemScore = scoresByLineItemUuid.get(lineItem.getUuid());
                    return lineItemScore == null ? null : new ScoredLineItem(lineItem, lineItemScore);
                })
                .filter(Objects::nonNull)
                .toList();

        if (scoredLineItems.isEmpty()) {
            courseAssessmentScoreRepository.deleteByEnrollmentUuidAndAssessmentUuid(enrollmentUuid, assessment.getUuid());
            return null;
        }

        AggregationResult aggregationResult = aggregateAssessmentScore(assessment, scoredLineItems);
        CourseAssessmentScore aggregateScore = courseAssessmentScoreRepository
                .findByEnrollmentUuidAndAssessmentUuid(enrollmentUuid, assessment.getUuid())
                .orElseGet(CourseAssessmentScore::new);

        aggregateScore.setEnrollmentUuid(enrollmentUuid);
        aggregateScore.setAssessmentUuid(assessment.getUuid());
        aggregateScore.setScore(aggregationResult.score());
        aggregateScore.setMaxScore(aggregationResult.maxScore());
        aggregateScore.setPercentage(aggregationResult.percentage());
        aggregateScore.setComments("Derived from " + scoredLineItems.size() + " graded line items");
        aggregateScore.setGradedAt(aggregationResult.gradedAt());
        aggregateScore.setGradedByUuid(null);

        return courseAssessmentScoreRepository.save(aggregateScore);
    }

    private BigDecimal recalculateCourseFinalGrade(UUID courseUuid, UUID enrollmentUuid) {
        CourseEnrollment enrollment = getEnrollmentOrThrow(courseUuid, enrollmentUuid);
        List<CourseAssessment> assessments = courseAssessmentRepository.findByCourseUuidOrderByCreatedDateAsc(courseUuid);
        if (assessments.isEmpty()) {
            enrollment.setFinalGrade(null);
            courseEnrollmentRepository.save(enrollment);
            return null;
        }

        List<UUID> assessmentUuids = assessments.stream()
                .map(CourseAssessment::getUuid)
                .toList();
        Map<UUID, CourseAssessmentScore> componentScoresByAssessmentUuid = courseAssessmentScoreRepository
                .findByEnrollmentUuidAndAssessmentUuidIn(enrollmentUuid, assessmentUuids)
                .stream()
                .collect(Collectors.toMap(CourseAssessmentScore::getAssessmentUuid, Function.identity()));

        BigDecimal weightedScoreTotal = BigDecimal.ZERO;
        BigDecimal appliedWeightTotal = BigDecimal.ZERO;

        for (CourseAssessment assessment : assessments) {
            CourseAssessmentScore componentScore = componentScoresByAssessmentUuid.get(assessment.getUuid());
            if (componentScore == null || componentScore.getPercentage() == null) {
                continue;
            }

            BigDecimal assessmentWeight = defaultWeight(assessment.getWeightPercentage());
            if (assessmentWeight.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            weightedScoreTotal = weightedScoreTotal.add(componentScore.getPercentage().multiply(assessmentWeight));
            appliedWeightTotal = appliedWeightTotal.add(assessmentWeight);
        }

        BigDecimal finalGrade = null;
        if (appliedWeightTotal.compareTo(BigDecimal.ZERO) > 0) {
            finalGrade = weightedScoreTotal
                    .divide(appliedWeightTotal, DIVISION_SCALE, RoundingMode.HALF_UP)
                    .setScale(2, RoundingMode.HALF_UP);
        }

        enrollment.setFinalGrade(finalGrade);
        courseEnrollmentRepository.save(enrollment);
        return finalGrade;
    }

    private AggregationResult aggregateAssessmentScore(
            CourseAssessment assessment,
            List<ScoredLineItem> scoredLineItems
    ) {
        CourseAssessmentAggregationStrategy aggregationStrategy = assessment.getAggregationStrategy() != null
                ? assessment.getAggregationStrategy()
                : CourseAssessmentAggregationStrategy.POINTS_SUM;

        return switch (aggregationStrategy) {
            case POINTS_SUM -> aggregatePointsSum(scoredLineItems);
            case WEIGHTED_AVERAGE -> aggregateWeightedAverage(scoredLineItems);
        };
    }

    private AggregationResult aggregatePointsSum(List<ScoredLineItem> scoredLineItems) {
        BigDecimal totalScore = scoredLineItems.stream()
                .map(ScoredLineItem::score)
                .map(CourseAssessmentLineItemScore::getScore)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalMaxScore = scoredLineItems.stream()
                .map(ScoredLineItem::score)
                .map(CourseAssessmentLineItemScore::getMaxScore)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalMaxScore.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Cannot aggregate line items without a positive total maximum score");
        }

        BigDecimal percentage = totalScore
                .divide(totalMaxScore, DIVISION_SCALE, RoundingMode.HALF_UP)
                .multiply(ONE_HUNDRED)
                .setScale(2, RoundingMode.HALF_UP);

        LocalDateTime latestGradedAt = scoredLineItems.stream()
                .map(ScoredLineItem::score)
                .map(CourseAssessmentLineItemScore::getGradedAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        return new AggregationResult(totalScore, totalMaxScore, percentage, latestGradedAt);
    }

    private AggregationResult aggregateWeightedAverage(List<ScoredLineItem> scoredLineItems) {
        BigDecimal weightedTotal = BigDecimal.ZERO;
        BigDecimal weightTotal = BigDecimal.ZERO;

        for (ScoredLineItem scoredLineItem : scoredLineItems) {
            BigDecimal lineItemWeight = scoredLineItem.lineItem().getWeightPercentage();
            if (lineItemWeight == null || lineItemWeight.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalStateException(
                        "Weighted line item aggregation requires a positive weight for line item " + scoredLineItem.lineItem().getUuid()
                );
            }

            weightedTotal = weightedTotal.add(scoredLineItem.score().getPercentage().multiply(lineItemWeight));
            weightTotal = weightTotal.add(lineItemWeight);
        }

        if (weightTotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Cannot aggregate weighted line items without a positive weight total");
        }

        BigDecimal percentage = weightedTotal
                .divide(weightTotal, DIVISION_SCALE, RoundingMode.HALF_UP)
                .setScale(2, RoundingMode.HALF_UP);

        LocalDateTime latestGradedAt = scoredLineItems.stream()
                .map(ScoredLineItem::score)
                .map(CourseAssessmentLineItemScore::getGradedAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        return new AggregationResult(percentage, ONE_HUNDRED, percentage, latestGradedAt);
    }

    private void applyCreateDefaults(CourseAssessmentLineItem lineItem) {
        if (lineItem.getDisplayOrder() == null && lineItem.getCourseAssessmentUuid() != null) {
            lineItem.setDisplayOrder(nextDisplayOrder(lineItem.getCourseAssessmentUuid()));
        }
        if (lineItem.getActive() == null) {
            lineItem.setActive(Boolean.TRUE);
        }
    }

    private Integer nextDisplayOrder(UUID assessmentUuid) {
        return lineItemRepository.findByCourseAssessmentUuidOrderByDisplayOrderAscCreatedDateAsc(assessmentUuid)
                .stream()
                .map(CourseAssessmentLineItem::getDisplayOrder)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .map(maxOrder -> maxOrder + 1)
                .orElse(1);
    }

    private void updateLineItemFields(CourseAssessmentLineItem lineItem, CourseAssessmentLineItemDTO dto) {
        if (dto.courseAssessmentUuid() != null) {
            lineItem.setCourseAssessmentUuid(dto.courseAssessmentUuid());
        }
        if (dto.title() != null) {
            lineItem.setTitle(dto.title());
        }
        if (dto.description() != null) {
            lineItem.setDescription(dto.description());
        }
        if (dto.itemType() != null) {
            lineItem.setItemType(dto.itemType());
        }
        lineItem.setAssignmentUuid(dto.assignmentUuid());
        lineItem.setQuizUuid(dto.quizUuid());
        if (dto.rubricUuid() != null) {
            lineItem.setRubricUuid(dto.rubricUuid());
        }
        if (dto.maxScore() != null) {
            lineItem.setMaxScore(dto.maxScore());
        }
        if (dto.weightPercentage() != null) {
            lineItem.setWeightPercentage(dto.weightPercentage());
        }
        if (dto.displayOrder() != null) {
            lineItem.setDisplayOrder(dto.displayOrder());
        }
        if (dto.active() != null) {
            lineItem.setActive(dto.active());
        }
        if (dto.dueAt() != null) {
            lineItem.setDueAt(dto.dueAt());
        }
    }

    private void validateLineItem(
            CourseAssessment assessment,
            UUID courseUuid,
            CourseAssessmentLineItem lineItem,
            UUID existingLineItemUuid
    ) {
        if (lineItem.getCourseAssessmentUuid() == null || !lineItem.getCourseAssessmentUuid().equals(assessment.getUuid())) {
            throw new IllegalArgumentException("Gradebook line item must belong to the assessment identified in the route");
        }
        if (lineItem.getItemType() == null) {
            throw new IllegalArgumentException("Line item type is required");
        }
        if (lineItem.getTitle() == null || lineItem.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Line item title is required");
        }
        if (lineItem.getDisplayOrder() == null || lineItem.getDisplayOrder() <= 0) {
            throw new IllegalArgumentException("Display order must be a positive integer");
        }
        if (Boolean.TRUE.equals(usesWeightedLineItems(assessment))) {
            if (lineItem.getWeightPercentage() == null || lineItem.getWeightPercentage().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Weighted assessment components require a positive line item weight percentage");
            }
        }

        if (lineItem.getAssignmentUuid() != null && lineItem.getQuizUuid() != null) {
            throw new IllegalArgumentException("A gradebook line item cannot reference both an assignment and a quiz");
        }
        if (lineItem.getAssignmentUuid() != null) {
            validateAssignmentLineItem(courseUuid, lineItem, existingLineItemUuid);
        }
        if (lineItem.getQuizUuid() != null) {
            validateQuizLineItem(courseUuid, lineItem, existingLineItemUuid);
        }
    }

    private void validateAssignmentLineItem(UUID courseUuid, CourseAssessmentLineItem lineItem, UUID existingLineItemUuid) {
        if (lineItem.getAssignmentUuid() == null) {
            throw new IllegalArgumentException("Assignment line items must reference an assignment UUID");
        }
        if (lineItem.getQuizUuid() != null) {
            throw new IllegalArgumentException("Assignment line items cannot reference a quiz UUID");
        }

        Assignment assignment = assignmentRepository.findByUuid(lineItem.getAssignmentUuid())
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Assignment with ID %s not found", lineItem.getAssignmentUuid())));
        Lesson lesson = lessonRepository.findByUuid(assignment.getLessonUuid())
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Lesson with ID %s not found", assignment.getLessonUuid())));
        if (!Objects.equals(lesson.getCourseUuid(), courseUuid)) {
            throw new IllegalArgumentException("Assignment line items must reference assignments from the same course");
        }

        lineItemRepository.findByAssignmentUuid(lineItem.getAssignmentUuid())
                .filter(existing -> !existing.getUuid().equals(existingLineItemUuid))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("The selected assignment is already linked to another gradebook line item");
                });
    }

    private void validateQuizLineItem(UUID courseUuid, CourseAssessmentLineItem lineItem, UUID existingLineItemUuid) {
        if (lineItem.getQuizUuid() == null) {
            throw new IllegalArgumentException("Quiz line items must reference a quiz UUID");
        }
        if (lineItem.getAssignmentUuid() != null) {
            throw new IllegalArgumentException("Quiz line items cannot reference an assignment UUID");
        }

        Quiz quiz = quizRepository.findByUuid(lineItem.getQuizUuid())
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Quiz with ID %s not found", lineItem.getQuizUuid())));
        Lesson lesson = lessonRepository.findByUuid(quiz.getLessonUuid())
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Lesson with ID %s not found", quiz.getLessonUuid())));
        if (!Objects.equals(lesson.getCourseUuid(), courseUuid)) {
            throw new IllegalArgumentException("Quiz line items must reference quizzes from the same course");
        }

        lineItemRepository.findByQuizUuid(lineItem.getQuizUuid())
                .filter(existing -> !existing.getUuid().equals(existingLineItemUuid))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("The selected quiz is already linked to another gradebook line item");
                });
    }

    private boolean usesWeightedLineItems(CourseAssessment assessment) {
        return (assessment.getAggregationStrategy() != null
                ? assessment.getAggregationStrategy()
                : CourseAssessmentAggregationStrategy.POINTS_SUM) == CourseAssessmentAggregationStrategy.WEIGHTED_AVERAGE;
    }

    private BigDecimal defaultWeight(BigDecimal weightPercentage) {
        return weightPercentage != null ? weightPercentage : BigDecimal.ZERO;
    }

    private CourseAssessment getAssessmentOrThrow(UUID courseUuid, UUID assessmentUuid) {
        return courseAssessmentRepository.findByUuidAndCourseUuid(assessmentUuid, courseUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ASSESSMENT_NOT_FOUND_TEMPLATE, assessmentUuid, courseUuid)));
    }

    private CourseAssessmentLineItem getLineItemOrThrow(UUID assessmentUuid, UUID lineItemUuid) {
        return lineItemRepository.findByUuidAndCourseAssessmentUuid(lineItemUuid, assessmentUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(LINE_ITEM_NOT_FOUND_TEMPLATE, lineItemUuid, assessmentUuid)));
    }

    private CourseEnrollment getEnrollmentOrThrow(UUID courseUuid, UUID enrollmentUuid) {
        return courseEnrollmentRepository.findByUuidAndCourseUuid(enrollmentUuid, courseUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ENROLLMENT_NOT_FOUND_TEMPLATE, enrollmentUuid, courseUuid)));
    }

    private record ScoredLineItem(CourseAssessmentLineItem lineItem, CourseAssessmentLineItemScore score) {
    }

    private record AggregationResult(
            BigDecimal score,
            BigDecimal maxScore,
            BigDecimal percentage,
            LocalDateTime gradedAt
    ) {
    }
}
