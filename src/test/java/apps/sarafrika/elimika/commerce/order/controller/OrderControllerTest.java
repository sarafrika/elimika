package apps.sarafrika.elimika.commerce.order.controller;

import apps.sarafrika.elimika.commerce.order.service.OrderService;
import apps.sarafrika.elimika.shared.dto.commerce.CartItemResponse;
import apps.sarafrika.elimika.shared.dto.commerce.OrderResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private apps.sarafrika.elimika.commerce.order.service.OrderReceiptNotifier receiptNotifier;

    @Mock
    private apps.sarafrika.elimika.shared.security.DomainSecurityService domainSecurityService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new OrderController(orderService, receiptNotifier, domainSecurityService))
                .setControllerAdvice(new apps.sarafrika.elimika.shared.config.GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void listsTheCallersOwnOrdersWithTheirItems() throws Exception {
        OrderResponse paid = OrderResponse.builder()
                .id("order-1")
                .displayId("23")
                .paymentStatus("CAPTURED")
                .currencyCode("KES")
                .total(new BigDecimal("10000.00"))
                .createdAt(OffsetDateTime.parse("2026-10-17T20:19:30Z"))
                .items(List.of(CartItemResponse.builder().id("line-1").title("AI and ML").quantity(1)
                        .total(new BigDecimal("10000.00")).build()))
                .build();
        when(orderService.listMyOrders(any())).thenReturn(new PageImpl<>(List.of(paid), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/commerce/orders/mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value("order-1"))
                .andExpect(jsonPath("$.data.content[0].payment_status").value("CAPTURED"))
                .andExpect(jsonPath("$.data.content[0].items[0].title").value("AI and ML"))
                .andExpect(jsonPath("$.data.metadata.totalElements").value(1));
    }

    @Test
    void theBuyerDownloadsTheReceiptPdf() throws Exception {
        java.util.UUID buyer = java.util.UUID.randomUUID();
        OrderResponse paid = OrderResponse.builder().id("order-1").displayId("ELM-HH48B3").userUuid(buyer)
                .paymentStatus("CAPTURED").build();
        when(orderService.getOrder("order-1")).thenReturn(paid);
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(buyer);
        when(receiptNotifier.receiptPdf(paid)).thenReturn(java.util.Optional.of(new byte[] {'%', 'P', 'D', 'F'}));

        mockMvc.perform(get("/api/v1/commerce/orders/order-1/receipt.pdf"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("Content-Disposition", org.hamcrest.Matchers.containsString("Elimika-receipt-ELM-HH48B3.pdf")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .contentType("application/pdf"));
    }

    @Test
    void someoneElsesReceiptIsNotFound() throws Exception {
        OrderResponse paid = OrderResponse.builder().id("order-1").userUuid(java.util.UUID.randomUUID())
                .paymentStatus("CAPTURED").build();
        when(orderService.getOrder("order-1")).thenReturn(paid);
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(java.util.UUID.randomUUID());

        mockMvc.perform(get("/api/v1/commerce/orders/order-1/receipt.pdf"))
                .andExpect(status().isNotFound());
        org.mockito.Mockito.verifyNoInteractions(receiptNotifier);
    }
}
