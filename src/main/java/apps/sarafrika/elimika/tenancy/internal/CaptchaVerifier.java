package apps.sarafrika.elimika.tenancy.internal;

import apps.sarafrika.elimika.tenancy.config.RegistrationProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/** Verifies a Turnstile token on public registration endpoints; a no-op while captcha is disabled. */
@Component
@Slf4j
public class CaptchaVerifier {

    private final RegistrationProperties.Captcha captcha;
    private final RestClient restClient;

    public CaptchaVerifier(RegistrationProperties properties) {
        this.captcha = properties.getCaptcha();
        this.restClient = RestClient.create();
    }

    /** True when captcha is off, or the token verifies. */
    public boolean verify(String token, String clientIp) {
        if (!captcha.isEnabled()) {
            return true;
        }
        if (!StringUtils.hasText(token) || !StringUtils.hasText(captcha.getSecret())) {
            return false;
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("secret", captcha.getSecret());
        form.add("response", token);
        if (StringUtils.hasText(clientIp)) {
            form.add("remoteip", clientIp);
        }
        try {
            JsonNode result = restClient.post()
                    .uri(captcha.getVerifyUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(JsonNode.class);
            return result != null && result.path("success").asBoolean(false);
        } catch (Exception e) {
            log.warn("Captcha verification could not be completed: {}", e.getMessage());
            return false;
        }
    }
}
