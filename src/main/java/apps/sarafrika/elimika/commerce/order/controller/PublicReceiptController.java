package apps.sarafrika.elimika.commerce.order.controller;

import apps.sarafrika.elimika.commerce.order.service.OrderReceiptNotifier;
import apps.sarafrika.elimika.commerce.order.service.OrderService;
import apps.sarafrika.elimika.commerce.order.service.ReceiptLinks;
import apps.sarafrika.elimika.shared.dto.commerce.OrderResponse;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Opens a paid order's receipt from the signed link in its payment email; no sign-in. */
@RestController
@RequiredArgsConstructor
@Tag(name = "Public Receipts", description = "Signed, no-sign-in receipt downloads")
public class PublicReceiptController {

    private final ReceiptLinks receiptLinks;
    private final OrderService orderService;
    private final OrderReceiptNotifier receiptNotifier;

    @Operation(summary = "Download a receipt PDF from a signed link",
            description = "404 unless the link's token matches the order and the order is paid")
    @GetMapping(value = ReceiptLinks.PATH + "{orderId}.pdf", produces = "application/pdf")
    public ResponseEntity<byte[]> download(@PathVariable String orderId, @RequestParam(name = "t", required = false) String token) {
        if (!receiptLinks.verify(orderId, token)) {
            throw new ResourceNotFoundException("Receipt not found");
        }
        OrderResponse order = orderService.getOrder(orderId);
        byte[] pdf = receiptNotifier.receiptPdf(order).orElseThrow(() -> new ResourceNotFoundException("Receipt not found"));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename("Elimika-receipt-" + order.getDisplayId() + ".pdf").build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .header("X-Robots-Tag", "noindex")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
