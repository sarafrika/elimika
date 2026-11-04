package apps.sarafrika.elimika.wallet.controller;

import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.shared.dto.PagedDTO;
import apps.sarafrika.elimika.wallet.dto.SaleSplitDTO;
import apps.sarafrika.elimika.wallet.dto.PurseTransferRequestDTO;
import apps.sarafrika.elimika.wallet.dto.PurseTransferResponseDTO;
import apps.sarafrika.elimika.wallet.dto.WalletCreditRequest;
import apps.sarafrika.elimika.wallet.dto.WalletDTO;
import apps.sarafrika.elimika.wallet.dto.WalletTransactionDTO;
import apps.sarafrika.elimika.wallet.dto.WalletTransferRequest;
import apps.sarafrika.elimika.wallet.dto.WalletTransferResponse;
import apps.sarafrika.elimika.wallet.entity.UserWallet;
import apps.sarafrika.elimika.wallet.entity.UserWalletTransaction;
import apps.sarafrika.elimika.wallet.mapper.WalletMapper;
import apps.sarafrika.elimika.wallet.security.WalletSecurityService;
import apps.sarafrika.elimika.wallet.service.SaleSplitService;
import apps.sarafrika.elimika.wallet.service.PurseTransferResult;
import apps.sarafrika.elimika.wallet.service.WalletOwner;
import apps.sarafrika.elimika.wallet.service.WalletPurse;
import apps.sarafrika.elimika.wallet.service.WalletPurseBalance;
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
import java.util.Map;
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
    private final SaleSplitService saleSplitService;

    @GetMapping("/{userUuid}")
    @PreAuthorize("@walletSecurityService.canAccessWallet(#userUuid)")
    @Operation(summary = "Get a user's wallet balance",
            description = "One balance per purse (COURSE_CREATOR, INSTRUCTOR, STUDENT) in purses[], each with what is "
                    + "held and available; the top-level figures are their sums. Creates nothing.")
    public ResponseEntity<ApiResponse<WalletDTO>> getWallet(
            @PathVariable("userUuid") UUID userUuid,
            @RequestParam(value = "currency_code", required = false) String currencyCode
    ) {
        WalletOwner owner = WalletOwner.user(userUuid);
        List<WalletPurseBalance> purses = walletService.pursesOf(owner, currencyCode);
        WalletTotals totals = walletService.totalsOf(owner, currencyCode);
        WalletStanding standing = walletService.standingOf(owner, currencyCode, walletSecurityService.canWithdraw(userUuid));
        return ResponseEntity.ok(ApiResponse.success(
                WalletMapper.toDto(owner, purses.getFirst().currencyCode(), purses, totals, standing),
                "Wallet retrieved successfully"));
    }

    @GetMapping("/{userUuid}/transactions")
    @PreAuthorize("@walletSecurityService.canAccessWallet(#userUuid)")
    @Operation(summary = "List wallet transactions",
            description = "Newest first, every purse unless purse is given; each row says which purse it moved.")
    public ResponseEntity<ApiResponse<PagedDTO<WalletTransactionDTO>>> listTransactions(
            @PathVariable("userUuid") UUID userUuid,
            @RequestParam(value = "currency_code", required = false) String currencyCode,
            @RequestParam(value = "purse", required = false) WalletPurse purse,
            @RequestParam(value = "type", required = false) List<WalletTransactionType> types,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Pageable pageable
    ) {
        Page<UserWalletTransaction> transactions = walletService.statementOf(
                WalletOwner.user(userUuid), currencyCode, WalletTransactionFilter.of(types, from, to, purse), pageable);
        Map<UUID, UUID> splits = saleSplitService.splitUuidsOf(transactions.getContent());
        Page<WalletTransactionDTO> payload = transactions.map(txn -> WalletMapper.toDto(txn, splits.get(txn.getUuid())));
        PagedDTO<WalletTransactionDTO> paged = PagedDTO.from(
                payload,
                ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString()
        );
        return ResponseEntity.ok(ApiResponse.success(paged, "Wallet transactions retrieved successfully"));
    }

    @GetMapping(value = "/{userUuid}/transactions.csv", produces = "text/csv")
    @PreAuthorize("@walletSecurityService.canAccessWallet(#userUuid)")
    @Operation(summary = "Download wallet transactions as CSV",
            description = "Every matching row, newest first; dates are UTC and inclusive. Every purse unless purse is given.")
    public ResponseEntity<String> exportTransactions(
            @PathVariable("userUuid") UUID userUuid,
            @RequestParam(value = "currency_code", required = false) String currencyCode,
            @RequestParam(value = "purse", required = false) WalletPurse purse,
            @RequestParam(value = "type", required = false) List<WalletTransactionType> types,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        String csv = WalletCsv.transactions(walletService.exportOf(
                WalletOwner.user(userUuid), currencyCode, WalletTransactionFilter.of(types, from, to, purse)));
        return csvAttachment(csv, "wallet-transactions.csv");
    }

    @GetMapping("/{userUuid}/statements")
    @PreAuthorize("@walletSecurityService.canAccessWallet(#userUuid)")
    @Operation(summary = "List a user's monthly wallet statements",
            description = "Newest month first, UTC months; quiet months carry the balance over. One purse when purse is "
                    + "given, otherwise every purse summed, leaving out money moved between the user's own purses.")
    public ResponseEntity<ApiResponse<List<WalletStatementDTO>>> listStatements(
            @PathVariable("userUuid") UUID userUuid,
            @RequestParam(value = "currency_code", required = false) String currencyCode,
            @RequestParam(value = "purse", required = false) WalletPurse purse,
            @RequestParam(value = "months", defaultValue = "12") int months
    ) {
        WalletBalanceSummary balance = walletService.balanceOf(WalletOwner.user(userUuid), currencyCode);
        List<WalletStatementDTO> statements = walletService.monthlyStatementsOf(WalletOwner.user(userUuid), purse,
                        currencyCode, months)
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

    @GetMapping("/{userUuid}/sales")
    @PreAuthorize("@walletSecurityService.canAccessWallet(#userUuid)")
    @Operation(operationId = "listWalletSales", summary = "List the sales that paid this user, with each split",
            description = "Newest first. Each captured class sale line this user was paid on as course creator or "
                    + "instructor, with the creator's and the delivery party's gross, fee and net, the platform fee, "
                    + "anything retained and the fee rule in force at purchase. The other party is named only. With "
                    + "purse=COURSE_CREATOR only creator legs, with purse=INSTRUCTOR only delivery legs; your_role, "
                    + "your_purse and your_* then describe that leg.")
    public ResponseEntity<ApiResponse<PagedDTO<SaleSplitDTO>>> listSales(
            @PathVariable("userUuid") UUID userUuid,
            @RequestParam(value = "purse", required = false) WalletPurse purse,
            Pageable pageable
    ) {
        Page<SaleSplitDTO> sales = saleSplitService.salesOf(WalletOwner.user(userUuid), purse, pageable);
        return ResponseEntity.ok(ApiResponse.success(
                PagedDTO.from(sales, ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString()),
                "Wallet sales retrieved successfully"));
    }

    @GetMapping("/{userUuid}/sales/{splitUuid}")
    @PreAuthorize("@walletSecurityService.canAccessWallet(#userUuid)")
    @Operation(operationId = "getWalletSale", summary = "Get one sale split this user was paid on")
    public ResponseEntity<ApiResponse<SaleSplitDTO>> getSale(
            @PathVariable("userUuid") UUID userUuid,
            @PathVariable("splitUuid") UUID splitUuid,
            @RequestParam(value = "purse", required = false) WalletPurse purse
    ) {
        return ResponseEntity.ok(ApiResponse.success(saleSplitService.saleOf(WalletOwner.user(userUuid), purse, splitUuid),
                "Wallet sale retrieved successfully"));
    }

    @PostMapping("/{userUuid}/deposits")
    @PreAuthorize("@walletSecurityService.canCreditWallet(#userUuid)")
    @Operation(summary = "Record a wallet deposit", description = "Into the STUDENT purse unless purse says otherwise.")
    public ResponseEntity<ApiResponse<WalletDTO>> deposit(
            @PathVariable("userUuid") UUID userUuid,
            @Valid @RequestBody WalletCreditRequest request
    ) {
        UserWallet wallet = walletService.deposit(
                userUuid,
                request.purse(),
                request.amount(),
                request.currencyCode(),
                request.reference(),
                request.description()
        );
        return ResponseEntity.ok(ApiResponse.success(WalletMapper.toDto(wallet), "Wallet deposit recorded successfully"));
    }

    @PostMapping("/{userUuid}/sales")
    @PreAuthorize("@walletSecurityService.canCreditWallet(#userUuid)")
    @Operation(summary = "Record a wallet sale credit", description = "purse is required: COURSE_CREATOR or INSTRUCTOR.")
    public ResponseEntity<ApiResponse<WalletDTO>> creditSale(
            @PathVariable("userUuid") UUID userUuid,
            @Valid @RequestBody WalletCreditRequest request
    ) {
        UserWallet wallet = walletService.creditSale(
                userUuid,
                request.purse(),
                request.amount(),
                request.currencyCode(),
                request.reference(),
                request.description()
        );
        return ResponseEntity.ok(ApiResponse.success(WalletMapper.toDto(wallet), "Wallet sale credit recorded successfully"));
    }

    @PostMapping("/{userUuid}/transfers")
    @PreAuthorize("@walletSecurityService.canTransferFrom(#userUuid)")
    @Operation(summary = "Transfer funds between wallets",
            description = "From the sender's source_purse (or their only funded purse) into the receiver's STUDENT purse.")
    public ResponseEntity<ApiResponse<WalletTransferResponse>> transfer(
            @PathVariable("userUuid") UUID userUuid,
            @Valid @RequestBody WalletTransferRequest request
    ) {
        WalletTransferResult result = walletService.transfer(
                userUuid,
                request.sourcePurse(),
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

    @PostMapping("/{userUuid}/purse-transfers")
    @PreAuthorize("@walletSecurityService.canMoveBetweenPurses(#userUuid)")
    @Operation(operationId = "transferBetweenPurses", summary = "Move money between two of your own purses",
            description = "A TRANSFER_OUT row on from_purse and a TRANSFER_IN row on to_purse with one "
                    + "transfer_reference, balanced in the ledger, no fee. Refused when from_purse holds less.")
    public ResponseEntity<ApiResponse<PurseTransferResponseDTO>> transferBetweenPurses(
            @PathVariable("userUuid") UUID userUuid,
            @Valid @RequestBody PurseTransferRequestDTO request
    ) {
        PurseTransferResult result = walletService.transferBetweenPurses(WalletOwner.user(userUuid),
                request.fromPurse(), request.toPurse(), request.amount(), request.currencyCode(), request.reference(),
                request.description());
        return ResponseEntity.ok(ApiResponse.success(new PurseTransferResponseDTO(result.transferReference(),
                request.amount(), result.fromWallet().getCurrencyCode(), WalletMapper.toDto(result.fromWallet()),
                WalletMapper.toDto(result.toWallet())), "Money moved between purses"));
    }

    static ResponseEntity<String> csvAttachment(String csv, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
