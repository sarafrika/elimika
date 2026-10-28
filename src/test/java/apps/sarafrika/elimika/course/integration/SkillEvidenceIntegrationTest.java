package apps.sarafrika.elimika.course.integration;

import apps.sarafrika.elimika.course.dto.CourseAssessmentLineItemRubricEvaluationDTO;
import apps.sarafrika.elimika.course.dto.CourseAssessmentLineItemRubricEvaluationRowDTO;
import apps.sarafrika.elimika.course.internal.LearnerSkillAchievedEvent;
import apps.sarafrika.elimika.course.internal.ProgramSkillEvidenceRecorder;
import apps.sarafrika.elimika.course.model.AssessmentRubric;
import apps.sarafrika.elimika.course.model.CourseAssessment;
import apps.sarafrika.elimika.course.model.CourseAssessmentLineItem;
import apps.sarafrika.elimika.course.model.CourseEnrollment;
import apps.sarafrika.elimika.course.model.LearnerSkillEvidence;
import apps.sarafrika.elimika.course.model.LearnerSkillLevel;
import apps.sarafrika.elimika.course.model.RubricCriteria;
import apps.sarafrika.elimika.course.model.RubricScoringLevel;
import apps.sarafrika.elimika.course.repository.AssessmentRubricRepository;
import apps.sarafrika.elimika.course.repository.CourseAssessmentLineItemRepository;
import apps.sarafrika.elimika.course.repository.CourseAssessmentRepository;
import apps.sarafrika.elimika.course.repository.CourseEnrollmentRepository;
import apps.sarafrika.elimika.course.repository.LearnerSkillEvidenceRepository;
import apps.sarafrika.elimika.course.repository.LearnerSkillLevelRepository;
import apps.sarafrika.elimika.course.repository.RubricCriteriaRepository;
import apps.sarafrika.elimika.course.repository.RubricScoringLevelRepository;
import apps.sarafrika.elimika.course.service.impl.CourseGradeBookServiceImpl;
import apps.sarafrika.elimika.course.util.enums.ContentStatus;
import apps.sarafrika.elimika.course.util.enums.CourseAssessmentAggregationStrategy;
import apps.sarafrika.elimika.course.util.enums.CourseAssessmentLineItemType;
import apps.sarafrika.elimika.course.util.enums.EnrollmentStatus;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService;
import apps.sarafrika.elimika.skills.dto.SkillLevelDTO;
import apps.sarafrika.elimika.skills.internal.SkillLevelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Grades on mapped rubric criteria become append-only skill evidence; the latest sets the level. */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@RecordApplicationEvents
@Import({CourseGradeBookServiceImpl.class, apps.sarafrika.elimika.course.internal.CourseResultService.class,
        apps.sarafrika.elimika.course.internal.GradeRevisionRecorder.class,
        apps.sarafrika.elimika.course.internal.CourseProgressReportBuilder.class,
        ProgramSkillEvidenceRecorder.class, SkillLevelService.class, SkillEvidenceIntegrationTest.TestConfig.class})
@DisplayName("Skill evidence from grading")
class SkillEvidenceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @EnableJpaAuditing
    static class TestConfig {
        @Bean
        @Primary
        AuditorAware<String> auditorAware() {
            return () -> Optional.of("integration-test");
        }

        @Bean
        ClassDefinitionLookupService classDefinitionLookupService() {
            return Mockito.mock(ClassDefinitionLookupService.class);
        }

        @Bean
        apps.sarafrika.elimika.shared.spi.enrollment.EnrollmentLookupService enrollmentLookupService() {
            return Mockito.mock(apps.sarafrika.elimika.shared.spi.enrollment.EnrollmentLookupService.class);
        }
    }

    @Autowired private CourseGradeBookServiceImpl gradeBookService;
    @Autowired private SkillLevelService skillLevelService;
    @Autowired private CourseAssessmentRepository assessmentRepository;
    @Autowired private CourseAssessmentLineItemRepository lineItemRepository;
    @Autowired private CourseEnrollmentRepository courseEnrollmentRepository;
    @Autowired private AssessmentRubricRepository rubricRepository;
    @Autowired private RubricCriteriaRepository criteriaRepository;
    @Autowired private RubricScoringLevelRepository scoringLevelRepository;
    @Autowired private LearnerSkillEvidenceRepository evidenceRepository;
    @Autowired private LearnerSkillLevelRepository levelRepository;
    @Autowired private ApplicationEvents events;
    @Autowired private JdbcTemplate jdbc;

    private UUID creatorUuid;
    private UUID programUuid;
    private UUID courseUuid;
    private UUID skillUuid;
    private CourseAssessment performance;
    private CourseAssessmentLineItem recital;
    private RubricCriteria techniqueCriterion;
    private RubricCriteria toneCriterion;
    private final List<RubricScoringLevel> levels = new ArrayList<>();
    private UUID graderUuid;

    @BeforeEach
    void seed() {
        levels.clear();
        graderUuid = user("grader");
        creatorUuid = UUID.randomUUID();
        jdbc.update("INSERT INTO course_creators (uuid, user_uuid, full_name, admin_verified, created_by) VALUES (?, ?, 'Creator', true, 'test')",
                creatorUuid, user("creator"));
        programUuid = UUID.randomUUID();
        jdbc.update("INSERT INTO training_programs (uuid, title, course_creator_uuid, status, created_by) "
                + "VALUES (?, 'Foundation Music Certificate', ?, 'draft', 'test')", programUuid, creatorUuid);
        courseUuid = UUID.randomUUID();
        jdbc.update("INSERT INTO courses (uuid, name, course_creator_uuid, status, active, created_by) VALUES (?, 'Introduction to Piano', ?, 'published', true, 'test')",
                courseUuid, creatorUuid);
        jdbc.update("INSERT INTO program_courses (uuid, program_uuid, course_uuid, sequence_order, is_required, weight_percentage, created_by) "
                + "VALUES (?, ?, ?, 1, true, 100, 'test')", UUID.randomUUID(), programUuid, courseUuid);

        AssessmentRubric rubric = new AssessmentRubric();
        rubric.setTitle("Piano performance");
        rubric.setRubricType("performance");
        rubric.setCourseCreatorUuid(creatorUuid);
        rubric.setStatus(ContentStatus.PUBLISHED);
        rubric.setIsActive(true);
        rubric.setIsPublic(false);
        rubric.setUsesCustomLevels(true);
        rubric.setTotalWeight(new BigDecimal("100.00"));
        rubric.setWeightUnit("percentage");
        rubric = rubricRepository.saveAndFlush(rubric);
        techniqueCriterion = criterion(rubric, "Technique", 1);
        toneCriterion = criterion(rubric, "Tonal Quality", 2);
        for (int points = 4; points >= 1; points--) {
            RubricScoringLevel level = new RubricScoringLevel();
            level.setRubricUuid(rubric.getUuid());
            level.setName("Level " + points);
            level.setPoints(BigDecimal.valueOf(points));
            level.setLevelOrder(5 - points);
            level.setIsPassing(points >= 2);
            levels.add(scoringLevelRepository.saveAndFlush(level));
        }

        performance = new CourseAssessment();
        performance.setCourseUuid(courseUuid);
        performance.setAssessmentType("performance");
        performance.setTitle("Performance");
        performance.setWeightPercentage(new BigDecimal("100.00"));
        performance.setAggregationStrategy(CourseAssessmentAggregationStrategy.POINTS_SUM);
        performance.setRubricUuid(rubric.getUuid());
        performance.setPerLesson(false);
        performance.setIsRequired(true);
        performance.setActive(true);
        performance.setSyncClassAttendance(false);
        performance = assessmentRepository.saveAndFlush(performance);

        recital = new CourseAssessmentLineItem();
        recital.setCourseAssessmentUuid(performance.getUuid());
        recital.setTitle("Recital");
        recital.setItemType(CourseAssessmentLineItemType.PERFORMANCE);
        recital.setRubricUuid(rubric.getUuid());
        recital.setMaxScore(new BigDecimal("8.00"));
        recital.setDisplayOrder(1);
        recital.setActive(true);
        recital = lineItemRepository.saveAndFlush(recital);

        skillUuid = UUID.randomUUID();
        jdbc.update("INSERT INTO skills (uuid, name, slug, created_by) VALUES (?, 'Performance technique', ?, 'test')",
                skillUuid, "performance-technique-" + Long.toHexString(System.nanoTime()));
        jdbc.update("INSERT INTO program_target_skills (program_uuid, skill_uuid, display_order, created_by) VALUES (?, ?, 1, 'test')",
                programUuid, skillUuid);
        mapping(techniqueCriterion, "60");
        mapping(toneCriterion, "40");
    }

    @Test
    @DisplayName("criterion shares give the evidence score, and a regrade supersedes it while keeping history")
    void evidenceFromCriteriaAndRegrade() {
        CourseEnrollment enrollment = enrolInProgramme();

        grade(enrollment, 4, 2);
        // 60% × 100 + 40% × 50 = 80
        LearnerSkillLevel level = level(enrollment);
        assertThat(level.getScore()).isEqualByComparingTo("80.00");
        assertThat(level.getLevelName()).isEqualTo("Achieved");
        assertThat(events.stream(LearnerSkillAchievedEvent.class)).hasSize(1);

        grade(enrollment, 2, 2);
        level = level(enrollment);
        assertThat(level.getScore()).isEqualByComparingTo("50.00");
        assertThat(level.getLevelName()).isEqualTo("Developing");
        assertThat(level.getFirstAchievedAt()).isNotNull();

        List<LearnerSkillEvidence> history = evidenceRepository.findByStudentUuidAndProgramUuidOrderByGradedAtDescIdDesc(
                enrollment.getStudentUuid(), programUuid);
        assertThat(history).hasSize(2);
        assertThat(history).filteredOn(LearnerSkillEvidence::getSuperseded).hasSize(1);
        assertThat(history.getFirst().getSupersedesUuid()).isEqualTo(history.get(1).getUuid());
        assertThat(history).extracting(LearnerSkillEvidence::getGradedByUuid).containsOnly(graderUuid);
    }

    @Test
    @DisplayName("a learner not enrolled in the programme produces no evidence")
    void noEvidenceOutsideProgramme() {
        CourseEnrollment enrollment = courseEnrollment(student());
        grade(enrollment, 4, 4);
        assertThat(levelRepository.findByStudentUuidAndProgramUuid(enrollment.getStudentUuid(), programUuid)).isEmpty();
    }

    @Test
    @DisplayName("changing the level scale re-levels current levels but not the evidence history")
    void scaleChangeRelevels() {
        CourseEnrollment enrollment = enrolInProgramme();
        grade(enrollment, 2, 2);
        assertThat(level(enrollment).getLevelName()).isEqualTo("Developing");

        skillLevelService.replace(List.of(
                new SkillLevelDTO(null, "Foundation", BigDecimal.ZERO, false),
                new SkillLevelDTO(null, "Proficient", new BigDecimal("45"), true)));

        assertThat(level(enrollment).getLevelName()).isEqualTo("Proficient");
        assertThat(level(enrollment).getCountsAsAchieved()).isTrue();
        assertThat(evidenceRepository.findByStudentUuidAndProgramUuidOrderByGradedAtDescIdDesc(enrollment.getStudentUuid(), programUuid))
                .extracting(LearnerSkillEvidence::getLevelName).containsExactly("Developing");
    }

    private void grade(CourseEnrollment enrollment, int technique, int tone) {
        CourseAssessmentLineItemRubricEvaluationDTO dto = new CourseAssessmentLineItemRubricEvaluationDTO(
                null, null, null, null, null, null, null, null, null, null, LocalDateTime.now(), graderUuid,
                List.of(row(techniqueCriterion, technique), row(toneCriterion, tone)));
        gradeBookService.upsertLineItemRubricEvaluation(courseUuid, performance.getUuid(), recital.getUuid(), enrollment.getUuid(), dto);
    }

    private CourseAssessmentLineItemRubricEvaluationRowDTO row(RubricCriteria criterion, int points) {
        RubricScoringLevel level = levels.stream().filter(l -> l.getPoints().intValue() == points).findFirst().orElseThrow();
        return new CourseAssessmentLineItemRubricEvaluationRowDTO(null, criterion.getUuid(), null, level.getUuid(), null, null, null);
    }

    private LearnerSkillLevel level(CourseEnrollment enrollment) {
        return levelRepository.findByStudentUuidAndProgramUuidAndSkillUuid(enrollment.getStudentUuid(), programUuid, skillUuid).orElseThrow();
    }

    private CourseEnrollment enrolInProgramme() {
        UUID student = student();
        jdbc.update("INSERT INTO program_enrollments (uuid, student_uuid, program_uuid, status, created_by) VALUES (?, ?, ?, 'active', 'test')",
                UUID.randomUUID(), student, programUuid);
        return courseEnrollment(student);
    }

    private CourseEnrollment courseEnrollment(UUID student) {
        CourseEnrollment enrollment = new CourseEnrollment();
        enrollment.setStudentUuid(student);
        enrollment.setCourseUuid(courseUuid);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        return courseEnrollmentRepository.saveAndFlush(enrollment);
    }

    private void mapping(RubricCriteria criterion, String share) {
        jdbc.update("INSERT INTO program_skill_mappings (program_uuid, skill_uuid, course_assessment_uuid, rubric_criteria_uuid, share_percentage, created_by) "
                + "VALUES (?, ?, ?, ?, ?, 'test')", programUuid, skillUuid, performance.getUuid(), criterion.getUuid(), new BigDecimal(share));
    }

    private RubricCriteria criterion(AssessmentRubric rubric, String name, int order) {
        RubricCriteria criterion = new RubricCriteria();
        criterion.setRubricUuid(rubric.getUuid());
        criterion.setComponentName(name);
        criterion.setDisplayOrder(order);
        return criteriaRepository.saveAndFlush(criterion);
    }

    private UUID student() {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO students (uuid, user_uuid, full_name, created_by) VALUES (?, ?, 'Learner', 'test')", uuid, user("student"));
        return uuid;
    }

    private UUID user(String role) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO users (uuid, first_name, last_name, email, user_no, created_by) VALUES (?, 'Test', ?, ?, ?, 'test')",
                uuid, role, role + Long.toHexString(System.nanoTime()) + "@example.com",
                String.valueOf(100_000_000 + (int) (Math.random() * 899_999_999)));
        return uuid;
    }
}
