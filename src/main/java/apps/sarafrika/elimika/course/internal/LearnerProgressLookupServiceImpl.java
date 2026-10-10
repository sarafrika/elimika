package apps.sarafrika.elimika.course.internal;

import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.model.CourseEnrollment;
import apps.sarafrika.elimika.course.model.ProgramEnrollment;
import apps.sarafrika.elimika.course.model.TrainingProgram;
import apps.sarafrika.elimika.course.repository.AssignmentSubmissionRepository;
import apps.sarafrika.elimika.course.repository.CourseEnrollmentRepository;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.repository.ProgramEnrollmentRepository;
import apps.sarafrika.elimika.course.repository.QuizAttemptRepository;
import apps.sarafrika.elimika.course.repository.TrainingProgramRepository;
import apps.sarafrika.elimika.course.spi.LearnerCourseProgressView;
import apps.sarafrika.elimika.course.spi.LearnerProgramProgressView;
import apps.sarafrika.elimika.course.spi.LearnerProgressLookupService;
import apps.sarafrika.elimika.course.util.enums.AttemptStatus;
import apps.sarafrika.elimika.course.util.enums.SubmissionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
    private final AssignmentSubmissionRepository assignmentSubmissionRepository;
    private final QuizAttemptRepository quizAttemptRepository;

    @Override
    public Page<LearnerCourseProgressView> findCourseProgress(UUID studentUuid, Pageable pageable) {
        if (studentUuid == null) {
            return Page.empty(resolveCourseProgressPageable(pageable));
        }

        Map<UUID, String> courseNameCache = new HashMap<>();
        return courseEnrollmentRepository.findByStudentUuid(studentUuid, resolveCourseProgressPageable(pageable))
                .map(enrollment -> toCourseView(enrollment, courseNameCache));
    }

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

    @Override
    public Map<UUID, LearnerCourseProgressView> findCourseProgressByCourse(UUID studentUuid, Collection<UUID> courseUuids) {
        List<UUID> courses = distinctNonNull(courseUuids);
        if (studentUuid == null || courses.isEmpty()) {
            return Map.of();
        }
        Map<UUID, String> names = new HashMap<>();
        courseRepository.findByUuidIn(courses).forEach(course ->
                names.put(course.getUuid(), course.getName() != null ? course.getName() : "Unknown Course"));
        Map<UUID, LearnerCourseProgressView> progress = new LinkedHashMap<>();
        for (CourseEnrollment enrollment : courseEnrollmentRepository.findByStudentUuidAndCourseUuidIn(studentUuid, courses)) {
            progress.merge(enrollment.getCourseUuid(), toCourseView(enrollment, names), this::moreRecent);
        }
        return progress;
    }

    @Override
    public Set<UUID> findSubmittedAssignmentUuids(UUID studentUuid, Collection<UUID> assignmentUuids) {
        List<UUID> assignments = distinctNonNull(assignmentUuids);
        if (studentUuid == null || assignments.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(assignmentSubmissionRepository.findSubmittedAssignmentUuids(
                studentUuid, assignments, SubmissionStatus.DRAFT));
    }

    @Override
    public Set<UUID> findSubmittedQuizUuids(UUID studentUuid, Collection<UUID> quizUuids) {
        List<UUID> quizzes = distinctNonNull(quizUuids);
        if (studentUuid == null || quizzes.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(quizAttemptRepository.findSubmittedQuizUuids(
                studentUuid, quizzes, AttemptStatus.IN_PROGRESS));
    }

    private static List<UUID> distinctNonNull(Collection<UUID> uuids) {
        return uuids == null ? List.of() : uuids.stream().filter(Objects::nonNull).distinct().toList();
    }

    private LearnerCourseProgressView moreRecent(LearnerCourseProgressView a, LearnerCourseProgressView b) {
        if (a.updatedDate() == null) {
            return b;
        }
        return b.updatedDate() != null && b.updatedDate().isAfter(a.updatedDate()) ? b : a;
    }

    private Pageable buildPageable(int limit) {
        int pageSize = limit > 0 ? limit : DEFAULT_LIMIT;
        return PageRequest.of(0, pageSize, Sort.by(Sort.Direction.DESC, "lastModifiedDate"));
    }

    private Pageable resolveCourseProgressPageable(Pageable pageable) {
        if (pageable == null || pageable.isUnpaged()) {
            return PageRequest.of(0, DEFAULT_LIMIT, Sort.by(Sort.Direction.DESC, "lastModifiedDate"));
        }
        if (pageable.getSort().isSorted()) {
            return pageable;
        }
        return PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "lastModifiedDate"));
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
