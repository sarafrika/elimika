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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1")
@RequiredArgsConstructor
@Tag(name = "Competitions API", description = "Organisation-scoped competitions/events and team registrations")
public class CompetitionController {

    private final CompetitionService competitionService;

    @Operation(summary = "List competitions for an organisation", description = "Returns all competitions for the organisation with team counts.")
    @GetMapping("/organisations/{organisationUuid}/competitions")
    public ResponseEntity<ApiResponse<List<CompetitionDTO>>> listCompetitions(@PathVariable UUID organisationUuid) {
        List<CompetitionDTO> competitions = competitionService.getCompetitionsForOrganisation(organisationUuid);
        return ResponseEntity.ok(ApiResponse.success(competitions, "Competitions retrieved successfully"));
    }

    @Operation(summary = "Create a competition")
    @PostMapping("/organisations/{organisationUuid}/competitions")
    public ResponseEntity<ApiResponse<CompetitionDTO>> createCompetition(
            @PathVariable UUID organisationUuid,
            @Valid @RequestBody CreateCompetitionRequestDTO request) {
        CompetitionDTO created = competitionService.createCompetition(organisationUuid, request);
        return ResponseEntity.status(201).body(ApiResponse.success(created, "Competition created successfully"));
    }

    @Operation(summary = "Delete a competition")
    @DeleteMapping("/competitions/{competitionUuid}")
    public ResponseEntity<ApiResponse<Void>> deleteCompetition(@PathVariable UUID competitionUuid) {
        competitionService.deleteCompetition(competitionUuid);
        return ResponseEntity.ok(ApiResponse.success(null, "Competition deleted successfully"));
    }

    @Operation(summary = "List competition teams")
    @GetMapping("/competitions/{competitionUuid}/teams")
    public ResponseEntity<ApiResponse<List<CompetitionTeamDTO>>> listTeams(@PathVariable UUID competitionUuid) {
        List<CompetitionTeamDTO> teams = competitionService.getTeams(competitionUuid);
        return ResponseEntity.ok(ApiResponse.success(teams, "Competition teams retrieved successfully"));
    }

    @Operation(summary = "Register a team for a competition")
    @PostMapping("/competitions/{competitionUuid}/teams")
    public ResponseEntity<ApiResponse<CompetitionTeamDTO>> addTeam(
            @PathVariable UUID competitionUuid,
            @Valid @RequestBody CompetitionTeamDTO request) {
        CompetitionTeamDTO created = competitionService.addTeam(competitionUuid, request.teamName());
        return ResponseEntity.status(201).body(ApiResponse.success(created, "Team registered successfully"));
    }

    @Operation(summary = "Remove a team from a competition")
    @DeleteMapping("/competitions/{competitionUuid}/teams/{teamUuid}")
    public ResponseEntity<ApiResponse<Void>> removeTeam(
            @PathVariable UUID competitionUuid,
            @PathVariable UUID teamUuid) {
        competitionService.removeTeam(competitionUuid, teamUuid);
        return ResponseEntity.ok(ApiResponse.success(null, "Team removed successfully"));
    }
}
