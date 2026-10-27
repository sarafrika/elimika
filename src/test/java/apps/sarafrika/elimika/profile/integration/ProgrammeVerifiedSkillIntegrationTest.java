package apps.sarafrika.elimika.profile.integration;

import apps.sarafrika.elimika.profile.internal.service.UserSkillSection;
import apps.sarafrika.elimika.profile.spi.UserSkillDTO;
import apps.sarafrika.elimika.profile.spi.WalletVerificationStatus;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import apps.sarafrika.elimika.shared.utils.enums.ProficiencyLevel;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Skills achieved through programme grading land in the wallet as verified, keyed by taxonomy skill. */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import({UserSkillSection.class, SkillLookupServiceImpl.class, GenericSpecificationBuilder.class,
        ProgrammeVerifiedSkillIntegrationTest.TestConfig.class})
@DisplayName("Programme-verified wallet skills")
class ProgrammeVerifiedSkillIntegrationTest {

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

    @Autowired private UserSkillSection section;
    @Autowired private JdbcTemplate jdbc;

    private UUID userUuid;
    private UUID skillUuid;
    private UUID programUuid;

    @BeforeEach
    void seed() {
        userUuid = UUID.randomUUID();
        jdbc.update("INSERT INTO users (uuid, first_name, last_name, email, user_no, created_by) VALUES (?, 'Amani', 'W', ?, ?, 'test')",
                userUuid, "u" + Long.toHexString(System.nanoTime()) + "@example.com",
                String.valueOf(100_000_000 + (int) (Math.random() * 899_999_999)));
        skillUuid = UUID.randomUUID();
        jdbc.update("INSERT INTO skills (uuid, name, slug, created_by) VALUES (?, 'Performance technique', ?, 'test')",
                skillUuid, "performance-technique-" + Long.toHexString(System.nanoTime()));
        UUID creator = UUID.randomUUID();
        jdbc.update("INSERT INTO course_creators (uuid, user_uuid, full_name, admin_verified, created_by) VALUES (?, ?, 'Creator', true, 'test')",
                creator, userUuid);
        programUuid = UUID.randomUUID();
        jdbc.update("INSERT INTO training_programs (uuid, title, course_creator_uuid, status, created_by) VALUES (?, 'FMC', ?, 'draft', 'test')",
                programUuid, creator);
    }

    @Test
    @DisplayName("adds a verified skill naming the programme, and a repeat updates rather than duplicates")
    void addsVerifiedSkillOnce() {
        section.recordVerified(userUuid, skillUuid, "Performance technique", ProficiencyLevel.INTERMEDIATE, "Achieved in FMC", programUuid);
        section.recordVerified(userUuid, skillUuid, "Performance technique", ProficiencyLevel.ADVANCED, "Advanced in FMC", programUuid);

        List<UserSkillDTO> skills = section.list(userUuid);
        assertThat(skills).hasSize(1);
        assertThat(skills.getFirst().verificationStatus()).isEqualTo(WalletVerificationStatus.VERIFIED);
        assertThat(skills.getFirst().proficiencyLevel()).isEqualTo(ProficiencyLevel.ADVANCED);
        assertThat(jdbc.queryForObject("SELECT source_program_uuid FROM user_skills WHERE user_uuid = ?", UUID.class, userUuid))
                .isEqualTo(programUuid);
    }

    @Test
    @DisplayName("verifies the learner's own entry for the same skill and never lowers its proficiency")
    void verifiesSelfEnteredSkill() {
        section.create(userUuid, new UserSkillDTO(null, null, "performance  Technique", null, ProficiencyLevel.EXPERT,
                "My own claim", null, null, null, null, null, null, null, null));

        section.recordVerified(userUuid, skillUuid, "Performance technique", ProficiencyLevel.INTERMEDIATE, "Achieved in FMC", programUuid);

        List<UserSkillDTO> skills = section.list(userUuid);
        assertThat(skills).hasSize(1);
        assertThat(skills.getFirst().verificationStatus()).isEqualTo(WalletVerificationStatus.VERIFIED);
        assertThat(skills.getFirst().proficiencyLevel()).isEqualTo(ProficiencyLevel.EXPERT);
        assertThat(skills.getFirst().skillUuid()).isEqualTo(skillUuid);
    }
}
