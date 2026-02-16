package apps.sarafrika.elimika.commerce.catalogue.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(name = "CommerceCatalogueItemUpsertRequest", description = "Payload for creating or updating catalogue mappings")
public record UpsertCommerceCatalogueItemRequest(
        @Schema(description = "Course UUID to associate", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @JsonProperty("course_uuid")
        UUID courseUuid,

        @Schema(description = "Class definition UUID to associate", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @JsonProperty("class_definition_uuid")
        UUID classDefinitionUuid,

        @Schema(description = "Training program UUID to associate", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @JsonProperty("program_uuid")
        UUID programUuid,

        @Schema(description = "Internal commerce product code", example = "course-01J0ABCXYZ")
        @NotBlank(message = "Product code is required")
        @JsonProperty("product_code")
        String productCode,

        @Schema(description = "Internal commerce variant code", example = "variant-01J0ABCXYZ")
        @NotBlank(message = "Variant code is required")
        @JsonProperty("variant_code")
        String variantCode,

        @Schema(description = "Currency code for the variant. Defaults to the platform currency when omitted.", example = "USD")
        @JsonProperty("currency_code")
        String currencyCode,

        @Schema(description = "Active flag")
        @JsonProperty("active")
        Boolean active,

        @Schema(description = "Whether the catalogue item should be visible to public storefront queries")
        @JsonProperty("publicly_visible")
        Boolean publiclyVisible) {
}
