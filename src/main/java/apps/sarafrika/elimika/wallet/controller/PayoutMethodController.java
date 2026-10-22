package apps.sarafrika.elimika.wallet.controller;

import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.wallet.dto.PayoutMethodCreatedDTO;
import apps.sarafrika.elimika.wallet.dto.PayoutMethodDTO;
import apps.sarafrika.elimika.wallet.dto.PayoutMethodRequestDTO;
import apps.sarafrika.elimika.wallet.dto.VerificationCodeDTO;
import apps.sarafrika.elimika.wallet.dto.VerifyCodeRequestDTO;
import apps.sarafrika.elimika.wallet.service.PayoutMethodService;
import apps.sarafrika.elimika.wallet.service.WalletOwner;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** A person's own payout numbers; only they may see or change them. */
@RestController
@RequestMapping("/api/v1/wallets/{userUuid}/payout-methods")
@Tag(name = "Payout methods", description = "Where a user's withdrawals go")
@RequiredArgsConstructor
@PreAuthorize("@walletSecurityService.canManagePayouts(#userUuid)")
public class PayoutMethodController {

    private final PayoutMethodService payoutMethodService;

    @GetMapping
    @Operation(operationId = "listPayoutMethods", summary = "List my payout numbers", description = "Default first.")
    public ResponseEntity<ApiResponse<List<PayoutMethodDTO>>> list(@PathVariable UUID userUuid) {
        return ResponseEntity.ok(ApiResponse.success(payoutMethodService.list(WalletOwner.user(userUuid)).stream()
                .map(PayoutMethodDTO::from).toList(), "Payout methods retrieved successfully"));
    }

    @PostMapping
    @Operation(operationId = "addPayoutMethod", summary = "Add an M-Pesa number",
            description = "Saved as pending; a six-digit code is sent to it by SMS.")
    public ResponseEntity<ApiResponse<PayoutMethodCreatedDTO>> add(@PathVariable UUID userUuid,
                                                                   @Valid @RequestBody PayoutMethodRequestDTO request) {
        PayoutMethodService.AddedPayoutMethod added = payoutMethodService.add(WalletOwner.user(userUuid),
                request.msisdn(), request.label(), request.confirmsRegisteredName());
        return ResponseEntity.ok(ApiResponse.success(new PayoutMethodCreatedDTO(PayoutMethodDTO.from(added.method()),
                VerificationCodeDTO.from(added.verification())), "Verification code sent"));
    }

    @PostMapping("/{methodUuid}/verification-code")
    @Operation(operationId = "resendPayoutMethodCode", summary = "Send a new verification code")
    public ResponseEntity<ApiResponse<VerificationCodeDTO>> resend(@PathVariable UUID userUuid, @PathVariable UUID methodUuid) {
        return ResponseEntity.ok(ApiResponse.success(VerificationCodeDTO.from(
                payoutMethodService.resendCode(WalletOwner.user(userUuid), methodUuid)), "Verification code sent"));
    }

    @PostMapping("/{methodUuid}/verify")
    @Operation(operationId = "verifyPayoutMethod", summary = "Confirm a number with its code",
            description = "A verified number can receive money once its hold ends.")
    public ResponseEntity<ApiResponse<PayoutMethodDTO>> verify(@PathVariable UUID userUuid, @PathVariable UUID methodUuid,
                                                               @Valid @RequestBody VerifyCodeRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success(PayoutMethodDTO.from(
                payoutMethodService.verify(WalletOwner.user(userUuid), methodUuid, request.code())), "Number verified"));
    }

    @PutMapping("/{methodUuid}/default")
    @Operation(operationId = "makePayoutMethodDefault", summary = "Make a verified number the default")
    public ResponseEntity<ApiResponse<PayoutMethodDTO>> makeDefault(@PathVariable UUID userUuid, @PathVariable UUID methodUuid) {
        return ResponseEntity.ok(ApiResponse.success(PayoutMethodDTO.from(
                payoutMethodService.makeDefault(WalletOwner.user(userUuid), methodUuid)), "Default payout number updated"));
    }

    @DeleteMapping("/{methodUuid}")
    @Operation(operationId = "removePayoutMethod", summary = "Remove a payout number")
    public ResponseEntity<ApiResponse<Void>> remove(@PathVariable UUID userUuid, @PathVariable UUID methodUuid) {
        payoutMethodService.remove(WalletOwner.user(userUuid), methodUuid);
        return ResponseEntity.ok(ApiResponse.success(null, "Payout number removed"));
    }
}
