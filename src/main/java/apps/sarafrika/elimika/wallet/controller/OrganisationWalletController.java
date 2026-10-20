package apps.sarafrika.elimika.wallet.controller;

import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.shared.dto.PagedDTO;
import apps.sarafrika.elimika.wallet.dto.WalletTransactionDTO;
import apps.sarafrika.elimika.wallet.mapper.WalletMapper;
import apps.sarafrika.elimika.wallet.service.WalletBalanceSummary;
import apps.sarafrika.elimika.wallet.service.WalletOwner;
import apps.sarafrika.elimika.wallet.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** An organisation's earnings from the classes it sells through hired instructors; organisation managers only. */
@RestController
@RequestMapping("/api/v1/organisations/{organisationUuid}/wallet")
@RequiredArgsConstructor
@Tag(name = "Organisation Wallet", description = "Balance and statement of an organisation's earnings")
@PreAuthorize("@organisationSecurityService.canManageOrganisation(#organisationUuid)")
public class OrganisationWalletController {

    private final WalletService walletService;

    @GetMapping
    @Operation(operationId = "getOrganisationWallet", summary = "Get an organisation's wallet balance")
    public ResponseEntity<ApiResponse<WalletBalanceSummary>> getWallet(
            @PathVariable UUID organisationUuid,
            @RequestParam(value = "currency_code", required = false) String currencyCode
    ) {
        WalletBalanceSummary summary = walletService.balanceOf(WalletOwner.organisation(organisationUuid), currencyCode);
        return ResponseEntity.ok(ApiResponse.success(summary, "Organisation wallet retrieved successfully"));
    }

    @GetMapping("/transactions")
    @Operation(operationId = "listOrganisationWalletTransactions", summary = "List an organisation's wallet statement",
            description = "Newest first. A class sale share appears as a SALE row followed by its PLATFORM_FEE row.")
    public ResponseEntity<ApiResponse<PagedDTO<WalletTransactionDTO>>> listTransactions(
            @PathVariable UUID organisationUuid,
            @RequestParam(value = "currency_code", required = false) String currencyCode,
            Pageable pageable
    ) {
        Page<WalletTransactionDTO> payload = walletService
                .statementOf(WalletOwner.organisation(organisationUuid), currencyCode, pageable)
                .map(WalletMapper::toDto);
        PagedDTO<WalletTransactionDTO> paged = PagedDTO.from(
                payload, ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString());
        return ResponseEntity.ok(ApiResponse.success(paged, "Organisation wallet transactions retrieved successfully"));
    }
}
