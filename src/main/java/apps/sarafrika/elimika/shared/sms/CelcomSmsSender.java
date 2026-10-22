package apps.sarafrika.elimika.shared.sms;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Celcom Africa bulk SMS (POST /api/services/sendsms/). A message counts as accepted only when the
 * response code is 200; Celcom's documented key is spelled "respose-code", so both spellings are read.
 */
@Slf4j
@RequiredArgsConstructor
public class CelcomSmsSender implements SmsSender {

    private static final String SEND_PATH = "/api/services/sendsms/";

    private final RestClient restClient;
    private final SmsProperties.Celcom properties;

    @Override
    public SmsDelivery send(String msisdn, String message) {
        Map<String, String> body = Map.of(
                "apikey", properties.getApiKey(),
                "partnerID", properties.getPartnerId(),
                "shortcode", properties.getShortcode(),
                "mobile", msisdn,
                "message", message);
        try {
            CelcomResponse response = restClient.post()
                    .uri(SEND_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(CelcomResponse.class);
            CelcomResult result = response == null || response.responses() == null || response.responses().isEmpty()
                    ? null : response.responses().getFirst();
            if (result != null && result.code() == 200) {
                return SmsDelivery.accepted(result.messageId());
            }
            String failure = result == null ? "empty response"
                    : result.code() + " " + result.description();
            log.warn("Celcom refused an SMS to {}: {}", Msisdn.mask(msisdn), failure);
            return SmsDelivery.refused(failure);
        } catch (RestClientException ex) {
            log.warn("Celcom SMS to {} failed: {}", Msisdn.mask(msisdn), ex.getMessage());
            return SmsDelivery.refused("transport error");
        }
    }

    record CelcomResponse(@JsonProperty("responses") List<CelcomResult> responses) {
    }

    record CelcomResult(
            @JsonProperty("respose-code") Integer misspeltCode,
            @JsonProperty("response-code") Integer responseCode,
            @JsonProperty("response-description") String description,
            @JsonProperty("messageid") String messageId
    ) {
        int code() {
            Integer code = responseCode != null ? responseCode : misspeltCode;
            return code == null ? -1 : code;
        }
    }
}
