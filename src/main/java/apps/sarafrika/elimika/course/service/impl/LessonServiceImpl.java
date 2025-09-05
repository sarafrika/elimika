package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import apps.sarafrika.elimika.course.dto.LessonDTO;
import apps.sarafrika.elimika.course.factory.LessonFactory;
import apps.sarafrika.elimika.course.model.Lesson;
import apps.sarafrika.elimika.course.repository.LessonRepository;
import apps.sarafrika.elimika.course.service.LessonService;
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
public class LessonServiceImpl implements LessonService {

    private final LessonRepository lessonRepository;
    private final GenericSpecificationBuilder<Lesson> specificationBuilder;

    private static final String LESSON_NOT_FOUND_TEMPLATE = "Lesson with ID %s not found";

    @Override
    public LessonDTO createLesson(LessonDTO lessonDTO) {
        Lesson lesson = LessonFactory.toEntity(lessonDTO);
        lesson.setCreatedDate(LocalDateTime.now());

        // Set defaults based on LessonDTO business logic
        if (lesson.getStatus() == null) {
            lesson.setStatus(ContentStatus.DRAFT);
        }
        if (lesson.getActive() == null) {
            lesson.setActive(false);
        }

        Lesson savedLesson = lessonRepository.save(lesson);
        return LessonFactory.toDTO(savedLesson);
    }

    @Override
    @Transactional(readOnly = true)
    public LessonDTO getLessonByUuid(UUID uuid) {
        return lessonRepository.findByUuid(uuid)
                .map(LessonFactory::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(LESSON_NOT_FOUND_TEMPLATE, uuid)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LessonDTO> getAllLessons(Pageable pageable) {
        return lessonRepository.findAll(pageable).map(LessonFactory::toDTO);
    }

    @Override
    public LessonDTO updateLesson(UUID uuid, LessonDTO lessonDTO) {
        Lesson existingLesson = lessonRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(LESSON_NOT_FOUND_TEMPLATE, uuid)));

        updateLessonFields(existingLesson, lessonDTO);

        Lesson updatedLesson = lessonRepository.save(existingLesson);
        return LessonFactory.toDTO(updatedLesson);
    }

    @Override
    public void deleteLesson(UUID uuid) {
        if (!lessonRepository.existsByUuid(uuid)) {
            throw new ResourceNotFoundException(
                    String.format(LESSON_NOT_FOUND_TEMPLATE, uuid));
        }
        lessonRepository.deleteByUuid(uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LessonDTO> search(Map<String, String> searchParams, Pageable pageable) {
        Specification<Lesson> spec = specificationBuilder.buildSpecification(
                Lesson.class, searchParams);
        return lessonRepository.findAll(spec, pageable).map(LessonFactory::toDTO);
    }

    private void updateLessonFields(Lesson existingLesson, LessonDTO dto) {
        if (dto.courseUuid() != null) {
            existingLesson.setCourseUuid(dto.courseUuid());
        }
        if (dto.lessonNumber() != null) {
            existingLesson.setLessonNumber(dto.lessonNumber());
        }
        if (dto.title() != null) {
            existingLesson.setTitle(dto.title());
        }
        if (dto.durationHours() != null) {
            existingLesson.setDurationHours(dto.durationHours());
        }
        if (dto.durationMinutes() != null) {
            existingLesson.setDurationMinutes(dto.durationMinutes());
        }
        if (dto.description() != null) {
            existingLesson.setDescription(dto.description());
        }
        if (dto.learningObjectives() != null) {
            existingLesson.setLearningObjectives(dto.learningObjectives());
        }
        if (dto.status() != null) {
            existingLesson.setStatus(dto.status());
        }
        if (dto.active() != null) {
            existingLesson.setActive(dto.active());
        }
    }
}