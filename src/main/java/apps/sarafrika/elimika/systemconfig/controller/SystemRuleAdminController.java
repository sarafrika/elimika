package apps.sarafrika.elimika.systemconfig.controller;

import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.shared.dto.PagedDTO;
import apps.sarafrika.elimika.systemconfig.dto.SystemRuleRequest;
import apps.sarafrika.elimika.systemconfig.dto.SystemRuleResponse;
import apps.sarafrika.elimika.systemconfig.enums.RuleCategory;
import apps.sarafrika.elimika.systemconfig.enums.RuleStatus;
import apps.sarafrika.elimika.systemconfig.service.SystemRuleAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/system-rules")
@RequiredArgsConstructor
@Tag(name = "System Rules", description = "Administration endpoints for platform-wide configuration rules")
public class SystemRuleAdminController {

    private final SystemRuleAdminService systemRuleAdminService;

    @GetMapping
    @Operation(summary = "List rules", description = "Returns paginated list of system rules with optional filters")
    public ResponseEntity<ApiResponse<PagedDTO<SystemRuleResponse>>> listRules(
            @RequestParam(value = "category", required = false) RuleCategory category,
            @RequestParam(value = "status", required = false) RuleStatus status,
            Pageable pageable
    ) {
        Page<SystemRuleResponse> page = systemRuleAdminService.listRules(category, status, pageable);
        PagedDTO<SystemRuleResponse> payload = PagedDTO.from(
                page,
                ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString()
        );
        return ResponseEntity.ok(ApiResponse.success(payload, "System rules retrieved successfully"));
    }

    @GetMapping("/{uuid}")
    @Operation(summary = "Fetch rule", description = "Fetches a single rule by its UUID")
    public ResponseEntity<ApiResponse<SystemRuleResponse>> getRule(@PathVariable UUID uuid) {
        SystemRuleResponse response = systemRuleAdminService.getRule(uuid);
        return ResponseEntity.ok(ApiResponse.success(response, "System rule retrieved successfully"));
    }

    @PostMapping
    @Operation(summary = "Create rule")
    public ResponseEntity<ApiResponse<SystemRuleResponse>> createRule(
            @Valid @RequestBody SystemRuleRequest request
    ) {
        SystemRuleResponse response = systemRuleAdminService.createRule(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "System rule created successfully"));
    }

    @PutMapping("/{uuid}")
    @Operation(summary = "Update rule")
    public ResponseEntity<ApiResponse<SystemRuleResponse>> updateRule(
            @PathVariable UUID uuid,
            @Valid @RequestBody SystemRuleRequest request
    ) {
        SystemRuleResponse response = systemRuleAdminService.updateRule(uuid, request);
        return ResponseEntity.ok(ApiResponse.success(response, "System rule updated successfully"));
    }
}
