package apps.sarafrika.elimika.shared.integration;

import apps.sarafrika.elimika.profile.internal.model.UserDocument;
import apps.sarafrika.elimika.profile.internal.model.UserSkill;
import apps.sarafrika.elimika.profile.spi.WalletVerificationStatus;
import apps.sarafrika.elimika.shared.enums.Gender;
import apps.sarafrika.elimika.shared.utils.enums.DocumentStatus;
import apps.sarafrika.elimika.shared.utils.enums.ProficiencyLevel;
import apps.sarafrika.elimika.tenancy.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import apps.sarafrika.elimika.shared.config.DatabaseAuditConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Writes every formerly PostgreSQL-enum column through JPA on a plain JDBC URL (no
 * {@code stringtype=unspecified}), the way staging connects. The converters bind varchar, so these
 * writes only succeed because the columns are varchar with CHECK constraints.
 */
// The container dies with this class; a cached context must not outlive it.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DataJpaTest
// The audit listener reads its EntityManager from a static field; wire it to THIS context, or it
// keeps the one a previous (now closed) test context left behind.
@Import(DatabaseAuditConfiguration.class)
@ImportAutoConfiguration(ValidationAutoConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@DisplayName("Enum-backed columns accept writes on a plain JDBC URL")
class EnumColumnWriteIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private DataSource dataSource;
    @Autowired private Environment environment;

    private UUID userUuid;
    private UUID instructorUuid;
    private UUID courseCreatorUuid;

    @BeforeEach
    void seed() {
        userUuid = UUID.randomUUID();
        jdbc.update("INSERT INTO users (uuid, user_no, first_name, last_name, email, created_by) "
                        + "VALUES (?, ?, 'Amina', 'Otieno', ?, 'test')",
                userUuid, String.format("%09d", Math.abs(userUuid.hashCode()) % 1000000000),
                userUuid + "@test.local");

        instructorUuid = UUID.randomUUID();
        jdbc.update("INSERT INTO instructors (uuid, user_uuid, full_name, created_by) VALUES (?, ?, 'ignored', 'test')",
                instructorUuid, userUuid);

        courseCreatorUuid = UUID.randomUUID();
        jdbc.update("INSERT INTO course_creators (uuid, user_uuid, full_name, created_by) "
                + "VALUES (?, ?, 'Amina Otieno', 'test')", courseCreatorUuid, userUuid);
    }

    @Test
    @DisplayName("the datasource connects without stringtype=unspecified")
    void datasourceUsesPlainJdbcUrl() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getURL()).doesNotContainIgnoringCase("stringtype");
        }
        assertThat(environment.getProperty("spring.datasource.hikari.data-source-properties.stringtype")).isNull();
    }

    @Test
    @DisplayName("a profile skill is inserted with its proficiency level and verification status")
    void userSkillInsert() {
        UserSkill skill = new UserSkill();
        skill.setUserUuid(userUuid);
        skill.setSkillName("Kubernetes");
        skill.setProficiencyLevel(ProficiencyLevel.ADVANCED);
        audit(skill);
        entityManager.persist(skill);
        entityManager.flush();
        entityManager.clear();

        assertThat(jdbc.queryForObject("SELECT proficiency_level FROM user_skills WHERE user_uuid = ?",
                String.class, userUuid)).isEqualTo("ADVANCED");
        UserSkill reloaded = entityManager.find(UserSkill.class, skill.getId());
        assertThat(reloaded.getProficiencyLevel()).isEqualTo(ProficiencyLevel.ADVANCED);
        assertThat(reloaded.getVerificationStatus()).isEqualTo(WalletVerificationStatus.PENDING);
    }

    @Test
    @DisplayName("a profile document status is updated")
    void userDocumentStatusUpdate() {
        UUID documentUuid = UUID.randomUUID();
        jdbc.update("INSERT INTO user_documents (uuid, user_uuid, document_type_uuid, original_filename, "
                        + "stored_filename, file_path, file_size_bytes, mime_type, title, created_by) "
                        + "VALUES (?, ?, (SELECT uuid FROM document_types ORDER BY id LIMIT 1), 'cv.pdf', 'cv.pdf', "
                        + "'docs/cv.pdf', 10, 'application/pdf', 'CV', 'test')",
                documentUuid, userUuid);
        assertThat(jdbc.queryForObject("SELECT status FROM user_documents WHERE uuid = ?", String.class,
                documentUuid)).isEqualTo("PENDING");

        UserDocument document = entityManager
                .createQuery("SELECT d FROM UserDocument d WHERE d.uuid = :uuid", UserDocument.class)
                .setParameter("uuid", documentUuid).getSingleResult();
        assertThat(document.getStatus()).isEqualTo(DocumentStatus.PENDING);
        document.setStatus(DocumentStatus.APPROVED);
        entityManager.flush();
        entityManager.clear();

        assertThat(jdbc.queryForObject("SELECT status FROM user_documents WHERE uuid = ?", String.class,
                documentUuid)).isEqualTo("APPROVED");
    }

    @Test
    @DisplayName("a user gender is updated")
    void userGenderUpdate() {
        User user = entityManager.createQuery("SELECT u FROM User u WHERE u.uuid = :uuid", User.class)
                .setParameter("uuid", userUuid).getSingleResult();
        user.setGender(Gender.PREFER_NOT_TO_SAY);
        entityManager.flush();
        entityManager.clear();

        assertThat(jdbc.queryForObject("SELECT gender FROM users WHERE uuid = ?", String.class, userUuid))
                .isEqualTo("PREFER_NOT_TO_SAY");
        User reloaded = entityManager.createQuery("SELECT u FROM User u WHERE u.uuid = :uuid", User.class)
                .setParameter("uuid", userUuid).getSingleResult();
        assertThat(reloaded.getGender()).isEqualTo(Gender.PREFER_NOT_TO_SAY);
    }

    private static void audit(apps.sarafrika.elimika.shared.model.BaseEntity entity) {
        entity.setCreatedBy("test");
        entity.setCreatedDate(LocalDateTime.now());
    }
}
