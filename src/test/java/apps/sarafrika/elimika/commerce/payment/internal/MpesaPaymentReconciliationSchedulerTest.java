package apps.sarafrika.elimika.commerce.payment.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import apps.sarafrika.elimika.commerce.internal.entity.CommerceOrder;
import apps.sarafrika.elimika.commerce.internal.entity.CommerceOrderItem;
import apps.sarafrika.elimika.commerce.internal.enums.OrderStatus;
import apps.sarafrika.elimika.commerce.internal.enums.PaymentStatus;
import apps.sarafrika.elimika.commerce.internal.repository.CommerceOrderRepository;
import apps.sarafrika.elimika.commerce.internal.service.LearnerResolver;
import apps.sarafrika.elimika.commerce.payment.service.OrderPaymentService;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.spi.ClassEnrolmentGateService;
import apps.sarafrika.elimika.student.spi.StudentGuardianLookupService;
import apps.sarafrika.elimika.student.spi.StudentLookupService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MpesaPaymentReconciliationSchedulerTest {

    private static final UUID BUYER = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID CLASS = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    @Mock
    private CommerceOrderRepository orderRepository;
    @Mock
    private OrderPaymentService orderPaymentService;
    @Mock
    private ClassEnrolmentGateService classEnrolmentGateService;
    @Mock
    private DomainSecurityService domainSecurityService;
    @Mock
    private StudentGuardianLookupService studentGuardianLookupService;
    @Mock
    private StudentLookupService studentLookupService;

    private MpesaPaymentReconciliationScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new MpesaPaymentReconciliationScheduler(orderRepository, orderPaymentService,
                classEnrolmentGateService,
                new LearnerResolver(domainSecurityService, studentGuardianLookupService, studentLookupService),
                new ObjectMapper());
    }

    @Test
    void anUnpaidOrderPastTheWindowIsCancelledAndGivesBackItsSeat() {
        CommerceOrder order = order("ws_CO_1", 10);
        when(orderRepository.findByPaymentStatusAndCheckoutRequestIdIsNotNullAndPlacedAtBefore(any(), any()))
                .thenReturn(List.of(order));

        scheduler.settleUnresolvedPayments();

        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.CANCELED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(classEnrolmentGateService).releaseSeats(CLASS, BUYER);
    }

    @Test
    void anOrderWhosePromptNeverStartedIsCancelledToo() {
        CommerceOrder order = order(null, 10);
        when(orderRepository.findByPaymentStatusAndCheckoutRequestIdIsNullAndPlacedAtBefore(any(), any()))
                .thenReturn(List.of(order));

        scheduler.settleUnresolvedPayments();

        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.CANCELED);
        verify(classEnrolmentGateService).releaseSeats(CLASS, BUYER);
    }

    @Test
    void anOrderPaidDuringTheSweepKeepsItsSeat() {
        CommerceOrder order = order("ws_CO_2", 10);
        when(orderRepository.findByPaymentStatusAndCheckoutRequestIdIsNotNullAndPlacedAtBefore(any(), any()))
                .thenReturn(List.of(order));
        when(orderPaymentService.getPaymentStatus(order.getUuid().toString())).thenAnswer(call -> {
            order.setPaymentStatus(PaymentStatus.CAPTURED);
            return null;
        });

        scheduler.settleUnresolvedPayments();

        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.CAPTURED);
        verify(classEnrolmentGateService, never()).releaseSeats(any(), any());
    }

    @Test
    void anOrderCancelledInTheLastHourIsAskedAboutAgainSoALatePaymentIsRecorded() {
        CommerceOrder order = order("ws_CO_3", 20);
        order.setPaymentStatus(PaymentStatus.CANCELED);
        when(orderRepository.findByPaymentStatusAndCheckoutRequestIdIsNotNullAndPlacedAtAfter(any(), any()))
                .thenReturn(List.of(order));

        scheduler.recheckRecentlyCancelled();

        verify(orderPaymentService).getPaymentStatus(order.getUuid().toString());
    }

    private CommerceOrder order(String checkoutRequestId, int minutesAgo) {
        CommerceOrderItem item = new CommerceOrderItem();
        item.setMetadataJson("{\"class_definition_uuid\":\"" + CLASS + "\"}");
        CommerceOrder order = new CommerceOrder();
        order.setUuid(UUID.randomUUID());
        order.setOrderNumber("ELM-K7Q9PX");
        order.setUserUuid(BUYER);
        order.setCheckoutRequestId(checkoutRequestId);
        order.setPaymentStatus(PaymentStatus.AWAITING_PAYMENT);
        order.setPlacedAt(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(minutesAgo));
        order.setItems(List.of(item));
        return order;
    }
}
