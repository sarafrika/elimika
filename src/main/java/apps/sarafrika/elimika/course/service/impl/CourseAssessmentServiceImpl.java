package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import apps.sarafrika.elimika.course.dto.CourseAssessmentDTO;
import apps.sarafrika.elimika.course.factory.CourseAssessmentFactory;
import apps.sarafrika.elimika.course.model.CourseAssessment;
import apps.sarafrika.elimika.course.repository.CourseAssessmentLineItemRepository;
import apps.sarafrika.elimika.course.repository.CourseAssessmentRepository;
import apps.sarafrika.elimika.course.service.CourseAssessmentService;
import apps.sarafrika.elimika.course.service.CourseGradebookService;
import apps.sarafrika.elimika.course.util.enums.CourseAssessmentAggregationStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CourseAssessmentServiceImpl implements CourseAssessmentService {

    private final CourseAssessmentRepository courseAssessmentRepository;
    private final CourseAssessmentLineItemRepository courseAssessmentLineItemRepository;
    private final GenericSpecificationBuilder<CourseAssessment> specificationBuilder;
    private final CourseGradebookService courseGradebookService;

    private static final String COURSE_ASSESSMENT_NOT_FOUND_TEMPLATE = "Course assessment with ID %s not found";

    @Override
    public CourseAssessmentDTO createCourseAssessment(UUID courseUuid, CourseAssessmentDTO courseAssessmentDTO) {
        CourseAssessment courseAssessment = CourseAssessmentFactory.toEntity(courseAssessmentDTO);
        courseAssessment.setCourseUuid(courseUuid);
        if (courseAssessment.getAggregationStrategy() == null) {
            courseAssessment.setAggregationStrategy(CourseAssessmentAggregationStrategy.POINTS_SUM);
        }
        validateCourseWeight(courseUuid, null, courseAssessment.getWeightPercentage());

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
    public CourseAssessmentDTO updateCourseAssessment(UUID courseUuid, UUID uuid, CourseAssessmentDTO courseAssessmentDTO) {
        CourseAssessment existingCourseAssessment = courseAssessmentRepository.findByUuidAndCourseUuid(uuid, courseUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(COURSE_ASSESSMENT_NOT_FOUND_TEMPLATE, uuid)));

        updateCourseAssessmentFields(existingCourseAssessment, courseAssessmentDTO);
        existingCourseAssessment.setCourseUuid(courseUuid);
        validateCourseWeight(courseUuid, uuid, existingCourseAssessment.getWeightPercentage());
        validateAggregationConfiguration(existingCourseAssessment);

        CourseAssessment updatedCourseAssessment = courseAssessmentRepository.save(existingCourseAssessment);
        courseGradebookService.recalculateCourseAssessment(courseUuid, uuid);
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
        if (dto.aggregationStrategy() != null) {
            existingCourseAssessment.setAggregationStrategy(dto.aggregationStrategy());
        }
        if (dto.rubricUuid() != null) {
            existingCourseAssessment.setRubricUuid(dto.rubricUuid());
        }
        if (dto.isRequired() != null) {
            existingCourseAssessment.setIsRequired(dto.isRequired());
        }
    }

    private void validateCourseWeight(UUID courseUuid, UUID assessmentUuid, BigDecimal assessmentWeight) {
        BigDecimal otherWeightTotal = courseAssessmentRepository.sumWeightPercentageByCourseUuidExcluding(courseUuid, assessmentUuid);
        BigDecimal totalWeight = otherWeightTotal.add(assessmentWeight != null ? assessmentWeight : BigDecimal.ZERO);
        if (totalWeight.compareTo(new BigDecimal("100.00")) > 0) {
            throw new IllegalArgumentException("Course assessment weights cannot exceed 100% for a single course");
        }
    }

    private void validateAggregationConfiguration(CourseAssessment assessment) {
        CourseAssessmentAggregationStrategy strategy = assessment.getAggregationStrategy() != null
                ? assessment.getAggregationStrategy()
                : CourseAssessmentAggregationStrategy.POINTS_SUM;
        if (strategy != CourseAssessmentAggregationStrategy.WEIGHTED_AVERAGE) {
            return;
        }

        boolean hasUnweightedLineItem = courseAssessmentLineItemRepository
                .findByCourseAssessmentUuidOrderByDisplayOrderAscCreatedDateAsc(assessment.getUuid())
                .stream()
                .filter(lineItem -> !Boolean.FALSE.equals(lineItem.getActive()))
                .anyMatch(lineItem -> lineItem.getWeightPercentage() == null || lineItem.getWeightPercentage().compareTo(BigDecimal.ZERO) <= 0);

        if (hasUnweightedLineItem) {
            throw new IllegalArgumentException("Weighted assessment components require all active linked tasks to have positive weights");
        }
    }
}
