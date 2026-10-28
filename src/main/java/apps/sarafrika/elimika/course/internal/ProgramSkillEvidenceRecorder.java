package apps.sarafrika.elimika.course.internal;

import apps.sarafrika.elimika.course.model.CourseAssessmentLineItem;
import apps.sarafrika.elimika.course.model.CourseAssessmentLineItemRubricEvaluation;
import apps.sarafrika.elimika.course.model.CourseAssessmentLineItemRubricEvaluationRow;
import apps.sarafrika.elimika.course.model.CourseAssessmentLineItemScore;
import apps.sarafrika.elimika.course.model.CourseEnrollment;
import apps.sarafrika.elimika.course.model.LearnerSkillEvidence;
import apps.sarafrika.elimika.course.model.LearnerSkillLevel;
import apps.sarafrika.elimika.course.model.ProgramSkillMapping;
import apps.sarafrika.elimika.course.model.RubricScoringLevel;
import apps.sarafrika.elimika.course.repository.CourseAssessmentLineItemRepository;
import apps.sarafrika.elimika.course.repository.CourseAssessmentLineItemRubricEvaluationRepository;
import apps.sarafrika.elimika.course.repository.CourseAssessmentLineItemRubricEvaluationRowRepository;
import apps.sarafrika.elimika.course.repository.CourseAssessmentLineItemScoreRepository;
import apps.sarafrika.elimika.course.repository.CourseEnrollmentRepository;
import apps.sarafrika.elimika.course.repository.LearnerSkillEvidenceRepository;
import apps.sarafrika.elimika.course.repository.LearnerSkillLevelRepository;
import apps.sarafrika.elimika.course.repository.ProgramEnrollmentRepository;
import apps.sarafrika.elimika.course.repository.ProgramSkillMappingRepository;
import apps.sarafrika.elimika.course.repository.RubricScoringLevelRepository;
import apps.sarafrika.elimika.course.util.enums.CourseAssessmentLineItemRubricEvaluationStatus;
import apps.sarafrika.elimika.skills.spi.SkillLevelLookupService;
import apps.sarafrika.elimika.skills.spi.SkillLevelSummary;
import apps.sarafrika.elimika.skills.spi.SkillLevelsChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Turns each graded gradebook cell into skill evidence for every programme that maps its assessment.
 * Evidence is append-only; the learner's level follows their latest live evidence.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProgramSkillEvidenceRecorder {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final ProgramSkillMappingRepository mappingRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final ProgramEnrollmentRepository programEnrollmentRepository;
    private final CourseAssessmentLineItemRepository lineItemRepository;
    private final CourseAssessmentLineItemScoreRepository lineItemScoreRepository;
    private final CourseAssessmentLineItemRubricEvaluationRepository evaluationRepository;
    private final CourseAssessmentLineItemRubricEvaluationRowRepository evaluationRowRepository;
    private final RubricScoringLevelRepository scoringLevelRepository;
    private final LearnerSkillEvidenceRepository evidenceRepository;
    private final LearnerSkillLevelRepository levelRepository;
    private final SkillLevelLookupService skillLevels;
    private final ApplicationEventPublisher eventPublisher;

    @EventListener
    public void onLineItemGraded(LineItemGradedEvent event) {
        List<ProgramSkillMapping> mappings = mappingRepository.findByCourseAssessmentUuidAndActiveTrue(event.courseAssessmentUuid());
        if (mappings.isEmpty()) {
            return;
        }
        CourseEnrollment enrollment = courseEnrollmentRepository.findByUuid(event.enrollmentUuid()).orElse(null);
        CourseAssessmentLineItem lineItem = lineItemRepository.findByUuid(event.lineItemUuid()).orElse(null);
        if (enrollment == null || lineItem == null) {
            return;
        }
        Map<String, List<ProgramSkillMapping>> bySkill = mappings.stream()
                .collect(Collectors.groupingBy(mapping -> mapping.getProgramUuid() + "/" + mapping.getSkillUuid()));
        for (List<ProgramSkillMapping> skillMappings : bySkill.values()) {
            ProgramSkillMapping first = skillMappings.getFirst();
            boolean enrolled = programEnrollmentRepository
                    .findFirstByStudentUuidAndProgramUuidOrderByCreatedDateDesc(enrollment.getStudentUuid(), first.getProgramUuid())
                    .isPresent();
            if (!enrolled) {
                continue;
            }
            evidenceScore(skillMappings, lineItem, enrollment.getUuid())
                    .ifPresent(score -> record(enrollment, lineItem, first, score, event.gradedAt()));
        }
    }

    @EventListener
    public void onSkillLevelsChanged(SkillLevelsChangedEvent event) {
        levelRepository.findAll().forEach(level -> {
            SkillLevelSummary summary = skillLevels.levelFor(level.getScore());
            apply(level, summary, level.getGradedAt());
        });
    }

    /** Σ share × criterion %, or the cell's whole percentage; empty while a mapped criterion is unscored. */
    private Optional<BigDecimal> evidenceScore(List<ProgramSkillMapping> mappings, CourseAssessmentLineItem lineItem,
                                               UUID enrollmentUuid) {
        boolean whole = mappings.stream().anyMatch(mapping -> mapping.getRubricCriteriaUuid() == null);
        if (whole) {
            return lineItemScoreRepository.findByLineItemUuidAndEnrollmentUuid(lineItem.getUuid(), enrollmentUuid)
                    .map(CourseAssessmentLineItemScore::getPercentage)
                    .map(percentage -> percentage.setScale(2, RoundingMode.HALF_UP));
        }
        CourseAssessmentLineItemRubricEvaluation evaluation = evaluationRepository
                .findByLineItemUuidAndEnrollmentUuid(lineItem.getUuid(), enrollmentUuid)
                .filter(found -> found.getStatus() == CourseAssessmentLineItemRubricEvaluationStatus.COMPLETED)
                .orElse(null);
        if (evaluation == null) {
            return Optional.empty();
        }
        BigDecimal maxPoints = scoringLevelRepository.findByRubricUuidOrderByLevelOrder(evaluation.getRubricUuid()).stream()
                .map(RubricScoringLevel::getPoints)
                .filter(Objects::nonNull)
                .max(BigDecimal::compareTo)
                .orElse(null);
        if (maxPoints == null || maxPoints.signum() == 0) {
            return Optional.empty();
        }
        Map<UUID, BigDecimal> points = evaluationRowRepository.findByEvaluationUuid(evaluation.getUuid()).stream()
                .collect(Collectors.toMap(CourseAssessmentLineItemRubricEvaluationRow::getCriteriaUuid,
                        CourseAssessmentLineItemRubricEvaluationRow::getPoints, (a, b) -> b));
        BigDecimal total = BigDecimal.ZERO;
        for (ProgramSkillMapping mapping : mappings) {
            BigDecimal criterionPoints = points.get(mapping.getRubricCriteriaUuid());
            if (criterionPoints == null) {
                return Optional.empty();
            }
            BigDecimal percentage = criterionPoints.multiply(HUNDRED).divide(maxPoints, 6, RoundingMode.HALF_UP);
            total = total.add(percentage.multiply(mapping.getSharePercentage()).divide(HUNDRED, 6, RoundingMode.HALF_UP));
        }
        return Optional.of(total.min(HUNDRED).setScale(2, RoundingMode.HALF_UP));
    }

    private void record(CourseEnrollment enrollment, CourseAssessmentLineItem lineItem, ProgramSkillMapping mapping,
                        BigDecimal score, LocalDateTime gradedAt) {
        UUID student = enrollment.getStudentUuid();
        Optional<LearnerSkillEvidence> previous = evidenceRepository
                .findFirstByStudentUuidAndProgramUuidAndSkillUuidAndLineItemUuidAndSupersededFalse(
                        student, mapping.getProgramUuid(), mapping.getSkillUuid(), lineItem.getUuid());
        if (previous.isPresent() && previous.get().getScore().compareTo(score) == 0) {
            return;
        }
        SkillLevelSummary level = skillLevels.levelFor(score);
        LearnerSkillEvidence evidence = new LearnerSkillEvidence();
        evidence.setStudentUuid(student);
        evidence.setProgramUuid(mapping.getProgramUuid());
        evidence.setSkillUuid(mapping.getSkillUuid());
        evidence.setCourseEnrollmentUuid(enrollment.getUuid());
        evidence.setCourseAssessmentUuid(lineItem.getCourseAssessmentUuid());
        evidence.setLineItemUuid(lineItem.getUuid());
        evidence.setScore(score);
        evidence.setLevelName(level.name());
        evidence.setCountsAsAchieved(level.countsAsAchieved());
        evidence.setGradedAt(gradedAt != null ? gradedAt : LocalDateTime.now(java.time.ZoneOffset.UTC));
        evidence.setGradedByUuid(grader(lineItem.getUuid(), enrollment.getUuid()));
        previous.ifPresent(old -> {
            old.setSuperseded(Boolean.TRUE);
            evidenceRepository.save(old);
            evidence.setSupersedesUuid(old.getUuid());
        });
        evidenceRepository.save(evidence);
        refreshLevel(student, mapping.getProgramUuid(), mapping.getSkillUuid());
    }

    /** The cell's grader, falling back to the rubric evaluation's grader. */
    private UUID grader(UUID lineItemUuid, UUID enrollmentUuid) {
        return lineItemScoreRepository.findByLineItemUuidAndEnrollmentUuid(lineItemUuid, enrollmentUuid)
                .map(CourseAssessmentLineItemScore::getGradedByUuid)
                .or(() -> evaluationRepository.findByLineItemUuidAndEnrollmentUuid(lineItemUuid, enrollmentUuid)
                        .map(CourseAssessmentLineItemRubricEvaluation::getGradedByUuid))
                .orElse(null);
    }

    private void refreshLevel(UUID student, UUID program, UUID skill) {
        evidenceRepository.findFirstByStudentUuidAndProgramUuidAndSkillUuidAndSupersededFalseOrderByGradedAtDescIdDesc(
                        student, program, skill)
                .ifPresent(latest -> {
                    LearnerSkillLevel level = levelRepository.findByStudentUuidAndProgramUuidAndSkillUuid(student, program, skill)
                            .orElseGet(() -> {
                                LearnerSkillLevel created = new LearnerSkillLevel();
                                created.setStudentUuid(student);
                                created.setProgramUuid(program);
                                created.setSkillUuid(skill);
                                return created;
                            });
                    level.setEvidenceUuid(latest.getUuid());
                    level.setScore(latest.getScore());
                    apply(level, skillLevels.levelFor(latest.getScore()), latest.getGradedAt());
                });
    }

    private void apply(LearnerSkillLevel level, SkillLevelSummary summary, LocalDateTime gradedAt) {
        boolean wasAchieved = Boolean.TRUE.equals(level.getCountsAsAchieved());
        Integer previousRank = level.getLevelRank();
        level.setLevelName(summary.name());
        level.setLevelRank(summary.rank());
        level.setCountsAsAchieved(summary.countsAsAchieved());
        level.setGradedAt(gradedAt);
        if (summary.countsAsAchieved() && level.getFirstAchievedAt() == null) {
            level.setFirstAchievedAt(gradedAt);
        }
        LearnerSkillLevel saved = levelRepository.save(level);
        if (summary.countsAsAchieved() && (!wasAchieved || !Integer.valueOf(summary.rank()).equals(previousRank))) {
            log.info("Learner {} achieved skill {} in programme {}", saved.getStudentUuid(), saved.getSkillUuid(), saved.getProgramUuid());
            eventPublisher.publishEvent(new LearnerSkillAchievedEvent(saved.getStudentUuid(), saved.getProgramUuid(),
                    saved.getSkillUuid(), saved.getLevelName(), saved.getLevelRank(), saved.getEvidenceUuid()));
        }
    }
}
