package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.common.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.common.util.GenericSpecificationBuilder;
import apps.sarafrika.elimika.course.dto.CourseAssessmentScoreDTO;
import apps.sarafrika.elimika.course.factory.CourseAssessmentScoreFactory;
import apps.sarafrika.elimika.course.model.CourseAssessmentScore;
import apps.sarafrika.elimika.course.repository.CourseAssessmentScoreRepository;
import apps.sarafrika.elimika.course.service.CourseAssessmentScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CourseAssessmentScoreServiceImpl implements CourseAssessmentScoreService {

    private final CourseAssessmentScoreRepository courseAssessmentScoreRepository;
    private final GenericSpecificationBuilder<CourseAssessmentScore> specificationBuilder;

    private static final String COURSE_ASSESSMENT_SCORE_NOT_FOUND_TEMPLATE = "Course assessment score with ID %s not found";

    @Override
    public CourseAssessmentScoreDTO createCourseAssessmentScore(CourseAssessmentScoreDTO courseAssessmentScoreDTO) {
        CourseAssessmentScore courseAssessmentScore = CourseAssessmentScoreFactory.toEntity(courseAssessmentScoreDTO);

        // Set defaults and calculate percentage
        if (courseAssessmentScore.getGradedAt() == null) {
            courseAssessmentScore.setGradedAt(LocalDateTime.now());
        }

        // Calculate percentage if score and max_score are provided
        if (courseAssessmentScore.getScore() != null && courseAssessmentScore.getMaxScore() != null &&
                courseAssessmentScore.getMaxScore().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal percentage = courseAssessmentScore.getScore()
                    .divide(courseAssessmentScore.getMaxScore(), 2, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            courseAssessmentScore.setPercentage(percentage);
        }

        CourseAssessmentScore savedCourseAssessmentScore = courseAssessmentScoreRepository.save(courseAssessmentScore);
        return CourseAssessmentScoreFactory.toDTO(savedCourseAssessmentScore);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseAssessmentScoreDTO getCourseAssessmentScoreByUuid(UUID uuid) {
        return courseAssessmentScoreRepository.findByUuid(uuid)
                .map(CourseAssessmentScoreFactory::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(COURSE_ASSESSMENT_SCORE_NOT_FOUND_TEMPLATE, uuid)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseAssessmentScoreDTO> getAllCourseAssessmentScores(Pageable pageable) {
        return courseAssessmentScoreRepository.findAll(pageable).map(CourseAssessmentScoreFactory::toDTO);
    }

    @Override
    public CourseAssessmentScoreDTO updateCourseAssessmentScore(UUID uuid, CourseAssessmentScoreDTO courseAssessmentScoreDTO) {
        CourseAssessmentScore existingCourseAssessmentScore = courseAssessmentScoreRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(COURSE_ASSESSMENT_SCORE_NOT_FOUND_TEMPLATE, uuid)));

        updateCourseAssessmentScoreFields(existingCourseAssessmentScore, courseAssessmentScoreDTO);

        // Recalculate percentage if score or max_score changed
        if (existingCourseAssessmentScore.getScore() != null && existingCourseAssessmentScore.getMaxScore() != null &&
                existingCourseAssessmentScore.getMaxScore().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal percentage = existingCourseAssessmentScore.getScore()
                    .divide(existingCourseAssessmentScore.getMaxScore(), 2, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            existingCourseAssessmentScore.setPercentage(percentage);
        }

        CourseAssessmentScore updatedCourseAssessmentScore = courseAssessmentScoreRepository.save(existingCourseAssessmentScore);
        return CourseAssessmentScoreFactory.toDTO(updatedCourseAssessmentScore);
    }

    @Override
    public void deleteCourseAssessmentScore(UUID uuid) {
        if (!courseAssessmentScoreRepository.existsByUuid(uuid)) {
            throw new ResourceNotFoundException(
                    String.format(COURSE_ASSESSMENT_SCORE_NOT_FOUND_TEMPLATE, uuid));
        }
        courseAssessmentScoreRepository.deleteByUuid(uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseAssessmentScoreDTO> search(Map<String, String> searchParams, Pageable pageable) {
        Specification<CourseAssessmentScore> spec = specificationBuilder.buildSpecification(
                CourseAssessmentScore.class, searchParams);
        return courseAssessmentScoreRepository.findAll(spec, pageable).map(CourseAssessmentScoreFactory::toDTO);
    }

    private void updateCourseAssessmentScoreFields(CourseAssessmentScore existingCourseAssessmentScore, CourseAssessmentScoreDTO dto) {
        if (dto.enrollmentUuid() != null) {
            existingCourseAssessmentScore.setEnrollmentUuid(dto.enrollmentUuid());
        }
        if (dto.assessmentUuid() != null) {
            existingCourseAssessmentScore.setAssessmentUuid(dto.assessmentUuid());
        }
        if (dto.score() != null) {
            existingCourseAssessmentScore.setScore(dto.score());
        }
        if (dto.maxScore() != null) {
            existingCourseAssessmentScore.setMaxScore(dto.maxScore());
        }
        if (dto.percentage() != null) {
            existingCourseAssessmentScore.setPercentage(dto.percentage());
        }
        if (dto.gradedAt() != null) {
            existingCourseAssessmentScore.setGradedAt(dto.gradedAt());
        }
        if (dto.gradedByUuid() != null) {
            existingCourseAssessmentScore.setGradedByUuid(dto.gradedByUuid());
        }
        if (dto.comments() != null) {
            existingCourseAssessmentScore.setComments(dto.comments());
        }
    }
}