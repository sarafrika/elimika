package apps.sarafrika.elimika.commerce.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A catalogue row carries enough to render a course card, to an anonymous browser.
 * <p>
 * Before this, a storefront got a course uuid and a price and had to fetch each course itself —
 * one request per row, against {@code GET /courses/{uuid}}, which requires a token. The public
 * catalogue therefore rendered empty for logged-out visitors while the landing page's own counters,
 * which never made that call, cheerfully reported the courses that were there.
 * <p>
 * The negative assertion is the one that matters most. A projection built for public consumption
 * must not carry the commercial terms behind the course: the minimum training fee and the revenue
 * split between creator and instructor are the owner's business, and the reason the course endpoint
 * was authenticated in the first place. Reading the raw response bytes is the only way to prove
 * they did not cross the wire.
 */
// The container dies with this class; a cached context must not outlive it.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Catalogue course projection")
class CatalogueCourseProjectionIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> "http://localhost/realms/test");
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", () -> "http://localhost/realms/test/certs");
        registry.add("MAIL_SERVER", () -> "localhost");
        registry.add("MAIL_USERNAME", () -> "test");
        registry.add("MAIL_PASSWORD", () -> "test");
        registry.add("app.keycloak.admin.clientId", () -> "test-admin");
        registry.add("app.keycloak.admin.clientSecret", () -> "test-secret");
        registry.add("encryption.secret-key", () -> "0123456789abcdef0123456789abcdef");
        registry.add("encryption.salt", () -> "0123456789abcdef");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    private static final String COURSE_NAME = "Piano Foundations for Beginners";
    private static final String CREATOR_NAME = "Grace Wanjiru";
    /** Commercially sensitive. If either string appears in the response, the projection leaks. */
    private static final String TRAINING_FEE = "2400.00";
    private static final String REVENUE_NOTES = "Creator keeps 70 percent of every seat sold";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    @MockBean private JwtDecoder jwtDecoder;

    @BeforeEach
    void seed() {
        jdbc.execute("TRUNCATE commerce_catalogue_item, course_category_mappings, course_categories, "
                + "courses, course_creators, users RESTART IDENTITY CASCADE");

        UUID creatorUser = UUID.randomUUID();
        jdbc.update("INSERT INTO users (uuid, user_no, first_name, last_name, email, created_by) "
                        + "VALUES (?, ?, 'Grace', 'Wanjiru', 'grace@test.local', 'test')",
                creatorUser, String.format("%09d", Math.abs(creatorUser.hashCode()) % 1000000000));
        UUID creator = UUID.randomUUID();
        jdbc.update("INSERT INTO course_creators (uuid, user_uuid, full_name, created_by) VALUES (?, ?, ?, 'test')",
                creator, creatorUser, CREATOR_NAME);

        UUID course = UUID.randomUUID();
        jdbc.update("INSERT INTO courses (uuid, name, description, course_creator_uuid, status, active, "
                        + "admin_approved, duration_hours, duration_minutes, price, minimum_training_fee, "
                        + "revenue_share_notes, thumbnail_url, created_by) "
                        + "VALUES (?, ?, 'Posture, hand position and your first scales.', ?, 'published', true, "
                        + "true, 12, 30, 6500.00, ?::numeric, ?, 'courses/piano.png', 'test')",
                course, COURSE_NAME, creator, TRAINING_FEE, REVENUE_NOTES);

        UUID category = UUID.randomUUID();
        jdbc.update("INSERT INTO course_categories (uuid, name, created_by) VALUES (?, 'Music', 'test')", category);
        jdbc.update("INSERT INTO course_category_mappings (uuid, course_uuid, category_uuid, created_by) "
                + "VALUES (?, ?, ?, 'test')", UUID.randomUUID(), course, category);

        // The catalogue row carries no amount of its own — the price comes from the product
        // variant, and the figure asserted below is the course's own list price.
        jdbc.update("INSERT INTO commerce_catalogue_item (uuid, course_uuid, product_code, variant_code, "
                + "currency_code, active, publicly_visible, created_by) "
                + "VALUES (?, ?, 'piano-foundations', 'default', 'KES', true, true, 'test')",
                UUID.randomUUID(), course);
    }

    @Test
    @DisplayName("An anonymous catalogue search returns enough to draw the card")
    void anonymousSearchCarriesTheCourse() throws Exception {
        mockMvc.perform(get("/api/v1/commerce/catalogue/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].course.name").value(COURSE_NAME))
                .andExpect(jsonPath("$.data.content[0].course.creator_name").value(CREATOR_NAME))
                .andExpect(jsonPath("$.data.content[0].course.duration_hours").value(12))
                .andExpect(jsonPath("$.data.content[0].course.duration_minutes").value(30))
                .andExpect(jsonPath("$.data.content[0].course.category_names[0]").value("Music"))
                // A resolved URL, not the bare storage key: next/image rejects a key with
                // "url parameter is invalid", which is what broke the home page thumbnails.
                .andExpect(jsonPath("$.data.content[0].course.thumbnail_url").value("/api/v1/files/courses/piano.png"))
                .andExpect(jsonPath("$.data.content[0].course.published").value(true))
                .andExpect(jsonPath("$.data.content[0].course.accepts_new_enrollments").value(true));
    }

    @Test
    @DisplayName("The projection carries no commercial terms")
    void commercialTermsDoNotCrossTheWire() throws Exception {
        String body = mockMvc.perform(get("/api/v1/commerce/catalogue/search"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(body)
                .as("the minimum training fee and the revenue split are why /courses/{uuid} is authenticated")
                .doesNotContain(TRAINING_FEE)
                .doesNotContain(REVENUE_NOTES)
                .doesNotContain("minimum_training_fee")
                .doesNotContain("creator_share_percentage")
                .doesNotContain("instructor_share_percentage")
                .doesNotContain("revenue_share_notes");

        // The control: the list price a shopper is quoted is public, and must still be there.
        assertThat(body).contains("6500.00");
    }
}
