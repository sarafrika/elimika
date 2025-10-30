package apps.sarafrika.elimika.shared.currency.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Currency")
public record CurrencyDTO(
        @Schema(description = "ISO 4217 alpha code", example = "KES")
        String code,

        @Schema(description = "Official currency name", example = "Kenyan Shilling")
        String name,

        @Schema(description = "ISO numeric code", example = "404")
        Integer numericCode,

        @Schema(description = "Optional display symbol", example = "KES")
        String symbol,

        @Schema(description = "Number of fractional decimal places", example = "2")
        Integer decimalPlaces,

        @Schema(description = "Indicates whether the currency is active for the platform")
        boolean active,

        @Schema(description = "Indicates whether this is the platform default currency")
        boolean defaultCurrency
) {
}
