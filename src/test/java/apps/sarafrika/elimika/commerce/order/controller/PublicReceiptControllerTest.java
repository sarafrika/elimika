package apps.sarafrika.elimika.commerce.order.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import apps.sarafrika.elimika.commerce.order.service.OrderReceiptNotifier;
import apps.sarafrika.elimika.commerce.order.service.OrderService;
import apps.sarafrika.elimika.commerce.order.service.ReceiptLinks;
import apps.sarafrika.elimika.shared.config.GlobalExceptionHandler;
import apps.sarafrika.elimika.shared.dto.commerce.OrderResponse;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class PublicReceiptControllerTest {

    private static final String ORDER = "be402957-1985-4ba9-84ac-06b4040b67bc";

    @Mock private OrderService orderService;
    @Mock private OrderReceiptNotifier receiptNotifier;
    private final ReceiptLinks links = new ReceiptLinks("test-secret", "https://api.example.com");
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PublicReceiptController(links, orderService, receiptNotifier))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void theIssuedLinkOpensThePdfWithoutSigningIn() throws Exception {
        OrderResponse paid = OrderResponse.builder().id(ORDER).displayId("ELM-HH48B3").paymentStatus("CAPTURED").build();
        when(orderService.getOrderForSignedLink(ORDER)).thenReturn(paid);
        when(receiptNotifier.receiptPdf(paid)).thenReturn(Optional.of(new byte[] {'%', 'P', 'D', 'F'}));
        String url = links.url(ORDER).orElseThrow();

        mockMvc.perform(get(url.substring("https://api.example.com".length())))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"));
    }

    @Test
    void aWrongTokenNeverLoadsTheOrder() throws Exception {
        mockMvc.perform(get("/api/v1/public/receipts/" + ORDER + ".pdf?t=guess")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/public/receipts/" + ORDER + ".pdf")).andExpect(status().isNotFound());
        verifyNoInteractions(orderService, receiptNotifier);
    }
}
