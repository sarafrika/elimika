package apps.sarafrika.elimika.commerce.medusa.dto;

import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

/**
 * Minimal representation of a Medusa product returned from the Admin API.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class MedusaDigitalProductResponse {

    private String id;

    private String title;

    private String handle;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt;

    private List<MedusaVariantResponse> variants;
}
