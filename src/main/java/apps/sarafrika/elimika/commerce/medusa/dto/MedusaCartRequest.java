package apps.sarafrika.elimika.commerce.medusa.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request payload to bootstrap a Medusa cart for checkout flows.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class MedusaCartRequest {

    @NotBlank
    @JsonProperty("region_id")
    private String regionId;

    @JsonProperty("customer_id")
    private String customerId;

    @JsonProperty("sales_channel_id")
    private String salesChannelId;

    @Builder.Default
    @JsonProperty("metadata")
    private Map<String, Object> metadata = Map.of();

    @Valid
    @Builder.Default
    @JsonProperty("items")
    private List<MedusaLineItemRequest> items = List.of();
}
