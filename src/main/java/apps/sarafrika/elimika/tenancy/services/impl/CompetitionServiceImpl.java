package apps.sarafrika.elimika.tenancy.services.impl;

import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.tenancy.dto.CompetitionDTO;
import apps.sarafrika.elimika.tenancy.dto.CompetitionTeamDTO;
import apps.sarafrika.elimika.tenancy.dto.CreateCompetitionRequestDTO;
import apps.sarafrika.elimika.tenancy.entity.Competition;
import apps.sarafrika.elimika.tenancy.entity.CompetitionTeam;
import apps.sarafrika.elimika.tenancy.repository.CompetitionRepository;
import apps.sarafrika.elimika.tenancy.repository.CompetitionTeamRepository;
import apps.sarafrika.elimika.tenancy.services.CompetitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CompetitionServiceImpl implements CompetitionService {

    private final CompetitionRepository competitionRepository;
    private final CompetitionTeamRepository competitionTeamRepository;

    @Override
    public CompetitionDTO createCompetition(UUID organisationUuid, CreateCompetitionRequestDTO request) {
        Competition competition = Competition.builder()
                .organisationUuid(organisationUuid)
                .name(request.name())
                .category(request.category())
                .eventDate(request.eventDate())
                .venueName(request.venueName())
                .capacity(request.capacity())
                .status(request.status() != null && !request.status().isBlank() ? request.status() : "Upcoming")
                .description(request.description())
                .build();
        return toDTO(competitionRepository.save(competition), 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompetitionDTO> getCompetitionsForOrganisation(UUID organisationUuid) {
        List<Competition> competitions = competitionRepository.findByOrganisationUuidOrderByEventDateAsc(organisationUuid);
        if (competitions.isEmpty()) {
            return List.of();
        }
        Map<UUID, Long> counts = new HashMap<>();
        for (Object[] row : competitionTeamRepository.countByCompetitionUuids(competitions.stream().map(Competition::getUuid).toList())) {
            counts.put((UUID) row[0], ((Number) row[1]).longValue());
        }
        return competitions.stream()
                .map(c -> toDTO(c, counts.getOrDefault(c.getUuid(), 0L)))
                .toList();
    }

    @Override
    public void deleteCompetition(UUID competitionUuid) {
        Competition competition = findOrThrow(competitionUuid);
        competitionRepository.delete(competition);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompetitionTeamDTO> getTeams(UUID competitionUuid) {
        return competitionTeamRepository.findByCompetitionUuid(competitionUuid).stream()
                .map(this::toTeamDTO)
                .toList();
    }

    @Override
    public CompetitionTeamDTO addTeam(UUID competitionUuid, String teamName) {
        findOrThrow(competitionUuid);
        CompetitionTeam team = competitionTeamRepository.save(CompetitionTeam.builder()
                .competitionUuid(competitionUuid)
                .teamName(teamName)
                .build());
        return toTeamDTO(team);
    }

    @Override
    public void removeTeam(UUID competitionUuid, UUID teamUuid) {
        competitionTeamRepository.deleteByCompetitionUuidAndUuid(competitionUuid, teamUuid);
    }

    private Competition findOrThrow(UUID competitionUuid) {
        return competitionRepository.findByUuid(competitionUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Competition not found: " + competitionUuid));
    }

    private CompetitionDTO toDTO(Competition c, long teamCount) {
        return new CompetitionDTO(
                c.getUuid(),
                c.getOrganisationUuid(),
                c.getName(),
                c.getCategory(),
                c.getEventDate(),
                c.getVenueName(),
                c.getCapacity(),
                c.getStatus(),
                c.getDescription(),
                teamCount,
                c.getCreatedDate()
        );
    }

    private CompetitionTeamDTO toTeamDTO(CompetitionTeam t) {
        return new CompetitionTeamDTO(t.getUuid(), t.getCompetitionUuid(), t.getTeamName(), t.getCreatedDate());
    }
}
