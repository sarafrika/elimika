package apps.sarafrika.elimika.skills.integration;

import apps.sarafrika.elimika.classes.model.ClassMarketplaceJob;
import apps.sarafrika.elimika.classes.repository.ClassMarketplaceJobRepository;
import apps.sarafrika.elimika.classes.util.enums.ClassMarketplaceJobStatus;
import apps.sarafrika.elimika.instructor.dto.InstructorSkillDTO;
import apps.sarafrika.elimika.instructor.service.InstructorSkillService;
import apps.sarafrika.elimika.shared.enums.ClassVisibility;
import apps.sarafrika.elimika.shared.enums.LocationType;
import apps.sarafrika.elimika.shared.enums.SessionFormat;
import apps.sarafrika.elimika.shared.utils.enums.ProficiencyLevel;
import apps.sarafrika.elimika.shared.utils.enums.RateBasis;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Skills taxonomy and tagging end to end: the admin taxonomy and picker, owner-only course tags,
 * organisation-managed job tags with inheritance from the course, instructor skill linking, and the
 * search documents that carry the tags.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Skills taxonomy and tagging (end-to-end)")
class SkillTaggingIntegrationTest {

    private static final String MASTER_KEY = "integration-test-master-key-0123456789";
    private static final LocalDateTime NOW = LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MINUTES);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Container
    static GenericContainer<?> meilisearch = new GenericContainer<>(DockerImageName.parse("getmeili/meilisearch:v1.54.1"))
            .withEnv("MEILI_MASTER_KEY", MASTER_KEY)
            .withEnv("MEILI_NO_ANALYTICS", "true")
            .withExposedPorts(7700)
            .waitingFor(Wait.forHttp("/health").forStatusCode(200));

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

        registry.add("search.enabled", () -> "true");
        registry.add("search.meilisearch.host",
                () -> "http://" + meilisearch.getHost() + ":" + meilisearch.getMappedPort(7700));
        registry.add("search.meilisearch.api-key", () -> MASTER_KEY);
        registry.add("search.read-enabled.marketplace_jobs", () -> "true");
        registry.add("search.read-enabled.courses", () -> "true");
    }

    @MockBean private JwtDecoder jwtDecoder;

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ClassMarketplaceJobRepository jobRepository;
    @Autowired private InstructorSkillService instructorSkillService;
    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactionManager;

    private String ownerSubject;
    private String otherCreatorSubject;
    private String adminSubject;
    private String managerSubject;
    private String outsiderSubject;
    private UUID courseUuid;
    private UUID organisationUuid;

    @BeforeEach
    void seed() {
        String run = UUID.randomUUID().toString().substring(0, 8);
        ownerSubject = "owner-" + run;
        otherCreatorSubject = "other-creator-" + run;
        adminSubject = "admin-" + run;
        managerSubject = "manager-" + run;
        outsiderSubject = "outsider-" + run;

        UUID ownerUser = user(ownerSubject);
        grantDomain(ownerUser, "course_creator");
        courseUuid = course("Cloud Operations " + run, courseCreator(ownerUser));

        UUID otherUser = user(otherCreatorSubject);
        grantDomain(otherUser, "course_creator");
        courseCreator(otherUser);

        grantDomain(user(adminSubject), "admin");

        organisationUuid = organisation("Skills Org " + run);
        UUID managerUser = user(managerSubject);
        membership(managerUser, organisationUuid, "organisation_user");

        grantDomain(user(outsiderSubject), "student");
    }

    // ===== Taxonomy =====

    @Test
    @DisplayName("Only platform admins curate the taxonomy; everyone signed in lists active skills, matched in memory")
    void taxonomyIsAdminCuratedAndPublicListingIsActiveOnly() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        mockMvc.perform(post("/api/v1/admin/skills").with(jwt(outsiderSubject))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Forbidden " + suffix + "\"}"))
                .andExpect(status().isForbidden());

        UUID active = createSkill("Quantum Knitting " + suffix, "[\"qknit" + suffix + "\"]");
        UUID retired = createSkill("Quantum Weaving " + suffix, "[]");
        mockMvc.perform(put("/api/v1/admin/skills/" + retired).with(jwt(adminSubject))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Quantum Weaving " + suffix + "\",\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));

        // The slug is taken: 409.
        mockMvc.perform(post("/api/v1/admin/skills").with(jwt(adminSubject))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"quantum-knitting " + suffix + "\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/v1/skills").param("q", "quantum").with(jwt(outsiderSubject)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].uuid").value(org.hamcrest.Matchers.hasItem(active.toString())))
                .andExpect(jsonPath("$.data[*].uuid").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem(retired.toString()))));
        // An alias matches too.
        mockMvc.perform(get("/api/v1/skills").param("q", "qknit" + suffix).with(jwt(outsiderSubject)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].uuid").value(active.toString()));
    }

    // ===== Course tags =====

    @Test
    @DisplayName("Only the course's owner may tag it; anyone who can read the course reads the tags")
    void courseTaggingIsOwnerOnly() throws Exception {
        UUID skill = createSkill("Container Scheduling " + UUID.randomUUID().toString().substring(0, 6), "[]");
        String body = "{\"skills\":[{\"skill_uuid\":\"" + skill + "\",\"level\":\"ADVANCED\",\"weight\":4}]}";

        mockMvc.perform(put(courseSkillsUrl()).with(jwt(otherCreatorSubject))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mockMvc.perform(put(courseSkillsUrl()).with(jwt(outsiderSubject))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());

        mockMvc.perform(put(courseSkillsUrl()).with(jwt(ownerSubject))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].skill_uuid").value(skill.toString()))
                .andExpect(jsonPath("$.data[0].level").value("advanced"))
                .andExpect(jsonPath("$.data[0].weight").value(4));

        mockMvc.perform(get(courseSkillsUrl()).with(jwt(outsiderSubject)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].skill_uuid").value(skill.toString()));

        // An unknown skill is a 400, not a silent drop.
        mockMvc.perform(put(courseSkillsUrl()).with(jwt(ownerSubject)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skills\":[{\"skill_uuid\":\"" + UUID.randomUUID() + "\"}]}"))
                .andExpect(status().isBadRequest());
    }

    // ===== Job tags and inheritance =====

    @Test
    @DisplayName("Job tags are for the posting organisation's managers and admins; an untagged job inherits its course's skills")
    void jobTagsInheritFromTheCourse() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        UUID courseSkill = createSkill("Service Meshing " + suffix, "[]");
        UUID jobSkill = createSkill("Incident Paging " + suffix, "[]");
        tagCourse(courseSkill);
        ClassMarketplaceJob job = saveJob("Cloud trainer " + suffix, courseUuid);
        String url = "/api/v1/classes/jobs/" + job.getUuid() + "/required-skills";

        mockMvc.perform(get(url).with(jwt(outsiderSubject))).andExpect(status().isForbidden());
        mockMvc.perform(put(url).with(jwt(outsiderSubject)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skills\":[]}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(url).with(jwt(managerSubject)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.inherited").value(true))
                .andExpect(jsonPath("$.data.inherited_from_course_uuid").value(courseUuid.toString()))
                .andExpect(jsonPath("$.data.skills[0].skill_uuid").value(courseSkill.toString()))
                .andExpect(jsonPath("$.data.skills[0].inherited").value(true));

        mockMvc.perform(put(url).with(jwt(managerSubject)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skills\":[{\"skill_uuid\":\"" + jobSkill + "\",\"min_proficiency\":\"INTERMEDIATE\","
                                + "\"is_mandatory\":false}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.inherited").value(false))
                .andExpect(jsonPath("$.data.skills.length()").value(1))
                .andExpect(jsonPath("$.data.skills[0].skill_uuid").value(jobSkill.toString()))
                .andExpect(jsonPath("$.data.skills[0].min_proficiency").value("intermediate"))
                .andExpect(jsonPath("$.data.skills[0].is_mandatory").value(false));

        // Clearing the job's own tags brings the course's back; admins may read.
        mockMvc.perform(put(url).with(jwt(managerSubject)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skills\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.inherited").value(true));
        mockMvc.perform(get(url).with(jwt(adminSubject)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.skills[0].skill_uuid").value(courseSkill.toString()));
    }

    // ===== Search =====

    @Test
    @DisplayName("A skill name finds a job tagged with it, and a job that inherits it from its course")
    void searchBySkillNameFindsTaggedAndInheritingJobs() throws Exception {
        String word = "zorblax" + UUID.randomUUID().toString().substring(0, 4).replaceAll("[0-9]", "q");
        String inheritedWord = "quendrix" + UUID.randomUUID().toString().substring(0, 4).replaceAll("[0-9]", "q");
        UUID ownSkill = createSkill(word + " Automation", "[]");
        UUID courseSkill = createSkill(inheritedWord + " Modelling", "[]");

        ClassMarketplaceJob tagged = saveJob("Evening trainer", UUID.randomUUID());
        mockMvc.perform(put("/api/v1/classes/jobs/" + tagged.getUuid() + "/required-skills").with(jwt(managerSubject))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skills\":[{\"skill_uuid\":\"" + ownSkill + "\"}]}"))
                .andExpect(status().isOk());
        awaitJobs(word, tagged.getUuid());

        // The job exists before the course is tagged: the course's event re-indexes it.
        ClassMarketplaceJob inheriting = saveJob("Weekend trainer", courseUuid);
        tagCourse(courseSkill);
        awaitJobs(inheritedWord, inheriting.getUuid());

        // The courses index carries the tag as a filterable skill_uuids.
        awaitCourse(courseSkill);
    }

    // ===== Instructor skills =====

    @Test
    @DisplayName("An instructor skill whose name resolves by slug or alias is linked; free text stays free text")
    void instructorSkillsLinkToTheTaxonomy() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        UUID skill = createSkill("Soil Science " + suffix, "[\"Pedology " + suffix + "\"]");
        UUID instructor = UUID.randomUUID();
        jdbc.update("INSERT INTO instructors (uuid, user_uuid, full_name, created_by) VALUES (?, ?, 'Wanjiru Test', 'test')",
                instructor, user("instructor-" + suffix));

        SecurityContextHolder.clearContext();
        InstructorSkillDTO bySlug = asSystem(() -> instructorSkillService.createInstructorSkill(new InstructorSkillDTO(
                null, instructor, "  soil-SCIENCE " + suffix + "!", null, ProficiencyLevel.EXPERT, null, null, null, null)));
        InstructorSkillDTO byAlias = asSystem(() -> instructorSkillService.createInstructorSkill(new InstructorSkillDTO(
                null, instructor, "pedology " + suffix, null, ProficiencyLevel.BEGINNER, null, null, null, null)));
        InstructorSkillDTO free = asSystem(() -> instructorSkillService.createInstructorSkill(new InstructorSkillDTO(
                null, instructor, "Unlisted craft " + suffix, null, ProficiencyLevel.BEGINNER, null, null, null, null)));

        assertThat(bySlug.skillUuid()).isEqualTo(skill);
        assertThat(byAlias.skillUuid()).isEqualTo(skill);
        assertThat(free.skillUuid()).isNull();
        assertThat(free.skillName()).isEqualTo("Unlisted craft " + suffix);
    }

    // ===== Helpers =====

    private UUID createSkill(String name, String aliasesJson) throws Exception {
        String response = mockMvc.perform(post("/api/v1/admin/skills").with(jwt(adminSubject))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"aliases\":" + aliasesJson + "}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(response, "$.data.uuid"));
    }

    private void tagCourse(UUID skill) throws Exception {
        mockMvc.perform(put(courseSkillsUrl()).with(jwt(ownerSubject)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skills\":[{\"skill_uuid\":\"" + skill + "\"}]}"))
                .andExpect(status().isOk());
    }

    private String courseSkillsUrl() {
        return "/api/v1/courses/" + courseUuid + "/skills";
    }

    private void awaitJobs(String q, UUID expected) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(20).toNanos();
        while (true) {
            String body = mockMvc.perform(get("/api/v1/classes/jobs").param("q", q).with(jwt(outsiderSubject)))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            List<String> uuids = JsonPath.read(body, "$.data.content[*].uuid");
            if (uuids.contains(expected.toString())) {
                return;
            }
            if (System.nanoTime() > deadline) {
                throw new AssertionError("Job " + expected + " not found for q=" + q + ": " + body);
            }
            Thread.sleep(200);
        }
    }

    private void awaitCourse(UUID skill) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(20).toNanos();
        while (true) {
            String body = mockMvc.perform(get("/api/v1/search/courses").param("skill_uuids", skill.toString())
                            .with(jwt(adminSubject)))
                    .andReturn().getResponse().getContentAsString();
            if (body.contains(courseUuid.toString())) {
                return;
            }
            if (System.nanoTime() > deadline) {
                throw new AssertionError("Course " + courseUuid + " not found by skill_uuids: " + body);
            }
            Thread.sleep(200);
        }
    }

    private ClassMarketplaceJob saveJob(String title, UUID course) {
        ClassMarketplaceJob job = new ClassMarketplaceJob();
        job.setOrganisationUuid(organisationUuid);
        job.setCourseUuid(course);
        job.setTitle(title);
        job.setDescription("About " + title);
        job.setStatus(ClassMarketplaceJobStatus.OPEN);
        job.setClassVisibility(ClassVisibility.PUBLIC);
        job.setSessionFormat(SessionFormat.GROUP);
        job.setDefaultStartTime(NOW.plusDays(10));
        job.setDefaultEndTime(NOW.plusDays(10).plusHours(2));
        job.setRegistrationPeriodStartDate(NOW.toLocalDate().minusDays(30));
        job.setRegistrationPeriodEndDate(NOW.toLocalDate().plusDays(30));
        job.setLocationType(LocationType.ONLINE);
        job.setMaxParticipants(20);
        job.setAllowWaitlist(true);
        job.setRateBasis(RateBasis.PER_HOUR);
        return asSystem(() -> {
            // The job may name a course this test did not create.
            entityManager.createNativeQuery("SET LOCAL session_replication_role = replica").executeUpdate();
            return jobRepository.saveAndFlush(job);
        });
    }

    private <T> T asSystem(java.util.function.Supplier<T> work) {
        return new TransactionTemplate(transactionManager).execute(status -> work.get());
    }

    private RequestPostProcessor jwt(String subject) {
        return SecurityMockMvcRequestPostProcessors.jwt().jwt(builder -> builder.subject(subject).claim("sub", subject));
    }

    private UUID user(String keycloakId) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO users (uuid, user_no, first_name, last_name, email, keycloak_id, created_by) "
                        + "VALUES (?, ?, 'Test', 'User', ?, ?, 'test')",
                uuid, String.format("%09d", Math.abs(uuid.hashCode()) % 1000000000), keycloakId + "@test.local", keycloakId);
        return uuid;
    }

    private void grantDomain(UUID userUuid, String domainName) {
        jdbc.update("INSERT INTO user_domain_mapping (user_uuid, domain_uuid) "
                + "VALUES (?, (SELECT uuid FROM user_domain WHERE domain_name = ?))", userUuid, domainName);
    }

    private UUID courseCreator(UUID userUuid) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO course_creators (uuid, user_uuid, full_name, created_by) "
                + "VALUES (?, ?, 'Course Creator', 'test')", uuid, userUuid);
        return uuid;
    }

    private UUID course(String name, UUID courseCreatorUuid) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO courses (uuid, name, course_creator_uuid, status, active, admin_approved, created_by) "
                + "VALUES (?, ?, ?, 'published', true, true, 'test')", uuid, name, courseCreatorUuid);
        return uuid;
    }

    private UUID organisation(String name) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO organisation (uuid, name, location, country, lat, long, created_by) "
                + "VALUES (?, ?, 'Westlands', 'Kenya', -1.264400, 36.803400, 'test')", uuid, name);
        return uuid;
    }

    private void membership(UUID userUuid, UUID organisation, String domainName) {
        jdbc.update("INSERT INTO user_organisation_domain_mapping "
                        + "(uuid, user_uuid, organisation_uuid, domain_uuid, active, created_by) "
                        + "VALUES (?, ?, ?, (SELECT uuid FROM user_domain WHERE domain_name = ?), true, 'test')",
                UUID.randomUUID(), userUuid, organisation, domainName);
    }
}
