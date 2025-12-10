package apps.sarafrika.elimika.shared.controller;

import apps.sarafrika.elimika.shared.currency.dto.CurrencyDTO;
import apps.sarafrika.elimika.shared.currency.service.CurrencyService;
import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.shared.dto.PagedDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/currencies")
@Tag(name = "Currencies", description = "Public currency endpoints for platform clients")
@RequiredArgsConstructor
public class CurrencyController {

    private final CurrencyService currencyService;

    @GetMapping
    @Operation(summary = "List platform currencies (paginated)")
    public ResponseEntity<ApiResponse<PagedDTO<CurrencyDTO>>> listCurrencies(Pageable pageable) {
        Page<CurrencyDTO> currencies = currencyService.getCurrencies(pageable);
        PagedDTO<CurrencyDTO> payload = PagedDTO.from(
                currencies,
                ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString()
        );
        return ResponseEntity.ok(ApiResponse.success(payload, "Currencies retrieved successfully"));
    }

    @GetMapping("/default")
    @Operation(summary = "Get the platform default currency")
    public ResponseEntity<ApiResponse<CurrencyDTO>> getDefaultCurrency() {
        CurrencyDTO currency = currencyService.getDefaultCurrency();
        return ResponseEntity.ok(ApiResponse.success(currency, "Default currency retrieved successfully"));
    }
}
