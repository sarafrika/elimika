package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.config.exception.LessonNotFoundException;
import apps.sarafrika.elimika.course.dto.request.CreateLessonRequestDTO;
import apps.sarafrika.elimika.course.dto.request.UpdateLessonRequestDTO;
import apps.sarafrika.elimika.course.dto.response.ClassResponseDTO;
import apps.sarafrika.elimika.course.dto.response.LessonResponseDTO;
import apps.sarafrika.elimika.course.persistence.Lesson;
import apps.sarafrika.elimika.course.persistence.LessonFactory;
import apps.sarafrika.elimika.course.persistence.LessonRepository;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponsePageableDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LessonServiceImpl implements LessonService {

    private static final String ERROR_LESSON_NOT_FOUND = "Lesson not found.";
    private static final String LESSON_FOUND_SUCCESS = "Lesson retrieved successfully.";
    private static final String LESSON_CREATED_SUCCESS = "Lesson created successfully.";
    private static final String LESSON_UPDATED_SUCCESS = "Lesson updated successfully.";

    private final ClassService classService;
    private final LessonRepository lessonRepository;

    @Override
    public ResponseDTO<LessonResponseDTO> findLesson(Long id) {

        final Lesson lesson = findLessonById(id);

        LessonResponseDTO lessonResponseDTO = LessonResponseDTO.from(lesson);

        return new ResponseDTO<>(lessonResponseDTO, HttpStatus.OK.value(), LESSON_FOUND_SUCCESS, null, LocalDateTime.now());
    }

    @Override
    public ResponsePageableDTO<LessonResponseDTO> findAllLessons(Long classId, Pageable pageable) {
        Page<LessonResponseDTO> lessonsPage = lessonRepository.findAll(pageable)
                .stream()
                .map(LessonResponseDTO::from)
                .collect(Collectors.collectingAndThen(Collectors.toList(), PageImpl::new));

        return new ResponsePageableDTO<>(lessonsPage.getContent(), lessonsPage.getNumber(), lessonsPage.getSize(),
                lessonsPage.getTotalPages(), lessonsPage.getTotalElements(), HttpStatus.OK.value(), LESSON_FOUND_SUCCESS);
    }

    @Override
    public ResponseDTO<Void> createLesson(CreateLessonRequestDTO createLessonRequestDTO) {
        ResponseDTO<ClassResponseDTO> classEntity = classService.findClass(createLessonRequestDTO.classId());

        Lesson lesson = LessonFactory.create(createLessonRequestDTO);

        lesson.setClassId(classEntity.data().id());

        lessonRepository.save(lesson);

        return new ResponseDTO<>(null, HttpStatus.CREATED.value(), LESSON_CREATED_SUCCESS, null, LocalDateTime.now());
    }

    @Override
    public ResponseDTO<Void> updateLesson(UpdateLessonRequestDTO updateLessonRequestDTO, Long id) {
        Lesson lesson = findLessonById(id);

        LessonFactory.update(lesson, updateLessonRequestDTO);

        lessonRepository.save(lesson);

        return new ResponseDTO<>(null, HttpStatus.OK.value(), LESSON_UPDATED_SUCCESS, null, LocalDateTime.now());
    }

    @Override
    public void deleteLesson(Long id) {

        Lesson lesson = findLessonById(id);

        lessonRepository.delete(lesson);
    }

    private Lesson findLessonById(final Long id) {

        return lessonRepository.findById(id).orElseThrow(() -> new LessonNotFoundException(ERROR_LESSON_NOT_FOUND));
    }

}
