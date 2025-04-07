package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.config.exception.CourseNotFoundException;
import apps.sarafrika.elimika.course.dto.request.CourseRequestDTO;
import apps.sarafrika.elimika.course.dto.request.CreateCourseRequestDTO;
import apps.sarafrika.elimika.course.dto.request.UpdateCourseRequestDTO;
import apps.sarafrika.elimika.course.dto.response.CategoryResponseDTO;
import apps.sarafrika.elimika.course.dto.response.CourseLearningObjectiveResponseDTO;
import apps.sarafrika.elimika.course.dto.response.CourseResponseDTO;
import apps.sarafrika.elimika.course.dto.response.PricingResponseDTO;
import apps.sarafrika.elimika.course.event.CreateCourseEvent;
import apps.sarafrika.elimika.course.persistence.Course;
import apps.sarafrika.elimika.course.persistence.CourseFactory;
import apps.sarafrika.elimika.course.persistence.CourseRepository;
import apps.sarafrika.elimika.course.persistence.CourseSpecification;
import apps.sarafrika.elimika.course.service.CourseCategoryService;
import apps.sarafrika.elimika.course.service.CourseLearningObjectiveService;
import apps.sarafrika.elimika.course.service.CourseService;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponsePageableDTO;
import apps.sarafrika.elimika.shared.storage.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
class CourseServiceImpl implements CourseService {

    private static final String ERROR_COURSE_NOT_FOUND = "Course not found.";
    private static final String COURSE_FOUND_SUCCESS = "Course retrieved successfully.";
    private static final String COURSE_CREATED_SUCCESS = "Course has been persisted successfully.";
    private static final String COURSES_FOUND_SUCCESS = "Courses retrieved successfully.";
    private static final String COURSE_UPDATED_SUCCESS = "Course has been updated successfully.";

    private final StorageService storageService;
    private final CourseRepository courseRepository;
    private final CourseCategoryService courseCategoryService;
    private final CourseLearningObjectiveService courseLearningObjectiveService;

    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    @Override
    public ResponseDTO<CourseResponseDTO> createCourse(CreateCourseRequestDTO createCourseRequestDTO, MultipartFile thumbnail) {

        final Course course = CourseFactory.create(createCourseRequestDTO);
        course.setThumbnailUrl(storeCourseThumbnail(thumbnail));

        eventPublisher.publishEvent(new CreateCourseEvent(course, createCourseRequestDTO));

        Course savedCourse = courseRepository.save(course);

        PricingResponseDTO pricing = PricingResponseDTO.from(savedCourse);

        List<CourseLearningObjectiveResponseDTO> learningObjectives = courseLearningObjectiveService.createCourseLearningObjectives(createCourseRequestDTO.learningObjectives(), savedCourse.getId()).data();

        List<CategoryResponseDTO> courseCategories = courseCategoryService.updateCourseCategories(savedCourse.getId(), createCourseRequestDTO.categories()).data();

        return new ResponseDTO<>(CourseResponseDTO.from(savedCourse, pricing, learningObjectives, courseCategories), HttpStatus.CREATED.value(), COURSE_CREATED_SUCCESS, null, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    @Override
    public ResponseDTO<CourseResponseDTO> findCourse(Long courseId) {

        final Course course = findCourseById(courseId);

        List<CourseLearningObjectiveResponseDTO> learningObjectives = courseLearningObjectiveService.findAllCourseLearningObjectives(course.getId()).data();

        PricingResponseDTO pricing = PricingResponseDTO.from(course);

        List<CategoryResponseDTO> courseCategories = courseCategoryService.findCourseCategoriesByCourseId(course.getId()).data();

        CourseResponseDTO courseResponseDTO = CourseResponseDTO.from(course, pricing, learningObjectives, courseCategories);

        return new ResponseDTO<>(courseResponseDTO, HttpStatus.OK.value(), COURSE_FOUND_SUCCESS, null, LocalDateTime.now());
    }

    private Course findCourseById(Long id) {

        return courseRepository.findById(id).orElseThrow(() -> new CourseNotFoundException(ERROR_COURSE_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    @Override
    public ResponsePageableDTO<CourseResponseDTO> findAllCourses(CourseRequestDTO courseRequestDTO, Pageable pageable) {

        Page<CourseResponseDTO> coursesPage = courseRepository.findAll(new CourseSpecification(courseRequestDTO), pageable)
                .stream()
                .map((Course course) -> {
                    List<CourseLearningObjectiveResponseDTO> learningObjectives = courseLearningObjectiveService.findAllCourseLearningObjectives(course.getId()).data();

                    PricingResponseDTO pricing = PricingResponseDTO.from(course);

                    List<CategoryResponseDTO> courseCategories = courseCategoryService.findCourseCategoriesByCourseId(course.getId()).data();

                    return CourseResponseDTO.from(course, pricing, learningObjectives, courseCategories);
                })
                .collect(Collectors.collectingAndThen(Collectors.toList(), PageImpl::new));

        return new ResponsePageableDTO<>(coursesPage.getContent(), coursesPage.getNumber(), coursesPage.getSize(),
                coursesPage.getTotalPages(), coursesPage.getTotalElements(), HttpStatus.OK.value(), COURSES_FOUND_SUCCESS);

    }


    @Transactional
    @Override
    public ResponseDTO<CourseResponseDTO> updateCourse(UpdateCourseRequestDTO updateCourseRequestDTO, Long courseId) {

        final Course course = findCourseById(courseId);

        CourseFactory.update(course, updateCourseRequestDTO);

        List<CourseLearningObjectiveResponseDTO> learningObjectives = courseLearningObjectiveService.updateCourseLearningObjectives(updateCourseRequestDTO.learningObjectives()).data();

        List<CategoryResponseDTO> courseCategories = courseCategoryService.updateCourseCategories(course.getId(), updateCourseRequestDTO.categories()).data();

        Course updatedCourse = courseRepository.save(course);

        PricingResponseDTO pricing = PricingResponseDTO.from(updatedCourse);

        return new ResponseDTO<>(CourseResponseDTO.from(updatedCourse, pricing, learningObjectives, courseCategories), HttpStatus.OK.value(), COURSE_UPDATED_SUCCESS, null, LocalDateTime.now());
    }


    @Transactional
    @Override
    public void deleteCourse(Long courseId) {

        final Course course = findCourseById(courseId);

        courseRepository.delete(course);
    }

    private String storeCourseThumbnail(MultipartFile file) {
        String fileName = storageService.store(file);
        // Build the URL to access the profile image through your endpoint
        return ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/api/v1/users/profile-image/")
                .path(fileName)
                .build()
                .toUriString();

    }
}
