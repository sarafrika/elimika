package apps.sarafrika.elimika.course.integration;

import apps.sarafrika.elimika.course.dto.ProgramSkillMappingDTO;
import apps.sarafrika.elimika.course.dto.ProgramTargetSkillDTO;
import apps.sarafrika.elimika.course.model.AssessmentRubric;
import apps.sarafrika.elimika.course.model.CourseAssessment;
import apps.sarafrika.elimika.course.model.RubricCriteria;
import apps.sarafrika.elimika.course.repository.AssessmentRubricRepository;
import apps.sarafrika.elimika.course.repository.CourseAssessmentRepository;
import apps.sarafrika.elimika.course.repository.RubricCriteriaRepository;
import apps.sarafrika.elimika.course.service.impl.ProgramSkillServiceImpl;
import apps.sarafrika.elimika.course.util.enums.ContentStatus;
import apps.sarafrika.elimika.course.util.enums.CourseAssessmentAggregationStrategy;
import apps.sarafrika.elimika.skills.internal.SkillLookupServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Programme target skills and the rubric criteria or whole scores linked to them. */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import({ProgramSkillServiceImpl.class, SkillLookupServiceImpl.class, ProgramSkillMappingIntegrationTest.TestConfig.class})
@DisplayName("Programme skill mappings")
class ProgramSkillMappingIntegrationTest {

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
    }

    @Autowired private ProgramSkillServiceImpl service;
    @Autowired private CourseAssessmentRepository assessmentRepository;
    @Autowired private AssessmentRubricRepository rubricRepository;
    @Autowired private RubricCriteriaRepository criteriaRepository;
    @Autowired private JdbcTemplate jdbc;

    private UUID creatorUuid;
    private UUID programUuid;
    private UUID technique;
    private UUID rhythm;
    private CourseAssessment lessonScores;
    private CourseAssessment finalExam;
    private CourseAssessment outsideAssessment;
    private RubricCriteria techniqueCriterion;
    private RubricCriteria toneCriterion;
    private RubricCriteria foreignCriterion;

    @BeforeEach
    void seed() {
        UUID creatorUser = UUID.randomUUID();
        jdbc.update("INSERT INTO users (uuid, first_name, last_name, email, user_no, created_by) VALUES (?, 'Test', 'Creator', ?, ?, 'test')",
                creatorUser, "c" + Long.toHexString(System.nanoTime()) + "@example.com",
                String.valueOf(100_000_000 + (int) (Math.random() * 899_999_999)));
        creatorUuid = UUID.randomUUID();
        jdbc.update("INSERT INTO course_creators (uuid, user_uuid, full_name, admin_verified, created_by) VALUES (?, ?, 'Creator', true, 'test')",
                creatorUuid, creatorUser);
        programUuid = UUID.randomUUID();
        jdbc.update("INSERT INTO training_programs (uuid, title, course_creator_uuid, status, created_by) "
                + "VALUES (?, 'Foundation Music Certificate', ?, 'draft', 'test')", programUuid, creatorUuid);
        UUID piano = course("Introduction to Piano");
        UUID outside = course("Unrelated Course");
        jdbc.update("INSERT INTO program_courses (uuid, program_uuid, course_uuid, sequence_order, is_required, created_by) "
                + "VALUES (?, ?, ?, 1, true, 'test')", UUID.randomUUID(), programUuid, piano);

        technique = skill("Performance technique");
        rhythm = skill("Rhythm accuracy");

        AssessmentRubric rubric = rubric("Piano performance");
        techniqueCriterion = criterion(rubric, "Technique", 1);
        toneCriterion = criterion(rubric, "Tonal Quality", 2);
        foreignCriterion = criterion(rubric("Other rubric"), "Timing", 1);
        lessonScores = assessment(piano, "Lesson scores", rubric.getUuid());
        finalExam = assessment(piano, "Final exam", null);
        outsideAssessment = assessment(outside, "Outside", null);
    }

    @Test
    @DisplayName("criteria shares of 60/40 link to a skill and the skill becomes ready")
    void linksCriteriaToSkill() {
        service.replaceTargetSkills(programUuid, List.of(technique, rhythm));

        ProgramTargetSkillDTO saved = service.replaceMappings(programUuid, technique, List.of(
                link(lessonScores, techniqueCriterion, "60"), link(lessonScores, toneCriterion, "40"), link(finalExam, null, "100")));

        assertThat(saved.ready()).isTrue();
        assertThat(saved.skillName()).isEqualTo("Performance technique");
        assertThat(saved.mappings()).extracting(ProgramSkillMappingDTO::rubricCriteriaName)
                .containsExactlyInAnyOrder("Technique", "Tonal Quality", null);
        assertThat(service.getSkills(programUuid)).extracting(ProgramTargetSkillDTO::ready).containsExactly(true, false);
    }

    @Test
    @DisplayName("rejects shares not totalling 100, outside assessments, foreign criteria, mixed whole/criteria and non-target skills")
    void validation() {
        service.replaceTargetSkills(programUuid, List.of(technique));

        assertThatThrownBy(() -> service.replaceMappings(programUuid, technique, List.of(link(lessonScores, techniqueCriterion, "90"))))
                .hasMessageContaining("100%");
        assertThatThrownBy(() -> service.replaceMappings(programUuid, technique, List.of(link(outsideAssessment, null, "100"))))
                .hasMessageContaining("outside this programme");
        assertThatThrownBy(() -> service.replaceMappings(programUuid, technique, List.of(link(lessonScores, foreignCriterion, "100"))))
                .hasMessageContaining("not a criterion");
        assertThatThrownBy(() -> service.replaceMappings(programUuid, technique, List.of(
                link(lessonScores, null, "50"), link(lessonScores, techniqueCriterion, "50"))))
                .hasMessageContaining("whole score or by criteria");
        assertThatThrownBy(() -> service.replaceMappings(programUuid, rhythm, List.of(link(finalExam, null, "100"))))
                .hasMessageContaining("target skills");
    }

    @Test
    @DisplayName("publishing needs every target skill linked, and removing a skill drops its links")
    void publishGateAndRemoval() {
        service.replaceTargetSkills(programUuid, List.of(technique, rhythm));
        service.replaceMappings(programUuid, technique, List.of(link(finalExam, null, "100")));

        assertThatThrownBy(() -> service.enforceSkillsMapped(programUuid))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Rhythm accuracy");

        service.replaceTargetSkills(programUuid, List.of(rhythm));
        service.replaceTargetSkills(programUuid, List.of(rhythm, technique));
        assertThat(service.getSkills(programUuid)).allSatisfy(skill -> assertThat(skill.mappings()).isEmpty());
    }

    private static ProgramSkillMappingDTO link(CourseAssessment assessment, RubricCriteria criterion, String share) {
        return new ProgramSkillMappingDTO(null, assessment.getUuid(), criterion == null ? null : criterion.getUuid(),
                new BigDecimal(share), null, null, null);
    }

    private UUID course(String name) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO courses (uuid, name, course_creator_uuid, status, active, created_by) VALUES (?, ?, ?, 'published', true, 'test')",
                uuid, name, creatorUuid);
        return uuid;
    }

    private UUID skill(String name) {
        UUID uuid = UUID.randomUUID();
        String slug = name.toLowerCase().replace(' ', '-') + "-" + Long.toHexString(System.nanoTime());
        jdbc.update("INSERT INTO skills (uuid, name, slug, created_by) VALUES (?, ?, ?, 'test')", uuid, name, slug);
        return uuid;
    }

    private AssessmentRubric rubric(String title) {
        AssessmentRubric rubric = new AssessmentRubric();
        rubric.setTitle(title);
        rubric.setRubricType("performance");
        rubric.setCourseCreatorUuid(creatorUuid);
        rubric.setStatus(ContentStatus.PUBLISHED);
        rubric.setIsActive(true);
        rubric.setIsPublic(false);
        rubric.setUsesCustomLevels(true);
        rubric.setTotalWeight(new BigDecimal("100.00"));
        rubric.setWeightUnit("percentage");
        return rubricRepository.saveAndFlush(rubric);
    }

    private RubricCriteria criterion(AssessmentRubric rubric, String name, int order) {
        RubricCriteria criterion = new RubricCriteria();
        criterion.setRubricUuid(rubric.getUuid());
        criterion.setComponentName(name);
        criterion.setDisplayOrder(order);
        return criteriaRepository.saveAndFlush(criterion);
    }

    private CourseAssessment assessment(UUID courseUuid, String title, UUID rubricUuid) {
        CourseAssessment assessment = new CourseAssessment();
        assessment.setCourseUuid(courseUuid);
        assessment.setAssessmentType("performance");
        assessment.setTitle(title);
        assessment.setWeightPercentage(new BigDecimal("50.00"));
        assessment.setAggregationStrategy(CourseAssessmentAggregationStrategy.POINTS_SUM);
        assessment.setRubricUuid(rubricUuid);
        assessment.setPerLesson(false);
        assessment.setIsRequired(true);
        assessment.setActive(true);
        assessment.setSyncClassAttendance(false);
        return assessmentRepository.saveAndFlush(assessment);
    }
}
