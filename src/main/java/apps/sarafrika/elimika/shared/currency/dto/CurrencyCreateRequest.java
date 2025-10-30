package apps.sarafrika.elimika.shared.currency.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "CurrencyCreateRequest", description = "Admin payload to register an additional platform currency")
public record CurrencyCreateRequest(

        @Schema(description = "ISO 4217 alpha code", example = "KES", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(min = 3, max = 3, message = "Currency code must be exactly 3 characters")
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "Currency code must be alphabetic ISO 4217 code")
        @JsonProperty("code")
        String code,

        @Schema(description = "ISO numeric code", example = "404")
        @JsonProperty("numeric_code")
        Integer numericCode,

        @Schema(description = "Official currency name", example = "Kenyan Shilling", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Currency name is required")
        @Size(max = 128)
        @JsonProperty("name")
        String name,

        @Schema(description = "Optional display symbol", example = "KES")
        @Size(max = 16)
        @JsonProperty("symbol")
        String symbol,

        @Schema(description = "Number of fractional decimal places", example = "2", defaultValue = "2")
        @NotNull(message = "Decimal places is required")
        @Min(value = 0, message = "Decimal places cannot be negative")
        @Max(value = 6, message = "Decimal places cannot exceed 6")
        @JsonProperty("decimal_places")
        Integer decimalPlaces,

        @Schema(description = "Whether the currency is active immediately", defaultValue = "true")
        @JsonProperty("active")
        Boolean active,

        @Schema(description = "Whether to set this currency as the platform default", defaultValue = "false")
        @JsonProperty("default_currency")
        Boolean defaultCurrency
) {
}
