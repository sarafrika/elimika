package apps.sarafrika.elimika.tenancy.integration;

import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.tenancy.dto.UserRecipientDTO;
import apps.sarafrika.elimika.tenancy.services.UserRecipientLookupService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs the user-number recipient lookup against real PostgreSQL: exact match only, active users only,
 * and a payload that carries no contact details.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(UserRecipientLookupService.class)
@DisplayName("User recipient lookup by user number")
class UserRecipientLookupIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @Autowired
    private UserRecipientLookupService lookupService;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("an exact user number resolves to the uuid and a masked name")
    void exactMatchResolves() throws Exception {
        UUID uuid = insertUser("Wilfred", "Njuguna", "481516234", true);

        UserRecipientDTO recipient = lookupService.lookupByUserNo("481516234");

        assertThat(recipient.userUuid()).isEqualTo(uuid);
        assertThat(recipient.displayName()).isEqualTo("Wilfred N.");
        String json = new ObjectMapper().writeValueAsString(recipient);
        assertThat(json).doesNotContain("email", "phone", "Njuguna", "@example.test", "+254");
        assertThat(new ObjectMapper().readTree(json).size()).isEqualTo(2);
    }

    @Test
    @DisplayName("a partial user number finds nothing")
    void partialValueIsNotFound() {
        insertUser("Partial", "Match", "271828182", true);

        assertThatThrownBy(() -> lookupService.lookupByUserNo("2718281")).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> lookupService.lookupByUserNo("271828")).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> lookupService.lookupByUserNo("%71828182")).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> lookupService.lookupByUserNo(" 271828182")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("an inactive user is not found")
    void inactiveUserIsNotFound() {
        insertUser("Dormant", "Account", "314159265", false);

        assertThatThrownBy(() -> lookupService.lookupByUserNo("314159265"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private UUID insertUser(String firstName, String lastName, String userNo, boolean active) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO users (uuid, first_name, last_name, email, phone_number, user_no, active, created_by) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, 'test')",
                uuid, firstName, lastName, "u" + uuid.toString().substring(0, 8) + "@example.test",
                "+2547" + userNo.substring(0, 8), userNo, active);
        return uuid;
    }
}
