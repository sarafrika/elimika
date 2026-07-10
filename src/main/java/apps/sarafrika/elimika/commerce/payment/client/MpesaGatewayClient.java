package apps.sarafrika.elimika.commerce.payment.client;

import apps.sarafrika.elimika.commerce.payment.client.dto.MpesaGatewayEnvelope;
import apps.sarafrika.elimika.commerce.payment.client.dto.MpesaPaymentStatusResponse;
import apps.sarafrika.elimika.commerce.payment.client.dto.StkPushGatewayRequest;
import apps.sarafrika.elimika.commerce.payment.client.dto.StkPushGatewayResponse;
import apps.sarafrika.elimika.commerce.payment.config.MpesaGatewayProperties;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Thin client over the generic mpesa-service Daraja gateway. Handles STK Push initiation and
 * payment status polling, wrapping transport/parsing failures in {@link MpesaGatewayException}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MpesaGatewayClient {

    private static final String STK_PUSH_PATH = "/api/v1/mpesa/stk-push";
    private static final String STATUS_PATH = "/api/v1/mpesa/payments/by-checkout/{checkoutRequestId}";

    private final RestClient mpesaRestClient;
    private final MpesaGatewayProperties properties;

    /**
     * Initiates an STK Push against the configured shortcode and returns the checkout request id
     * used to later poll the payment status.
     */
    public String initiateStkPush(String phoneNumber, BigDecimal amount, String accountReference, String desc) {
        StkPushGatewayRequest body = new StkPushGatewayRequest(
                properties.getShortcodeUuid(), phoneNumber, amount, accountReference, desc);
        try {
            MpesaGatewayEnvelope<StkPushGatewayResponse> envelope = mpesaRestClient.post()
                    .uri(STK_PUSH_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            if (envelope == null || envelope.data() == null
                    || !StringUtils.hasText(envelope.data().checkoutRequestId())) {
                throw new MpesaGatewayException(
                        "M-Pesa STK Push returned no checkout request id for reference " + accountReference);
            }
            String checkoutRequestId = envelope.data().checkoutRequestId();
            log.info("Initiated M-Pesa STK Push for reference {} -> checkoutRequestId {}",
                    accountReference, checkoutRequestId);
            return checkoutRequestId;
        } catch (RestClientException ex) {
            log.error("M-Pesa STK Push failed for reference {}: {}", accountReference, ex.getMessage());
            throw new MpesaGatewayException("Failed to initiate M-Pesa STK Push", ex);
        }
    }

    /**
     * Fetches the current payment status for a checkout request id. Returns one of
     * PENDING/SUCCESS/FAILED/CANCELLED.
     */
    public String getPaymentStatus(String checkoutRequestId) {
        try {
            MpesaPaymentStatusResponse response = mpesaRestClient.get()
                    .uri(STATUS_PATH, checkoutRequestId)
                    .retrieve()
                    .body(MpesaPaymentStatusResponse.class);
            if (response == null || !StringUtils.hasText(response.status())) {
                throw new MpesaGatewayException(
                        "M-Pesa payment status missing for checkout request " + checkoutRequestId);
            }
            return response.status();
        } catch (RestClientException ex) {
            log.error("M-Pesa payment status lookup failed for checkout request {}: {}",
                    checkoutRequestId, ex.getMessage());
            throw new MpesaGatewayException("Failed to fetch M-Pesa payment status", ex);
        }
    }
}
