package apps.sarafrika.elimika.wallet.controller;

import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.shared.dto.PagedDTO;
import apps.sarafrika.elimika.wallet.dto.WalletCreditRequest;
import apps.sarafrika.elimika.wallet.dto.WalletDTO;
import apps.sarafrika.elimika.wallet.dto.WalletTransactionDTO;
import apps.sarafrika.elimika.wallet.dto.WalletTransferRequest;
import apps.sarafrika.elimika.wallet.dto.WalletTransferResponse;
import apps.sarafrika.elimika.wallet.entity.UserWallet;
import apps.sarafrika.elimika.wallet.entity.UserWalletTransaction;
import apps.sarafrika.elimika.wallet.mapper.WalletMapper;
import apps.sarafrika.elimika.wallet.service.WalletService;
import apps.sarafrika.elimika.wallet.service.WalletTransferResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/wallets")
@Tag(name = "Wallets", description = "Wallet balances and ledger operations")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/{userUuid}")
    @PreAuthorize("@walletSecurityService.canAccessWallet(#userUuid)")
    @Operation(summary = "Get a user's wallet balance")
    public ResponseEntity<ApiResponse<WalletDTO>> getWallet(
            @PathVariable("userUuid") UUID userUuid,
            @RequestParam(value = "currency_code", required = false) String currencyCode
    ) {
        UserWallet wallet = walletService.getOrCreateWallet(userUuid, currencyCode);
        return ResponseEntity.ok(ApiResponse.success(WalletMapper.toDto(wallet), "Wallet retrieved successfully"));
    }

    @GetMapping("/{userUuid}/transactions")
    @PreAuthorize("@walletSecurityService.canAccessWallet(#userUuid)")
    @Operation(summary = "List wallet transactions")
    public ResponseEntity<ApiResponse<PagedDTO<WalletTransactionDTO>>> listTransactions(
            @PathVariable("userUuid") UUID userUuid,
            @RequestParam(value = "currency_code", required = false) String currencyCode,
            Pageable pageable
    ) {
        Page<UserWalletTransaction> transactions = walletService.getTransactions(userUuid, currencyCode, pageable);
        Page<WalletTransactionDTO> payload = transactions.map(WalletMapper::toDto);
        PagedDTO<WalletTransactionDTO> paged = PagedDTO.from(
                payload,
                ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString()
        );
        return ResponseEntity.ok(ApiResponse.success(paged, "Wallet transactions retrieved successfully"));
    }

    @PostMapping("/{userUuid}/deposits")
    @PreAuthorize("@domainSecurityService.isOrganizationAdmin()")
    @Operation(summary = "Record a wallet deposit")
    public ResponseEntity<ApiResponse<WalletDTO>> deposit(
            @PathVariable("userUuid") UUID userUuid,
            @Valid @RequestBody WalletCreditRequest request
    ) {
        UserWallet wallet = walletService.deposit(
                userUuid,
                request.amount(),
                request.currencyCode(),
                request.reference(),
                request.description()
        );
        return ResponseEntity.ok(ApiResponse.success(WalletMapper.toDto(wallet), "Wallet deposit recorded successfully"));
    }

    @PostMapping("/{userUuid}/sales")
    @PreAuthorize("@domainSecurityService.isOrganizationAdmin()")
    @Operation(summary = "Record a wallet sale credit")
    public ResponseEntity<ApiResponse<WalletDTO>> creditSale(
            @PathVariable("userUuid") UUID userUuid,
            @Valid @RequestBody WalletCreditRequest request
    ) {
        UserWallet wallet = walletService.creditSale(
                userUuid,
                request.amount(),
                request.currencyCode(),
                request.reference(),
                request.description()
        );
        return ResponseEntity.ok(ApiResponse.success(WalletMapper.toDto(wallet), "Wallet sale credit recorded successfully"));
    }

    @PostMapping("/{userUuid}/transfers")
    @PreAuthorize("@walletSecurityService.canTransferFrom(#userUuid)")
    @Operation(summary = "Transfer funds between wallets")
    public ResponseEntity<ApiResponse<WalletTransferResponse>> transfer(
            @PathVariable("userUuid") UUID userUuid,
            @Valid @RequestBody WalletTransferRequest request
    ) {
        WalletTransferResult result = walletService.transfer(
                userUuid,
                request.targetUserUuid(),
                request.amount(),
                request.currencyCode(),
                request.reference(),
                request.description()
        );

        WalletTransferResponse response = new WalletTransferResponse(
                result.transferReference(),
                request.amount(),
                result.sourceWallet().getCurrencyCode(),
                WalletMapper.toDto(result.sourceWallet()),
                WalletMapper.toDto(result.targetWallet())
        );

        return ResponseEntity.ok(ApiResponse.success(response, "Wallet transfer completed successfully"));
    }
}
