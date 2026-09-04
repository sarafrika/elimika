package apps.sarafrika.elimika.payout.controller;

import apps.sarafrika.elimika.payout.dto.InstructorObligationCancellationRequestDTO;
import apps.sarafrika.elimika.payout.dto.InstructorObligationDTO;
import apps.sarafrika.elimika.payout.dto.InstructorObligationSettlementRequestDTO;
import apps.sarafrika.elimika.payout.dto.InstructorStatementDTO;
import apps.sarafrika.elimika.payout.dto.MonthlyPayoutPointDTO;
import apps.sarafrika.elimika.payout.enums.InstructorObligationStatus;
import apps.sarafrika.elimika.payout.service.InstructorObligationService;
import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.shared.dto.PagedDTO;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

import java.util.List;
import java.util.UUID;

/**
 * The ledger of what organisations owe their instructors for delivered sessions.
 * <p>
 * Settlement here is a record, not a transfer. The organisation pays the instructor by its own means
 * and then says so, with its own reference; no money moves through the platform. The endpoints
 * therefore read as bookkeeping: list what is owed, mark a row paid against evidence, withdraw a row
 * that should never have existed, and let the instructor see the same ledger from their side.
 */
@RestController
@RequestMapping("api/v1")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Instructor Obligations API",
        description = "What an organisation owes its instructors for delivered sessions, and the record of paying it")
public class InstructorObligationController {

    /**
     * Obligations are organisation money, and each row names an instructor and what they are owed —
     * a private figure between the organisation and that instructor. Reading the ledger is therefore
     * a management act, as is asserting that a row has been paid or withdrawing one. An instructor
     * reads their own side of it through the statement routes below, which are scoped to them.
     */
    private static final String MANAGE_ORGANISATION =
            "@organisationSecurityService.canManageOrganisation(#organisationUuid)";
    private static final String READ_STATEMENT =
            "@instructorObligationSecurityService.canReadStatement(#instructorUserUuid)";

    private final InstructorObligationService instructorObligationService;
    private final DomainSecurityService domainSecurityService;

    @Operation(summary = "List an organisation's instructor obligations",
            description = "One row per delivered session, at the rate that stood when the session completed. "
                    + "Optionally narrowed to a single instructor and/or status.")
    @GetMapping("/organisations/{organisationUuid}/instructor-obligations")
    @PreAuthorize(MANAGE_ORGANISATION)
    public ResponseEntity<ApiResponse<PagedDTO<InstructorObligationDTO>>> listObligations(
            @Parameter(description = "UUID of the organisation", required = true)
            @PathVariable UUID organisationUuid,
            @Parameter(description = "Narrow to a single instructor profile")
            @RequestParam(required = false) UUID instructorUuid,
            @Parameter(description = "Narrow to ACCRUED, SETTLED, CANCELLED or DISPUTED")
            @RequestParam(required = false) InstructorObligationStatus status,
            @PageableDefault(size = 20, sort = "accruedAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<InstructorObligationDTO> page = instructorObligationService
                .findForOrganisation(organisationUuid, instructorUuid, status, pageable);
        String baseUrl = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString();
        return ResponseEntity.ok(ApiResponse.success(
                PagedDTO.from(page, baseUrl), "Instructor obligations retrieved successfully"));
    }

    @Operation(summary = "Monthly settled payouts for an organisation",
            description = "Money the organisation has actually paid out to instructors, one figure per "
                    + "calendar month over the trailing window (inclusive of the current month), oldest first.")
    @GetMapping("/organisations/{organisationUuid}/instructor-obligations/monthly-settlements")
    @PreAuthorize(MANAGE_ORGANISATION)
    public ResponseEntity<ApiResponse<List<MonthlyPayoutPointDTO>>> getMonthlySettlements(
            @Parameter(description = "UUID of the organisation", required = true)
            @PathVariable UUID organisationUuid,
            @Parameter(description = "Number of months to include (inclusive of the current month)")
            @RequestParam(defaultValue = "6") int months) {

        List<MonthlyPayoutPointDTO> settlements =
                instructorObligationService.getMonthlySettlements(organisationUuid, months);
        return ResponseEntity.ok(ApiResponse.success(
                settlements, "Monthly settled payouts retrieved successfully"));
    }

    @Operation(summary = "Record that an obligation has been paid",
            description = "Marks a single obligation settled against the organisation's own payment reference. "
                    + "The platform does not move the money; this records that the organisation did.")
    @PostMapping("/organisations/{organisationUuid}/instructor-obligations/{obligationUuid}/settle")
    @PreAuthorize(MANAGE_ORGANISATION)
    public ResponseEntity<ApiResponse<InstructorObligationDTO>> settleObligation(
            @Parameter(description = "UUID of the organisation that paid", required = true)
            @PathVariable UUID organisationUuid,
            @Parameter(description = "UUID of the obligation being settled", required = true)
            @PathVariable UUID obligationUuid,
            @Valid @RequestBody InstructorObligationSettlementRequestDTO request) {

        InstructorObligationDTO settled = instructorObligationService.settle(
                organisationUuid,
                obligationUuid,
                request.settlementReference(),
                request.note(),
                actingUser());
        return ResponseEntity.ok(ApiResponse.success(settled, "Instructor obligation marked as settled"));
    }

    @Operation(summary = "Withdraw an obligation that should never have accrued",
            description = "Excludes the obligation from what is owed while keeping the row and the reason. "
                    + "An already-settled obligation cannot be withdrawn.")
    @PostMapping("/organisations/{organisationUuid}/instructor-obligations/{obligationUuid}/cancel")
    @PreAuthorize(MANAGE_ORGANISATION)
    public ResponseEntity<ApiResponse<InstructorObligationDTO>> cancelObligation(
            @Parameter(description = "UUID of the organisation", required = true)
            @PathVariable UUID organisationUuid,
            @Parameter(description = "UUID of the obligation being withdrawn", required = true)
            @PathVariable UUID obligationUuid,
            @Valid @RequestBody InstructorObligationCancellationRequestDTO request) {

        InstructorObligationDTO cancelled = instructorObligationService.cancel(
                organisationUuid, obligationUuid, request.reason(), actingUser());
        return ResponseEntity.ok(ApiResponse.success(cancelled, "Instructor obligation cancelled"));
    }

    @Operation(summary = "An instructor's statement of what they are owed",
            description = "Per-organisation summary of outstanding and settled pay for delivered sessions.")
    @GetMapping("/instructors/users/{instructorUserUuid}/obligation-statement")
    @PreAuthorize(READ_STATEMENT)
    public ResponseEntity<ApiResponse<InstructorStatementDTO>> getStatement(
            @Parameter(description = "Platform user UUID of the instructor", required = true)
            @PathVariable UUID instructorUserUuid) {

        InstructorStatementDTO statement = instructorObligationService.getStatement(instructorUserUuid);
        return ResponseEntity.ok(ApiResponse.success(statement, "Instructor statement retrieved successfully"));
    }

    @Operation(summary = "An instructor's own obligation rows",
            description = "The session-by-session detail behind the statement, newest first.")
    @GetMapping("/instructors/users/{instructorUserUuid}/instructor-obligations")
    @PreAuthorize(READ_STATEMENT)
    public ResponseEntity<ApiResponse<PagedDTO<InstructorObligationDTO>>> listInstructorObligations(
            @Parameter(description = "Platform user UUID of the instructor", required = true)
            @PathVariable UUID instructorUserUuid,
            @PageableDefault(size = 20, sort = "accruedAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<InstructorObligationDTO> page =
                instructorObligationService.findForInstructorUser(instructorUserUuid, pageable);
        String baseUrl = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString();
        return ResponseEntity.ok(ApiResponse.success(
                PagedDTO.from(page, baseUrl), "Instructor obligations retrieved successfully"));
    }

    /**
     * Who is recording the settlement. A settlement with no name attached is an unattributable claim
     * that a debt disappeared, so this refuses rather than defaulting to "system".
     */
    private String actingUser() {
        UUID callerUuid = domainSecurityService.getCurrentUserUuid();
        if (callerUuid == null) {
            throw new IllegalStateException("The acting user could not be determined from the request");
        }
        return callerUuid.toString();
    }
}
