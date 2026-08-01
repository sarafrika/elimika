package apps.sarafrika.elimika.payout.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import apps.sarafrika.elimika.course.spi.CourseInfoService;
import apps.sarafrika.elimika.course.spi.CourseInfoService.RevenueShare;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.payout.service.OrderCaptureWalletCreditListener;
import apps.sarafrika.elimika.shared.dto.commerce.CartItemResponse;
import apps.sarafrika.elimika.shared.dto.commerce.OrderResponse;
import apps.sarafrika.elimika.shared.event.commerce.OrderCompletedEvent;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService;
import apps.sarafrika.elimika.wallet.service.WalletService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Proves that a failed wallet credit is durably recorded and can be retried, against the real
 * Spring Modulith event publication registry and the real {@code event_publication} table.
 * <p>
 * This is the assertion that matters most for the money model, and it cannot be made with mocks:
 * it depends on the interaction between {@code @TransactionalEventListener}, Modulith's
 * {@code PersistentApplicationEventMulticaster} (which writes the publication row) and its
 * completion advisor (which marks the row complete only on a clean return). In particular it pins
 * two things that are easy to get silently wrong:
 * <ul>
 *     <li>{@code fallbackExecution = true} - {@code OrderCompletedEvent} is published outside any
 *     transaction, so without it the listener would never run at all.</li>
 *     <li>advisor ordering - the async proxy must sit outside the completion advisor, otherwise a
 *     publication would be marked complete before the credit has even been attempted.</li>
 * </ul>
 * {@code @Async} is wired to a {@link SyncTaskExecutor} here so the test is deterministic while
 * keeping the production topology: the exception is still handled by the async machinery rather
 * than escaping to the publisher.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(WalletCreditEventDurabilityIntegrationTest.TestConfig.class)
// No ambient transaction: this mirrors production, where OrderServiceImpl publishes the event
// outside any transaction.
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("Wallet credit event durability")
class WalletCreditEventDurabilityIntegrationTest {

    private static final String LISTENER_ID = OrderCaptureWalletCreditListener.class.getName();

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    /**
     * Pulls in the Modulith event publication machinery. The JPA and Jackson pieces are
     * package-private auto-configuration classes, so they are imported by name.
     */
    static class ModulithEventsSelector implements ImportSelector {
        @Override
        public String[] selectImports(AnnotationMetadata metadata) {
            return new String[] {
                    "org.springframework.modulith.events.jpa.JpaEventPublicationAutoConfiguration",
                    "org.springframework.modulith.events.config.EventPublicationAutoConfiguration",
                    "org.springframework.modulith.events.jackson.JacksonEventSerializationConfiguration"
            };
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAsync
    @Import({ModulithEventsSelector.class, OrderCaptureWalletCreditListener.class})
    static class TestConfig implements AsyncConfigurer {

        @Override
        public Executor getAsyncExecutor() {
            // Inline, but still routed through the async interceptor - so a listener failure is
            // absorbed exactly as it is in production instead of escaping to the publisher.
            return new SyncTaskExecutor();
        }

        @Bean
        ObjectMapper objectMapper() {
            // findAndAddModules() mirrors Boot: JavaTime for OffsetDateTime and ParameterNames so
            // the Lombok-built commerce DTOs can be read back when a publication is republished.
            return JsonMapper.builder().findAndAddModules().build();
        }
    }

    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @Autowired
    private IncompleteEventPublications incompleteEventPublications;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @MockitoBean
    private WalletService walletService;
    @MockitoBean
    private CourseInfoService courseInfoService;
    @MockitoBean
    private InstructorLookupService instructorLookupService;
    @MockitoBean
    private ClassDefinitionLookupService classDefinitionLookupService;

    private final UUID courseUuid = UUID.randomUUID();
    private final UUID creatorUserUuid = UUID.randomUUID();
    private String orderId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from event_publication");
        orderId = UUID.randomUUID().toString();
        when(courseInfoService.getCourseCreatorUserUuid(courseUuid)).thenReturn(Optional.of(creatorUserUuid));
        when(courseInfoService.getRevenueShare(courseUuid))
                .thenReturn(Optional.of(new RevenueShare(new BigDecimal("70"), new BigDecimal("20"))));
    }

    private OrderCompletedEvent capturedOrder() {
        CartItemResponse item = CartItemResponse.builder()
                .id("line-1")
                .title("Advanced Excel Course")
                .quantity(1)
                .variantId("variant-1")
                .unitPrice(new BigDecimal("1000.00"))
                .subtotal(new BigDecimal("1000.00"))
                .total(new BigDecimal("1000.00"))
                .metadata(Map.of("course_uuid", courseUuid.toString()))
                .build();
        OrderResponse order = OrderResponse.builder()
                .id(orderId)
                .displayId("100012")
                .paymentStatus("CAPTURED")
                .currencyCode("KES")
                .subtotal(new BigDecimal("1000.00"))
                .total(new BigDecimal("1000.00"))
                .createdAt(OffsetDateTime.parse("2026-01-15T10:00:00Z"))
                .items(List.of(item))
                .build();
        return new OrderCompletedEvent(order, null);
    }

    private long incompletePublications() {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from event_publication where listener_id like ? and completion_date is null",
                Long.class, LISTENER_ID + "%");
        return count == null ? 0 : count;
    }

    private long completedPublications() {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from event_publication where listener_id like ? and completion_date is not null",
                Long.class, LISTENER_ID + "%");
        return count == null ? 0 : count;
    }

    @Test
    @DisplayName("a successful credit completes its event publication")
    void successfulCreditCompletesThePublication() {
        when(walletService.creditSaleIdempotent(any(), any(), any(), any(), any())).thenReturn(true);

        eventPublisher.publishEvent(capturedOrder());

        verify(walletService).creditSaleIdempotent(
                any(), any(BigDecimal.class), any(), any(), any());
        assertThat(completedPublications()).isOne();
        assertThat(incompletePublications()).isZero();
    }

    @Test
    @DisplayName("a failed credit is durably recorded as an incomplete publication")
    void failedCreditLeavesAnIncompletePublication() {
        when(walletService.creditSaleIdempotent(any(), any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("wallet unavailable"));

        eventPublisher.publishEvent(capturedOrder());

        // The 700.00 owed to the creator is not lost in a log line - it is a row someone can find.
        assertThat(incompletePublications()).isOne();
        assertThat(completedPublications()).isZero();

        String serialized = jdbcTemplate.queryForObject(
                "select serialized_event from event_publication where completion_date is null",
                String.class);
        assertThat(serialized).contains(orderId);
    }

    @Test
    @DisplayName("the order still completes when the credit fails")
    void publisherIsUnaffectedByACreditFailure() {
        when(walletService.creditSaleIdempotent(any(), any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("wallet unavailable"));

        // Checkout availability: publishing must not blow up in the caller's face.
        assertThatCode(() -> eventPublisher.publishEvent(capturedOrder())).doesNotThrowAnyException();
        assertThat(incompletePublications()).isOne();
    }

    @Test
    @DisplayName("an incomplete publication is retried and pays the earner")
    void incompletePublicationCanBeResubmitted() {
        when(walletService.creditSaleIdempotent(any(), any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("wallet unavailable"))
                .thenReturn(true);

        eventPublisher.publishEvent(capturedOrder());
        assertThat(incompletePublications()).isOne();

        incompleteEventPublications.resubmitIncompletePublications(
                publication -> publication.getEvent() instanceof OrderCompletedEvent);

        // Second attempt succeeded, so the publication is settled and the creator has been paid.
        verify(walletService, times(2)).creditSaleIdempotent(any(), any(), any(), any(), any());
        assertThat(incompletePublications()).isZero();
        assertThat(completedPublications()).isOne();
    }

    @Test
    @DisplayName("a stored publication can be read back, so restart republication works")
    void storedPublicationDeserialisesBackIntoTheEvent() {
        when(walletService.creditSaleIdempotent(any(), any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("wallet unavailable"));

        eventPublisher.publishEvent(capturedOrder());

        // resubmitIncompletePublications has to deserialise the row to replay it; if the commerce
        // DTOs were not readable this would surface here rather than silently on the next restart.
        assertThatCode(() -> incompleteEventPublications.resubmitIncompletePublications(
                publication -> {
                    assertThat(publication.getEvent()).isInstanceOf(OrderCompletedEvent.class);
                    OrderCompletedEvent event = (OrderCompletedEvent) publication.getEvent();
                    assertThat(event.order().getId()).isEqualTo(orderId);
                    assertThat(event.order().getPaymentStatus()).isEqualTo("CAPTURED");
                    assertThat(event.order().getItems()).hasSize(1);
                    assertThat(event.order().getItems().get(0).getMetadata())
                            .containsEntry("course_uuid", courseUuid.toString());
                    return false;
                })).doesNotThrowAnyException();
    }
}
