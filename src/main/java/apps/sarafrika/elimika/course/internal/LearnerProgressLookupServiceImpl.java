package apps.sarafrika.elimika.course.internal;

import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.model.CourseEnrollment;
import apps.sarafrika.elimika.course.model.ProgramEnrollment;
import apps.sarafrika.elimika.course.model.TrainingProgram;
import apps.sarafrika.elimika.course.repository.CourseEnrollmentRepository;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.repository.ProgramEnrollmentRepository;
import apps.sarafrika.elimika.course.repository.TrainingProgramRepository;
import apps.sarafrika.elimika.course.spi.LearnerCourseProgressView;
import apps.sarafrika.elimika.course.spi.LearnerProgramProgressView;
import apps.sarafrika.elimika.course.spi.LearnerProgressLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class LearnerProgressLookupServiceImpl implements LearnerProgressLookupService {

    private static final int DEFAULT_LIMIT = 5;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final ProgramEnrollmentRepository programEnrollmentRepository;
    private final CourseRepository courseRepository;
    private final TrainingProgramRepository trainingProgramRepository;

    @Override
    public List<LearnerCourseProgressView> findRecentCourseProgress(UUID studentUuid, int limit) {
        if (studentUuid == null) {
            return List.of();
        }

        Pageable pageable = buildPageable(limit);
        List<CourseEnrollment> enrollments =
                courseEnrollmentRepository.findByStudentUuid(studentUuid, pageable).getContent();

        Map<UUID, String> courseNameCache = new HashMap<>();
        return enrollments.stream()
                .map(enrollment -> toCourseView(enrollment, courseNameCache))
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public List<LearnerProgramProgressView> findRecentProgramProgress(UUID studentUuid, int limit) {
        if (studentUuid == null) {
            return List.of();
        }

        Pageable pageable = buildPageable(limit);
        List<ProgramEnrollment> enrollments =
                programEnrollmentRepository.findByStudentUuid(studentUuid, pageable).getContent();

        Map<UUID, String> programNameCache = new HashMap<>();
        return enrollments.stream()
                .map(enrollment -> toProgramView(enrollment, programNameCache))
                .filter(Objects::nonNull)
                .toList();
    }

    private Pageable buildPageable(int limit) {
        int pageSize = limit > 0 ? limit : DEFAULT_LIMIT;
        return PageRequest.of(0, pageSize, Sort.by(Sort.Direction.DESC, "lastModifiedDate"));
    }

    private LearnerCourseProgressView toCourseView(CourseEnrollment enrollment, Map<UUID, String> cache) {
        if (enrollment == null) {
            return null;
        }

        String name = cache.computeIfAbsent(
                enrollment.getCourseUuid(),
                courseUuid -> courseRepository.findByUuid(courseUuid)
                        .map(Course::getName)
                        .orElse("Unknown Course")
        );

        return new LearnerCourseProgressView(
                enrollment.getUuid(),
                enrollment.getCourseUuid(),
                name,
                enrollment.getStatus() != null ? enrollment.getStatus().name() : null,
                enrollment.getProgressPercentage(),
                enrollment.getLastModifiedDate()
        );
    }

    private LearnerProgramProgressView toProgramView(ProgramEnrollment enrollment, Map<UUID, String> cache) {
        if (enrollment == null) {
            return null;
        }

        String name = cache.computeIfAbsent(
                enrollment.getProgramUuid(),
                programUuid -> trainingProgramRepository.findByUuid(programUuid)
                        .map(TrainingProgram::getTitle)
                        .orElse("Unknown Program")
        );

        return new LearnerProgramProgressView(
                enrollment.getUuid(),
                enrollment.getProgramUuid(),
                name,
                enrollment.getStatus() != null ? enrollment.getStatus().name() : null,
                enrollment.getProgressPercentage(),
                enrollment.getLastModifiedDate()
        );
    }
}
