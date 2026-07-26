package apps.sarafrika.elimika.tenancy.services;

import apps.sarafrika.elimika.tenancy.dto.CompetitionDTO;
import apps.sarafrika.elimika.tenancy.dto.CompetitionTeamDTO;
import apps.sarafrika.elimika.tenancy.dto.CreateCompetitionRequestDTO;

import java.util.List;
import java.util.UUID;

/**
 * Organisation competitions/events and their team registrations.
 */
public interface CompetitionService {

    CompetitionDTO createCompetition(UUID organisationUuid, CreateCompetitionRequestDTO request);

    List<CompetitionDTO> getCompetitionsForOrganisation(UUID organisationUuid);

    void deleteCompetition(UUID competitionUuid);

    List<CompetitionTeamDTO> getTeams(UUID competitionUuid);

    CompetitionTeamDTO addTeam(UUID competitionUuid, String teamName);

    void removeTeam(UUID competitionUuid, UUID teamUuid);
}
