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
import apps.sarafrika.elimika.wallet.security.WalletSecurityService;
import apps.sarafrika.elimika.wallet.service.WalletOwner;
import apps.sarafrika.elimika.wallet.service.WalletService;
import apps.sarafrika.elimika.wallet.service.WalletStanding;
import apps.sarafrika.elimika.wallet.service.WalletTotals;
import apps.sarafrika.elimika.wallet.service.WalletTransferResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import apps.sarafrika.elimika.wallet.enums.WalletTransactionType;
import apps.sarafrika.elimika.wallet.service.WalletTransactionFilter;
import apps.sarafrika.elimika.wallet.util.WalletCsv;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import apps.sarafrika.elimika.wallet.dto.WalletStatementDTO;
import apps.sarafrika.elimika.wallet.service.WalletBalanceSummary;
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
    private final WalletSecurityService walletSecurityService;

    @GetMapping("/{userUuid}")
    @PreAuthorize("@walletSecurityService.canAccessWallet(#userUuid)")
    @Operation(summary = "Get a user's wallet balance")
    public ResponseEntity<ApiResponse<WalletDTO>> getWallet(
            @PathVariable("userUuid") UUID userUuid,
            @RequestParam(value = "currency_code", required = false) String currencyCode
    ) {
        UserWallet wallet = walletService.getOrCreateWallet(userUuid, currencyCode);
        WalletOwner owner = WalletOwner.user(userUuid);
        WalletTotals totals = walletService.totalsOf(owner, currencyCode);
        WalletStanding standing = walletService.standingOf(owner, currencyCode, walletSecurityService.canWithdraw(userUuid));
        return ResponseEntity.ok(ApiResponse.success(WalletMapper.toDto(wallet, totals, standing), "Wallet retrieved successfully"));
    }

    @GetMapping("/{userUuid}/transactions")
    @PreAuthorize("@walletSecurityService.canAccessWallet(#userUuid)")
    @Operation(summary = "List wallet transactions")
    public ResponseEntity<ApiResponse<PagedDTO<WalletTransactionDTO>>> listTransactions(
            @PathVariable("userUuid") UUID userUuid,
            @RequestParam(value = "currency_code", required = false) String currencyCode,
            @RequestParam(value = "type", required = false) List<WalletTransactionType> types,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Pageable pageable
    ) {
        Page<UserWalletTransaction> transactions = walletService.statementOf(
                WalletOwner.user(userUuid), currencyCode, WalletTransactionFilter.of(types, from, to), pageable);
        Page<WalletTransactionDTO> payload = transactions.map(WalletMapper::toDto);
        PagedDTO<WalletTransactionDTO> paged = PagedDTO.from(
                payload,
                ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString()
        );
        return ResponseEntity.ok(ApiResponse.success(paged, "Wallet transactions retrieved successfully"));
    }

    @GetMapping(value = "/{userUuid}/transactions.csv", produces = "text/csv")
    @PreAuthorize("@walletSecurityService.canAccessWallet(#userUuid)")
    @Operation(summary = "Download wallet transactions as CSV",
            description = "Every matching row, newest first; dates are UTC and inclusive.")
    public ResponseEntity<String> exportTransactions(
            @PathVariable("userUuid") UUID userUuid,
            @RequestParam(value = "currency_code", required = false) String currencyCode,
            @RequestParam(value = "type", required = false) List<WalletTransactionType> types,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        String csv = WalletCsv.transactions(walletService.exportOf(
                WalletOwner.user(userUuid), currencyCode, WalletTransactionFilter.of(types, from, to)));
        return csvAttachment(csv, "wallet-transactions.csv");
    }

    @GetMapping("/{userUuid}/statements")
    @PreAuthorize("@walletSecurityService.canAccessWallet(#userUuid)")
    @Operation(summary = "List a user's monthly wallet statements",
            description = "Newest month first, UTC months; quiet months carry the balance over.")
    public ResponseEntity<ApiResponse<List<WalletStatementDTO>>> listStatements(
            @PathVariable("userUuid") UUID userUuid,
            @RequestParam(value = "currency_code", required = false) String currencyCode,
            @RequestParam(value = "months", defaultValue = "12") int months
    ) {
        WalletBalanceSummary balance = walletService.balanceOf(WalletOwner.user(userUuid), currencyCode);
        List<WalletStatementDTO> statements = walletService.monthlyStatementsOf(WalletOwner.user(userUuid), currencyCode, months)
                .stream().map(statement -> WalletStatementDTO.from(statement, balance.currencyCode())).toList();
        return ResponseEntity.ok(ApiResponse.success(statements, "Wallet statements retrieved successfully"));
    }

    @GetMapping("/{userUuid}/audit")
    @PreAuthorize("@walletSecurityService.canAccessWallet(#userUuid)")
    @Operation(summary = "List a user's wallet audit trail", description = "Append-only, newest first.")
    public ResponseEntity<ApiResponse<PagedDTO<WalletAuditEventDTO>>> listAudit(
            @PathVariable("userUuid") UUID userUuid,
            Pageable pageable
    ) {
        Page<WalletAuditEventDTO> events = walletService.auditOf(WalletOwner.user(userUuid), pageable)
                .map(WalletAuditEventDTO::from);
        return ResponseEntity.ok(ApiResponse.success(
                PagedDTO.from(events, ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString()),
                "Wallet audit trail retrieved successfully"));
    }

    @PostMapping("/{userUuid}/deposits")
    @PreAuthorize("@walletSecurityService.canCreditWallet(#userUuid)")
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
    @PreAuthorize("@walletSecurityService.canCreditWallet(#userUuid)")
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

    static ResponseEntity<String> csvAttachment(String csv, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
