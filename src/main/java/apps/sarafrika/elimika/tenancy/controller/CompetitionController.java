package apps.sarafrika.elimika.tenancy.controller;

import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.tenancy.dto.CompetitionDTO;
import apps.sarafrika.elimika.tenancy.dto.CompetitionTeamDTO;
import apps.sarafrika.elimika.tenancy.dto.CreateCompetitionRequestDTO;
import apps.sarafrika.elimika.tenancy.services.CompetitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1")
@RequiredArgsConstructor
@Tag(name = "Competitions API", description = "Organisation-scoped competitions/events and team registrations")
public class CompetitionController {

    /**
     * A competition belongs to exactly one organisation, so every route here is authorised against
     * that organisation: its members may see the fixture list and the teams on it, and only the
     * people who may act for it can create, delete or change a registration.
     */
    private static final String READ_ORGANISATION =
            "@organisationSecurityService.canReadOrganisation(#organisationUuid)";
    private static final String MANAGE_ORGANISATION =
            "@organisationSecurityService.canManageOrganisation(#organisationUuid)";
    /**
     * The competition- and team-scoped routes carry no organisation in the path, so the owning
     * organisation is resolved from the competition itself.
     */
    private static final String READ_COMPETITION =
            "@organisationSecurityService.canReadCompetition(#competitionUuid)";
    private static final String MANAGE_COMPETITION =
            "@organisationSecurityService.canManageCompetition(#competitionUuid)";

    private final CompetitionService competitionService;

    @Operation(summary = "List competitions for an organisation", description = "Returns all competitions for the organisation with team counts.")
    @GetMapping("/organisations/{organisationUuid}/competitions")
    @PreAuthorize(READ_ORGANISATION)
    public ResponseEntity<ApiResponse<List<CompetitionDTO>>> listCompetitions(@PathVariable UUID organisationUuid) {
        List<CompetitionDTO> competitions = competitionService.getCompetitionsForOrganisation(organisationUuid);
        return ResponseEntity.ok(ApiResponse.success(competitions, "Competitions retrieved successfully"));
    }

    @Operation(summary = "Create a competition")
    @PostMapping("/organisations/{organisationUuid}/competitions")
    @PreAuthorize(MANAGE_ORGANISATION)
    public ResponseEntity<ApiResponse<CompetitionDTO>> createCompetition(
            @PathVariable UUID organisationUuid,
            @Valid @RequestBody CreateCompetitionRequestDTO request) {
        CompetitionDTO created = competitionService.createCompetition(organisationUuid, request);
        return ResponseEntity.status(201).body(ApiResponse.success(created, "Competition created successfully"));
    }

    @Operation(summary = "Delete a competition")
    @DeleteMapping("/competitions/{competitionUuid}")
    @PreAuthorize(MANAGE_COMPETITION)
    public ResponseEntity<ApiResponse<Void>> deleteCompetition(@PathVariable UUID competitionUuid) {
        competitionService.deleteCompetition(competitionUuid);
        return ResponseEntity.ok(ApiResponse.success(null, "Competition deleted successfully"));
    }

    @Operation(summary = "List competition teams")
    @GetMapping("/competitions/{competitionUuid}/teams")
    @PreAuthorize(READ_COMPETITION)
    public ResponseEntity<ApiResponse<List<CompetitionTeamDTO>>> listTeams(@PathVariable UUID competitionUuid) {
        List<CompetitionTeamDTO> teams = competitionService.getTeams(competitionUuid);
        return ResponseEntity.ok(ApiResponse.success(teams, "Competition teams retrieved successfully"));
    }

    @Operation(summary = "Register a team for a competition")
    @PostMapping("/competitions/{competitionUuid}/teams")
    @PreAuthorize(MANAGE_COMPETITION)
    public ResponseEntity<ApiResponse<CompetitionTeamDTO>> addTeam(
            @PathVariable UUID competitionUuid,
            @Valid @RequestBody CompetitionTeamDTO request) {
        CompetitionTeamDTO created = competitionService.addTeam(competitionUuid, request.teamName());
        return ResponseEntity.status(201).body(ApiResponse.success(created, "Team registered successfully"));
    }

    @Operation(summary = "Remove a team from a competition")
    @DeleteMapping("/competitions/{competitionUuid}/teams/{teamUuid}")
    @PreAuthorize(MANAGE_COMPETITION)
    public ResponseEntity<ApiResponse<Void>> removeTeam(
            @PathVariable UUID competitionUuid,
            @PathVariable UUID teamUuid) {
        competitionService.removeTeam(competitionUuid, teamUuid);
        return ResponseEntity.ok(ApiResponse.success(null, "Team removed successfully"));
    }
}
