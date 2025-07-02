package apps.sarafrika.elimika.instructor.service.impl;

import apps.sarafrika.elimika.common.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.common.util.GenericSpecificationBuilder;
import apps.sarafrika.elimika.instructor.dto.InstructorSkillDTO;
import apps.sarafrika.elimika.instructor.factory.InstructorSkillFactory;
import apps.sarafrika.elimika.instructor.model.InstructorSkill;
import apps.sarafrika.elimika.instructor.repository.InstructorSkillRepository;
import apps.sarafrika.elimika.instructor.service.InstructorSkillService;
import apps.sarafrika.elimika.instructor.util.enums.ProficiencyLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class InstructorSkillServiceImpl implements InstructorSkillService {

    private final InstructorSkillRepository instructorSkillRepository;
    private final GenericSpecificationBuilder<InstructorSkill> specificationBuilder;

    private static final String INSTRUCTOR_SKILL_NOT_FOUND_TEMPLATE = "Instructor skill with ID %s not found";

    @Override
    public InstructorSkillDTO createInstructorSkill(InstructorSkillDTO instructorSkillDTO) {
        InstructorSkill instructorSkill = InstructorSkillFactory.toEntity(instructorSkillDTO);
        instructorSkill.setCreatedDate(LocalDateTime.now());

        // Set default proficiency level if not specified
        if (instructorSkill.getProficiencyLevel() == null) {
            instructorSkill.setProficiencyLevel(ProficiencyLevel.BEGINNER);
        }

        InstructorSkill savedSkill = instructorSkillRepository.save(instructorSkill);
        return InstructorSkillFactory.toDTO(savedSkill);
    }

    @Override
    @Transactional(readOnly = true)
    public InstructorSkillDTO getInstructorSkillByUuid(UUID uuid) {
        return instructorSkillRepository.findByUuid(uuid)
                .map(InstructorSkillFactory::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(INSTRUCTOR_SKILL_NOT_FOUND_TEMPLATE, uuid)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InstructorSkillDTO> getAllInstructorSkills(Pageable pageable) {
        return instructorSkillRepository.findAll(pageable).map(InstructorSkillFactory::toDTO);
    }

    @Override
    public InstructorSkillDTO updateInstructorSkill(UUID uuid, InstructorSkillDTO instructorSkillDTO) {
        InstructorSkill existingSkill = instructorSkillRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(INSTRUCTOR_SKILL_NOT_FOUND_TEMPLATE, uuid)));

        // Update fields from DTO
        updateSkillFields(existingSkill, instructorSkillDTO);

        InstructorSkill updatedSkill = instructorSkillRepository.save(existingSkill);
        return InstructorSkillFactory.toDTO(updatedSkill);
    }

    @Override
    public void deleteInstructorSkill(UUID uuid) {
        if (!instructorSkillRepository.existsByUuid(uuid)) {
            throw new ResourceNotFoundException(String.format(INSTRUCTOR_SKILL_NOT_FOUND_TEMPLATE, uuid));
        }
        instructorSkillRepository.deleteByUuid(uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InstructorSkillDTO> search(Map<String, String> searchParams, Pageable pageable) {
        Specification<InstructorSkill> spec = specificationBuilder.buildSpecification(InstructorSkill.class, searchParams);
        return instructorSkillRepository.findAll(spec, pageable).map(InstructorSkillFactory::toDTO);
    }

    private void updateSkillFields(InstructorSkill existingSkill, InstructorSkillDTO dto) {
        if (dto.instructorUuid() != null) {
            existingSkill.setInstructorUuid(dto.instructorUuid());
        }
        if (dto.skillName() != null) {
            existingSkill.setSkillName(dto.skillName());
        }
        if (dto.proficiencyLevel() != null) {
            existingSkill.setProficiencyLevel(dto.proficiencyLevel());
        }
    }
}