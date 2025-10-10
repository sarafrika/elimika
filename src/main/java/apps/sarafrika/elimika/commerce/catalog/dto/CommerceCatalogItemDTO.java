package apps.sarafrika.elimika.commerce.catalog.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(name = "CommerceCatalogItem", description = "Mapping between Elimika courses/classes and Medusa catalog variants")
public record CommerceCatalogItemDTO(
        @Schema(description = "Catalog item UUID")
        @JsonProperty("uuid")
        UUID uuid,

        @Schema(description = "Associated course UUID if mapping is course level")
        @JsonProperty("course_uuid")
        UUID courseUuid,

        @Schema(description = "Associated class definition UUID if mapping is class specific")
        @JsonProperty("class_definition_uuid")
        UUID classDefinitionUuid,

        @Schema(description = "Medusa product identifier")
        @JsonProperty("medusa_product_id")
        String medusaProductId,

        @Schema(description = "Medusa variant identifier")
        @JsonProperty("medusa_variant_id")
        String medusaVariantId,

        @Schema(description = "Currency code configured for the variant")
        @JsonProperty("currency_code")
        String currencyCode,

        @Schema(description = "Whether this mapping is active")
        @JsonProperty("active")
        boolean active,

        @Schema(description = "Created timestamp")
        @JsonProperty("created_date")
        LocalDateTime createdDate,

        @Schema(description = "Last updated timestamp")
        @JsonProperty("updated_date")
        LocalDateTime updatedDate) {
}
