package apps.sarafrika.elimika.wallet.controller;

import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.wallet.dto.PayoutRulesDTO;
import apps.sarafrika.elimika.wallet.service.PayoutSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** The withdrawal rules any signed-in earner reads before withdrawing; admins change them elsewhere. */
@RestController
@Tag(name = "Withdrawals")
@RequiredArgsConstructor
public class PayoutRulesController {

    private final PayoutSettingsService settingsService;

    @Value("${wallet.payout-methods.hold:PT24H}")
    private Duration hold;

    @Value("${wallet.withdrawals.enabled:false}")
    private boolean enabled;

    @GetMapping("/api/v1/wallets/payout-rules")
    @Operation(operationId = "getPayoutRules", summary = "Withdrawal limits, the M-Pesa charge and the new-number hold")
    public ResponseEntity<ApiResponse<PayoutRulesDTO>> rules() {
        return ResponseEntity.ok(ApiResponse.success(PayoutRulesDTO.from(settingsService.current(), hold.toHours(), enabled),
                "Payout rules retrieved successfully"));
    }
}
