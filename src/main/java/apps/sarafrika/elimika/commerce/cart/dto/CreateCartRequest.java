package apps.sarafrika.elimika.commerce.cart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload used by the Elimika frontend to bootstrap a checkout cart on Medusa.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(name = "CreateCartRequest", description = "Request body for creating a new cart that synchronises with Medusa")
public class CreateCartRequest {

    @Schema(
            description = "Identifier of the Medusa region the cart belongs to",
            example = "reg_01HZX1W8GX9YYB01X54MB2F15C",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Region identifier is required")
    @JsonProperty("region_id")
    private String regionId;

    @Schema(
            description = "Medusa customer identifier to associate with the cart",
            example = "cus_01HZX1X6QAQCCYT11S3R6G9KVN",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    @JsonProperty("customer_id")
    private String customerId;

    @Schema(
            description = "Sales channel identifier configured in Medusa",
            example = "sc_01HZX20Y4D9CK3R6RHY9PRF20C",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    @JsonProperty("sales_channel_id")
    private String salesChannelId;

    @Builder.Default
    @Schema(
            description = "Arbitrary metadata forwarded to Medusa",
            example = "{\"campaign\":\"back-to-school\"}",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    @JsonProperty("metadata")
    private Map<String, Object> metadata = Map.of();

    @Valid
    @Builder.Default
    @ArraySchema(
            schema = @Schema(implementation = CartLineItemRequest.class),
            arraySchema = @Schema(
                    description = "Optional collection of line items to pre-populate the cart",
                    requiredMode = Schema.RequiredMode.NOT_REQUIRED
            )
    )
    @JsonProperty("items")
    private List<CartLineItemRequest> items = List.of();
}
