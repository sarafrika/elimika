package apps.sarafrika.elimika.timetabling.controller;

import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.timetabling.dto.OrganisationTimetableEntryDTO;
import apps.sarafrika.elimika.timetabling.service.OrganisationTimetableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(OrganisationTimetableController.API_ROOT_PATH)
@RequiredArgsConstructor
@Tag(name = "Timetable API", description = "Class scheduling and timetable management")
public class OrganisationTimetableController {

    public static final String API_ROOT_PATH = "/api/v1/timetable/organisations/{organisationUuid}";

    private final OrganisationTimetableService organisationTimetableService;

    @Operation(
            summary = "Get the timetable of every class an organisation owns within a date range",
            description = "Non-cancelled sessions overlapping the inclusive date range, ordered by start time, each "
                    + "with its class title, instructor name and enrolled count, so an organisation calendar needs "
                    + "one request instead of one per class. The range may span at most 366 days.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Organisation timetable retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid or over-wide date range")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Caller does not manage this organisation")
    @GetMapping
    @PreAuthorize("@domainSecurityService.isPlatformAdmin() or @domainSecurityService.managesOrganisation(#organisationUuid)")
    public ResponseEntity<ApiResponse<List<OrganisationTimetableEntryDTO>>> getOrganisationTimetable(
            @PathVariable UUID organisationUuid,
            @Parameter(description = "Start date of the range (YYYY-MM-DD)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @Parameter(description = "End date of the range, inclusive (YYYY-MM-DD)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        List<OrganisationTimetableEntryDTO> result =
                organisationTimetableService.getOrganisationTimetable(organisationUuid, start, end);
        return ResponseEntity.ok(ApiResponse.success(result, "Organisation timetable retrieved successfully"));
    }
}
