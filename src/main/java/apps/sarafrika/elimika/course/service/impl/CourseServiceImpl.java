package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.common.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.common.util.GenericSpecificationBuilder;
import apps.sarafrika.elimika.course.dto.CourseDTO;
import apps.sarafrika.elimika.course.factory.CourseFactory;
import apps.sarafrika.elimika.course.internal.CourseMediaValidationService;
import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.repository.CourseCategoryMappingRepository;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.service.CourseCategoryService;
import apps.sarafrika.elimika.course.service.CourseEnrollmentService;
import apps.sarafrika.elimika.course.service.CourseService;
import apps.sarafrika.elimika.course.service.LessonService;
import apps.sarafrika.elimika.course.util.enums.ContentStatus;
import apps.sarafrika.elimika.shared.storage.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

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
    private final GenericSpecificationBuilder<Course> specificationBuilder;
    private final LessonService lessonService;
    private final CourseEnrollmentService courseEnrollmentService;
    private final CourseCategoryService courseCategoryService;
    private final StorageService storageService;
    private final CourseMediaValidationService validationService;

    public static final String COURSE_THUMBNAILS_FOLDER = "course_thumbnails";
    public static final String COURSE_BANNERS_FOLDER = "course_banners";
    public static final String COURSE_INTRO_VIDEOS_FOLDER = "course_intro_videos";

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

        return CourseFactory.toDTO(course, categoryNames);
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

    @Override
    public CourseDTO updateCourse(UUID uuid, CourseDTO courseDTO) {
        log.debug("Updating course: {}", uuid);

        Course existingCourse = courseRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(COURSE_NOT_FOUND_TEMPLATE, uuid)));

        updateCourseFields(existingCourse, courseDTO);
        Course updatedCourse = courseRepository.save(existingCourse);

        // Handle category assignments if provided
        handleCategoryAssignments(uuid, courseDTO);

        // Fetch the updated course with category names for response
        return getCourseByUuid(uuid);
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

        courseRepository.deleteByUuid(uuid);
        log.info("Successfully deleted course: {}", uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseDTO> search(Map<String, String> searchParams, Pageable pageable) {
        Specification<Course> spec = specificationBuilder.buildSpecification(
                Course.class, searchParams);

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
            log.warn("Failed to calculate completion rate for course {}: {}", uuid, e.getMessage());
            return 0.0;
        }
    }

    @Override
    public CourseDTO uploadThumbnail(UUID courseUuid, MultipartFile thumbnail) {
        log.debug("Uploading thumbnail for course: {}", courseUuid);

        // Validate file
        validationService.validateThumbnail(thumbnail);

        Course course = courseRepository.findByUuid(courseUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(COURSE_NOT_FOUND_TEMPLATE, courseUuid)));

        try {
            course.setThumbnailUrl(thumbnail != null ? storeCourseImage(thumbnail, COURSE_THUMBNAILS_FOLDER) : null);
            Course savedCourse = courseRepository.save(course);
            log.info("Successfully uploaded thumbnail for course: {}", courseUuid);
            return getCourseByUuid(courseUuid);
        } catch (Exception ex) {
            log.error("Failed to upload course thumbnail for UUID: {}", courseUuid, ex);
            throw new RuntimeException("Failed to upload course thumbnail: " + ex.getMessage(), ex);
        }
    }

    @Override
    public CourseDTO uploadBanner(UUID courseUuid, MultipartFile banner) {
        log.debug("Uploading banner for course: {}", courseUuid);

        // Validate file
        validationService.validateBanner(banner);

        Course course = courseRepository.findByUuid(courseUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(COURSE_NOT_FOUND_TEMPLATE, courseUuid)));

        try {
            course.setBannerUrl(banner != null ? storeCourseImage(banner, COURSE_BANNERS_FOLDER) : null);
            Course savedCourse = courseRepository.save(course);
            log.info("Successfully uploaded banner for course: {}", courseUuid);
            return getCourseByUuid(courseUuid);
        } catch (Exception ex) {
            log.error("Failed to upload course banner for UUID: {}", courseUuid, ex);
            throw new RuntimeException("Failed to upload course banner: " + ex.getMessage(), ex);
        }
    }

    @Override
    public CourseDTO uploadIntroVideo(UUID courseUuid, MultipartFile introVideo) {
        log.debug("Uploading intro video for course: {}", courseUuid);

        // Validate file
        validationService.validateIntroVideo(introVideo);

        Course course = courseRepository.findByUuid(courseUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(COURSE_NOT_FOUND_TEMPLATE, courseUuid)));

        try {
            course.setIntroVideoUrl(introVideo != null ? storeCourseImage(introVideo, COURSE_INTRO_VIDEOS_FOLDER) : null);
            Course savedCourse = courseRepository.save(course);
            log.info("Successfully uploaded intro video for course: {}", courseUuid);
            return getCourseByUuid(courseUuid);
        } catch (Exception ex) {
            log.error("Failed to upload course intro video for UUID: {}", courseUuid, ex);
            throw new RuntimeException("Failed to upload course intro video: " + ex.getMessage(), ex);
        }
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
        if (dto.instructorUuid() != null) {
            existingCourse.setInstructorUuid(dto.instructorUuid());
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
        if (dto.ageLowerLimit() != null) {
            existingCourse.setAgeLowerLimit(dto.ageLowerLimit());
        }
        if (dto.ageUpperLimit() != null) {
            existingCourse.setAgeUpperLimit(dto.ageUpperLimit());
        }
        if (dto.thumbnailUrl() != null) {
            existingCourse.setThumbnailUrl(dto.thumbnailUrl());
        }
        if (dto.introVideoUrl() != null) {
            existingCourse.setIntroVideoUrl(dto.introVideoUrl());
        }
        if (dto.bannerUrl() != null) {
            existingCourse.setBannerUrl(dto.bannerUrl());
        }
        if (dto.status() != null) {
            existingCourse.setStatus(dto.status());
        }
        if (dto.active() != null) {
            existingCourse.setActive(dto.active());
        }
    }

    /**
     * Stores a course-related file and returns the full URL
     */
    private String storeCourseImage(MultipartFile file, String folder) {
        String fileName = storageService.store(file, folder);
        return ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/api/v1/courses/media/")
                .path(fileName)
                .build()
                .toUriString();
    }
}