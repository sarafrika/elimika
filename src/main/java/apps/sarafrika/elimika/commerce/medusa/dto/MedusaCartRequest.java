package apps.sarafrika.elimika.commerce.medusa.dto;

import java.util.List;
import java.util.Map;

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
public class MedusaCartRequest {

    @NotBlank
    private String regionId;

    private String customerId;

    private String salesChannelId;

    @Builder.Default
    private Map<String, Object> metadata = Map.of();

    @Valid
    @Builder.Default
    private List<MedusaLineItemRequest> items = List.of();
}
