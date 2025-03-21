package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.CourseDTO;
import apps.sarafrika.elimika.course.exception.CourseNotFoundException;
import apps.sarafrika.elimika.course.dto.request.UpdateCourseRequestDTO;
import apps.sarafrika.elimika.course.dto.response.CategoryResponseDTO;
import apps.sarafrika.elimika.course.dto.response.CourseLearningObjectiveResponseDTO;
import apps.sarafrika.elimika.course.dto.response.CourseResponseDTO;
import apps.sarafrika.elimika.course.dto.response.PricingResponseDTO;
import apps.sarafrika.elimika.course.event.CreateCourseEvent;
import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.mappers.CourseMapper;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.service.CourseCategoryService;
import apps.sarafrika.elimika.course.service.CourseLearningObjectiveService;
import apps.sarafrika.elimika.course.service.CourseService;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponsePageableDTO;
import apps.sarafrika.elimika.common.storage.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

    @Override
    public CourseDTO createCourse(CourseDTO courseDTO) {
        return null;
    }

    @Override
    public CourseDTO getCourseByUuid(UUID uuid) {
        return null;
    }

    @Override
    public Page<CourseDTO> getAllCourses(Pageable pageable) {
        return null;
    }

    @Override
    public CourseDTO updateCourse(UUID uuid, CourseDTO courseDTO) {
        return null;
    }

    @Override
    public void deleteCourse(UUID uuid) {

    }

    @Override
    public Page<CourseDTO> searchCourses(Map<String, String> searchParams, Pageable pageable) {
        return null;
    }
}
