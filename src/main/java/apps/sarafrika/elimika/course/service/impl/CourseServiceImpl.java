package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.common.exceptions.DuplicateResourceException;
import apps.sarafrika.elimika.common.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.common.util.GenericSpecificationBuilder;
import apps.sarafrika.elimika.course.dto.CourseDTO;
import apps.sarafrika.elimika.course.factory.CourseFactory;
import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.service.CourseService;
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

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Course Service Implementation
 * <p>
 * Implementation of the CourseService interface providing all business logic
 * for course management operations in the Sarafrika Elimika system.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since Thursday, June 26, 2025
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final GenericSpecificationBuilder<Course> specificationBuilder;

    private final StorageService storageService;

    @Override
    @Transactional
    public CourseDTO createCourse(CourseDTO courseDTO) {
        log.debug("Creating new course with code: {}", courseDTO.courseCode());

        if (courseRepository.existsByCourseCode(courseDTO.courseCode())) {
            throw new DuplicateResourceException("Course with code '" + courseDTO.courseCode() + "' already exists");
        }

        try {
            Course course = CourseFactory.toEntity(courseDTO);
            Course savedCourse = courseRepository.save(course);

            log.info("Successfully created course with UUID: {}", savedCourse.getUuid());
            return CourseFactory.toDTO(savedCourse);
        } catch (Exception e) {
            log.error("Failed to create course with code: {}", courseDTO.courseCode(), e);
            throw new RuntimeException("Failed to create course: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public CourseDTO updateCourse(UUID uuid, CourseDTO courseDTO) {
        log.debug("Updating course with UUID: {}", uuid);

        if (courseDTO == null) {
            throw new IllegalArgumentException("Course data cannot be null");
        }

        Course existingCourse = findCourseOrThrow(uuid);

        // Check for course code duplication if it's being changed
        if (courseDTO.courseCode() != null &&
                !existingCourse.getCourseCode().equals(courseDTO.courseCode()) &&
                courseRepository.existsByCourseCode(courseDTO.courseCode())) {
            throw new DuplicateResourceException("Course with code '" + courseDTO.courseCode() + "' already exists");
        }

        try {
            updateCourseFields(existingCourse, courseDTO);
            Course updatedCourse = courseRepository.save(existingCourse);

            log.info("Successfully updated course with UUID: {}", uuid);
            return CourseFactory.toDTO(updatedCourse);
        } catch (Exception e) {
            log.error("Failed to update course with UUID: {}", uuid, e);
            throw new RuntimeException("Failed to update course: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CourseDTO> getCourseByUuid(UUID uuid) {
        log.debug("Retrieving course by UUID: {}", uuid);
        return courseRepository.findByUuid(uuid)
                .map(CourseFactory::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseDTO> getAllCourses(Pageable pageable) {
        log.debug("Retrieving all courses with pagination");
        return courseRepository.findAll(pageable)
                .map(CourseFactory::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseDTO> search(Map<String, String> searchParams, Pageable pageable) {
        log.debug("Searching courses with parameters: {}", searchParams);
        Specification<Course> spec = specificationBuilder.buildSpecification(Course.class, searchParams);
        Page<Course> courses = courseRepository.findAll(spec, pageable);
        return courses.map(CourseFactory::toDTO);
    }

    @Override
    @Transactional
    public void deleteCourse(UUID uuid) {
        log.debug("Deleting course with UUID: {}", uuid);
        try {
            Course course = findCourseOrThrow(uuid);
            courseRepository.delete(course);
            log.info("Successfully deleted course with UUID: {}", uuid);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to delete course with UUID: {}", uuid, e);
            throw new RuntimeException("Failed to delete course: " + e.getMessage(), e);
        }
    }

    @Override
    public CourseDTO uploadCourseThumbnail(UUID courseUuid, MultipartFile thumbnailImage) {
        log.debug("Uploading thumbnail for course: {}", courseUuid);

        Course course = findCourseOrThrow(courseUuid);

        try {
            course.setCourseThumbnail(thumbnailImage != null ? storeCourseThumbnail(thumbnailImage) : null);
            Course updatedCourse = courseRepository.save(course);

            log.info("Successfully uploaded thumbnail for course: {}", courseUuid);
            return CourseFactory.toDTO(updatedCourse);
        } catch (Exception ex) {
            log.error("Failed to upload course thumbnail for UUID: {}", courseUuid, ex);
            throw new RuntimeException("Failed to upload course thumbnail: " + ex.getMessage(), ex);
        }
    }

    private String storeCourseThumbnail(MultipartFile file) {
        String fileName = storageService.store(file);
        // Build the URL to access the course thumbnail through your endpoint
        return ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/api/v1/courses/thumbnail/")
                .path(fileName)
                .build()
                .toUriString();
    }

    private Course findCourseOrThrow(UUID uuid) {
        return courseRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found for UUID: " + uuid));
    }

    private void updateCourseFields(Course course, CourseDTO courseDTO) {
        if (courseDTO.courseCode() != null) {
            course.setCourseCode(courseDTO.courseCode());
        }
        if (courseDTO.courseName() != null) {
            course.setCourseName(courseDTO.courseName());
        }
        if (courseDTO.courseDescription() != null) {
            course.setCourseDescription(courseDTO.courseDescription());
        }
        if (courseDTO.initialPrice() != null) {
            course.setInitialPrice(courseDTO.initialPrice());
        }
        if (courseDTO.currentPrice() != null) {
            course.setCurrentPrice(courseDTO.currentPrice());
        }
        if (courseDTO.accessStartDate() != null) {
            course.setAccessStartDate(courseDTO.accessStartDate());
        }
        if (courseDTO.classLimit() != null) {
            course.setClassLimit(courseDTO.classLimit());
        }
        if (courseDTO.ageUpperLimit() != null) {
            course.setAgeUpperLimit(courseDTO.ageUpperLimit());
        }
        if (courseDTO.ageLowerLimit() != null) {
            course.setAgeLowerLimit(courseDTO.ageLowerLimit());
        }
        if (courseDTO.difficulty() != null) {
            course.setDifficulty(courseDTO.difficulty());
        }
        if (courseDTO.courseObjectives() != null) {
            course.setCourseObjectives(courseDTO.courseObjectives());
        }
        if (courseDTO.courseStatus() != null) {
            course.setCourseStatus(courseDTO.courseStatus());
        }
    }
}