package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.ProgramRequirementDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface ProgramRequirementService {
    ProgramRequirementDTO createProgramRequirement(ProgramRequirementDTO programRequirementDTO);

    ProgramRequirementDTO getProgramRequirementByUuid(UUID uuid);

    Page<ProgramRequirementDTO> getAllProgramRequirements(Pageable pageable);

    ProgramRequirementDTO updateProgramRequirement(UUID uuid, ProgramRequirementDTO programRequirementDTO);

    void deleteProgramRequirement(UUID uuid);

    Page<ProgramRequirementDTO> search(Map<String, String> searchParams, Pageable pageable);
}