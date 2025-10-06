package apps.sarafrika.elimika.commerce.medusa.config;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for the Medusa API integration.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "medusa")
public class MedusaProperties {

    /**
     * Base URL of the Medusa backend (e.g. https://medusa.example.com).
     */
    @NotBlank
    private String baseUrl;

    /**
     * API token used to authenticate with the Medusa Admin or Store APIs.
     */
    @NotBlank
    private String apiToken;
}
