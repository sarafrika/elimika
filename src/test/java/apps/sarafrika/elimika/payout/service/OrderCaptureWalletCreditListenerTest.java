package apps.sarafrika.elimika.payout.service;

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
}
