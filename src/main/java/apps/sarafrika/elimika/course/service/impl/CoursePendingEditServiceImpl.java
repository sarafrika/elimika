package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.CourseEditDiffDTO;
import apps.sarafrika.elimika.course.dto.CoursePendingEditDTO;
import apps.sarafrika.elimika.course.dto.CourseVersionSnapshotDTO;
import apps.sarafrika.elimika.course.factory.CoursePendingEditFactory;
import apps.sarafrika.elimika.course.factory.CourseVersionSnapshotFactory;
import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.model.CoursePendingEdit;
import apps.sarafrika.elimika.course.repository.CoursePendingEditRepository;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.repository.CourseVersionSnapshotRepository;
import apps.sarafrika.elimika.course.service.CourseDraftService;
import apps.sarafrika.elimika.course.service.CoursePendingEditService;
import apps.sarafrika.elimika.course.util.enums.ContentStatus;
import apps.sarafrika.elimika.course.util.enums.PendingEditStatus;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.service.UserContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CoursePendingEditServiceImpl implements CoursePendingEditService {

    private final CoursePendingEditRepository pendingEditRepository;
    private final CourseVersionSnapshotRepository snapshotRepository;
    private final CourseRepository courseRepository;
    private final CourseDraftService courseDraftService;
    private final UserContextService userContextService;

    private static final String COURSE_NOT_FOUND = "Course not found with UUID: %s";
    private static final String NO_PENDING_EDIT = "No edit awaiting review for course: %s";

    @Override
    public CoursePendingEditDTO submitOrRefresh(UUID courseUuid, UUID draftCourseUuid) {
        CoursePendingEdit edit = pendingEditRepository
                .findByCourseUuidAndStatus(courseUuid, PendingEditStatus.PENDING)
                .orElseGet(CoursePendingEdit::new);

        edit.setCourseUuid(courseUuid);
        edit.setDraftCourseUuid(draftCourseUuid);
        edit.setStatus(PendingEditStatus.PENDING);
        edit.setSubmittedAt(now());
        userContextService.getCurrentUserUuidOptional().ifPresent(edit::setSubmittedByUuid);

        CoursePendingEdit saved = pendingEditRepository.save(edit);
        log.info("Course {} has an edit awaiting review (draft {})", courseUuid, draftCourseUuid);
        return CoursePendingEditFactory.toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CoursePendingEditDTO> findPending(UUID courseUuid) {
        return pendingEditRepository.findByCourseUuidAndStatus(courseUuid, PendingEditStatus.PENDING)
                .map(CoursePendingEditFactory::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CoursePendingEditDTO> getHistory(UUID courseUuid, Pageable pageable) {
        return pendingEditRepository.findByCourseUuidOrderBySubmittedAtDesc(courseUuid, pageable)
                .map(CoursePendingEditFactory::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CoursePendingEditDTO> getPendingQueue(Pageable pageable) {
        return pendingEditRepository
                .findByStatusOrderBySubmittedAtDesc(PendingEditStatus.PENDING, pageable)
                .map(CoursePendingEditFactory::toDTO);
    }

    @Override
    public CoursePendingEditDTO approve(UUID courseUuid, String reason) {
        CoursePendingEdit edit = requirePending(courseUuid);

        courseDraftService.promote(courseUuid, edit.getUuid());

        edit.setStatus(PendingEditStatus.APPROVED);
        // The draft is gone once promoted; the row stays as history.
        edit.setDraftCourseUuid(null);
        resolve(edit, reason);

        log.info("Approved and promoted edit {} for course {}", edit.getUuid(), courseUuid);
        return CoursePendingEditFactory.toDTO(pendingEditRepository.save(edit));
    }

    @Override
    public CoursePendingEditDTO reject(UUID courseUuid, String reason) {
        CoursePendingEdit edit = requirePending(courseUuid);

        courseDraftService.discard(courseUuid);

        edit.setStatus(PendingEditStatus.REJECTED);
        edit.setDraftCourseUuid(null);
        resolve(edit, reason);

        // Deliberately does not touch the live course's admin_approved: the published
        // content was never at fault, only the proposed change was.
        log.info("Rejected edit {} for course {}; live course untouched", edit.getUuid(), courseUuid);
        return CoursePendingEditFactory.toDTO(pendingEditRepository.save(edit));
    }

    @Override
    public CoursePendingEditDTO withdraw(UUID courseUuid) {
        CoursePendingEdit edit = requirePending(courseUuid);

        courseDraftService.discard(courseUuid);

        edit.setStatus(PendingEditStatus.WITHDRAWN);
        edit.setDraftCourseUuid(null);
        edit.setReviewedAt(now());

        log.info("Creator withdrew edit {} for course {}", edit.getUuid(), courseUuid);
        return CoursePendingEditFactory.toDTO(pendingEditRepository.save(edit));
    }

    @Override
    @Transactional(readOnly = true)
    public CourseEditDiffDTO diff(UUID courseUuid) {
        requirePending(courseUuid);
        return courseDraftService.diff(courseUuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseVersionSnapshotDTO> getVersions(UUID courseUuid, Pageable pageable) {
        return snapshotRepository.findByCourseUuidOrderByVersionNumberDesc(courseUuid, pageable)
                .map(CourseVersionSnapshotFactory::toDTO);
    }

    /**
     * An edit needs review only once the course is live and approved. Drafts, courses still
     * awaiting their first approval, and archived courses are edited directly, as before.
     */
    @Override
    @Transactional(readOnly = true)
    public boolean requiresReview(UUID courseUuid) {
        Course course = courseRepository.findByUuid(courseUuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(COURSE_NOT_FOUND, courseUuid)));
        return requiresReview(course);
    }

    static boolean requiresReview(Course course) {
        return course.getParentCourseUuid() == null
                && course.getStatus() == ContentStatus.PUBLISHED
                && Boolean.TRUE.equals(course.getAdminApproved());
    }

    private CoursePendingEdit requirePending(UUID courseUuid) {
        return pendingEditRepository.findByCourseUuidAndStatus(courseUuid, PendingEditStatus.PENDING)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(NO_PENDING_EDIT, courseUuid)));
    }

    private void resolve(CoursePendingEdit edit, String reason) {
        edit.setReviewedAt(now());
        edit.setReviewReason(reason);
        userContextService.getCurrentUserUuidOptional().ifPresent(edit::setReviewedByUuid);
    }

    private static LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
