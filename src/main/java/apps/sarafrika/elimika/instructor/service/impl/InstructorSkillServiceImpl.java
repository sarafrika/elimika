package apps.sarafrika.elimika.instructor.service.impl;

import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import apps.sarafrika.elimika.instructor.dto.InstructorSkillDTO;
import apps.sarafrika.elimika.instructor.factory.InstructorSkillFactory;
import apps.sarafrika.elimika.instructor.model.InstructorSkill;
import apps.sarafrika.elimika.instructor.repository.InstructorSkillRepository;
import apps.sarafrika.elimika.instructor.service.InstructorSkillService;
import apps.sarafrika.elimika.shared.utils.enums.ProficiencyLevel;
import apps.sarafrika.elimika.instructor.search.InstructorSearchSource;
import apps.sarafrika.elimika.shared.search.SearchIndexRequests;
import apps.sarafrika.elimika.skills.spi.SkillLookupService;
import apps.sarafrika.elimika.skills.spi.SkillSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class InstructorSkillServiceImpl implements InstructorSkillService {

    private final InstructorSkillRepository instructorSkillRepository;
    private final GenericSpecificationBuilder<InstructorSkill> specificationBuilder;
    private final SearchIndexRequests searchIndexRequests;
    private final SkillLookupService skillLookupService;

    private static final String INSTRUCTOR_SKILL_NOT_FOUND_TEMPLATE = "Instructor skill with ID %s not found";

    @Override
    public InstructorSkillDTO createInstructorSkill(InstructorSkillDTO instructorSkillDTO) {
        InstructorSkill instructorSkill = InstructorSkillFactory.toEntity(instructorSkillDTO);
        instructorSkill.setCreatedDate(LocalDateTime.now());

        // Set default proficiency level if not specified
        if (instructorSkill.getProficiencyLevel() == null) {
            instructorSkill.setProficiencyLevel(ProficiencyLevel.BEGINNER);
        }
        linkToTaxonomy(instructorSkill);

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
        specificationBuilder.validateSortProperties(InstructorSkill.class, pageable);
        return instructorSkillRepository.findAll(pageable).map(InstructorSkillFactory::toDTO);
    }

    @Override
    public InstructorSkillDTO updateInstructorSkill(UUID uuid, InstructorSkillDTO instructorSkillDTO) {
        InstructorSkill existingSkill = instructorSkillRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(INSTRUCTOR_SKILL_NOT_FOUND_TEMPLATE, uuid)));

        // Update fields from DTO
        String previousName = existingSkill.getSkillName();
        updateSkillFields(existingSkill, instructorSkillDTO);
        // A renamed skill is re-resolved; an unchanged one keeps its link (even to a since-retired
        // skill) and only picks one up if it had none.
        if (!java.util.Objects.equals(previousName, existingSkill.getSkillName()) || existingSkill.getSkillUuid() == null) {
            linkToTaxonomy(existingSkill);
        }

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
        specificationBuilder.validateSortProperties(InstructorSkill.class, pageable);
        Specification<InstructorSkill> spec = specificationBuilder.buildSpecification(InstructorSkill.class, searchParams);
        return instructorSkillRepository.findAll(spec, pageable).map(InstructorSkillFactory::toDTO);
    }

    /**
     * Points the skill at the curated taxonomy entry its name resolves to (slug or alias match), or
     * clears the link when it resolves to none. The free-text name is kept either way.
     */
    private void linkToTaxonomy(InstructorSkill skill) {
        String name = skill.getSkillName();
        if (name == null || name.isBlank()) {
            skill.setSkillUuid(null);
            return;
        }
        SkillSummary match = skillLookupService.resolve(List.of(name)).get(name);
        skill.setSkillUuid(match == null ? null : match.uuid());
    }

    private void updateSkillFields(InstructorSkill existingSkill, InstructorSkillDTO dto) {
        if (dto.instructorUuid() != null) {
            UUID previousInstructorUuid = existingSkill.getInstructorUuid();
            existingSkill.setInstructorUuid(dto.instructorUuid());
            if (previousInstructorUuid != null && !previousInstructorUuid.equals(dto.instructorUuid())) {
                // The entity trigger only sees the new owner; the old owner's search document still lists this row.
                searchIndexRequests.enqueue(InstructorSearchSource.INDEX, previousInstructorUuid);
            }
        }
        if (dto.skillName() != null) {
            existingSkill.setSkillName(dto.skillName());
        }
        if (dto.proficiencyLevel() != null) {
            existingSkill.setProficiencyLevel(dto.proficiencyLevel());
        }
    }
}
