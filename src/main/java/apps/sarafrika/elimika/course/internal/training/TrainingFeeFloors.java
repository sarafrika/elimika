package apps.sarafrika.elimika.course.internal.training;

import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.model.ProgramCourse;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.repository.ProgramCourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** The minimum training fee a rate card is held to: the course's own, or the highest across a program's courses. */
@Component
@RequiredArgsConstructor
public class TrainingFeeFloors {

    private final CourseRepository courseRepository;
    private final ProgramCourseRepository programCourseRepository;

    public BigDecimal forCourse(UUID courseUuid) {
        return courseRepository.findByUuid(courseUuid)
                .map(Course::getMinimumTrainingFee)
                .orElse(BigDecimal.ZERO);
    }

    public BigDecimal forProgram(UUID programUuid) {
        return forCourses(programCourseUuids(programUuid));
    }

    public BigDecimal forCourses(Collection<UUID> courseUuids) {
        if (courseUuids == null || courseUuids.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return courseRepository.findByUuidIn(List.copyOf(courseUuids)).stream()
                .map(Course::getMinimumTrainingFee)
                .filter(Objects::nonNull)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    /** Each course's own minimum fee, zero when unset; courses that do not exist are absent. */
    public Map<UUID, BigDecimal> perCourse(Collection<UUID> courseUuids) {
        if (courseUuids == null || courseUuids.isEmpty()) {
            return Map.of();
        }
        Map<UUID, BigDecimal> floors = new HashMap<>();
        courseRepository.findByUuidIn(List.copyOf(courseUuids)).forEach(course -> floors.put(course.getUuid(),
                course.getMinimumTrainingFee() == null ? BigDecimal.ZERO : course.getMinimumTrainingFee()));
        return floors;
    }

    public List<UUID> programCourseUuids(UUID programUuid) {
        return programCourseRepository.findByProgramUuidOrderBySequenceOrderAsc(programUuid).stream()
                .map(ProgramCourse::getCourseUuid)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }
}
