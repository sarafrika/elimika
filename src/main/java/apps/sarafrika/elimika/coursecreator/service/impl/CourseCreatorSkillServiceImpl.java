package apps.sarafrika.elimika.coursecreator.service.impl;

import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorSkillDTO;
import apps.sarafrika.elimika.coursecreator.factory.CourseCreatorSkillFactory;
import apps.sarafrika.elimika.coursecreator.model.CourseCreatorSkill;
import apps.sarafrika.elimika.coursecreator.repository.CourseCreatorSkillRepository;
import apps.sarafrika.elimika.coursecreator.service.CourseCreatorSkillService;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import apps.sarafrika.elimika.shared.utils.enums.ProficiencyLevel;
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
public class CourseCreatorSkillServiceImpl implements CourseCreatorSkillService {

    private final CourseCreatorSkillRepository skillRepository;
    private final GenericSpecificationBuilder<CourseCreatorSkill> specificationBuilder;

    private static final String SKILL_NOT_FOUND_TEMPLATE = "Course creator skill with ID %s not found";

    @Override
    public CourseCreatorSkillDTO createCourseCreatorSkill(CourseCreatorSkillDTO dto) {
        CourseCreatorSkill skill = CourseCreatorSkillFactory.toEntity(dto);
        if (skill.getProficiencyLevel() == null) {
            skill.setProficiencyLevel(ProficiencyLevel.BEGINNER);
        }
        skill.setCreatedDate(LocalDateTime.now());
        return CourseCreatorSkillFactory.toDTO(skillRepository.save(skill));
    }

    @Override
    @Transactional(readOnly = true)
    public CourseCreatorSkillDTO getCourseCreatorSkillByUuid(UUID uuid) {
        return skillRepository.findByUuid(uuid)
                .map(CourseCreatorSkillFactory::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(SKILL_NOT_FOUND_TEMPLATE, uuid)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseCreatorSkillDTO> getAllCourseCreatorSkills(Pageable pageable) {
        return skillRepository.findAll(pageable).map(CourseCreatorSkillFactory::toDTO);
    }

    @Override
    public CourseCreatorSkillDTO updateCourseCreatorSkill(UUID uuid, CourseCreatorSkillDTO dto) {
        CourseCreatorSkill existing = skillRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(SKILL_NOT_FOUND_TEMPLATE, uuid)));

        if (dto.courseCreatorUuid() != null) {
            existing.setCourseCreatorUuid(dto.courseCreatorUuid());
        }
        if (dto.skillName() != null) {
            existing.setSkillName(dto.skillName());
        }
        if (dto.proficiencyLevel() != null) {
            existing.setProficiencyLevel(dto.proficiencyLevel());
        }

        return CourseCreatorSkillFactory.toDTO(skillRepository.save(existing));
    }

    @Override
    public void deleteCourseCreatorSkill(UUID uuid) {
        if (!skillRepository.existsByUuid(uuid)) {
            throw new ResourceNotFoundException(String.format(SKILL_NOT_FOUND_TEMPLATE, uuid));
        }
        skillRepository.deleteByUuid(uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseCreatorSkillDTO> search(Map<String, String> searchParams, Pageable pageable) {
        Specification<CourseCreatorSkill> spec = specificationBuilder.buildSpecification(CourseCreatorSkill.class, searchParams);
        return skillRepository.findAll(spec, pageable).map(CourseCreatorSkillFactory::toDTO);
    }
}
