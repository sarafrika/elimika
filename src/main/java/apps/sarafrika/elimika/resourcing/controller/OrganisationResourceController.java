package apps.sarafrika.elimika.resourcing.controller;

import apps.sarafrika.elimika.resourcing.dto.OrganisationResourceDTO;
import apps.sarafrika.elimika.resourcing.dto.ResourceAvailabilityRuleDTO;
import apps.sarafrika.elimika.resourcing.dto.ResourceBookingDTO;
import apps.sarafrika.elimika.resourcing.dto.ResourceCalendarEntryDTO;
import apps.sarafrika.elimika.resourcing.service.OrganisationResourceServiceInterface;
import apps.sarafrika.elimika.resourcing.spi.ResourceBookingStatus;
import apps.sarafrika.elimika.resourcing.spi.ResourceType;
import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.shared.dto.PagedDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping(OrganisationResourceController.API_ROOT_PATH)
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Organisation Resources", description = "Bookable venues and equipment pools registered by organisations, with calendar-style availability management")
public class OrganisationResourceController {

    public static final String API_ROOT_PATH = "/api/v1/organisations/{organisationUuid}/resources";

    private final OrganisationResourceServiceInterface resourceService;

    @Operation(summary = "Register a bookable resource (venue or equipment pool)")
    @PostMapping
    public ResponseEntity<ApiResponse<OrganisationResourceDTO>> createResource(
            @PathVariable UUID organisationUuid,
            @Valid @RequestBody OrganisationResourceDTO request) {
        OrganisationResourceDTO result = resourceService.createResource(organisationUuid, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(result, "Organisation resource created successfully"));
    }

    @Operation(summary = "List the organisation's resources")
    @GetMapping
    public ResponseEntity<ApiResponse<PagedDTO<OrganisationResourceDTO>>> listResources(
            @PathVariable UUID organisationUuid,
            @RequestParam(value = "resource_type", required = false) String resourceType,
            @RequestParam(value = "branch_uuid", required = false) UUID branchUuid,
            @RequestParam(value = "active", required = false) Boolean active,
            Pageable pageable) {
        Optional<ResourceType> typeFilter = Optional.ofNullable(resourceType)
                .filter(value -> !value.isBlank())
                .map(ResourceType::fromValue);
        Page<OrganisationResourceDTO> page = resourceService.listResources(
                organisationUuid, typeFilter.orElse(null), branchUuid, active, pageable);
        String baseUrl = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString();
        return ResponseEntity.ok(ApiResponse.success(PagedDTO.from(page, baseUrl),
                "Organisation resources retrieved successfully"));
    }

    @Operation(summary = "Get one resource")
    @GetMapping("/{resourceUuid}")
    public ResponseEntity<ApiResponse<OrganisationResourceDTO>> getResource(
            @PathVariable UUID organisationUuid,
            @PathVariable UUID resourceUuid) {
        return ResponseEntity.ok(ApiResponse.success(
                resourceService.getResource(organisationUuid, resourceUuid),
                "Organisation resource retrieved successfully"));
    }

    @Operation(summary = "Update a resource")
    @PutMapping("/{resourceUuid}")
    public ResponseEntity<ApiResponse<OrganisationResourceDTO>> updateResource(
            @PathVariable UUID organisationUuid,
            @PathVariable UUID resourceUuid,
            @Valid @RequestBody OrganisationResourceDTO request) {
        return ResponseEntity.ok(ApiResponse.success(
                resourceService.updateResource(organisationUuid, resourceUuid, request),
                "Organisation resource updated successfully"));
    }

    @Operation(summary = "Deactivate a resource", description = "Rejected while the resource still has future holds or confirmed bookings")
    @DeleteMapping("/{resourceUuid}")
    public ResponseEntity<ApiResponse<Void>> deactivateResource(
            @PathVariable UUID organisationUuid,
            @PathVariable UUID resourceUuid) {
        resourceService.deactivateResource(organisationUuid, resourceUuid);
        return ResponseEntity.ok(ApiResponse.success(null, "Organisation resource deactivated successfully"));
    }

    @Operation(summary = "Add an availability rule (open hours or blackout)")
    @PostMapping("/{resourceUuid}/availability-rules")
    public ResponseEntity<ApiResponse<ResourceAvailabilityRuleDTO>> addAvailabilityRule(
            @PathVariable UUID organisationUuid,
            @PathVariable UUID resourceUuid,
            @Valid @RequestBody ResourceAvailabilityRuleDTO request) {
        OrganisationResourceServiceInterface.RuleChangeResult result =
                resourceService.addAvailabilityRule(organisationUuid, resourceUuid, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(result.rule(), ruleChangeMessage("created", result.affectedActiveBookings())));
    }

    @Operation(summary = "List a resource's availability rules")
    @GetMapping("/{resourceUuid}/availability-rules")
    public ResponseEntity<ApiResponse<List<ResourceAvailabilityRuleDTO>>> listAvailabilityRules(
            @PathVariable UUID organisationUuid,
            @PathVariable UUID resourceUuid) {
        return ResponseEntity.ok(ApiResponse.success(
                resourceService.listAvailabilityRules(organisationUuid, resourceUuid),
                "Availability rules retrieved successfully"));
    }

    @Operation(summary = "Update an availability rule")
    @PutMapping("/{resourceUuid}/availability-rules/{ruleUuid}")
    public ResponseEntity<ApiResponse<ResourceAvailabilityRuleDTO>> updateAvailabilityRule(
            @PathVariable UUID organisationUuid,
            @PathVariable UUID resourceUuid,
            @PathVariable UUID ruleUuid,
            @Valid @RequestBody ResourceAvailabilityRuleDTO request) {
        OrganisationResourceServiceInterface.RuleChangeResult result =
                resourceService.updateAvailabilityRule(organisationUuid, resourceUuid, ruleUuid, request);
        return ResponseEntity.ok(ApiResponse.success(
                result.rule(), ruleChangeMessage("updated", result.affectedActiveBookings())));
    }

    @Operation(summary = "Delete an availability rule")
    @DeleteMapping("/{resourceUuid}/availability-rules/{ruleUuid}")
    public ResponseEntity<ApiResponse<Void>> deleteAvailabilityRule(
            @PathVariable UUID organisationUuid,
            @PathVariable UUID resourceUuid,
            @PathVariable UUID ruleUuid) {
        resourceService.deleteAvailabilityRule(organisationUuid, resourceUuid, ruleUuid);
        return ResponseEntity.ok(ApiResponse.success(null, "Availability rule deleted successfully"));
    }

    @Operation(summary = "Merged calendar view of a resource",
            description = "Expanded open-hours and blackout windows plus recruitment holds and confirmed bookings for the date range")
    @GetMapping("/{resourceUuid}/calendar")
    public ResponseEntity<ApiResponse<List<ResourceCalendarEntryDTO>>> getCalendar(
            @PathVariable UUID organisationUuid,
            @PathVariable UUID resourceUuid,
            @RequestParam("start_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("end_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(ApiResponse.success(
                resourceService.getCalendar(organisationUuid, resourceUuid, startDate, endDate),
                "Resource calendar retrieved successfully"));
    }

    @Operation(summary = "List a resource's bookings")
    @GetMapping("/{resourceUuid}/bookings")
    public ResponseEntity<ApiResponse<PagedDTO<ResourceBookingDTO>>> listBookings(
            @PathVariable UUID organisationUuid,
            @PathVariable UUID resourceUuid,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "start_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "end_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Pageable pageable) {
        Optional<ResourceBookingStatus> statusFilter = Optional.ofNullable(status)
                .filter(value -> !value.isBlank())
                .map(ResourceBookingStatus::fromValue);
        Page<ResourceBookingDTO> page = resourceService.listBookings(
                organisationUuid, resourceUuid, statusFilter.orElse(null), startDate, endDate, pageable);
        String baseUrl = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString();
        return ResponseEntity.ok(ApiResponse.success(PagedDTO.from(page, baseUrl),
                "Resource bookings retrieved successfully"));
    }

    private String ruleChangeMessage(String action, long affectedActiveBookings) {
        if (affectedActiveBookings > 0) {
            return String.format(
                    "Availability rule %s successfully; %d existing active booking(s) intersect this rule and should be reviewed",
                    action, affectedActiveBookings);
        }
        return String.format("Availability rule %s successfully", action);
    }
}
