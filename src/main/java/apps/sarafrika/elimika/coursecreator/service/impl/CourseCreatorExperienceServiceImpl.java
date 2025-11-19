package apps.sarafrika.elimika.coursecreator.service.impl;

import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorExperienceDTO;
import apps.sarafrika.elimika.coursecreator.factory.CourseCreatorExperienceFactory;
import apps.sarafrika.elimika.coursecreator.model.CourseCreatorExperience;
import apps.sarafrika.elimika.coursecreator.repository.CourseCreatorExperienceRepository;
import apps.sarafrika.elimika.coursecreator.service.CourseCreatorExperienceService;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
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
public class CourseCreatorExperienceServiceImpl implements CourseCreatorExperienceService {

    private final CourseCreatorExperienceRepository experienceRepository;
    private final GenericSpecificationBuilder<CourseCreatorExperience> specificationBuilder;

    private static final String EXPERIENCE_NOT_FOUND_TEMPLATE = "Course creator experience with ID %s not found";

    @Override
    public CourseCreatorExperienceDTO createCourseCreatorExperience(CourseCreatorExperienceDTO dto) {
        CourseCreatorExperience experience = CourseCreatorExperienceFactory.toEntity(dto);
        return CourseCreatorExperienceFactory.toDTO(experienceRepository.save(experience));
    }

    @Override
    @Transactional(readOnly = true)
    public CourseCreatorExperienceDTO getCourseCreatorExperienceByUuid(UUID uuid) {
        return experienceRepository.findByUuid(uuid)
                .map(CourseCreatorExperienceFactory::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(EXPERIENCE_NOT_FOUND_TEMPLATE, uuid)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseCreatorExperienceDTO> getAllCourseCreatorExperience(Pageable pageable) {
        return experienceRepository.findAll(pageable).map(CourseCreatorExperienceFactory::toDTO);
    }

    @Override
    public CourseCreatorExperienceDTO updateCourseCreatorExperience(UUID uuid, CourseCreatorExperienceDTO dto) {
        CourseCreatorExperience existing = experienceRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(EXPERIENCE_NOT_FOUND_TEMPLATE, uuid)));

        if (dto.courseCreatorUuid() != null) {
            existing.setCourseCreatorUuid(dto.courseCreatorUuid());
        }
        if (dto.position() != null) {
            existing.setPosition(dto.position());
        }
        if (dto.organizationName() != null) {
            existing.setOrganizationName(dto.organizationName());
        }
        if (dto.responsibilities() != null) {
            existing.setResponsibilities(dto.responsibilities());
        }
        if (dto.yearsOfExperience() != null) {
            existing.setYearsOfExperience(dto.yearsOfExperience());
        }
        if (dto.startDate() != null) {
            existing.setStartDate(dto.startDate());
        }
        if (dto.endDate() != null) {
            existing.setEndDate(dto.endDate());
        }
        if (dto.isCurrentPosition() != null) {
            existing.setIsCurrentPosition(dto.isCurrentPosition());
        }

        return CourseCreatorExperienceFactory.toDTO(experienceRepository.save(existing));
    }

    @Override
    public void deleteCourseCreatorExperience(UUID uuid) {
        if (!experienceRepository.existsByUuid(uuid)) {
            throw new ResourceNotFoundException(String.format(EXPERIENCE_NOT_FOUND_TEMPLATE, uuid));
        }
        experienceRepository.deleteByUuid(uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseCreatorExperienceDTO> search(Map<String, String> searchParams, Pageable pageable) {
        Specification<CourseCreatorExperience> spec = specificationBuilder.buildSpecification(CourseCreatorExperience.class, searchParams);
        return experienceRepository.findAll(spec, pageable).map(CourseCreatorExperienceFactory::toDTO);
    }
}
