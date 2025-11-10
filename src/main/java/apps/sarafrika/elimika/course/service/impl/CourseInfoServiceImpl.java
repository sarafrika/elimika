package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.spi.CourseInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of Course Information Service
 * <p>
 * Provides read-only access to course information for other modules.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2025-10-23
 */
@Service
@RequiredArgsConstructor
public class CourseInfoServiceImpl implements CourseInfoService {

    private final CourseRepository courseRepository;

    @Override
    public Optional<BigDecimal> getMinimumTrainingFee(UUID courseUuid) {
        return courseRepository.findByUuid(courseUuid)
                .map(course -> course.getMinimumTrainingFee() != null
                        ? course.getMinimumTrainingFee()
                        : BigDecimal.ZERO);
    }

    @Override
    public boolean courseExists(UUID courseUuid) {
        return courseRepository.existsByUuid(courseUuid);
    }

    @Override
    public Optional<String> getCourseName(UUID courseUuid) {
        return courseRepository.findByUuid(courseUuid)
                .map(Course::getName);
    }

    @Override
    public Optional<AgeLimits> getAgeLimits(UUID courseUuid) {
        return courseRepository.findByUuid(courseUuid)
                .map(course -> new AgeLimits(course.getAgeLowerLimit(), course.getAgeUpperLimit()))
                .filter(limits -> limits.minAge() != null || limits.maxAge() != null);
    }
}
