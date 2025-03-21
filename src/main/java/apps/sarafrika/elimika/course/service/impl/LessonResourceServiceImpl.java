package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.LessonResourceDTO;
import apps.sarafrika.elimika.course.repository.LessonResourceRepository;
import apps.sarafrika.elimika.course.service.LessonResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class LessonResourceServiceImpl implements LessonResourceService {

    private static final String ERROR_LESSON_RESOURCE_NOT_FOUND = "Lesson resource not found.";
    private static final String LESSON_RESOURCE_FOUND_SUCCESS = "Lesson resource retrieved successfully.";
    private static final String LESSON_RESOURCE_CREATED_SUCCESS = "Lesson resource persisted successfully.";
    private static final String LESSON_RESOURCE_UPDATED_SUCCESS = "Lesson resource updated successfully.";

    private final LessonResourceRepository lessonResourceRepository;

    @Override
    public LessonResourceDTO createLessonResource(LessonResourceDTO lessonResourceDTO) {
        return null;
    }

    @Override
    public LessonResourceDTO getLessonResourceByUuid(UUID uuid) {
        return null;
    }

    @Override
    public Page<LessonResourceDTO> getAllLessonResources(Pageable pageable) {
        return null;
    }

    @Override
    public LessonResourceDTO updateLessonResource(UUID uuid, LessonResourceDTO lessonResourceDTO) {
        return null;
    }

    @Override
    public void deleteLessonResource(UUID uuid) {

    }

    @Override
    public Page<LessonResourceDTO> searchLessonResources(Map<String, String> searchParams, Pageable pageable) {
        return null;
    }
}
