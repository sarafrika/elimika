package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.LessonDTO;
import apps.sarafrika.elimika.course.repository.LessonRepository;
import apps.sarafrika.elimika.course.service.CourseService;
import apps.sarafrika.elimika.course.service.LessonContentService;
import apps.sarafrika.elimika.course.service.LessonResourceService;
import apps.sarafrika.elimika.course.service.LessonService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class LessonServiceImpl implements LessonService {

    private static final String ERROR_LESSON_NOT_FOUND = "Lesson not found.";
    private static final String LESSON_FOUND_SUCCESS = "Lesson retrieved successfully.";
    private static final String LESSON_CREATED_SUCCESS = "Lesson created successfully.";
    private static final String LESSON_UPDATED_SUCCESS = "Lesson updated successfully.";

    private final CourseService courseService;
    private final LessonRepository lessonRepository;
    private final LessonContentService lessonContentService;
    private final LessonResourceService lessonResourceService;

    @Override
    public LessonDTO createLesson(LessonDTO lessonDTO) {
        return null;
    }

    @Override
    public LessonDTO getLessonByUuid(UUID uuid) {
        return null;
    }

    @Override
    public Page<LessonDTO> getAllLessons(Pageable pageable) {
        return null;
    }

    @Override
    public LessonDTO updateLesson(UUID uuid, LessonDTO lessonDTO) {
        return null;
    }

    @Override
    public void deleteLesson(UUID uuid) {

    }

    @Override
    public Page<LessonDTO> searchLessons(Map<String, String> searchParams, Pageable pageable) {
        return null;
    }
}
