package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.common.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.common.util.GenericSpecificationBuilder;
import apps.sarafrika.elimika.course.dto.CourseDTO;
import apps.sarafrika.elimika.course.factory.CourseFactory;
import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.service.CourseService;
import apps.sarafrika.elimika.course.util.enums.ContentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final GenericSpecificationBuilder<Course> specificationBuilder;

    private static final String COURSE_NOT_FOUND_TEMPLATE = "Course with ID %s not found";

    @Override
    public CourseDTO createCourse(CourseDTO courseDTO) {
        Course course = CourseFactory.toEntity(courseDTO);
        course.setCreatedDate(LocalDateTime.now());

        // Set defaults based on CourseDTO business logic
        if (course.getStatus() == null) {
            course.setStatus(ContentStatus.DRAFT);
        }
        if (course.getActive() == null) {
            course.setActive(false); // Only published courses can be active
        }

        Course savedCourse = courseRepository.save(course);
        return CourseFactory.toDTO(savedCourse);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseDTO getCourseByUuid(UUID uuid) {
        return courseRepository.findByUuid(uuid)
                .map(CourseFactory::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(COURSE_NOT_FOUND_TEMPLATE, uuid)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseDTO> getAllCourses(Pageable pageable) {
        return courseRepository.findAll(pageable).map(CourseFactory::toDTO);
    }

    @Override
    public CourseDTO updateCourse(UUID uuid, CourseDTO courseDTO) {
        Course existingCourse = courseRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(COURSE_NOT_FOUND_TEMPLATE, uuid)));

        updateCourseFields(existingCourse, courseDTO);

        Course updatedCourse = courseRepository.save(existingCourse);
        return CourseFactory.toDTO(updatedCourse);
    }

    @Override
    public void deleteCourse(UUID uuid) {
        if (!courseRepository.existsByUuid(uuid)) {
            throw new ResourceNotFoundException(
                    String.format(COURSE_NOT_FOUND_TEMPLATE, uuid));
        }
        courseRepository.deleteByUuid(uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseDTO> search(Map<String, String> searchParams, Pageable pageable) {
        Specification<Course> spec = specificationBuilder.buildSpecification(
                Course.class, searchParams);
        return courseRepository.findAll(spec, pageable).map(CourseFactory::toDTO);
    }

    private void updateCourseFields(Course existingCourse, CourseDTO dto) {
        if (dto.name() != null) {
            existingCourse.setName(dto.name());
        }
        if (dto.instructorUuid() != null) {
            existingCourse.setInstructorUuid(dto.instructorUuid());
        }
        if (dto.categoryUuid() != null) {
            existingCourse.setCategoryUuid(dto.categoryUuid());
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
}