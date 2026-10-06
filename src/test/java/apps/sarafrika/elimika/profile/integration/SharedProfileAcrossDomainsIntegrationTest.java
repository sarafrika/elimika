package apps.sarafrika.elimika.profile.integration;

import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorSkillDTO;
import apps.sarafrika.elimika.coursecreator.dto.WalletVerificationRequest;
import apps.sarafrika.elimika.coursecreator.internal.CourseCreatorProfileBridge;
import apps.sarafrika.elimika.coursecreator.service.impl.CourseCreatorSkillServiceImpl;
import apps.sarafrika.elimika.coursecreator.service.impl.CourseCreatorWalletServiceImpl;
import apps.sarafrika.elimika.instructor.dto.InstructorSkillDTO;
import apps.sarafrika.elimika.instructor.internal.InstructorProfileBridge;
import apps.sarafrika.elimika.instructor.service.impl.InstructorSkillServiceImpl;
import apps.sarafrika.elimika.profile.internal.service.ProfessionalProfileServiceImpl;
import apps.sarafrika.elimika.profile.internal.service.UserAchievementSection;
import apps.sarafrika.elimika.profile.internal.service.UserCertificationSection;
import apps.sarafrika.elimika.profile.internal.service.UserCompetencySection;
import apps.sarafrika.elimika.profile.internal.service.UserDocumentSection;
import apps.sarafrika.elimika.profile.internal.service.UserEducationSection;
import apps.sarafrika.elimika.profile.internal.service.UserExperienceSection;
import apps.sarafrika.elimika.profile.internal.service.UserMembershipSection;
import apps.sarafrika.elimika.profile.internal.service.UserPortfolioSection;
import apps.sarafrika.elimika.profile.internal.service.UserSkillSection;
import apps.sarafrika.elimika.profile.spi.ProfessionalProfileDTO;
import apps.sarafrika.elimika.profile.spi.ProfessionalProfileService;
import apps.sarafrika.elimika.profile.spi.WalletVerificationStatus;
import apps.sarafrika.elimika.shared.search.SearchIndexRequests;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.storage.service.MediaStorageService;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import apps.sarafrika.elimika.shared.utils.enums.ProficiencyLevel;
import apps.sarafrika.elimika.skills.spi.SkillLookupService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** An instructor who is also a course creator: what one domain writes, the other reads, verification included. */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import({
        ProfessionalProfileServiceImpl.class, UserSkillSection.class, UserEducationSection.class,
        UserExperienceSection.class, UserMembershipSection.class, UserCertificationSection.class,
        UserPortfolioSection.class, UserCompetencySection.class, UserAchievementSection.class,
        UserDocumentSection.class, GenericSpecificationBuilder.class, SearchIndexRequests.class,
        InstructorSkillServiceImpl.class, InstructorProfileBridge.class,
        CourseCreatorSkillServiceImpl.class, CourseCreatorWalletServiceImpl.class, CourseCreatorProfileBridge.class,
        SharedProfileAcrossDomainsIntegrationTest.TestConfig.class
})
@DisplayName("One professional profile shared by the instructor and course creator domains")
class SharedProfileAcrossDomainsIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @EnableJpaAuditing
    @ComponentScan(basePackages = {"apps.sarafrika.elimika.instructor.internal", "apps.sarafrika.elimika.coursecreator.internal"},
            useDefaultFilters = false,
            includeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*ProfileSync"))
    static class TestConfig {
        @Bean
        @Primary
        AuditorAware<String> auditorAware() {
            return () -> Optional.of("integration-test");
        }
    }

    @MockBean private SkillLookupService skillLookupService;
    @MockBean private MediaStorageService mediaStorageService;
    @MockBean private DomainSecurityService domainSecurityService;

    @Autowired private InstructorSkillServiceImpl instructorSkills;
    @Autowired private CourseCreatorSkillServiceImpl courseCreatorSkills;
    @Autowired private CourseCreatorWalletServiceImpl courseCreatorWallet;
    @Autowired private ProfessionalProfileService profileService;
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbc;

    private UUID userUuid;
    private UUID instructorUuid;
    private UUID courseCreatorUuid;

    @BeforeEach
    void seedOneUserWithTwoDomains() {
        userUuid = UUID.randomUUID();
        jdbc.update("INSERT INTO users (uuid, first_name, last_name, email, user_no, created_by) "
                        + "VALUES (?, 'Amina', 'Otieno', ?, ?, 'test')",
                userUuid, "p" + Long.toHexString(System.nanoTime()) + "@example.com",
                String.format("%09d", Math.abs(userUuid.hashCode()) % 1000000000));
        instructorUuid = UUID.randomUUID();
        jdbc.update("INSERT INTO instructors (uuid, user_uuid, full_name, created_by) VALUES (?, ?, 'Amina', 'test')",
                instructorUuid, userUuid);
        courseCreatorUuid = UUID.randomUUID();
        jdbc.update("INSERT INTO course_creators (uuid, user_uuid, full_name, created_by) VALUES (?, ?, 'Amina', 'test')",
                courseCreatorUuid, userUuid);
    }

    @Test
    @DisplayName("a skill added as an instructor is the course creator's skill, and one verdict serves both")
    void skillIsSharedAndVerifiedOnce() {
        InstructorSkillDTO added = instructorSkills.createInstructorSkill(new InstructorSkillDTO(null, instructorUuid,
                "Robotics", null, ProficiencyLevel.ADVANCED, null, null, null, null));

        Page<CourseCreatorSkillDTO> creatorSkills = courseCreatorSkills.search(
                Map.of("courseCreatorUuid", courseCreatorUuid.toString()), PageRequest.of(0, 10));
        assertThat(creatorSkills.getContent()).singleElement().satisfies(skill -> {
            assertThat(skill.uuid()).isEqualTo(added.uuid());
            assertThat(skill.courseCreatorUuid()).isEqualTo(courseCreatorUuid);
            assertThat(skill.verificationStatus()).isEqualTo(WalletVerificationStatus.PENDING);
        });

        courseCreatorWallet.verifyItem(courseCreatorUuid, "skills", added.uuid(),
                new WalletVerificationRequest(WalletVerificationStatus.VERIFIED, "portfolio checked"));
        assertThat(profileService.skills().find(added.uuid()).orElseThrow().verificationStatus())
                .isEqualTo(WalletVerificationStatus.VERIFIED);

        instructorSkills.updateInstructorSkill(added.uuid(), new InstructorSkillDTO(null, instructorUuid,
                "Robotics and AI", null, null, null, null, null, null));
        CourseCreatorSkillDTO renamed = courseCreatorSkills.getCourseCreatorSkillByUuid(added.uuid());
        assertThat(renamed.skillName()).isEqualTo("Robotics and AI");
        assertThat(renamed.proficiencyLevel()).isEqualTo(ProficiencyLevel.ADVANCED);
        assertThat(renamed.verificationStatus()).isEqualTo(WalletVerificationStatus.PENDING);
    }

    @Test
    @DisplayName("the same skill added by both domains is stored once")
    void sameSkillFromBothDomainsIsStoredOnce() {
        InstructorSkillDTO fromInstructor = instructorSkills.createInstructorSkill(new InstructorSkillDTO(null,
                instructorUuid, "Public Speaking", null, ProficiencyLevel.BEGINNER, null, null, null, null));
        CourseCreatorSkillDTO fromCreator = courseCreatorSkills.createCourseCreatorSkill(new CourseCreatorSkillDTO(null,
                courseCreatorUuid, "public  speaking", null, ProficiencyLevel.EXPERT, "Toastmasters", null, null, null,
                null, null, null, null, null));

        assertThat(fromCreator.uuid()).isEqualTo(fromInstructor.uuid());
        assertThat(profileService.skills().list(userUuid)).singleElement().satisfies(skill -> {
            assertThat(skill.proficiencyLevel()).isEqualTo(ProficiencyLevel.EXPERT);
            assertThat(skill.evidence()).isEqualTo("Toastmasters");
        });
    }

    @Test
    @DisplayName("basics saved once are copied onto both domain rows")
    void basicsReachBothDomainRows() {
        profileService.mergeBasics(userUuid, new ProfessionalProfileDTO(null, "Builds robots with learners",
                "Robotics trainer", null, "Kisumu", null, null, null));
        entityManager.flush();

        assertThat(jdbc.queryForObject("SELECT professional_headline FROM instructors WHERE uuid = ?", String.class,
                instructorUuid)).isEqualTo("Robotics trainer");
        assertThat(jdbc.queryForObject("SELECT professional_headline FROM course_creators WHERE uuid = ?", String.class,
                courseCreatorUuid)).isEqualTo("Robotics trainer");
        assertThat(jdbc.queryForObject("SELECT bio FROM course_creators WHERE uuid = ?", String.class,
                courseCreatorUuid)).isEqualTo("Builds robots with learners");
    }
}
