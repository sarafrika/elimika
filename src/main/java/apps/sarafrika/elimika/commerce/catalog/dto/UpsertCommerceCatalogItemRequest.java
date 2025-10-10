package apps.sarafrika.elimika.commerce.catalog.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(name = "CommerceCatalogItemUpsertRequest", description = "Payload for creating or updating catalog mappings")
public record UpsertCommerceCatalogItemRequest(
        @Schema(description = "Course UUID to associate", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @JsonProperty("course_uuid")
        UUID courseUuid,

        @Schema(description = "Class definition UUID to associate", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @JsonProperty("class_definition_uuid")
        UUID classDefinitionUuid,

        @Schema(description = "Medusa product identifier", example = "prod_01J0ABCXYZ")
        @NotBlank(message = "Medusa product identifier is required")
        @JsonProperty("medusa_product_id")
        String medusaProductId,

        @Schema(description = "Medusa variant identifier", example = "variant_01J0ABCXYZ")
        @NotBlank(message = "Medusa variant identifier is required")
        @JsonProperty("medusa_variant_id")
        String medusaVariantId,

        @Schema(description = "Currency code for the variant", example = "USD")
        @JsonProperty("currency_code")
        String currencyCode,

        @Schema(description = "Active flag")
        @JsonProperty("active")
        Boolean active) {
}
