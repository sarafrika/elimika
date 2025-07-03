package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.common.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.common.util.GenericSpecificationBuilder;
import apps.sarafrika.elimika.course.dto.CourseAssessmentDTO;
import apps.sarafrika.elimika.course.factory.CourseAssessmentFactory;
import apps.sarafrika.elimika.course.model.CourseAssessment;
import apps.sarafrika.elimika.course.repository.CourseAssessmentRepository;
import apps.sarafrika.elimika.course.service.CourseAssessmentService;
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
public class CourseAssessmentServiceImpl implements CourseAssessmentService {

    private final CourseAssessmentRepository courseAssessmentRepository;
    private final GenericSpecificationBuilder<CourseAssessment> specificationBuilder;

    private static final String COURSE_ASSESSMENT_NOT_FOUND_TEMPLATE = "Course assessment with ID %s not found";

    @Override
    public CourseAssessmentDTO createCourseAssessment(CourseAssessmentDTO courseAssessmentDTO) {
        CourseAssessment courseAssessment = CourseAssessmentFactory.toEntity(courseAssessmentDTO);

        // Set defaults
        if (courseAssessment.getIsRequired() == null) {
            courseAssessment.setIsRequired(false);
        }

        CourseAssessment savedCourseAssessment = courseAssessmentRepository.save(courseAssessment);
        return CourseAssessmentFactory.toDTO(savedCourseAssessment);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseAssessmentDTO getCourseAssessmentByUuid(UUID uuid) {
        return courseAssessmentRepository.findByUuid(uuid)
                .map(CourseAssessmentFactory::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(COURSE_ASSESSMENT_NOT_FOUND_TEMPLATE, uuid)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseAssessmentDTO> getAllCourseAssessments(Pageable pageable) {
        return courseAssessmentRepository.findAll(pageable).map(CourseAssessmentFactory::toDTO);
    }

    @Override
    public CourseAssessmentDTO updateCourseAssessment(UUID uuid, CourseAssessmentDTO courseAssessmentDTO) {
        CourseAssessment existingCourseAssessment = courseAssessmentRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(COURSE_ASSESSMENT_NOT_FOUND_TEMPLATE, uuid)));

        updateCourseAssessmentFields(existingCourseAssessment, courseAssessmentDTO);

        CourseAssessment updatedCourseAssessment = courseAssessmentRepository.save(existingCourseAssessment);
        return CourseAssessmentFactory.toDTO(updatedCourseAssessment);
    }

    @Override
    public void deleteCourseAssessment(UUID uuid) {
        if (!courseAssessmentRepository.existsByUuid(uuid)) {
            throw new ResourceNotFoundException(
                    String.format(COURSE_ASSESSMENT_NOT_FOUND_TEMPLATE, uuid));
        }
        courseAssessmentRepository.deleteByUuid(uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseAssessmentDTO> search(Map<String, String> searchParams, Pageable pageable) {
        Specification<CourseAssessment> spec = specificationBuilder.buildSpecification(
                CourseAssessment.class, searchParams);
        return courseAssessmentRepository.findAll(spec, pageable).map(CourseAssessmentFactory::toDTO);
    }

    private void updateCourseAssessmentFields(CourseAssessment existingCourseAssessment, CourseAssessmentDTO dto) {
        if (dto.courseUuid() != null) {
            existingCourseAssessment.setCourseUuid(dto.courseUuid());
        }
        if (dto.assessmentType() != null) {
            existingCourseAssessment.setAssessmentType(dto.assessmentType());
        }
        if (dto.title() != null) {
            existingCourseAssessment.setTitle(dto.title());
        }
        if (dto.description() != null) {
            existingCourseAssessment.setDescription(dto.description());
        }
        if (dto.weightPercentage() != null) {
            existingCourseAssessment.setWeightPercentage(dto.weightPercentage());
        }
        if (dto.rubricUuid() != null) {
            existingCourseAssessment.setRubricUuid(dto.rubricUuid());
        }
        if (dto.isRequired() != null) {
            existingCourseAssessment.setIsRequired(dto.isRequired());
        }
    }
}