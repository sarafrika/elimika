package apps.sarafrika.elimika.shared.controller.admin;

import apps.sarafrika.elimika.shared.currency.dto.CurrencyCreateRequest;
import apps.sarafrika.elimika.shared.currency.dto.CurrencyDTO;
import apps.sarafrika.elimika.shared.currency.dto.CurrencyUpdateRequest;
import apps.sarafrika.elimika.shared.currency.service.CurrencyService;
import apps.sarafrika.elimika.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/currencies")
@Tag(name = "Admin Currency Management", description = "Administrative endpoints for managing platform currencies")
@RequiredArgsConstructor
@PreAuthorize("@domainSecurityService.isOrganizationAdmin()")
public class CurrencyAdminController {

    private final CurrencyService currencyService;

    @GetMapping
    @Operation(summary = "List all platform currencies (active and inactive)")
    public ResponseEntity<ApiResponse<List<CurrencyDTO>>> listAll() {
        List<CurrencyDTO> currencies = currencyService.getAllCurrencies();
        return ResponseEntity.ok(ApiResponse.success(currencies, "Currencies retrieved successfully"));
    }

    @PostMapping
    @Operation(summary = "Register a new currency")
    public ResponseEntity<ApiResponse<CurrencyDTO>> createCurrency(@Valid @RequestBody CurrencyCreateRequest request) {
        CurrencyDTO created = currencyService.createCurrency(request);
        return ResponseEntity.ok(ApiResponse.success(created, "Currency created successfully"));
    }

    @PutMapping("/{code}")
    @Operation(summary = "Update an existing currency")
    public ResponseEntity<ApiResponse<CurrencyDTO>> updateCurrency(@PathVariable String code,
                                                                   @Valid @RequestBody CurrencyUpdateRequest request) {
        CurrencyDTO updated = currencyService.updateCurrency(code, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Currency updated successfully"));
    }

    @PostMapping("/{code}/default")
    @Operation(summary = "Set the default platform currency")
    public ResponseEntity<ApiResponse<CurrencyDTO>> makeDefault(@PathVariable String code) {
        CurrencyDTO updated = currencyService.setDefaultCurrency(code);
        return ResponseEntity.ok(ApiResponse.success(updated, "Default currency updated successfully"));
    }

    @PostMapping("/{code}/activate")
    @Operation(summary = "Activate a currency for use on the platform")
    public ResponseEntity<ApiResponse<CurrencyDTO>> activate(@PathVariable String code) {
        CurrencyDTO updated = currencyService.toggleCurrency(code, true);
        return ResponseEntity.ok(ApiResponse.success(updated, "Currency activated successfully"));
    }

    @PostMapping("/{code}/deactivate")
    @Operation(summary = "Deactivate a currency, preventing new use on the platform")
    public ResponseEntity<ApiResponse<CurrencyDTO>> deactivate(@PathVariable String code) {
        CurrencyDTO updated = currencyService.toggleCurrency(code, false);
        return ResponseEntity.ok(ApiResponse.success(updated, "Currency deactivated successfully"));
    }
}
