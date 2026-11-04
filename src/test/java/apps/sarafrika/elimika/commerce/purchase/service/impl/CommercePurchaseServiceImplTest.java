package apps.sarafrika.elimika.commerce.purchase.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import apps.sarafrika.elimika.commerce.purchase.entity.CommercePurchase;
import apps.sarafrika.elimika.commerce.purchase.repository.CommercePurchaseRepository;
import apps.sarafrika.elimika.commerce.purchase.service.CommerceAccessService;
import apps.sarafrika.elimika.shared.dto.commerce.CartItemResponse;
import apps.sarafrika.elimika.shared.dto.commerce.OrderResponse;
import apps.sarafrika.elimika.shared.event.commerce.ClassPurchaseRecordedEvent;
import apps.sarafrika.elimika.student.spi.StudentLookupService;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class CommercePurchaseServiceImplTest {

    private static final UUID STUDENT = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID CLASS = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Mock
    private CommercePurchaseRepository purchaseRepository;
    @Mock
    private UserLookupService userLookupService;
    @Mock
    private StudentLookupService studentLookupService;
    @Mock
    private CommerceAccessService accessService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private CommercePurchaseServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CommercePurchaseServiceImpl(purchaseRepository, userLookupService, studentLookupService,
                new ObjectMapper(), accessService, eventPublisher);
        when(purchaseRepository.findByOrderId(any())).thenReturn(Optional.empty());
        when(purchaseRepository.save(any(CommercePurchase.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void anOrderThatIsStillAwaitingPaymentEnrolsNobody() {
        service.recordOrder(order("AWAITING_PAYMENT"), null);

        verify(eventPublisher, never()).publishEvent(any(ClassPurchaseRecordedEvent.class));
        verify(accessService, never()).hasClassAccess(any(), any());
    }

    @Test
    void aCapturedOrderAnnouncesTheSeatItPaidFor() {
        when(accessService.hasClassAccess(STUDENT, CLASS)).thenReturn(true);

        service.recordOrder(order("CAPTURED"), null);

        verify(eventPublisher).publishEvent(any(ClassPurchaseRecordedEvent.class));
    }

    private OrderResponse order(String paymentStatus) {
        CartItemResponse line = CartItemResponse.builder()
                .id(UUID.randomUUID().toString())
                .title("Class seat")
                .quantity(1)
                .total(new BigDecimal("625.00"))
                .metadata(Map.of("class_definition_uuid", CLASS.toString(), "student_uuid", STUDENT.toString()))
                .build();
        return OrderResponse.builder()
                .id(UUID.randomUUID().toString())
                .displayId("ELM-K7Q9PX")
                .paymentStatus(paymentStatus)
                .currencyCode("KES")
                .total(new BigDecimal("625.00"))
                .items(List.of(line))
                .build();
    }
}
