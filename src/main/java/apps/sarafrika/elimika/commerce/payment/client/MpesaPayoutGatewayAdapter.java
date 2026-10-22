package apps.sarafrika.elimika.commerce.payment.client;

import apps.sarafrika.elimika.commerce.payment.client.dto.MpesaGatewayEnvelope;
import apps.sarafrika.elimika.commerce.payment.client.dto.MpesaPayoutFloatResponse;
import apps.sarafrika.elimika.commerce.payment.client.dto.MpesaPayoutGatewayRequest;
import apps.sarafrika.elimika.commerce.payment.client.dto.MpesaPayoutGatewayResponse;
import apps.sarafrika.elimika.commerce.payment.config.MpesaGatewayProperties;
import apps.sarafrika.elimika.shared.spi.payout.MobileMoneyPayoutGateway;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Wallet withdrawals over mpesa-service's B2C API. Transport failures throw
 * {@link MpesaGatewayException} so the caller retries with the same reference; a payout M-Pesa
 * refused comes back as a FAILED outcome.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MpesaPayoutGatewayAdapter implements MobileMoneyPayoutGateway {

    private static final String B2C_PATH = "/api/v1/mpesa/b2c";
    private static final String BY_REFERENCE_PATH = "/api/v1/mpesa/payouts?client_reference={reference}";
    private static final String FLOAT_PATH = "/api/v1/mpesa/shortcodes/{uuid}/float";

    private final RestClient mpesaRestClient;
    private final MpesaGatewayProperties properties;

    @Override
    public boolean isConfigured() {
        return StringUtils.hasText(properties.getBaseUrl()) && properties.getPayoutShortcodeUuid() != null;
    }

    @Override
    public PayoutOutcome submit(String clientReference, String msisdn, BigDecimal amount, String remarks) {
        requireConfigured();
        MpesaPayoutGatewayRequest body = new MpesaPayoutGatewayRequest(properties.getPayoutShortcodeUuid(), msisdn,
                amount, clientReference, remarks);
        try {
            MpesaGatewayEnvelope<MpesaPayoutGatewayResponse> envelope = mpesaRestClient.post()
                    .uri(B2C_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    // 502 carries a payout M-Pesa refused; read it rather than throw.
                    .onStatus(status -> status.value() == 502, (request, response) -> { })
                    .body(new ParameterizedTypeReference<>() {
                    });
            if (envelope == null || envelope.data() == null) {
                throw new MpesaGatewayException("mpesa-service returned no payout for " + clientReference);
            }
            return outcome(envelope.data());
        } catch (HttpClientErrorException ex) {
            log.warn("mpesa-service refused payout {}: {} {}", clientReference, ex.getStatusCode(), ex.getResponseBodyAsString());
            return new PayoutOutcome(null, PayoutState.FAILED, null, null, null,
                    "Payout request refused: " + ex.getStatusCode().value(), false, null);
        } catch (RestClientException ex) {
            throw new MpesaGatewayException("Failed to submit payout " + clientReference, ex);
        }
    }

    @Override
    public Optional<PayoutOutcome> status(String clientReference) {
        requireConfigured();
        try {
            MpesaPayoutGatewayResponse response = mpesaRestClient.get()
                    .uri(BY_REFERENCE_PATH, clientReference)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (request, res) -> { })
                    .body(MpesaPayoutGatewayResponse.class);
            return Optional.ofNullable(response).filter(r -> r.status() != null).map(MpesaPayoutGatewayAdapter::outcome);
        } catch (RestClientException ex) {
            throw new MpesaGatewayException("Failed to read payout " + clientReference, ex);
        }
    }

    @Override
    public Optional<PayoutFloat> currentFloat() {
        if (!isConfigured()) {
            return Optional.empty();
        }
        try {
            MpesaPayoutFloatResponse response = mpesaRestClient.get()
                    .uri(FLOAT_PATH, properties.getPayoutShortcodeUuid())
                    .retrieve()
                    .body(MpesaPayoutFloatResponse.class);
            return Optional.ofNullable(response).filter(r -> r.reportedAt() != null)
                    .map(r -> new PayoutFloat(r.utilityAccountAvailableFunds(), r.workingAccountAvailableFunds(), r.reportedAt()));
        } catch (RestClientException ex) {
            log.warn("Could not read the payout float: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private void requireConfigured() {
        if (!isConfigured()) {
            throw new IllegalStateException("Payouts are not configured: set mpesa.service.base-url and payout-shortcode-uuid");
        }
    }

    private static PayoutOutcome outcome(MpesaPayoutGatewayResponse response) {
        PayoutState state = switch (response.status().toUpperCase(Locale.ROOT)) {
            case "SUCCESS" -> PayoutState.SUCCESS;
            case "FAILED", "CANCELLED" -> PayoutState.FAILED;
            default -> PayoutState.PENDING;
        };
        return new PayoutOutcome(response.uuid() == null ? null : response.uuid().toString(), state,
                response.transactionId(), response.recipientName(), response.chargesPaid(),
                state == PayoutState.FAILED ? response.resultDesc() : null, response.timedOutAt() != null,
                response.processedAt());
    }
}
