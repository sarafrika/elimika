package apps.sarafrika.elimika.commerce.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import apps.sarafrika.elimika.commerce.internal.entity.CommercePayment;
import apps.sarafrika.elimika.commerce.internal.enums.PaymentStatus;
import apps.sarafrika.elimika.commerce.internal.repository.CommercePaymentRepository;
import apps.sarafrika.elimika.shared.dto.commerce.CartItemResponse;
import apps.sarafrika.elimika.shared.dto.commerce.OrderResponse;
import apps.sarafrika.elimika.shared.event.notification.NotificationRequestedEvent;
import apps.sarafrika.elimika.shared.sms.SmsSender;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class OrderReceiptNotifierTest {

    private static final String EMAIL = "buyer@example.com";
    private static final UUID ORDER = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private UserLookupService userLookupService;
    @Mock private CommercePaymentRepository paymentRepository;
    @Mock private SmsSender smsSender;
    @InjectMocks private OrderReceiptNotifier notifier;

    private final UUID buyer = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        lenient().when(userLookupService.getUserEmail(buyer)).thenReturn(Optional.of(EMAIL));
        lenient().when(userLookupService.getUserFullName(buyer)).thenReturn(Optional.of("Buyer"));
        lenient().when(userLookupService.getUserOrganizations(buyer)).thenReturn(List.of());
        lenient().when(userLookupService.userHasDomain(any(), any())).thenReturn(false);
        CommercePayment payment = new CommercePayment();
        payment.setProviderReceipt("UK42C120ZR");
        payment.setPayerMsisdn("254712345678");
        lenient().when(paymentRepository.findFirstByOrderUuidAndStatus(ORDER, PaymentStatus.CAPTURED)).thenReturn(Optional.of(payment));
    }

    @Test
    void anUnpaidOrderGetsNoReceipt() {
        notifier.sendReceipt(order("AWAITING_PAYMENT"));

        verifyNoInteractions(eventPublisher, smsSender);
    }

    @Test
    void aPaidOrderIsEmailedWithWholeShillingsAndTheMpesaReceipt() {
        notifier.sendReceipt(order("CAPTURED"));

        ArgumentCaptor<NotificationRequestedEvent> captor = ArgumentCaptor.forClass(NotificationRequestedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().recipientEmail()).isEqualTo(EMAIL);
        assertThat(captor.getValue().body()).isEqualTo("We received KES 10,500 for Flower Gardening - Sat cohort. Your seat is confirmed.");
        assertThat(captor.getValue().templateVariables()).containsEntry("mpesaReceipt", "UK42C120ZR");
    }

    @Test
    void aPaidOrderIsTextedToTheNumberThatPaidInOneSegment() {
        notifier.sendReceipt(order("CAPTURED"));

        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(smsSender).send(eq("254712345678"), text.capture());
        assertThat(text.getValue())
                .isEqualTo("Elimika: Paid KES 10,500 for Flower Gardening - Sat cohort. Order ELM-K7Q9PX, M-Pesa UK42C120ZR. Thank you!")
                .hasSizeLessThanOrEqualTo(160);
    }

    @Test
    void noPayingNumberMeansNoSms() {
        when(paymentRepository.findFirstByOrderUuidAndStatus(ORDER, PaymentStatus.CAPTURED)).thenReturn(Optional.empty());

        notifier.sendReceipt(order("CAPTURED"));

        verify(smsSender, never()).send(anyString(), anyString());
    }

    @Test
    void aStudentsReceiptLinksToTheStudentWallet() {
        when(userLookupService.userHasDomain(buyer, UserDomain.student)).thenReturn(true);

        notifier.sendReceipt(order("CAPTURED"));

        ArgumentCaptor<NotificationRequestedEvent> captor = ArgumentCaptor.forClass(NotificationRequestedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().actionUrl()).isEqualTo("/dashboard/student/wallet");
    }

    @Test
    void longClassTitlesAreCutSoTheSmsStaysOneSegment() {
        String text = OrderReceiptNotifier.smsText("KES 100,000", "A very long class title that goes on and on forever", "ELM-K7Q9PX", "UK42C120ZR");
        assertThat(text).hasSizeLessThanOrEqualTo(160).contains("...");
    }

    private OrderResponse order(String status) {
        return OrderResponse.builder()
                .id(ORDER.toString())
                .displayId("ELM-K7Q9PX")
                .userUuid(buyer)
                .paymentStatus(status)
                .currencyCode("KES")
                .total(new BigDecimal("10500.0000"))
                .items(List.of(CartItemResponse.builder().id("line-1").title("Flower Gardening - Sat cohort").quantity(1).build()))
                .build();
    }
}
