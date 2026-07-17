package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.course.dto.CourseDTO;
import apps.sarafrika.elimika.course.dto.CourseTrainingRequirementDTO;
import apps.sarafrika.elimika.course.factory.CourseFactory;
import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.repository.CourseCategoryMappingRepository;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.model.CourseCategoryMapping;
import apps.sarafrika.elimika.course.service.ContentModerationHistoryService;
import apps.sarafrika.elimika.course.service.CourseCategoryService;
import apps.sarafrika.elimika.course.service.CourseDraftService;
import apps.sarafrika.elimika.course.service.CoursePendingEditService;
import apps.sarafrika.elimika.course.service.CourseEnrollmentService;
import apps.sarafrika.elimika.course.service.CourseService;
import apps.sarafrika.elimika.course.service.CourseTrainingRequirementService;
import apps.sarafrika.elimika.course.service.LessonService;
import apps.sarafrika.elimika.course.util.CourseRevenueShareValidator;
import apps.sarafrika.elimika.course.util.CourseSpecificationBuilder;
import apps.sarafrika.elimika.course.util.enums.ContentStatus;
import apps.sarafrika.elimika.course.util.enums.EnrollmentStatus;
import apps.sarafrika.elimika.course.util.enums.ModerationAction;
import apps.sarafrika.elimika.course.util.enums.ModerationContentType;
import apps.sarafrika.elimika.coursecreator.spi.CourseCreatorLookupService;
import apps.sarafrika.elimika.shared.event.notification.NotificationRequestedEvent;
import apps.sarafrika.elimika.shared.storage.config.StorageProperties;
import apps.sarafrika.elimika.shared.storage.service.MediaStorageService;
import apps.sarafrika.elimika.shared.storage.service.MediaUploadRequest;
import apps.sarafrika.elimika.shared.storage.util.FileUrlResolver;
import apps.sarafrika.elimika.shared.storage.util.MediaCategory;
import apps.sarafrika.elimika.shared.storage.util.MediaOwnerType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final CourseCategoryMappingRepository mappingRepository;
    private final CourseSpecificationBuilder courseSpecificationBuilder;
    private final LessonService lessonService;
    private final CourseEnrollmentService courseEnrollmentService;
    private final CourseCategoryService courseCategoryService;
    private final CourseTrainingRequirementService courseTrainingRequirementService;
    private final MediaStorageService mediaStorageService;
    private final StorageProperties storageProperties;
    private final ApplicationEventPublisher eventPublisher;
    private final CourseCreatorLookupService courseCreatorLookupService;
    private final ContentModerationHistoryService contentModerationHistoryService;
    private final CourseDraftService courseDraftService;
    private final CoursePendingEditService coursePendingEditService;

    private static final String COURSE_NOT_FOUND_TEMPLATE = "Course with ID %s not found";

    @Override
    public CourseDTO createCourse(CourseDTO courseDTO) {
        log.debug("Creating new course: {}", courseDTO.name());

        Course course = CourseFactory.toEntity(courseDTO);

        // Set defaults based on CourseDTO business logic
        if (course.getStatus() == null) {
            course.setStatus(ContentStatus.DRAFT);
        }
        if (course.getActive() == null) {
            course.setActive(false); // Only published courses can be active
        }
        if (course.getAdminApproved() == null) {
            course.setAdminApproved(false);
        }
        if (course.getMinimumTrainingFee() == null) {
            course.setMinimumTrainingFee(course.getPrice() != null ? course.getPrice() : BigDecimal.ZERO);
        }

        validateRevenueShare(course);

        Course savedCourse = courseRepository.save(course);

        // Handle category assignments
        handleCategoryAssignments(savedCourse.getUuid(), courseDTO);

        // Fetch the course with category names for response
        return getCourseByUuid(savedCourse.getUuid());
    }

    @Override
    @Transactional(readOnly = true)
    public CourseDTO getCourseByUuid(UUID uuid) {
        Course course = courseRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(COURSE_NOT_FOUND_TEMPLATE, uuid)));

        // Fetch category names
        List<String> categoryNames = mappingRepository.findCategoryNamesByCourseUuid(uuid);
        List<CourseTrainingRequirementDTO> trainingRequirements = courseTrainingRequirementService.findByCourseUuid(uuid);

        return CourseFactory.toDTO(course, categoryNames, trainingRequirements.isEmpty() ? null : trainingRequirements);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseDTO> getAllCourses(Pageable pageable) {
        Page<Course> coursePage = courseRepository.findAll(pageable);

        return coursePage.map(course -> {
            List<String> categoryNames = mappingRepository.findCategoryNamesByCourseUuid(course.getUuid());
            return CourseFactory.toDTO(course, categoryNames);
        });
    }

    /**
     * Updates a course, routing material changes to a draft when the course is already live.
     * <p>
     * A published, admin-approved course keeps serving its approved content while an edit is
     * reviewed, so material changes are applied to a shadow draft rather than the live row.
     * Media fields are cosmetic and apply immediately — a creator should not wait on review
     * to swap a thumbnail. Courses that are not yet live are edited directly, as before.
     */
    @Override
    public CourseDTO updateCourse(UUID uuid, CourseDTO courseDTO) {
        log.debug("Updating course: {}", uuid);

        Course existingCourse = courseRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(COURSE_NOT_FOUND_TEMPLATE, uuid)));

        if (!CoursePendingEditServiceImpl.requiresReview(existingCourse)) {
            updateCourseFields(existingCourse, courseDTO);
            validateRevenueShare(existingCourse);
            courseRepository.save(existingCourse);
            handleCategoryAssignments(uuid, courseDTO);
            return getCourseByUuid(uuid);
        }

        applyCosmeticFields(existingCourse, courseDTO);
        courseRepository.save(existingCourse);

        if (hasMaterialChanges(uuid, courseDTO)) {
            Course draft = courseDraftService.openDraft(uuid);
            updateCourseFields(draft, courseDTO);
            validateRevenueShare(draft);
            courseRepository.save(draft);
            handleCategoryAssignments(draft.getUuid(), courseDTO);
            coursePendingEditService.submitOrRefresh(uuid, draft.getUuid());
            log.info("Material changes to live course {} routed to draft {} for review", uuid, draft.getUuid());
        }

        // Returns the live course: the creator's material changes are not live yet.
        return getCourseByUuid(uuid);
    }

    /**
     * Media fields only. These apply to the live course immediately and never trigger review.
     */
    private void applyCosmeticFields(Course course, CourseDTO dto) {
        if (dto.thumbnailUrl() != null) {
            course.setThumbnailUrl(FileUrlResolver.toStorableValue(dto.thumbnailUrl()));
        }
        if (dto.introVideoUrl() != null) {
            course.setIntroVideoUrl(FileUrlResolver.toStorableValue(dto.introVideoUrl()));
        }
        if (dto.bannerUrl() != null) {
            course.setBannerUrl(FileUrlResolver.toStorableValue(dto.bannerUrl()));
        }
    }

    /**
     * Whether the incoming payload actually changes anything material.
     * <p>
     * Compared against the open draft when there is one, so a creator refining an edit keeps
     * building on their working copy, and re-sending an unchanged value never re-opens review.
     */
    private boolean hasMaterialChanges(UUID liveCourseUuid, CourseDTO dto) {
        Course baseline = courseDraftService.findDraft(liveCourseUuid)
                .orElseGet(() -> courseRepository.findByUuid(liveCourseUuid)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                String.format(COURSE_NOT_FOUND_TEMPLATE, liveCourseUuid))));

        if (differs(dto.name(), baseline.getName())
                || differs(dto.description(), baseline.getDescription())
                || differs(dto.objectives(), baseline.getObjectives())
                || differs(dto.prerequisites(), baseline.getPrerequisites())
                || differs(dto.difficultyUuid(), baseline.getDifficultyUuid())
                || differs(dto.durationHours(), baseline.getDurationHours())
                || differs(dto.durationMinutes(), baseline.getDurationMinutes())
                || differs(dto.classLimit(), baseline.getClassLimit())
                || differs(dto.revenueShareNotes(), baseline.getRevenueShareNotes())
                || differs(dto.ageLowerLimit(), baseline.getAgeLowerLimit())
                || differs(dto.ageUpperLimit(), baseline.getAgeUpperLimit())) {
            return true;
        }

        // Money is compared by value, so 1500 and 1500.00 are not treated as a change.
        if (differsNumerically(dto.price(), baseline.getPrice())
                || differsNumerically(dto.minimumTrainingFee(), baseline.getMinimumTrainingFee())
                || differsNumerically(dto.creatorSharePercentage(), baseline.getCreatorSharePercentage())
                || differsNumerically(dto.instructorSharePercentage(), baseline.getInstructorSharePercentage())) {
            return true;
        }

        return categoriesDiffer(baseline.getUuid(), dto.categoryUuids());
    }

    private boolean categoriesDiffer(UUID baselineCourseUuid, Set<UUID> incoming) {
        if (incoming == null) {
            return false;
        }
        Set<UUID> current = mappingRepository.findByCourseUuid(baselineCourseUuid).stream()
                .map(CourseCategoryMapping::getCategoryUuid)
                .collect(java.util.stream.Collectors.toSet());
        return !current.equals(incoming);
    }

    /** A null incoming value means "not supplied", never "clear it". */
    private boolean differs(Object incoming, Object current) {
        return incoming != null && !incoming.equals(current);
    }

    private boolean differsNumerically(BigDecimal incoming, BigDecimal current) {
        if (incoming == null) {
            return false;
        }
        return current == null || incoming.compareTo(current) != 0;
    }

    @Override
    public void deleteCourse(UUID uuid) {
        log.debug("Deleting course: {}", uuid);

        if (!courseRepository.existsByUuid(uuid)) {
            throw new ResourceNotFoundException(
                    String.format(COURSE_NOT_FOUND_TEMPLATE, uuid));
        }

        // Remove all category associations first
        courseCategoryService.removeAllCategoriesFromCourse(uuid);

        mediaStorageService.deleteAllForOwner(MediaOwnerType.COURSE_THUMBNAIL, uuid);
        mediaStorageService.deleteAllForOwner(MediaOwnerType.COURSE_BANNER, uuid);
        mediaStorageService.deleteAllForOwner(MediaOwnerType.COURSE_INTRO_VIDEO, uuid);

        courseRepository.deleteByUuid(uuid);
        log.info("Successfully deleted course: {}", uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseDTO> search(Map<String, String> searchParams, Pageable pageable) {
        log.debug("Searching courses with params: {}", searchParams);
        Specification<Course> spec = courseSpecificationBuilder.buildCourseSpecification(searchParams);

        Page<Course> coursePage = courseRepository.findAll(spec, pageable);

        return coursePage.map(course -> {
            List<String> categoryNames = mappingRepository.findCategoryNamesByCourseUuid(course.getUuid());
            return CourseFactory.toDTO(course, categoryNames);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isCourseReadyForPublishing(UUID uuid) {
        CourseDTO course = getCourseByUuid(uuid);

        // Check basic requirements for publishing
        if (course.name() == null || course.name().trim().isEmpty()) {
            return false;
        }

        if (course.description() == null || course.description().trim().isEmpty()) {
            return false;
        }

        // Check if course has at least one lesson
        Map<String, String> searchParams = Map.of("courseUuid", uuid.toString());
        Page<apps.sarafrika.elimika.course.dto.LessonDTO> lessons =
                lessonService.search(searchParams, Pageable.ofSize(1));

        return !lessons.isEmpty();
    }

    @Override
    public CourseDTO publishCourse(UUID uuid) {
        log.debug("Publishing course: {}", uuid);

        Course course = courseRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(COURSE_NOT_FOUND_TEMPLATE, uuid)));

        // Validate course is ready for publishing
        if (!isCourseReadyForPublishing(uuid)) {
            throw new IllegalStateException("Course is not ready for publishing");
        }

        course.setStatus(ContentStatus.PUBLISHED);
        course.setActive(true);

        Course publishedCourse = courseRepository.save(course);
        log.info("Successfully published course: {}", uuid);

        return getCourseByUuid(uuid);
    }

    @Override
    public CourseDTO approveCourse(UUID uuid, String reason) {
        log.debug("Approving course {} with reason {}", uuid, reason);

        Course course = courseRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(COURSE_NOT_FOUND_TEMPLATE, uuid)));

        if (!Boolean.TRUE.equals(course.getAdminApproved())) {
            course.setAdminApproved(true);
            courseRepository.save(course);
            contentModerationHistoryService.record(ModerationContentType.COURSE, uuid, ModerationAction.APPROVED, reason);
            publishCourseModerationNotification(course, true, reason);
            log.info("Approved course {} for reason {}", uuid, reason);
        }

        return getCourseByUuid(uuid);
    }

    @Override
    public CourseDTO unapproveCourse(UUID uuid, String reason, ModerationAction action) {
        log.debug("Removing approval from course {} with reason {}", uuid, reason);

        Course course = courseRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(COURSE_NOT_FOUND_TEMPLATE, uuid)));

        boolean wasApproved = !Boolean.FALSE.equals(course.getAdminApproved());
        if (wasApproved) {
            course.setAdminApproved(false);
            courseRepository.save(course);
        }

        // A rejection of a still-pending course changes no state but must still be recorded and communicated
        if (wasApproved || action == ModerationAction.REJECTED) {
            contentModerationHistoryService.record(ModerationContentType.COURSE, uuid, action, reason);
            publishCourseModerationNotification(course, false, reason);
            log.info("Removed course approval {} for reason {}", uuid, reason);
        }

        return getCourseByUuid(uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isCourseApproved(UUID uuid) {
        Course course = courseRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(COURSE_NOT_FOUND_TEMPLATE, uuid)));
        return Boolean.TRUE.equals(course.getAdminApproved());
    }

    @Override
    @Transactional(readOnly = true)
    public double getCourseCompletionRate(UUID uuid) {
        // Verify course exists
        if (!courseRepository.existsByUuid(uuid)) {
            throw new ResourceNotFoundException(
                    String.format(COURSE_NOT_FOUND_TEMPLATE, uuid));
        }

        try {
            // Get total enrollments for the course
            Map<String, String> enrollmentParams = Map.of("courseUuid", uuid.toString());
            Page<apps.sarafrika.elimika.course.dto.CourseEnrollmentDTO> allEnrollments =
                    courseEnrollmentService.search(enrollmentParams, Pageable.unpaged());

            long totalEnrollments = allEnrollments.getTotalElements();

            if (totalEnrollments == 0) {
                return 0.0;
            }

            // Get completed enrollments
            Map<String, String> completedParams = Map.of(
                    "courseUuid", uuid.toString(),
                    "status", "COMPLETED"
            );
            Page<apps.sarafrika.elimika.course.dto.CourseEnrollmentDTO> completedEnrollments =
                    courseEnrollmentService.search(completedParams, Pageable.unpaged());

            long completedCount = completedEnrollments.getTotalElements();

            return (double) completedCount / totalEnrollments * 100.0;

        } catch (Exception e) {
            // If enrollment service is not available or fails, return 0
            return 0.0;
        }
    }

    @Override
    public CourseDTO uploadThumbnail(UUID courseUuid, MultipartFile thumbnail) {
        log.debug("Uploading thumbnail for course: {}", courseUuid);

        Course course = courseRepository.findByUuid(courseUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(COURSE_NOT_FOUND_TEMPLATE, courseUuid)));

        try {
            course.setThumbnailUrl(storeCourseMedia(thumbnail, MediaCategory.THUMBNAIL,
                    storageProperties.getFolders().getCourseThumbnails(),
                    MediaOwnerType.COURSE_THUMBNAIL, course.getUuid(), course.getThumbnailUrl()));
            Course savedCourse = courseRepository.save(course);
            return CourseFactory.toDTO(savedCourse);
        } catch (Exception ex) {
            log.error("Failed to upload course thumbnail for UUID: {}", courseUuid, ex);
            throw new RuntimeException("Failed to upload course thumbnail: " + ex.getMessage(), ex);
        }
    }

    @Override
    public CourseDTO uploadBanner(UUID courseUuid, MultipartFile banner) {
        log.debug("Uploading banner for course: {}", courseUuid);

        Course course = courseRepository.findByUuid(courseUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(COURSE_NOT_FOUND_TEMPLATE, courseUuid)));

        try {
            course.setBannerUrl(storeCourseMedia(banner, MediaCategory.BANNER,
                    storageProperties.getFolders().getCourseBanners(),
                    MediaOwnerType.COURSE_BANNER, course.getUuid(), course.getBannerUrl()));
            courseRepository.save(course);
            return getCourseByUuid(courseUuid);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to upload course banner: " + ex.getMessage(), ex);
        }
    }

    @Override
    public CourseDTO uploadIntroVideo(UUID courseUuid, MultipartFile introVideo) {
        log.debug("Uploading intro video for course: {}", courseUuid);

        Course course = courseRepository.findByUuid(courseUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(COURSE_NOT_FOUND_TEMPLATE, courseUuid)));

        try {
            course.setIntroVideoUrl(storeCourseMedia(introVideo, MediaCategory.VIDEO,
                    storageProperties.getFolders().getCourseMaterials(),
                    MediaOwnerType.COURSE_INTRO_VIDEO, course.getUuid(), course.getIntroVideoUrl()));
            courseRepository.save(course);
            return getCourseByUuid(courseUuid);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to upload course intro video: " + ex.getMessage(), ex);
        }
    }

    @Override
    public CourseDTO unpublishCourse(UUID uuid) {
        log.debug("Unpublishing course: {}", uuid);

        Course course = courseRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(COURSE_NOT_FOUND_TEMPLATE, uuid)));

        boolean hasActiveEnrollments = hasActiveEnrollments(uuid);

        course.setStatus(ContentStatus.DRAFT);

        course.setActive(hasActiveEnrollments);

        courseRepository.save(course);

        return getCourseByUuid(uuid);
    }

    @Override
    public CourseDTO archiveCourse(UUID uuid) {
        log.debug("Archiving course: {}", uuid);

        Course course = courseRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(COURSE_NOT_FOUND_TEMPLATE, uuid)));

        course.setStatus(ContentStatus.ARCHIVED);
        course.setActive(false);

        Course archivedCourse = courseRepository.save(course);
        log.info("Successfully archived course: {}", uuid);

        return getCourseByUuid(uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canUnpublishCourse(UUID uuid) {
        return true;
    }

    /**
     * Helper method to check if course has active enrollments
     */
    private boolean hasActiveEnrollments(UUID courseUuid) {
        return courseEnrollmentService.existsByCourseUuidAndStatusIn(courseUuid, List.of(EnrollmentStatus.ACTIVE));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContentStatus> getAvailableStatusTransitions(UUID uuid) {
        Course course = courseRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(COURSE_NOT_FOUND_TEMPLATE, uuid)));

        ContentStatus currentStatus = course.getStatus();

        // Define valid status transitions based on business rules
        return switch (currentStatus) {
            case DRAFT -> List.of(ContentStatus.IN_REVIEW, ContentStatus.PUBLISHED, ContentStatus.ARCHIVED);
            case IN_REVIEW -> List.of(ContentStatus.DRAFT, ContentStatus.PUBLISHED, ContentStatus.ARCHIVED);
            case PUBLISHED -> List.of(ContentStatus.DRAFT, ContentStatus.ARCHIVED); // Can always unpublish
            case ARCHIVED -> List.of();
        };
    }

    private void validateRevenueShare(Course course) {
        CourseRevenueShareValidator.validate(course);
    }

    /**
     * Handle category assignments for a course
     */
    private void handleCategoryAssignments(UUID courseUuid, CourseDTO courseDTO) {
        Set<UUID> categoryUuids = courseDTO.categoryUuids();

        // Handle category assignments if provided
        if (categoryUuids != null && !categoryUuids.isEmpty()) {
            log.debug("Updating categories for course {} with {} categories", courseUuid, categoryUuids.size());
            courseCategoryService.updateCourseCategories(courseUuid, categoryUuids);
        }
    }

    private void updateCourseFields(Course existingCourse, CourseDTO dto) {
        if (dto.name() != null) {
            existingCourse.setName(dto.name());
        }
        if (dto.courseCreatorUuid() != null) {
            existingCourse.setCourseCreatorUuid(dto.courseCreatorUuid());
        }
        if (dto.difficultyUuid() != null) {
            existingCourse.setDifficultyUuid(dto.difficultyUuid());
        }
        if (dto.description() != null) {
            existingCourse.setDescription(dto.description());
        }
        if (dto.objectives() != null) {
            existingCourse.setObjectives(dto.objectives());
        }
        if (dto.prerequisites() != null) {
            existingCourse.setPrerequisites(dto.prerequisites());
        }
        if (dto.durationHours() != null) {
            existingCourse.setDurationHours(dto.durationHours());
        }
        if (dto.durationMinutes() != null) {
            existingCourse.setDurationMinutes(dto.durationMinutes());
        }
        if (dto.classLimit() != null) {
            existingCourse.setClassLimit(dto.classLimit());
        }
        if (dto.price() != null) {
            existingCourse.setPrice(dto.price());
        }
        if (dto.minimumTrainingFee() != null) {
            existingCourse.setMinimumTrainingFee(dto.minimumTrainingFee());
        }
        if (dto.creatorSharePercentage() != null) {
            existingCourse.setCreatorSharePercentage(dto.creatorSharePercentage());
        }
        if (dto.instructorSharePercentage() != null) {
            existingCourse.setInstructorSharePercentage(dto.instructorSharePercentage());
        }
        if (dto.revenueShareNotes() != null) {
            existingCourse.setRevenueShareNotes(dto.revenueShareNotes());
        }
        if (dto.ageLowerLimit() != null) {
            existingCourse.setAgeLowerLimit(dto.ageLowerLimit());
        }
        if (dto.ageUpperLimit() != null) {
            existingCourse.setAgeUpperLimit(dto.ageUpperLimit());
        }
        // Media fields accept external URLs as-is but reduce our own resolved
        // /api/v1/files/... URLs back to storage keys so round-tripped DTOs
        // never persist a URL form.
        if (dto.thumbnailUrl() != null) {
            existingCourse.setThumbnailUrl(FileUrlResolver.toStorableValue(dto.thumbnailUrl()));
        }
        if (dto.introVideoUrl() != null) {
            existingCourse.setIntroVideoUrl(FileUrlResolver.toStorableValue(dto.introVideoUrl()));
        }
        if (dto.bannerUrl() != null) {
            existingCourse.setBannerUrl(FileUrlResolver.toStorableValue(dto.bannerUrl()));
        }
        // status and active are deliberately not settable here. Lifecycle changes go through
        // publishCourse/unpublishCourse/archiveCourse, which enforce the readiness checks and
        // the review workflow. Accepting them on a plain PUT let a client demote a live course
        // to draft — which is exactly how editing used to unpublish a published course.
    }

    /**
     * Stores a course media file and returns its canonical storage key, replacing the
     * previous file on disk and in the media registry. A null file clears the field
     * and removes the previous file.
     */
    private String storeCourseMedia(MultipartFile file, MediaCategory category, String folder,
                                    String ownerType, UUID ownerUuid, String previousValue) {
        if (file == null) {
            mediaStorageService.delete(previousValue);
            return null;
        }
        return mediaStorageService.store(new MediaUploadRequest(
                file, category, folder, ownerType, ownerUuid, previousValue)).key();
    }

    private void publishCourseModerationNotification(Course course, boolean approved, String reason) {
        if (course.getCourseCreatorUuid() == null) {
            return;
        }
        UUID recipientUserUuid = courseCreatorLookupService.getCourseCreatorUserUuid(course.getCourseCreatorUuid())
                .orElse(null);
        if (recipientUserUuid == null) {
            return;
        }
        String type = approved ? "COURSE_CONTENT_APPROVED" : "COURSE_CONTENT_REJECTED";
        String courseName = course.getName() == null ? "Your course" : course.getName();
        String body = approved
                ? courseName + " has been approved by admin."
                : courseName + " was rejected by admin.";
        eventPublisher.publishEvent(NotificationRequestedEvent.inApp(
                recipientUserUuid,
                type,
                "INBOX",
                approved ? "Course approved" : "Course rejected",
                body,
                "/dashboard/course-management/preview/" + course.getUuid(),
                Map.of(
                        "course_uuid", course.getUuid(),
                        "course_name", courseName,
                        "reason", reason == null ? "" : reason
                ),
                "course-moderation:" + course.getUuid() + ":" + type
        ));
    }
}
