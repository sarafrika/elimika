package apps.sarafrika.elimika.tenancy.controller;

import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.tenancy.dto.CreateSkillsFundSourceRequestDTO;
import apps.sarafrika.elimika.tenancy.dto.CreateSkillsFundTransactionRequestDTO;
import apps.sarafrika.elimika.tenancy.dto.SkillsFundSourceDTO;
import apps.sarafrika.elimika.tenancy.dto.SkillsFundSummaryDTO;
import apps.sarafrika.elimika.tenancy.dto.SkillsFundTransactionDTO;
import apps.sarafrika.elimika.tenancy.services.SkillsFundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1")
@RequiredArgsConstructor
@Tag(name = "Skills Fund API", description = "Organisation skills fund — sources, transactions and computed KPIs")
public class SkillsFundController {

    private final SkillsFundService skillsFundService;

    @Operation(summary = "Get skills fund summary (KPIs)")
    @GetMapping("/organisations/{organisationUuid}/skills-fund/summary")
    public ResponseEntity<ApiResponse<SkillsFundSummaryDTO>> getSummary(@PathVariable UUID organisationUuid) {
        return ResponseEntity.ok(ApiResponse.success(skillsFundService.getSummary(organisationUuid), "Skills fund summary retrieved successfully"));
    }

    @Operation(summary = "List funding sources")
    @GetMapping("/organisations/{organisationUuid}/skills-fund/sources")
    public ResponseEntity<ApiResponse<List<SkillsFundSourceDTO>>> listSources(@PathVariable UUID organisationUuid) {
        return ResponseEntity.ok(ApiResponse.success(skillsFundService.getSources(organisationUuid), "Funding sources retrieved successfully"));
    }

    @Operation(summary = "Add a funding source")
    @PostMapping("/organisations/{organisationUuid}/skills-fund/sources")
    public ResponseEntity<ApiResponse<SkillsFundSourceDTO>> addSource(
            @PathVariable UUID organisationUuid,
            @Valid @RequestBody CreateSkillsFundSourceRequestDTO request) {
        SkillsFundSourceDTO created = skillsFundService.addSource(organisationUuid, request);
        return ResponseEntity.status(201).body(ApiResponse.success(created, "Funding source added successfully"));
    }

    @Operation(summary = "Delete a funding source")
    @DeleteMapping("/skills-fund/sources/{sourceUuid}")
    public ResponseEntity<ApiResponse<Void>> deleteSource(@PathVariable UUID sourceUuid) {
        skillsFundService.deleteSource(sourceUuid);
        return ResponseEntity.ok(ApiResponse.success(null, "Funding source deleted successfully"));
    }

    @Operation(summary = "List fund transactions")
    @GetMapping("/organisations/{organisationUuid}/skills-fund/transactions")
    public ResponseEntity<ApiResponse<List<SkillsFundTransactionDTO>>> listTransactions(@PathVariable UUID organisationUuid) {
        return ResponseEntity.ok(ApiResponse.success(skillsFundService.getTransactions(organisationUuid), "Fund transactions retrieved successfully"));
    }

    @Operation(summary = "Record a fund transaction")
    @PostMapping("/organisations/{organisationUuid}/skills-fund/transactions")
    public ResponseEntity<ApiResponse<SkillsFundTransactionDTO>> addTransaction(
            @PathVariable UUID organisationUuid,
            @Valid @RequestBody CreateSkillsFundTransactionRequestDTO request) {
        SkillsFundTransactionDTO created = skillsFundService.addTransaction(organisationUuid, request);
        return ResponseEntity.status(201).body(ApiResponse.success(created, "Transaction recorded successfully"));
    }

    @Operation(summary = "Delete a fund transaction")
    @DeleteMapping("/skills-fund/transactions/{transactionUuid}")
    public ResponseEntity<ApiResponse<Void>> deleteTransaction(@PathVariable UUID transactionUuid) {
        skillsFundService.deleteTransaction(transactionUuid);
        return ResponseEntity.ok(ApiResponse.success(null, "Transaction deleted successfully"));
    }
}
