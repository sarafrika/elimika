package apps.sarafrika.elimika.classes.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import apps.sarafrika.elimika.classes.dto.ClassDefinitionResponseDTO;
import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobDTO;
import apps.sarafrika.elimika.classes.search.ClassSearchDocument;
import apps.sarafrika.elimika.classes.search.ClassSearchSource;
import apps.sarafrika.elimika.shared.search.NearMe;
import apps.sarafrika.elimika.shared.search.SearchGeoPoint;
import apps.sarafrika.elimika.classes.internal.ClassListingVisibility;
import apps.sarafrika.elimika.classes.model.ClassDefinition;
import apps.sarafrika.elimika.classes.model.ClassMarketplaceJob;
import apps.sarafrika.elimika.classes.repository.ClassDefinitionRepository;
import apps.sarafrika.elimika.classes.repository.ClassMarketplaceJobRepository;
import apps.sarafrika.elimika.classes.service.ClassDefinitionServiceInterface;
import apps.sarafrika.elimika.classes.service.ClassMarketplaceJobServiceInterface;
import apps.sarafrika.elimika.classes.util.enums.ClassMarketplaceJobStatus;
import apps.sarafrika.elimika.shared.enums.ClassVisibility;
import apps.sarafrika.elimika.shared.enums.LocationType;
import apps.sarafrika.elimika.shared.enums.SessionFormat;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.utils.enums.RateBasis;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * The {@code classes} and {@code marketplace_jobs} indexes end to end: rows saved through JPA reach
 * Meilisearch through the entity triggers, and the listing services answer {@code q} from the index
 * under the same visibility rules as their SQL listings.
 * <p>
 * The caller is simulated by mocking the two per-request lookups the services read it from:
 * {@link ClassListingVisibility} for classes and {@link DomainSecurityService} for jobs.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest
@Testcontainers
@DisplayName("Classes and marketplace job search (end-to-end)")
class ClassesSearchIntegrationTest {

    private static final String IMAGE = "getmeili/meilisearch:v1.54.1";
    private static final String MASTER_KEY = "integration-test-master-key-0123456789";
    private static final LocalDateTime NOW = LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MINUTES);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Container
    static GenericContainer<?> meilisearch = new GenericContainer<>(DockerImageName.parse(IMAGE))
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
        registry.add("search.read-enabled.classes", () -> "true");
        registry.add("search.read-enabled.marketplace_jobs", () -> "true");
    }

    @MockBean private JwtDecoder jwtDecoder;
    @MockBean private ClassListingVisibility classListingVisibility;
    @MockBean private DomainSecurityService domainSecurityService;

    @Autowired private ClassDefinitionServiceInterface classService;
    @Autowired private ClassMarketplaceJobServiceInterface jobService;
    @Autowired private ClassDefinitionRepository classRepository;
    @Autowired private ClassMarketplaceJobRepository jobRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private ApplicationContext applicationContext;
    @Autowired private ClassSearchSource classSearchSource;

    /** An unrelated signed-in user: no staffed organisation, no instructor profile, no enrolments. */
    private static final ClassListingVisibility.Scope STRANGER =
            new ClassListingVisibility.Scope(false, Set.of(), null, Set.of());

    @Test
    @DisplayName("A misspelt title still finds a public active class")
    void typoFindsPublicClass() {
        ClassDefinition watercolour = saveClass("Watercolour Painting Basics", UUID.randomUUID(), ClassVisibility.PUBLIC);
        when(classListingVisibility.forCurrentCaller()).thenReturn(STRANGER);

        List<UUID> found = awaitNonEmpty(() -> classUuids("Watercolor Paintng"));

        assertThat(found).contains(watercolour.getUuid());
        assertThat(classService.searchActiveClasses("watercolr painting"))
                .extracting(dto -> dto.classDefinition().uuid())
                .contains(watercolour.getUuid());
    }

    @Test
    @DisplayName("A private class is hidden from an unrelated user and visible to its organisation's staff")
    void privateClassVisibleOnlyToStaff() {
        UUID organisation = UUID.randomUUID();
        ClassDefinition seminar = saveClass("Cryptography Seminar", organisation, ClassVisibility.PRIVATE);
        ClassListingVisibility.Scope staff = new ClassListingVisibility.Scope(false, Set.of(organisation), null, Set.of());

        when(classListingVisibility.forCurrentCaller()).thenReturn(staff);
        assertThat(awaitNonEmpty(() -> classUuids("cryptography"))).containsExactly(seminar.getUuid());
        assertThat(classService.searchClassesForOrganisation(organisation, "cryptography"))
                .extracting(dto -> dto.classDefinition().uuid())
                .containsExactly(seminar.getUuid());

        when(classListingVisibility.forCurrentCaller()).thenReturn(STRANGER);
        assertThat(classUuids("cryptography")).isEmpty();
        assertThat(classService.searchClassesForOrganisation(organisation, "cryptography")).isEmpty();
    }

    @Test
    @DisplayName("A job that is no longer open is hidden from an instructor and visible to the posting organisation's staff")
    void nonOpenJobVisibleOnlyToPostingStaff() {
        UUID organisation = UUID.randomUUID();
        ClassMarketplaceJob job = saveJob("Robotics Trainer Wanted", organisation, ClassMarketplaceJobStatus.FILLED, NOW.plusDays(10));

        when(domainSecurityService.staffsOrganisation(organisation)).thenReturn(true);
        assertThat(awaitNonEmpty(() -> jobUuids(organisation, "robotics"))).containsExactly(job.getUuid());

        when(domainSecurityService.staffsOrganisation(organisation)).thenReturn(false);
        assertThat(jobUuids(organisation, "robotics")).isEmpty();
        assertThat(jobUuids(null, "robotics")).isEmpty();
    }

    @Test
    @DisplayName("An expired job drops out of an instructor's results")
    void expiredJobLeavesInstructorResults() {
        ClassMarketplaceJob job = saveJob("Astronomy Night Course", UUID.randomUUID(), ClassMarketplaceJobStatus.OPEN,
                NOW.minusHours(1));
        assertThat(awaitNonEmpty(() -> jobUuids(null, "astronomy"))).containsExactly(job.getUuid());

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            skipForeignKeys();
            ReflectionTestUtils.invokeMethod(applicationContext.getBean("classMarketplaceJobExpiryScheduler"),
                    "expireLapsedJobs");
        });

        assertThat(jobRepository.findByUuid(job.getUuid()).orElseThrow().getStatus())
                .isEqualTo(ClassMarketplaceJobStatus.EXPIRED);
        awaitTrue(() -> jobUuids(null, "astronomy").isEmpty());
    }

    @Test
    @DisplayName("Listing filters sent with q are applied by the index")
    void indexAppliesFilters() {
        UUID organisation = UUID.randomUUID();
        ClassDefinition online = saveClass("Filtered Pottery Online", organisation, ClassVisibility.PUBLIC);
        ClassDefinition inPerson = saveClass("Filtered Pottery Studio", organisation, ClassVisibility.PUBLIC,
                LocationType.IN_PERSON, new BigDecimal("4000.00"));
        when(classListingVisibility.forCurrentCaller()).thenReturn(STRANGER);
        awaitTrue(() -> filteredUuids(Map.of("q", "pottery", "organisation_uuid", organisation.toString()))
                .containsAll(List.of(online.getUuid(), inPerson.getUuid())));

        // Enum values match in any case: the UI sends location_type=online.
        assertThat(filteredUuids(Map.of("q", "pottery",
                "organisation_uuid", organisation.toString(), "location_type", "online")))
                .containsExactly(online.getUuid());
        assertThat(filteredUuids(Map.of("q", "pottery", "organisation_uuid", organisation.toString(),
                "sale_price_gte", "2000")))
                .containsExactly(inPerson.getUuid());
        String startsFrom = NOW.plusDays(6).toLocalDate().toString();
        assertThat(filteredUuids(Map.of("q", "pottery", "organisation_uuid", organisation.toString(),
                "starts_at_gte", startsFrom)))
                .containsExactlyInAnyOrder(online.getUuid(), inPerson.getUuid());
    }

    @Test
    @DisplayName("A filter the classes index cannot express is a 400, not a database query")
    void unsupportedFilterIsRejected() {
        when(classListingVisibility.forCurrentCaller()).thenReturn(STRANGER);

        assertThatThrownBy(() -> filteredUuids(Map.of("q", "pottery", "title_like", "pottery")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("title_like");
    }

    // ===== Near me =====

    @Test
    @DisplayName("Only in-person and hybrid classes get a rounded _geo; an online class has none and is never near")
    void onlyInPersonClassesAreLocatable() {
        UUID organisation = UUID.randomUUID();
        ClassDefinition online = saveClass("Geo Ceramics Online", organisation, ClassVisibility.PUBLIC);
        setLocation(online, "-1.292066", "36.821946");
        ClassDefinition studio = saveClass("Geo Ceramics Studio", organisation, ClassVisibility.PUBLIC,
                LocationType.IN_PERSON, new BigDecimal("2000.00"));
        setLocation(studio, "-1.292066", "36.821946");
        when(classListingVisibility.forCurrentCaller()).thenReturn(STRANGER);

        Map<UUID, ClassSearchDocument> documents = new java.util.HashMap<>();
        classSearchSource.loadByUuids(List.of(online.getUuid(), studio.getUuid()))
                .forEach(document -> documents.put(document.uuid(), document));
        assertThat(documents.get(online.getUuid()).geo()).isNull();
        assertThat(documents.get(studio.getUuid()).geo()).isEqualTo(new SearchGeoPoint(-1.29, 36.82));
        assertThat(ClassSearchSource.DEFINITION.displayedAttributes()).doesNotContain("_geo").contains("title");

        NearMe near = NearMe.parse("-1.30,36.83", "5").orElseThrow();
        List<ClassDefinitionResponseDTO> found = awaitNonEmpty(() -> classService
                .searchClassesNear(null, near, Map.of(), PageRequest.of(0, 20)).getContent());
        assertThat(found).extracting(dto -> dto.classDefinition().uuid()).containsExactly(studio.getUuid());
        ClassDefinitionResponseDTO row = found.getFirst();
        assertThat(row.distanceBand()).isEqualTo("<2 km");
        assertThat(row.classDefinition().locationLatitude()).isEqualByComparingTo("-1.29");
        assertThat(row.classDefinition().locationLongitude()).isEqualByComparingTo("36.82");
        assertThat(row.classDefinition().locationLatitude().scale()).isLessThanOrEqualTo(2);

        // With q, and outside the radius.
        assertThat(classService.searchClassesNear("ceramics", near, Map.of(), PageRequest.of(0, 20)).getContent())
                .extracting(dto -> dto.classDefinition().uuid()).containsExactly(studio.getUuid());
        assertThat(classService.searchClassesNear(null, NearMe.parse("-1.29,38.20", "5").orElseThrow(), Map.of(),
                PageRequest.of(0, 20)).getContent()).isEmpty();
    }

    @Test
    @DisplayName("An in-person job is found near its coordinates with a band and rounded coordinates")
    void inPersonJobIsNear() {
        ClassMarketplaceJob job = saveJob("Geo Pottery Trainer", UUID.randomUUID(), ClassMarketplaceJobStatus.OPEN,
                NOW.plusDays(10));
        inTransaction(() -> {
            ClassMarketplaceJob row = jobRepository.findByUuid(job.getUuid()).orElseThrow();
            row.setLocationType(LocationType.HYBRID);
            row.setLocationLatitude(new BigDecimal("-1.292066"));
            row.setLocationLongitude(new BigDecimal("36.821946"));
            return jobRepository.saveAndFlush(row);
        });
        ClassMarketplaceJob online = saveJob("Geo Pottery Remote", UUID.randomUUID(), ClassMarketplaceJobStatus.OPEN,
                NOW.plusDays(10));

        NearMe near = NearMe.parse("-1.30,36.83", null).orElseThrow();
        List<ClassMarketplaceJobDTO> found = awaitNonEmpty(() -> jobService
                .searchJobsNear(null, null, null, null, null, "pottery", near, PageRequest.of(0, 20)).getContent());
        assertThat(found).extracting(ClassMarketplaceJobDTO::uuid).containsExactly(job.getUuid())
                .doesNotContain(online.getUuid());
        assertThat(found.getFirst().distanceBand()).isEqualTo("<2 km");
        assertThat(found.getFirst().locationLatitude()).isEqualByComparingTo("-1.29");
        assertThat(found.getFirst().locationLongitude()).isEqualByComparingTo("36.82");
    }

    private void setLocation(ClassDefinition definition, String latitude, String longitude) {
        inTransaction(() -> {
            ClassDefinition row = classRepository.findByUuid(definition.getUuid()).orElseThrow();
            row.setLocationName("Sarit Centre");
            row.setLocationLatitude(new BigDecimal(latitude));
            row.setLocationLongitude(new BigDecimal(longitude));
            return classRepository.saveAndFlush(row);
        });
    }

    // ===== Helpers =====

    private List<UUID> filteredUuids(Map<String, String> params) {
        Map<String, String> filters = new java.util.HashMap<>(params);
        String q = filters.remove("q");
        return classService.searchClasses(q, filters, PageRequest.of(0, 20)).getContent().stream()
                .map(dto -> dto.classDefinition().uuid())
                .toList();
    }

    private List<UUID> classUuids(String q) {
        return classService.searchClasses(q, Map.of("q", q), PageRequest.of(0, 20)).getContent().stream()
                .map(dto -> dto.classDefinition().uuid())
                .toList();
    }

    private List<UUID> jobUuids(UUID organisation, String q) {
        return jobService.searchJobs(organisation, null, null, null, null, q, PageRequest.of(0, 20)).getContent().stream()
                .map(ClassMarketplaceJobDTO::uuid)
                .toList();
    }

    private ClassDefinition saveClass(String title, UUID organisation, ClassVisibility visibility) {
        return saveClass(title, organisation, visibility, LocationType.ONLINE, new BigDecimal("1500.00"));
    }

    private ClassDefinition saveClass(String title, UUID organisation, ClassVisibility visibility,
                                      LocationType locationType, BigDecimal salePrice) {
        ClassDefinition definition = new ClassDefinition();
        definition.setTitle(title);
        definition.setDescription("About " + title);
        definition.setOrganisationUuid(organisation);
        definition.setDefaultInstructorUuid(UUID.randomUUID());
        definition.setDefaultStartTime(NOW.plusDays(7));
        definition.setDefaultEndTime(NOW.plusDays(7).plusHours(2));
        definition.setAcademicPeriodStartDate(NOW.toLocalDate().plusDays(7));
        definition.setAcademicPeriodEndDate(NOW.toLocalDate().plusDays(60));
        definition.setRegistrationPeriodStartDate(NOW.toLocalDate().minusDays(30));
        definition.setRegistrationPeriodEndDate(NOW.toLocalDate().plusDays(6));
        definition.setLocationType(locationType);
        definition.setMaxParticipants(20);
        definition.setAllowWaitlist(true);
        definition.setIsActive(true);
        definition.setSalePrice(salePrice);
        definition.setInstructorPay(new BigDecimal("900.00"));
        definition.setRateBasis(RateBasis.PER_HOUR);
        definition.setClassVisibility(visibility);
        definition.setSessionFormat(SessionFormat.GROUP);
        return inTransaction(() -> classRepository.saveAndFlush(definition));
    }

    private ClassMarketplaceJob saveJob(String title, UUID organisation, ClassMarketplaceJobStatus status, LocalDateTime start) {
        ClassMarketplaceJob job = new ClassMarketplaceJob();
        job.setOrganisationUuid(organisation);
        job.setCourseUuid(UUID.randomUUID());
        job.setTitle(title);
        job.setDescription("About " + title);
        job.setStatus(status);
        job.setClassVisibility(ClassVisibility.PUBLIC);
        job.setSessionFormat(SessionFormat.GROUP);
        job.setDefaultStartTime(start);
        job.setDefaultEndTime(start.plusHours(2));
        job.setRegistrationPeriodStartDate(NOW.toLocalDate().minusDays(30));
        job.setRegistrationPeriodEndDate(NOW.toLocalDate().plusDays(30));
        job.setLocationType(LocationType.ONLINE);
        job.setMaxParticipants(20);
        job.setAllowWaitlist(true);
        job.setRateBasis(RateBasis.PER_HOUR);
        return inTransaction(() -> jobRepository.saveAndFlush(job));
    }

    /** Rows reference organisations and courses these tests do not create. */
    private void skipForeignKeys() {
        entityManager.createNativeQuery("SET LOCAL session_replication_role = replica").executeUpdate();
    }

    private <T> T inTransaction(Supplier<T> work) {
        return new TransactionTemplate(transactionManager).execute(status -> {
            skipForeignKeys();
            return work.get();
        });
    }

    private static <T> List<T> awaitNonEmpty(Supplier<List<T>> query) {
        long deadline = System.nanoTime() + Duration.ofSeconds(20).toNanos();
        List<T> result = query.get();
        while (result.isEmpty() && System.nanoTime() < deadline) {
            pause();
            result = query.get();
        }
        return result;
    }

    private static void awaitTrue(BooleanSupplier condition) {
        long deadline = System.nanoTime() + Duration.ofSeconds(20).toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            pause();
        }
        throw new AssertionError("Condition not met within 20s");
    }

    private static void pause() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AssertionError(ex);
        }
    }
}
