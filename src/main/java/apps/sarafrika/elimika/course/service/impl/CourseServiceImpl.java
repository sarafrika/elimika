package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.common.exceptions.CourseNotFoundException;
import apps.sarafrika.elimika.common.storage.service.StorageService;
import apps.sarafrika.elimika.common.util.GenericSpecificationBuilder;
import apps.sarafrika.elimika.course.dto.CourseDTO;
import apps.sarafrika.elimika.course.event.CourseCreatedEvent;
import apps.sarafrika.elimika.course.event.CourseDeletedEvent;
import apps.sarafrika.elimika.course.event.CourseUpdatedEvent;
import apps.sarafrika.elimika.course.mappers.CourseMapper;
import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.service.CourseCategoryService;
import apps.sarafrika.elimika.course.service.CourseLearningObjectiveService;
import apps.sarafrika.elimika.course.service.CourseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Implementation of the CourseService interface that provides
 * business logic for managing courses.
 */
@Slf4j
@Service
@RequiredArgsConstructor
class CourseServiceImpl implements CourseService {

    private static final String ERROR_COURSE_NOT_FOUND = "Course not found.";
    private static final String COURSE_EXISTS = "Course with the same name or code already exists.";

    private final StorageService storageService;
    private final CourseRepository courseRepository;
    private final CourseCategoryService courseCategoryService;
    private final CourseLearningObjectiveService courseLearningObjectiveService;
    private final GenericSpecificationBuilder<Course> specificationBuilder;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public CourseDTO createCourse(CourseDTO courseDTO) {
        log.info("Creating new course with name: {}", courseDTO.name());

        // Check if a course with the same name already exists
        if (courseRepository.existsByNameIgnoreCase(courseDTO.name())) {
            log.error("Course with name '{}' already exists", courseDTO.name());
            throw new DataIntegrityViolationException(COURSE_EXISTS);
        }

        // Generate a unique course code if not provided
        String courseCode = generateCourseCode(courseDTO.name());

        // Convert DTO to entity
        Course course = CourseMapper.toEntity(courseDTO);
        course.setCode(courseCode);

        // Save the course
        Course savedCourse = courseRepository.save(course);

        // Store thumbnail if provided
        if (!courseDTO.thumbnailUrl().isBlank()) {
            // Implement thumbnail storage logic if needed
        }

        // Publish course created event
        eventPublisher.publishEvent(new CourseCreatedEvent(this, savedCourse.getUuid()));

        return getCourseByUuid(savedCourse.getUuid());
    }

    @Override
    public CourseDTO getCourseByUuid(UUID uuid) {
        log.info("Fetching course with UUID: {}", uuid);

        Course course = courseRepository.findByUuid(uuid)
                .orElseThrow(() -> {
                    log.error("Course with UUID '{}' not found", uuid);
                    return new CourseNotFoundException(ERROR_COURSE_NOT_FOUND);
                });

        // Map to DTO with associations
        CourseDTO courseDTO = CourseMapper.toDto(course);

        // Load course categories
        courseDTO = loadCourseCategories(courseDTO);

        // Load learning objectives
        courseDTO = loadLearningObjectives(courseDTO);

        return courseDTO;
    }

    @Override
    public Page<CourseDTO> getAllCourses(Pageable pageable) {
        log.info("Fetching all courses with pagination");

        Page<Course> courses = courseRepository.findAll(pageable);
        return courses.map(course -> {
            CourseDTO dto = CourseMapper.toDto(course);
            // Load associations for each course
            dto = loadCourseCategories(dto);
            dto = loadLearningObjectives(dto);
            return dto;
        });
    }

    @Override
    public CourseDTO updateCourse(UUID uuid, CourseDTO courseDTO) {
        log.info("Updating course with UUID: {}", uuid);

        Course existingCourse = courseRepository.findByUuid(uuid)
                .orElseThrow(() -> new CourseNotFoundException(ERROR_COURSE_NOT_FOUND));

        // Check if name is being changed and if it would conflict with an existing course
        if (!existingCourse.getName().equalsIgnoreCase(courseDTO.name()) &&
                courseRepository.existsByNameIgnoreCase(courseDTO.name())) {
            log.error("Cannot update course. A course with name '{}' already exists", courseDTO.name());
            throw new DataIntegrityViolationException(COURSE_EXISTS);
        }

        // Update basic fields
        updateCourseFields(existingCourse, courseDTO);

        // Save the updated course
        Course updatedCourse = courseRepository.save(existingCourse);

        // Update thumbnail if changed
        if (!courseDTO.thumbnailUrl().equals(existingCourse.getThumbnailUrl())) {
            // Handle thumbnail update if needed
        }

        // Publish course updated event
        eventPublisher.publishEvent(new CourseUpdatedEvent(this, updatedCourse.getUuid()));

        return getCourseByUuid(updatedCourse.getUuid());
    }

    @Override
    public void deleteCourse(UUID uuid) {
        log.info("Deleting course with UUID: {}", uuid);

        Course course = courseRepository.findByUuid(uuid)
                .orElseThrow(() -> new CourseNotFoundException(ERROR_COURSE_NOT_FOUND));

        // Delete associated categories
        courseCategoryService.deleteCourseCategory(uuid);

        // Delete associated learning objectives
        courseLearningObjectiveService.deleteCourseLearningObjective(uuid);

        // Delete the course
        courseRepository.delete(course);

        // Delete thumbnail if exists
        if (course.getThumbnailUrl() != null && !course.getThumbnailUrl().isBlank()) {
            // Handle thumbnail deletion if needed
        }

        // Publish course deleted event
        eventPublisher.publishEvent(new CourseDeletedEvent(this, uuid));
    }

    @Override
    public Page<CourseDTO> searchCourses(Map<String, String> searchParams, Pageable pageable) {
        log.info("Searching courses with parameters: {}", searchParams);

        Specification<Course> specification = specificationBuilder.buildSpecification(Course.class, searchParams);
        Page<Course> courses = courseRepository.findAll(specification, pageable);

        return courses.map(course -> {
            CourseDTO dto = CourseMapper.toDto(course);
            // Load associations for each course
            dto = loadCourseCategories(dto);
            dto = loadLearningObjectives(dto);
            return dto;
        });
    }

    // Helper methods

    /**
     * Generates a unique course code based on the course name.
     *
     * @param courseName the name of the course
     * @return a generated course code
     */
    private String generateCourseCode(String courseName) {
        // Generate a unique code based on course name and a random number
        String baseCode = courseName.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        if (baseCode.length() > 5) {
            baseCode = baseCode.substring(0, 5);
        }
        return String.format("%s-%s", baseCode, UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
    /**
     * Loads course categories for the given course DTO.
     *
     * @param courseDTO the course DTO to load categories for
     * @return the updated course DTO with categories loaded
     */
    private CourseDTO loadCourseCategories(CourseDTO courseDTO) {
        return new CourseDTO(
                courseDTO.uuid(),
                courseDTO.name(),
                courseDTO.code(),
                courseDTO.description(),
                courseDTO.thumbnailUrl(),
                courseDTO.durationHours(),
                courseDTO.difficultyLevel(),
                courseDTO.isFree(),
                courseDTO.originalPrice(),
                courseDTO.salePrice(),
                courseDTO.minAge(),
                courseDTO.maxAge(),
                courseDTO.classSize(),
                courseDTO.instructors(),
                courseDTO.learningObjectives(),
                courseCategoryService.getCourseCategoryByUuid(courseDTO.uuid()),
                courseDTO.createdBy(),
                courseDTO.lastModifiedBy(),
                courseDTO.createdDate(),
                courseDTO.lastModifiedDate()
        );
    }

    /**
     * Loads learning objectives for the given course DTO.
     *
     * @param courseDTO the course DTO to load learning objectives for
     * @return the updated course DTO with learning objectives loaded
     */
    private CourseDTO loadLearningObjectives(CourseDTO courseDTO) {
        return new CourseDTO(
                courseDTO.uuid(),
                courseDTO.name(),
                courseDTO.code(),
                courseDTO.description(),
                courseDTO.thumbnailUrl(),
                courseDTO.durationHours(),
                courseDTO.difficultyLevel(),
                courseDTO.isFree(),
                courseDTO.originalPrice(),
                courseDTO.salePrice(),
                courseDTO.minAge(),
                courseDTO.maxAge(),
                courseDTO.classSize(),
                courseDTO.instructors(),
                courseLearningObjectiveService.getLearningObjectivesByCourseUuid(courseDTO.uuid()),
                courseDTO.courseCategories(),
                courseDTO.createdBy(),
                courseDTO.lastModifiedBy(),
                courseDTO.createdDate(),
                courseDTO.lastModifiedDate()
        );
    }

    /**
     * Updates the fields of a course entity from a DTO.
     *
     * @param course the course entity to update
     * @param courseDTO the DTO containing the new values
     */
    private void updateCourseFields(Course course, CourseDTO courseDTO) {
        course.setName(courseDTO.name());
        course.setDescription(courseDTO.description());
        course.setThumbnailUrl(courseDTO.thumbnailUrl());
        course.setDurationHours(courseDTO.durationHours());
        course.setDifficultyLevel(courseDTO.difficultyLevel());
        course.setFree(courseDTO.isFree());
        course.setOriginalPrice(courseDTO.originalPrice());
        course.setSalePrice(courseDTO.salePrice());
        course.setMinAge(courseDTO.minAge());
        course.setMaxAge(courseDTO.maxAge());
        course.setClassSize(courseDTO.classSize());
    }
}