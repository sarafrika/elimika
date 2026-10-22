package apps.sarafrika.elimika.wallet.controller;

import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.shared.dto.PagedDTO;
import apps.sarafrika.elimika.wallet.dto.WalletTransactionDTO;
import apps.sarafrika.elimika.wallet.mapper.WalletMapper;
import apps.sarafrika.elimika.wallet.service.WalletBalanceSummary;
import apps.sarafrika.elimika.wallet.service.WalletOwner;
import apps.sarafrika.elimika.wallet.security.WalletSecurityService;
import apps.sarafrika.elimika.wallet.service.WalletService;
import apps.sarafrika.elimika.wallet.service.WalletStanding;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import apps.sarafrika.elimika.wallet.enums.WalletTransactionType;
import apps.sarafrika.elimika.wallet.service.WalletTransactionFilter;
import apps.sarafrika.elimika.wallet.util.WalletCsv;
import java.time.LocalDate;
import java.util.List;
import apps.sarafrika.elimika.wallet.dto.WalletStatementDTO;
import apps.sarafrika.elimika.wallet.dto.WalletAuditEventDTO;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
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
    private final WalletSecurityService walletSecurityService;

    @GetMapping
    @Operation(operationId = "getOrganisationWallet", summary = "Get an organisation's wallet balance")
    public ResponseEntity<ApiResponse<WalletBalanceSummary>> getWallet(
            @PathVariable UUID organisationUuid,
            @RequestParam(value = "currency_code", required = false) String currencyCode
    ) {
        WalletOwner owner = WalletOwner.organisation(organisationUuid);
        WalletBalanceSummary summary = walletService.balanceOf(owner, currencyCode);
        WalletStanding standing = walletService.standingOf(
                owner, currencyCode, walletSecurityService.canWithdrawFromOrganisation(organisationUuid));
        return ResponseEntity.ok(ApiResponse.success(summary.withTotals(walletService.totalsOf(owner, currencyCode))
                        .withStanding(standing),
                "Organisation wallet retrieved successfully"));
    }

    @GetMapping("/transactions")
    @Operation(operationId = "listOrganisationWalletTransactions", summary = "List an organisation's wallet statement",
            description = "Newest first. A class sale share appears as a SALE row followed by its PLATFORM_FEE row.")
    public ResponseEntity<ApiResponse<PagedDTO<WalletTransactionDTO>>> listTransactions(
            @PathVariable UUID organisationUuid,
            @RequestParam(value = "currency_code", required = false) String currencyCode,
            @RequestParam(value = "type", required = false) List<WalletTransactionType> types,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Pageable pageable
    ) {
        Page<WalletTransactionDTO> payload = walletService
                .statementOf(WalletOwner.organisation(organisationUuid), currencyCode,
                        WalletTransactionFilter.of(types, from, to), pageable)
                .map(WalletMapper::toDto);
        PagedDTO<WalletTransactionDTO> paged = PagedDTO.from(
                payload, ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString());
        return ResponseEntity.ok(ApiResponse.success(paged, "Organisation wallet transactions retrieved successfully"));
    }

    @GetMapping(value = "/transactions.csv", produces = "text/csv")
    @Operation(operationId = "exportOrganisationWalletTransactions",
            summary = "Download an organisation's wallet transactions as CSV",
            description = "Every matching row, newest first; dates are UTC and inclusive.")
    public ResponseEntity<String> exportTransactions(
            @PathVariable UUID organisationUuid,
            @RequestParam(value = "currency_code", required = false) String currencyCode,
            @RequestParam(value = "type", required = false) List<WalletTransactionType> types,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        String csv = WalletCsv.transactions(walletService.exportOf(
                WalletOwner.organisation(organisationUuid), currencyCode, WalletTransactionFilter.of(types, from, to)));
        return WalletController.csvAttachment(csv, "organisation-wallet-transactions.csv");
    }

    @GetMapping("/statements")
    @Operation(operationId = "listOrganisationWalletStatements", summary = "List an organisation's monthly wallet statements",
            description = "Newest month first, UTC months; quiet months carry the balance over.")
    public ResponseEntity<ApiResponse<List<WalletStatementDTO>>> listStatements(
            @PathVariable UUID organisationUuid,
            @RequestParam(value = "currency_code", required = false) String currencyCode,
            @RequestParam(value = "months", defaultValue = "12") int months
    ) {
        WalletOwner owner = WalletOwner.organisation(organisationUuid);
        String currency = walletService.balanceOf(owner, currencyCode).currencyCode();
        List<WalletStatementDTO> statements = walletService.monthlyStatementsOf(owner, currencyCode, months)
                .stream().map(statement -> WalletStatementDTO.from(statement, currency)).toList();
        return ResponseEntity.ok(ApiResponse.success(statements, "Organisation wallet statements retrieved successfully"));
    }

    @GetMapping("/audit")
    @Operation(operationId = "listOrganisationWalletAudit", summary = "List an organisation's wallet audit trail",
            description = "Append-only, newest first.")
    public ResponseEntity<ApiResponse<PagedDTO<WalletAuditEventDTO>>> listAudit(
            @PathVariable UUID organisationUuid,
            Pageable pageable
    ) {
        Page<WalletAuditEventDTO> events = walletService.auditOf(WalletOwner.organisation(organisationUuid), pageable)
                .map(WalletAuditEventDTO::from);
        return ResponseEntity.ok(ApiResponse.success(
                PagedDTO.from(events, ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString()),
                "Organisation wallet audit trail retrieved successfully"));
    }
}
