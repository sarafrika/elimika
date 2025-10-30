package apps.sarafrika.elimika.shared.currency.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "CurrencyUpdateRequest", description = "Admin payload to update currency metadata")
public record CurrencyUpdateRequest(

        @Schema(description = "Official currency name", example = "Kenyan Shilling")
        @JsonProperty("name")
        @Size(max = 128)
        String name,

        @Schema(description = "Optional display symbol", example = "KES")
        @JsonProperty("symbol")
        @Size(max = 16)
        String symbol,

        @Schema(description = "ISO numeric code", example = "404")
        @JsonProperty("numeric_code")
        Integer numericCode,

        @Schema(description = "Number of fractional decimal places", example = "2")
        @JsonProperty("decimal_places")
        @Min(value = 0, message = "Decimal places cannot be negative")
        @Max(value = 6, message = "Decimal places cannot exceed 6")
        Integer decimalPlaces,

        @Schema(description = "Whether the currency is active")
        @JsonProperty("active")
        Boolean active
) {
}
