package apps.sarafrika.elimika.shared.controller;

import apps.sarafrika.elimika.shared.currency.dto.CurrencyDTO;
import apps.sarafrika.elimika.shared.currency.service.CurrencyService;
import apps.sarafrika.elimika.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/currencies")
@Tag(name = "Currencies", description = "Public currency endpoints for platform clients")
@RequiredArgsConstructor
public class CurrencyController {

    private final CurrencyService currencyService;

    @GetMapping
    @Operation(summary = "List active platform currencies")
    public ResponseEntity<ApiResponse<List<CurrencyDTO>>> listActiveCurrencies() {
        List<CurrencyDTO> currencies = currencyService.getActiveCurrencies();
        return ResponseEntity.ok(ApiResponse.success(currencies, "Active currencies retrieved successfully"));
    }

    @GetMapping("/default")
    @Operation(summary = "Get the platform default currency")
    public ResponseEntity<ApiResponse<CurrencyDTO>> getDefaultCurrency() {
        CurrencyDTO currency = currencyService.getDefaultCurrency();
        return ResponseEntity.ok(ApiResponse.success(currency, "Default currency retrieved successfully"));
    }
}
