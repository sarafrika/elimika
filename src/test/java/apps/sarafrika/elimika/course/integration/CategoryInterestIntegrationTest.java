package apps.sarafrika.elimika.course.integration;

import apps.sarafrika.elimika.testsupport.SharedPostgres;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code GET/PUT /api/v1/users/me/interests/{domain}} over real HTTP against the Flyway schema, using the
 * onboarding categories the migrations seed.
 */
// Each class gets its own database; a cached context must not outlive it.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Category interests per domain (end-to-end)")
class CategoryInterestIntegrationTest {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        SharedPostgres.register(registry);
        // The resource server would otherwise try to fetch Keycloak's JWKS at boot.
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> "http://localhost/realms/test");
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", () -> "http://localhost/realms/test/certs");
        // Deployment-supplied placeholders the app declares with no default.
        registry.add("MAIL_SERVER", () -> "localhost");
        registry.add("MAIL_USERNAME", () -> "test");
        registry.add("MAIL_PASSWORD", () -> "test");
        registry.add("app.keycloak.admin.clientId", () -> "test-admin");
        registry.add("app.keycloak.admin.clientSecret", () -> "test-secret");
        registry.add("encryption.secret-key", () -> "0123456789abcdef0123456789abcdef");
        registry.add("encryption.salt", () -> "0123456789abcdef");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    private static final String SUBJECT = "keycloak-interests";
    private static final String PATH = "/api/v1/users/me/interests/";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    /** Never invoked: the jwt() post-processor sets the SecurityContext directly. */
    @MockitoBean private JwtDecoder jwtDecoder;

    private UUID userUuid;
    private UUID music;
    private UUID tech;

    @BeforeEach
    void seed() {
        jdbc.execute("TRUNCATE category_interests, user_domain_mapping, users RESTART IDENTITY CASCADE");
        userUuid = UUID.randomUUID();
        jdbc.update("INSERT INTO users (uuid, user_no, first_name, last_name, email, keycloak_id, dob, created_by) "
                + "VALUES (?, '100000001', 'Amina', 'Otieno', 'amina@test.local', ?, '1995-02-03', 'test')",
                userUuid, SUBJECT);
        music = category("Music & Performing Arts");
        tech = category("Technology & Digital Skills");
    }

    @Test
    @DisplayName("an account with no domains saves, reorders and clears its student interests")
    void savesReplacesAndClears() throws Exception {
        mockMvc.perform(get(PATH + "student").with(user()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.domain").value("student"))
                .andExpect(jsonPath("$.data.categories").isEmpty());

        mockMvc.perform(put(PATH + "student").with(user()).contentType(MediaType.APPLICATION_JSON)
                        .content(body(tech, music, tech)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categories.length()").value(2))
                .andExpect(jsonPath("$.data.categories[0].category_uuid").value(tech.toString()))
                .andExpect(jsonPath("$.data.categories[0].name").value("Technology & Digital Skills"))
                .andExpect(jsonPath("$.data.categories[1].category_uuid").value(music.toString()));

        mockMvc.perform(put(PATH + "STUDENT").with(user()).contentType(MediaType.APPLICATION_JSON).content(body(music)))
                .andExpect(status().isOk());
        mockMvc.perform(get(PATH + "student").with(user()))
                .andExpect(jsonPath("$.data.categories.length()").value(1))
                .andExpect(jsonPath("$.data.categories[0].name").value("Music & Performing Arts"));

        mockMvc.perform(put(PATH + "student").with(user()).contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categories").isEmpty());
        assertThat(rows()).isZero();
    }

    @Test
    @DisplayName("keeps a separate set for each domain")
    void domainsAreSeparate() throws Exception {
        mockMvc.perform(put(PATH + "student").with(user()).contentType(MediaType.APPLICATION_JSON).content(body(music)))
                .andExpect(status().isOk());
        mockMvc.perform(put(PATH + "instructor").with(user()).contentType(MediaType.APPLICATION_JSON).content(body(tech)))
                .andExpect(status().isOk());

        mockMvc.perform(get(PATH + "student").with(user()))
                .andExpect(jsonPath("$.data.categories[0].category_uuid").value(music.toString()));
        mockMvc.perform(get(PATH + "instructor").with(user()))
                .andExpect(jsonPath("$.data.categories[0].category_uuid").value(tech.toString()));
        assertThat(jdbc.queryForObject("SELECT string_agg(user_domain, ',' ORDER BY user_domain) FROM category_interests",
                String.class)).isEqualTo("instructor,student");
    }

    @Test
    @DisplayName("rejects unknown or inactive categories, nulls and domains without interests, keeping what was saved")
    void rejectsBadInput() throws Exception {
        mockMvc.perform(put(PATH + "student").with(user()).contentType(MediaType.APPLICATION_JSON).content(body(music)))
                .andExpect(status().isOk());

        mockMvc.perform(put(PATH + "student").with(user()).contentType(MediaType.APPLICATION_JSON)
                        .content(body(tech, UUID.randomUUID())))
                .andExpect(status().isBadRequest());
        jdbc.update("UPDATE course_categories SET is_active = false WHERE uuid = ?", tech);
        try {
            mockMvc.perform(put(PATH + "student").with(user()).contentType(MediaType.APPLICATION_JSON).content(body(tech)))
                    .andExpect(status().isBadRequest());
        } finally {
            jdbc.update("UPDATE course_categories SET is_active = true WHERE uuid = ?", tech);
        }
        mockMvc.perform(put(PATH + "student").with(user()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category_uuids\":[null,\"" + tech + "\"]}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put(PATH + "admin").with(user()).contentType(MediaType.APPLICATION_JSON).content(body(tech)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(PATH + "wizard").with(user())).andExpect(status().isBadRequest());
        mockMvc.perform(put(PATH + "student").with(user()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        assertThat(rows()).isEqualTo(1);
    }

    @Test
    @DisplayName("overlapping saves of the same set all succeed and leave one copy")
    void concurrentSavesDoNotCollide() throws Exception {
        String body = body(tech, music);
        ExecutorService pool = Executors.newFixedThreadPool(6);
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                results.add(pool.submit(() -> mockMvc.perform(put(PATH + "student").with(user())
                        .contentType(MediaType.APPLICATION_JSON).content(body)).andReturn().getResponse().getStatus()));
            }
            for (Future<Integer> result : results) {
                assertThat(result.get()).isEqualTo(200);
            }
        } finally {
            pool.shutdown();
        }
        assertThat(rows()).isEqualTo(2);
    }

    @Test
    @DisplayName("requires a signed-in account")
    void anonymousCallersAreRejected() throws Exception {
        mockMvc.perform(get(PATH + "student")).andExpect(status().isUnauthorized());
    }

    private Integer rows() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM category_interests", Integer.class);
    }

    private UUID category(String name) {
        return jdbc.queryForObject("SELECT uuid FROM course_categories WHERE name = ?", UUID.class, name);
    }

    private RequestPostProcessor user() {
        return jwt().jwt(builder -> builder.subject(SUBJECT).claim("sub", SUBJECT));
    }

    private static String body(UUID... categoryUuids) {
        return "{\"category_uuids\":[" + java.util.Arrays.stream(categoryUuids)
                .map(uuid -> "\"" + uuid + "\"").collect(java.util.stream.Collectors.joining(",")) + "]}";
    }
}
