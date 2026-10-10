package apps.sarafrika.elimika.commerce.order.service;

import apps.sarafrika.elimika.shared.dto.commerce.CheckoutRequest;
import apps.sarafrika.elimika.shared.dto.commerce.OrderResponse;
import apps.sarafrika.elimika.shared.event.notification.NotificationRequestedEvent;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderReceiptNotifierTest {

    private static final String EMAIL = "buyer@example.com";

    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private UserLookupService userLookupService;

    @InjectMocks
    private OrderReceiptNotifier notifier;

    private final UUID userUuid = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(userLookupService.findUserUuidByEmail(EMAIL)).thenReturn(Optional.of(userUuid));
        when(userLookupService.getUserFullName(userUuid)).thenReturn(Optional.of("Buyer"));
        when(userLookupService.getUserOrganizations(userUuid)).thenReturn(List.of());
        lenient().when(userLookupService.userHasDomain(any(), any())).thenReturn(false);
    }

    @Test
    void studentReceiptLinksToTheStudentWallet() {
        when(userLookupService.userHasDomain(userUuid, UserDomain.student)).thenReturn(true);

        assertThat(sendAndCaptureActionUrl()).isEqualTo("/dashboard/student/wallet");
    }

    @Test
    void parentReceiptLinksToParentBilling() {
        when(userLookupService.userHasDomain(userUuid, UserDomain.parent)).thenReturn(true);

        assertThat(sendAndCaptureActionUrl()).isEqualTo("/dashboard/parent/billing");
    }

    @Test
    void otherBuyersGetTheRoleNeutralDashboardEntry() {
        assertThat(sendAndCaptureActionUrl()).isEqualTo("/dashboard");
    }

    private String sendAndCaptureActionUrl() {
        OrderResponse order = OrderResponse.builder().id("order-1").build();
        CheckoutRequest checkout = CheckoutRequest.builder().customerEmail(EMAIL).build();

        notifier.sendReceipt(order, checkout);

        ArgumentCaptor<NotificationRequestedEvent> captor = ArgumentCaptor.forClass(NotificationRequestedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        return captor.getValue().actionUrl();
    }
}
