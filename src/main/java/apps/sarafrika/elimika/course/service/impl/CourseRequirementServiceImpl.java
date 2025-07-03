package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.common.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.common.util.GenericSpecificationBuilder;
import apps.sarafrika.elimika.course.dto.CourseRequirementDTO;
import apps.sarafrika.elimika.course.factory.CourseRequirementFactory;
import apps.sarafrika.elimika.course.model.CourseRequirement;
import apps.sarafrika.elimika.course.repository.CourseRequirementRepository;
import apps.sarafrika.elimika.course.service.CourseRequirementService;
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
public class CourseRequirementServiceImpl implements CourseRequirementService {

    private final CourseRequirementRepository courseRequirementRepository;
    private final GenericSpecificationBuilder<CourseRequirement> specificationBuilder;

    private static final String COURSE_REQUIREMENT_NOT_FOUND_TEMPLATE = "Course requirement with ID %s not found";

    @Override
    public CourseRequirementDTO createCourseRequirement(CourseRequirementDTO courseRequirementDTO) {
        CourseRequirement courseRequirement = CourseRequirementFactory.toEntity(courseRequirementDTO);

        // Set defaults
        if (courseRequirement.getIsMandatory() == null) {
            courseRequirement.setIsMandatory(true);
        }

        CourseRequirement savedCourseRequirement = courseRequirementRepository.save(courseRequirement);
        return CourseRequirementFactory.toDTO(savedCourseRequirement);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseRequirementDTO getCourseRequirementByUuid(UUID uuid) {
        return courseRequirementRepository.findByUuid(uuid)
                .map(CourseRequirementFactory::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(COURSE_REQUIREMENT_NOT_FOUND_TEMPLATE, uuid)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseRequirementDTO> getAllCourseRequirements(Pageable pageable) {
        return courseRequirementRepository.findAll(pageable).map(CourseRequirementFactory::toDTO);
    }

    @Override
    public CourseRequirementDTO updateCourseRequirement(UUID uuid, CourseRequirementDTO courseRequirementDTO) {
        CourseRequirement existingCourseRequirement = courseRequirementRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(COURSE_REQUIREMENT_NOT_FOUND_TEMPLATE, uuid)));

        updateCourseRequirementFields(existingCourseRequirement, courseRequirementDTO);

        CourseRequirement updatedCourseRequirement = courseRequirementRepository.save(existingCourseRequirement);
        return CourseRequirementFactory.toDTO(updatedCourseRequirement);
    }

    @Override
    public void deleteCourseRequirement(UUID uuid) {
        if (!courseRequirementRepository.existsByUuid(uuid)) {
            throw new ResourceNotFoundException(
                    String.format(COURSE_REQUIREMENT_NOT_FOUND_TEMPLATE, uuid));
        }
        courseRequirementRepository.deleteByUuid(uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseRequirementDTO> search(Map<String, String> searchParams, Pageable pageable) {
        Specification<CourseRequirement> spec = specificationBuilder.buildSpecification(
                CourseRequirement.class, searchParams);
        return courseRequirementRepository.findAll(spec, pageable).map(CourseRequirementFactory::toDTO);
    }

    private void updateCourseRequirementFields(CourseRequirement existingCourseRequirement, CourseRequirementDTO dto) {
        if (dto.courseUuid() != null) {
            existingCourseRequirement.setCourseUuid(dto.courseUuid());
        }
        if (dto.requirementType() != null) {
            existingCourseRequirement.setRequirementType(dto.requirementType());
        }
        if (dto.requirementText() != null) {
            existingCourseRequirement.setRequirementText(dto.requirementText());
        }
        if (dto.isMandatory() != null) {
            existingCourseRequirement.setIsMandatory(dto.isMandatory());
        }
    }
}