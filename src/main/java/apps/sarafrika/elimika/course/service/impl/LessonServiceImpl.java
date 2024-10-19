package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.config.exception.LessonNotFoundException;
import apps.sarafrika.elimika.course.dto.request.CreateLessonRequestDTO;
import apps.sarafrika.elimika.course.dto.request.UpdateLessonRequestDTO;
import apps.sarafrika.elimika.course.dto.response.CourseResponseDTO;
import apps.sarafrika.elimika.course.dto.response.LessonResponseDTO;
import apps.sarafrika.elimika.course.persistence.Lesson;
import apps.sarafrika.elimika.course.persistence.LessonFactory;
import apps.sarafrika.elimika.course.persistence.LessonRepository;
import apps.sarafrika.elimika.course.service.CourseService;
import apps.sarafrika.elimika.course.service.LessonService;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponsePageableDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
class LessonServiceImpl implements LessonService {

    private static final String ERROR_LESSON_NOT_FOUND = "Lesson not found.";
    private static final String LESSON_FOUND_SUCCESS = "Lesson retrieved successfully.";
    private static final String LESSON_CREATED_SUCCESS = "Lesson created successfully.";
    private static final String LESSON_UPDATED_SUCCESS = "Lesson updated successfully.";

    private final CourseService courseService;
    private final LessonRepository lessonRepository;

    @Transactional(readOnly = true)
    @Override
    public ResponseDTO<LessonResponseDTO> findLesson(Long courseId, Long lessonId) {

        final Lesson lesson = findLessonByCourseIdAndLessonId(courseId, lessonId);

        LessonResponseDTO lessonResponseDTO = LessonResponseDTO.from(lesson);

        return new ResponseDTO<>(lessonResponseDTO, HttpStatus.OK.value(), LESSON_FOUND_SUCCESS, null, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    @Override
    public ResponsePageableDTO<LessonResponseDTO> findAllLessons(Long courseId, Pageable pageable) {

        Page<LessonResponseDTO> lessonsPage = lessonRepository.findAllByCourseId(courseId, pageable)
                .stream()
                .map(LessonResponseDTO::from)
                .collect(Collectors.collectingAndThen(Collectors.toList(), PageImpl::new));

        return new ResponsePageableDTO<>(lessonsPage.getContent(), lessonsPage.getNumber(), lessonsPage.getSize(),
                lessonsPage.getTotalPages(), lessonsPage.getTotalElements(), HttpStatus.OK.value(), LESSON_FOUND_SUCCESS);
    }

    @Transactional
    @Override
    public ResponseDTO<Void> createLesson(Long courseId, CreateLessonRequestDTO createLessonRequestDTO) {

        ResponseDTO<CourseResponseDTO> course = courseService.findCourse(courseId);

        Lesson lesson = LessonFactory.create(createLessonRequestDTO);

        lesson.setCourseId(course.data().id());

        lessonRepository.save(lesson);

        return new ResponseDTO<>(null, HttpStatus.CREATED.value(), LESSON_CREATED_SUCCESS, null, LocalDateTime.now());
    }

    @Transactional
    @Override
    public ResponseDTO<Void> updateLesson(Long courseId, UpdateLessonRequestDTO updateLessonRequestDTO, Long lessonId) {

        Lesson lesson = findLessonByCourseIdAndLessonId(courseId, lessonId);

        LessonFactory.update(lesson, updateLessonRequestDTO);

        lessonRepository.save(lesson);

        return new ResponseDTO<>(null, HttpStatus.OK.value(), LESSON_UPDATED_SUCCESS, null, LocalDateTime.now());
    }

    @Transactional
    @Override
    public void deleteLesson(Long courseId, Long lessonId) {

        final Lesson lesson = findLessonByCourseIdAndLessonId(courseId, lessonId);

        lessonRepository.delete(lesson);
    }

    @Transactional(readOnly = true)
    @Override
    public ResponseDTO<LessonResponseDTO> findLessonById(Long lessonId) {

        Lesson lesson = findById(lessonId);

        LessonResponseDTO lessonResponseDTO = LessonResponseDTO.from(lesson);

        return new ResponseDTO<>(lessonResponseDTO, HttpStatus.OK.value(), LESSON_FOUND_SUCCESS, null, LocalDateTime.now());
    }

    private Lesson findById(Long lessonId) {

        return lessonRepository.findById(lessonId).orElseThrow(() -> new LessonNotFoundException(ERROR_LESSON_NOT_FOUND));
    }

    private Lesson findLessonByCourseIdAndLessonId(final Long courseId, final Long lessonId) {

        return lessonRepository.findByCourseIdAndLessonId(courseId, lessonId).orElseThrow(() -> new LessonNotFoundException(ERROR_LESSON_NOT_FOUND));
    }

}
