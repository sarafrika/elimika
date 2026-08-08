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
import apps.sarafrika.elimika.shared.dto.commerce.PlatformFeeBreakdown;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService.ClassDefinitionSnapshot;
import apps.sarafrika.elimika.shared.spi.revenue.PurchaseLineSettlement;
import apps.sarafrika.elimika.shared.spi.revenue.PurchaseSettlementRecorder;
import apps.sarafrika.elimika.wallet.service.WalletService;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
    @Mock
    private PurchaseSettlementRecorder purchaseSettlementRecorder;

    @InjectMocks
    private OrderCaptureWalletCreditListener listener;

    private static final String ORDER_ID = "11111111-1111-1111-1111-111111111111";

    private OrderResponse order(String status, CartItemResponse... items) {
        return orderWithFee(status, null, items);
    }

    /** An order carrying a platform fee computed once on the whole order, as production does. */
    private OrderResponse orderWithFee(String status, String fee, CartItemResponse... items) {
        BigDecimal total = List.of(items).stream()
                .map(CartItemResponse::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return OrderResponse.builder()
                .id(ORDER_ID)
                .paymentStatus(status)
                .currencyCode("KES")
                .total(total)
                .platformFee(fee == null ? null
                        : new PlatformFeeBreakdown(new BigDecimal(fee), "KES", null, null, total, null, null))
                .items(List.of(items))
                .build();
    }

    private List<PurchaseLineSettlement> recordedSettlements() {
        ArgumentCaptor<PurchaseLineSettlement> captor =
                ArgumentCaptor.forClass(PurchaseLineSettlement.class);
        verify(purchaseSettlementRecorder, org.mockito.Mockito.atLeast(0))
                .recordLineSettlement(captor.capture());
        return captor.getAllValues();
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
                        null, null, apps.sarafrika.elimika.shared.utils.enums.RateBasis.PER_HOUR, null, null, null, null, null)));
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
                        null, null, apps.sarafrika.elimika.shared.utils.enums.RateBasis.PER_HOUR, null, null, null, null, null)));
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

    // ---------------------------------------------------------------------------------------
    // The platform fee comes off the top, and the unallocated share is booked
    // ---------------------------------------------------------------------------------------

    @Test
    void creditsAShareOfNetNotGrossOnceTheOrderCarriesAPlatformFee() {
        UUID courseUuid = UUID.randomUUID();
        UUID creatorUserUuid = UUID.randomUUID();
        CartItemResponse item = item("line-1", new BigDecimal("1000.00"),
                Map.of("course_uuid", courseUuid.toString()));

        when(courseInfoService.getCourseCreatorUserUuid(courseUuid))
                .thenReturn(Optional.of(creatorUserUuid));
        when(courseInfoService.getRevenueShare(courseUuid))
                .thenReturn(Optional.of(new RevenueShare(new BigDecimal("70"), new BigDecimal("20"))));
        when(walletService.creditSaleIdempotent(any(), any(), any(), any(), any())).thenReturn(true);

        listener.handleOrderCompleted(
                new OrderCompletedEvent(orderWithFee("CAPTURED", "10.00", item), null));

        // 10.00 fee off the top leaves 990.00; the creator earns 70% of that, not of the 1000.00
        // gross. Crediting 700.00 here would be paying out money the platform had already charged.
        verify(walletService).creditSaleIdempotent(
                eq(creatorUserUuid), eq(new BigDecimal("693.00")), eq("KES"),
                eq(ORDER_ID + ":line-1"), any());
    }

    @Test
    void instructorIsAlsoCreditedAShareOfNet() {
        UUID classUuid = UUID.randomUUID();
        UUID courseUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();
        UUID instructorUserUuid = UUID.randomUUID();
        CartItemResponse item = item("line-1", new BigDecimal("1000.00"),
                Map.of("class_definition_uuid", classUuid.toString()));

        when(classDefinitionLookupService.findByUuid(classUuid)).thenReturn(Optional.of(
                new ClassDefinitionSnapshot(classUuid, courseUuid, null, "T", null,
                        null, null, apps.sarafrika.elimika.shared.utils.enums.RateBasis.PER_HOUR, null, null, null, null, null)));
        when(classDefinitionLookupService.findDefaultInstructorUuid(classUuid))
                .thenReturn(Optional.of(instructorUuid));
        when(instructorLookupService.getInstructorUserUuid(instructorUuid))
                .thenReturn(Optional.of(instructorUserUuid));
        when(courseInfoService.getRevenueShare(courseUuid))
                .thenReturn(Optional.of(new RevenueShare(new BigDecimal("70"), new BigDecimal("20"))));
        when(walletService.creditSaleIdempotent(any(), any(), any(), any(), any())).thenReturn(true);

        listener.handleOrderCompleted(
                new OrderCompletedEvent(orderWithFee("CAPTURED", "10.00", item), null));

        // 20% of 990.00, not of 1000.00
        verify(walletService).creditSaleIdempotent(
                eq(instructorUserUuid), eq(new BigDecimal("198.00")), eq("KES"),
                eq(ORDER_ID + ":line-1"), any());
    }

    @Test
    void booksTheUnallocatedShareThatIsCreditedToNobody() {
        UUID courseUuid = UUID.randomUUID();
        CartItemResponse item = item("line-1", new BigDecimal("1000.00"),
                Map.of("course_uuid", courseUuid.toString()));

        when(courseInfoService.getCourseCreatorUserUuid(courseUuid))
                .thenReturn(Optional.of(UUID.randomUUID()));
        when(courseInfoService.getRevenueShare(courseUuid))
                .thenReturn(Optional.of(new RevenueShare(new BigDecimal("70"), new BigDecimal("20"))));
        when(walletService.creditSaleIdempotent(any(), any(), any(), any(), any())).thenReturn(true);

        listener.handleOrderCompleted(
                new OrderCompletedEvent(orderWithFee("CAPTURED", "10.00", item), null));

        // A 70/20 course sold as a course credits the creator only. The instructor's 20% is credited
        // to nobody, so 30% of the net stays with the platform - previously visible only as a
        // difference, now a figure someone can read.
        PurchaseLineSettlement settlement = recordedSettlements().getFirst();
        assertThat(settlement.grossAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(settlement.platformFeeAmount()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(settlement.creditedAmount()).isEqualByComparingTo(new BigDecimal("693.00"));
        assertThat(settlement.retainedAmount()).isEqualByComparingTo(new BigDecimal("297.00"));
        assertThat(settlement.orderId()).isEqualTo(ORDER_ID);
        assertThat(settlement.lineItemId()).isEqualTo("line-1");
    }

    @Test
    void everySettledLineReconcilesGrossToFeePlusCreditedPlusRetained() {
        UUID courseUuid = UUID.randomUUID();
        // The awkward apportionment case: a 10.00 order fee across 33.33 / 33.33 / 33.34.
        CartItemResponse first = item("line-1", new BigDecimal("33.33"),
                Map.of("course_uuid", courseUuid.toString()));
        CartItemResponse second = item("line-2", new BigDecimal("33.33"),
                Map.of("course_uuid", courseUuid.toString()));
        CartItemResponse third = item("line-3", new BigDecimal("33.34"),
                Map.of("course_uuid", courseUuid.toString()));

        when(courseInfoService.getCourseCreatorUserUuid(courseUuid))
                .thenReturn(Optional.of(UUID.randomUUID()));
        when(courseInfoService.getRevenueShare(courseUuid))
                .thenReturn(Optional.of(new RevenueShare(new BigDecimal("70"), new BigDecimal("20"))));
        when(walletService.creditSaleIdempotent(any(), any(), any(), any(), any())).thenReturn(true);

        listener.handleOrderCompleted(
                new OrderCompletedEvent(orderWithFee("CAPTURED", "10.00", first, second, third), null));

        List<PurchaseLineSettlement> settlements = recordedSettlements();
        assertThat(settlements).hasSize(3);
        for (PurchaseLineSettlement settlement : settlements) {
            assertThat(settlement.platformFeeAmount()
                    .add(settlement.creditedAmount())
                    .add(settlement.retainedAmount()))
                    .as("line %s must account for every cent it collected", settlement.lineItemId())
                    .isEqualByComparingTo(settlement.grossAmount());
        }

        // The fee charged across the lines is exactly the fee charged on the order - no cent
        // invented, none lost.
        BigDecimal feeCharged = settlements.stream()
                .map(PurchaseLineSettlement::platformFeeAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(feeCharged).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(settlements).extracting(PurchaseLineSettlement::platformFeeAmount)
                .containsExactly(new BigDecimal("3.33"), new BigDecimal("3.33"), new BigDecimal("3.34"));
    }

    @Test
    void aLineWithNoConfiguredRevenueShareCreditsNobodyAndRetainsEverything() {
        UUID courseUuid = UUID.randomUUID();
        CartItemResponse item = item("line-1", new BigDecimal("1000.00"),
                Map.of("course_uuid", courseUuid.toString()));

        when(courseInfoService.getCourseCreatorUserUuid(courseUuid))
                .thenReturn(Optional.of(UUID.randomUUID()));
        when(courseInfoService.getRevenueShare(courseUuid)).thenReturn(Optional.empty());

        listener.handleOrderCompleted(
                new OrderCompletedEvent(orderWithFee("CAPTURED", "10.00", item), null));

        // Unchanged behaviour: a missing split still credits nothing. What is new is that the whole
        // 990.00 is now recorded as retained rather than being unaccounted for.
        verify(walletService, never()).creditSaleIdempotent(any(), any(), any(), any(), any());
        PurchaseLineSettlement settlement = recordedSettlements().getFirst();
        assertThat(settlement.creditedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(settlement.platformFeeAmount()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(settlement.retainedAmount()).isEqualByComparingTo(new BigDecimal("990.00"));
    }

    @Test
    void aLineWithNoCourseOrClassScopeRetainsEverythingAfterTheFee() {
        CartItemResponse item = item("line-1", new BigDecimal("1000.00"), Map.of());

        listener.handleOrderCompleted(
                new OrderCompletedEvent(orderWithFee("CAPTURED", "10.00", item), null));

        verify(walletService, never()).creditSaleIdempotent(any(), any(), any(), any(), any());
        PurchaseLineSettlement settlement = recordedSettlements().getFirst();
        assertThat(settlement.creditedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(settlement.retainedAmount()).isEqualByComparingTo(new BigDecimal("990.00"));
    }

    @Test
    void aZeroTotalLineIsNeitherCreditedNorSettled() {
        CartItemResponse item = item("line-1", BigDecimal.ZERO,
                Map.of("course_uuid", UUID.randomUUID().toString()));

        listener.handleOrderCompleted(
                new OrderCompletedEvent(orderWithFee("CAPTURED", "10.00", item), null));

        // No money was collected, so there is nothing to charge, credit or retain. Recording zeros
        // would imply a settlement that never happened.
        verify(walletService, never()).creditSaleIdempotent(any(), any(), any(), any(), any());
        verify(purchaseSettlementRecorder, never()).recordLineSettlement(any());
    }

    @Test
    void aZeroTotalOrderCreditsNobodyAndDividesByNothing() {
        CartItemResponse first = item("line-1", BigDecimal.ZERO,
                Map.of("course_uuid", UUID.randomUUID().toString()));
        CartItemResponse second = item("line-2", BigDecimal.ZERO,
                Map.of("course_uuid", UUID.randomUUID().toString()));

        listener.handleOrderCompleted(
                new OrderCompletedEvent(orderWithFee("CAPTURED", "10.00", first, second), null));

        verify(walletService, never()).creditSaleIdempotent(any(), any(), any(), any(), any());
        verify(purchaseSettlementRecorder, never()).recordLineSettlement(any());
    }

    @Test
    void aSingleLineOrderCarriesTheWholeFee() {
        UUID courseUuid = UUID.randomUUID();
        CartItemResponse item = item("line-1", new BigDecimal("1000.00"),
                Map.of("course_uuid", courseUuid.toString()));

        when(courseInfoService.getCourseCreatorUserUuid(courseUuid))
                .thenReturn(Optional.of(UUID.randomUUID()));
        when(courseInfoService.getRevenueShare(courseUuid))
                .thenReturn(Optional.of(new RevenueShare(new BigDecimal("70"), new BigDecimal("20"))));
        when(walletService.creditSaleIdempotent(any(), any(), any(), any(), any())).thenReturn(true);

        listener.handleOrderCompleted(
                new OrderCompletedEvent(orderWithFee("CAPTURED", "15.50", item), null));

        assertThat(recordedSettlements().getFirst().platformFeeAmount())
                .isEqualByComparingTo(new BigDecimal("15.50"));
    }

    @Test
    void anOrderWithoutAPlatformFeeStillCreditsTheFullShare() {
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

        // No PLATFORM_FEE rule configured (or an active waiver) means no fee, so nothing changes.
        verify(walletService).creditSaleIdempotent(
                eq(creatorUserUuid), eq(new BigDecimal("700.00")), eq("KES"),
                eq(ORDER_ID + ":line-1"), any());
        assertThat(recordedSettlements().getFirst().platformFeeAmount())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void aFailedCreditRecordsNoSettlementForThatLine() {
        UUID courseUuid = UUID.randomUUID();
        CartItemResponse item = item("line-1", new BigDecimal("1000.00"),
                Map.of("course_uuid", courseUuid.toString()));

        when(courseInfoService.getCourseCreatorUserUuid(courseUuid))
                .thenReturn(Optional.of(UUID.randomUUID()));
        when(courseInfoService.getRevenueShare(courseUuid))
                .thenReturn(Optional.of(new RevenueShare(new BigDecimal("70"), new BigDecimal("20"))));
        when(walletService.creditSaleIdempotent(any(), any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("wallet unavailable"));

        assertThatThrownBy(() -> listener.handleOrderCompleted(
                new OrderCompletedEvent(orderWithFee("CAPTURED", "10.00", item), null)))
                .isInstanceOf(WalletCreditFailedException.class);

        // Claiming a settlement for a credit that did not happen would be worse than no record.
        verify(purchaseSettlementRecorder, never()).recordLineSettlement(any());
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
