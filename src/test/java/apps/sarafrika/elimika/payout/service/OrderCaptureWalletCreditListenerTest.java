package apps.sarafrika.elimika.payout.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import apps.sarafrika.elimika.course.spi.CourseInfoService;
import apps.sarafrika.elimika.course.spi.CourseInfoService.RevenueShare;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.shared.dto.commerce.CartItemResponse;
import apps.sarafrika.elimika.shared.dto.commerce.OrderResponse;
import apps.sarafrika.elimika.shared.event.commerce.OrderCompletedEvent;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService.ClassDefinitionSnapshot;
import apps.sarafrika.elimika.wallet.service.WalletService;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@ExtendWith(MockitoExtension.class)
class OrderCaptureWalletCreditListenerTest {

    @Mock
    private WalletService walletService;
    @Mock
    private CourseInfoService courseInfoService;
    @Mock
    private InstructorLookupService instructorLookupService;
    @Mock
    private ClassDefinitionLookupService classDefinitionLookupService;

    @InjectMocks
    private OrderCaptureWalletCreditListener listener;

    private static final String ORDER_ID = "11111111-1111-1111-1111-111111111111";

    private OrderResponse order(String status, CartItemResponse... items) {
        return OrderResponse.builder()
                .id(ORDER_ID)
                .paymentStatus(status)
                .currencyCode("KES")
                .total(new BigDecimal("1000.00"))
                .items(List.of(items))
                .build();
    }

    private CartItemResponse item(String id, BigDecimal total, Map<String, Object> metadata) {
        return CartItemResponse.builder()
                .id(id)
                .total(total)
                .metadata(metadata)
                .build();
    }

    @Test
    void creditsCourseCreatorShareForCourseScope() {
        UUID courseUuid = UUID.randomUUID();
        UUID creatorUserUuid = UUID.randomUUID();
        CartItemResponse item = item("line-1", new BigDecimal("1000.00"),
                Map.of("course_uuid", courseUuid.toString()));

        when(courseInfoService.getCourseCreatorUserUuid(courseUuid))
                .thenReturn(Optional.of(creatorUserUuid));
        when(courseInfoService.getRevenueShare(courseUuid))
                .thenReturn(Optional.of(new RevenueShare(new BigDecimal("70"), new BigDecimal("20"))));
        when(walletService.creditSaleIdempotent(any(), any(), any(), any(), any())).thenReturn(true);

        listener.handleOrderCompleted(new OrderCompletedEvent(order("CAPTURED", item), null));

        // 1000 * 70% = 700.00 credited to the creator's wallet
        verify(walletService).creditSaleIdempotent(
                eq(creatorUserUuid), eq(new BigDecimal("700.00")), eq("KES"),
                eq(ORDER_ID + ":line-1"), any());
    }

    @Test
    void creditsInstructorShareForClassScope() {
        UUID classUuid = UUID.randomUUID();
        UUID courseUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();
        UUID instructorUserUuid = UUID.randomUUID();
        CartItemResponse item = item("line-1", new BigDecimal("1000.00"),
                Map.of("class_definition_uuid", classUuid.toString()));

        when(classDefinitionLookupService.findByUuid(classUuid)).thenReturn(Optional.of(
                new ClassDefinitionSnapshot(classUuid, courseUuid, null, "T", null,
                        null, null, null, null, null, null)));
        when(classDefinitionLookupService.findDefaultInstructorUuid(classUuid))
                .thenReturn(Optional.of(instructorUuid));
        when(instructorLookupService.getInstructorUserUuid(instructorUuid))
                .thenReturn(Optional.of(instructorUserUuid));
        when(courseInfoService.getRevenueShare(courseUuid))
                .thenReturn(Optional.of(new RevenueShare(new BigDecimal("70"), new BigDecimal("20"))));
        when(walletService.creditSaleIdempotent(any(), any(), any(), any(), any())).thenReturn(true);

        listener.handleOrderCompleted(new OrderCompletedEvent(order("CAPTURED", item), null));

        // 1000 * 20% = 200.00 credited to the instructor's wallet
        verify(walletService).creditSaleIdempotent(
                eq(instructorUserUuid), eq(new BigDecimal("200.00")), eq("KES"),
                eq(ORDER_ID + ":line-1"), any());
    }

    @Test
    void doesNotCreditWhenOrderNotCaptured() {
        CartItemResponse item = item("line-1", new BigDecimal("1000.00"),
                Map.of("course_uuid", UUID.randomUUID().toString()));

        listener.handleOrderCompleted(new OrderCompletedEvent(order("AWAITING_PAYMENT", item), null));

        verify(walletService, never()).creditSaleIdempotent(any(), any(), any(), any(), any());
    }

    @Test
    void idempotencyIsDelegatedToWalletServiceByReference() {
        UUID courseUuid = UUID.randomUUID();
        UUID creatorUserUuid = UUID.randomUUID();
        CartItemResponse item = item("line-1", new BigDecimal("1000.00"),
                Map.of("course_uuid", courseUuid.toString()));

        when(courseInfoService.getCourseCreatorUserUuid(courseUuid))
                .thenReturn(Optional.of(creatorUserUuid));
        when(courseInfoService.getRevenueShare(courseUuid))
                .thenReturn(Optional.of(new RevenueShare(new BigDecimal("70"), new BigDecimal("20"))));
        // First delivery credits, replayed delivery is skipped by the wallet's reference guard
        when(walletService.creditSaleIdempotent(any(), any(), any(), any(), any()))
                .thenReturn(true)
                .thenReturn(false);

        OrderCompletedEvent event = new OrderCompletedEvent(order("CAPTURED", item), null);
        listener.handleOrderCompleted(event);
        listener.handleOrderCompleted(event);

        // Both deliveries call the idempotent credit with the SAME reference; the wallet guard
        // ensures only the first actually credits.
        verify(walletService, org.mockito.Mockito.times(2)).creditSaleIdempotent(
                eq(creatorUserUuid), eq(new BigDecimal("700.00")), eq("KES"),
                eq(ORDER_ID + ":line-1"), any());
    }

    @Test
    void classScopeCreditsOnlyTheInstructorAndNeverTheCreator() {
        UUID classUuid = UUID.randomUUID();
        UUID courseUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();
        UUID instructorUserUuid = UUID.randomUUID();
        CartItemResponse item = item("line-1", new BigDecimal("1000.00"),
                Map.of("class_definition_uuid", classUuid.toString()));

        when(classDefinitionLookupService.findByUuid(classUuid)).thenReturn(Optional.of(
                new ClassDefinitionSnapshot(classUuid, courseUuid, null, "T", null,
                        null, null, null, null, null, null)));
        when(classDefinitionLookupService.findDefaultInstructorUuid(classUuid))
                .thenReturn(Optional.of(instructorUuid));
        when(instructorLookupService.getInstructorUserUuid(instructorUuid))
                .thenReturn(Optional.of(instructorUserUuid));
        when(courseInfoService.getRevenueShare(courseUuid))
                .thenReturn(Optional.of(new RevenueShare(new BigDecimal("70"), new BigDecimal("20"))));
        when(walletService.creditSaleIdempotent(any(), any(), any(), any(), any())).thenReturn(true);

        listener.handleOrderCompleted(new OrderCompletedEvent(order("CAPTURED", item), null));

        // The creator earns nothing on a class sale - the creator is never even resolved. This is
        // the ground truth the revenue dashboard has to mirror.
        verify(courseInfoService, never()).getCourseCreatorUserUuid(any());
        verify(walletService, org.mockito.Mockito.times(1))
                .creditSaleIdempotent(any(), any(), any(), any(), any());
        verify(walletService).creditSaleIdempotent(
                eq(instructorUserUuid), eq(new BigDecimal("200.00")), eq("KES"),
                eq(ORDER_ID + ":line-1"), any());
    }

    @Test
    void creditsNobodyWhenTheCourseHasNoConfiguredRevenueShare() {
        UUID courseUuid = UUID.randomUUID();
        CartItemResponse item = item("line-1", new BigDecimal("1000.00"),
                Map.of("course_uuid", courseUuid.toString()));

        when(courseInfoService.getCourseCreatorUserUuid(courseUuid))
                .thenReturn(Optional.of(UUID.randomUUID()));
        when(courseInfoService.getRevenueShare(courseUuid)).thenReturn(Optional.empty());

        listener.handleOrderCompleted(new OrderCompletedEvent(order("CAPTURED", item), null));

        // A missing split credits nothing, so the dashboard must report zero rather than 100%.
        verify(walletService, never()).creditSaleIdempotent(any(), any(), any(), any(), any());
    }

    @Test
    void failedCreditIsRethrownSoTheEventPublicationIsLeftIncomplete() {
        UUID courseUuid = UUID.randomUUID();
        UUID creatorUserUuid = UUID.randomUUID();
        CartItemResponse item = item("line-1", new BigDecimal("1000.00"),
                Map.of("course_uuid", courseUuid.toString()));

        when(courseInfoService.getCourseCreatorUserUuid(courseUuid))
                .thenReturn(Optional.of(creatorUserUuid));
        when(courseInfoService.getRevenueShare(courseUuid))
                .thenReturn(Optional.of(new RevenueShare(new BigDecimal("70"), new BigDecimal("20"))));
        when(walletService.creditSaleIdempotent(any(), any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("wallet unavailable"));

        OrderCompletedEvent event = new OrderCompletedEvent(order("CAPTURED", item), null);

        // Swallowing here would lose 700.00 owed to a real person into a log line. Rethrowing is
        // what leaves the Modulith event publication incomplete so it can be retried.
        assertThatThrownBy(() -> listener.handleOrderCompleted(event))
                .isInstanceOf(WalletCreditFailedException.class)
                .hasMessageContaining(ORDER_ID)
                .hasMessageContaining("line-1")
                .hasRootCauseMessage("wallet unavailable");
    }

    @Test
    void oneFailingItemDoesNotStarveTheOtherEarnersOnTheSameOrder() {
        UUID firstCourse = UUID.randomUUID();
        UUID secondCourse = UUID.randomUUID();
        UUID firstCreator = UUID.randomUUID();
        UUID secondCreator = UUID.randomUUID();
        CartItemResponse failing = item("line-1", new BigDecimal("1000.00"),
                Map.of("course_uuid", firstCourse.toString()));
        CartItemResponse succeeding = item("line-2", new BigDecimal("500.00"),
                Map.of("course_uuid", secondCourse.toString()));

        when(courseInfoService.getCourseCreatorUserUuid(firstCourse)).thenReturn(Optional.of(firstCreator));
        when(courseInfoService.getCourseCreatorUserUuid(secondCourse)).thenReturn(Optional.of(secondCreator));
        when(courseInfoService.getRevenueShare(any()))
                .thenReturn(Optional.of(new RevenueShare(new BigDecimal("70"), new BigDecimal("20"))));
        when(walletService.creditSaleIdempotent(eq(firstCreator), any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("wallet unavailable"));
        when(walletService.creditSaleIdempotent(eq(secondCreator), any(), any(), any(), any()))
                .thenReturn(true);

        OrderCompletedEvent event = new OrderCompletedEvent(order("CAPTURED", failing, succeeding), null);

        assertThatThrownBy(() -> listener.handleOrderCompleted(event))
                .isInstanceOf(WalletCreditFailedException.class);

        // The second earner is paid even though the first item blew up; the retry re-runs the whole
        // order and the idempotent reference skips this already-applied credit.
        verify(walletService).creditSaleIdempotent(
                eq(secondCreator), eq(new BigDecimal("350.00")), eq("KES"),
                eq(ORDER_ID + ":line-2"), any());
    }

    @Test
    void listenerIsWiredForPersistentRetryableDelivery() throws Exception {
        Method method = OrderCaptureWalletCreditListener.class
                .getMethod("handleOrderCompleted", OrderCompletedEvent.class);

        TransactionalEventListener listener = method.getAnnotation(TransactionalEventListener.class);
        assertThat(listener)
                .as("@TransactionalEventListener is what makes Modulith persist an event_publication row")
                .isNotNull();
        assertThat(listener.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
        assertThat(listener.fallbackExecution())
                .as("OrderCompletedEvent is published outside any transaction (OrderServiceImpl and "
                        + "OrderPaymentServiceImpl are not @Transactional); without fallbackExecution "
                        + "the listener would never run and nothing would ever be credited")
                .isTrue();

        assertThat(method.getAnnotation(Async.class))
                .as("crediting must run off the checkout thread so a credit failure cannot fail the order")
                .isNotNull();

        Transactional transactional = method.getAnnotation(Transactional.class);
        assertThat(transactional).isNotNull();
        assertThat(transactional.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
    }
}
