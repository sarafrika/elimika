package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
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
    private static final String COURSE_REQUIREMENT_COURSE_MISMATCH_TEMPLATE = "Course requirement %s does not belong to course %s";

    @Override
    public CourseRequirementDTO createCourseRequirement(UUID courseUuid, CourseRequirementDTO courseRequirementDTO) {
        CourseRequirement courseRequirement = CourseRequirementFactory.toEntity(courseRequirementDTO);
        courseRequirement.setCourseUuid(courseUuid);

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
    public CourseRequirementDTO updateCourseRequirement(UUID courseUuid, UUID uuid, CourseRequirementDTO courseRequirementDTO) {
        CourseRequirement existingCourseRequirement = courseRequirementRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(COURSE_REQUIREMENT_NOT_FOUND_TEMPLATE, uuid)));

        assertBelongsToCourse(existingCourseRequirement, courseUuid);
        updateCourseRequirementFields(existingCourseRequirement, courseRequirementDTO);
        existingCourseRequirement.setCourseUuid(courseUuid);

        CourseRequirement updatedCourseRequirement = courseRequirementRepository.save(existingCourseRequirement);
        return CourseRequirementFactory.toDTO(updatedCourseRequirement);
    }

    @Override
    public void deleteCourseRequirement(UUID courseUuid, UUID uuid) {
        CourseRequirement existingCourseRequirement = courseRequirementRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(COURSE_REQUIREMENT_NOT_FOUND_TEMPLATE, uuid)));
        assertBelongsToCourse(existingCourseRequirement, courseUuid);
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

    private void assertBelongsToCourse(CourseRequirement requirement, UUID courseUuid) {
        if (!courseUuid.equals(requirement.getCourseUuid())) {
            throw new ResourceNotFoundException(
                    String.format(COURSE_REQUIREMENT_COURSE_MISMATCH_TEMPLATE, requirement.getUuid(), courseUuid));
        }
    }
}
