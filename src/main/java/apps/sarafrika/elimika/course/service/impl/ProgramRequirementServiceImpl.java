package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.common.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.common.util.GenericSpecificationBuilder;
import apps.sarafrika.elimika.course.dto.ProgramRequirementDTO;
import apps.sarafrika.elimika.course.factory.ProgramRequirementFactory;
import apps.sarafrika.elimika.course.model.ProgramRequirement;
import apps.sarafrika.elimika.course.repository.ProgramRequirementRepository;
import apps.sarafrika.elimika.course.service.ProgramRequirementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ProgramRequirementServiceImpl implements ProgramRequirementService {

    private final ProgramRequirementRepository programRequirementRepository;
    private final GenericSpecificationBuilder<ProgramRequirement> specificationBuilder;

    private static final String PROGRAM_REQUIREMENT_NOT_FOUND_TEMPLATE = "Program requirement with ID %s not found";

    @Override
    public ProgramRequirementDTO createProgramRequirement(ProgramRequirementDTO programRequirementDTO) {
        ProgramRequirement programRequirement = ProgramRequirementFactory.toEntity(programRequirementDTO);

        // Set defaults
        if (programRequirement.getIsMandatory() == null) {
            programRequirement.setIsMandatory(true);
        }

        ProgramRequirement savedProgramRequirement = programRequirementRepository.save(programRequirement);
        return ProgramRequirementFactory.toDTO(savedProgramRequirement);
    }

    @Override
    @Transactional(readOnly = true)
    public ProgramRequirementDTO getProgramRequirementByUuid(UUID uuid) {
        return programRequirementRepository.findByUuid(uuid)
                .map(ProgramRequirementFactory::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(PROGRAM_REQUIREMENT_NOT_FOUND_TEMPLATE, uuid)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProgramRequirementDTO> getAllProgramRequirements(Pageable pageable) {
        return programRequirementRepository.findAll(pageable).map(ProgramRequirementFactory::toDTO);
    }

    @Override
    public ProgramRequirementDTO updateProgramRequirement(UUID uuid, ProgramRequirementDTO programRequirementDTO) {
        ProgramRequirement existingProgramRequirement = programRequirementRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(PROGRAM_REQUIREMENT_NOT_FOUND_TEMPLATE, uuid)));

        updateProgramRequirementFields(existingProgramRequirement, programRequirementDTO);

        ProgramRequirement updatedProgramRequirement = programRequirementRepository.save(existingProgramRequirement);
        return ProgramRequirementFactory.toDTO(updatedProgramRequirement);
    }

    @Override
    public void deleteProgramRequirement(UUID uuid) {
        if (!programRequirementRepository.existsByUuid(uuid)) {
            throw new ResourceNotFoundException(
                    String.format(PROGRAM_REQUIREMENT_NOT_FOUND_TEMPLATE, uuid));
        }
        programRequirementRepository.deleteByUuid(uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProgramRequirementDTO> search(Map<String, String> searchParams, Pageable pageable) {
        Specification<ProgramRequirement> spec = specificationBuilder.buildSpecification(
                ProgramRequirement.class, searchParams);
        return programRequirementRepository.findAll(spec, pageable).map(ProgramRequirementFactory::toDTO);
    }

    private void updateProgramRequirementFields(ProgramRequirement existingProgramRequirement, ProgramRequirementDTO dto) {
        if (dto.programUuid() != null) {
            existingProgramRequirement.setProgramUuid(dto.programUuid());
        }
        if (dto.requirementType() != null) {
            existingProgramRequirement.setRequirementType(dto.requirementType());
        }
        if (dto.requirementText() != null) {
            existingProgramRequirement.setRequirementText(dto.requirementText());
        }
        if (dto.isMandatory() != null) {
            existingProgramRequirement.setIsMandatory(dto.isMandatory());
        }
    }
}