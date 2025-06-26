package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.common.exceptions.DuplicateResourceException;
import apps.sarafrika.elimika.common.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.common.util.GenericSpecificationBuilder;
import apps.sarafrika.elimika.course.dto.LessonDTO;
import apps.sarafrika.elimika.course.factory.LessonFactory;
import apps.sarafrika.elimika.course.model.Lesson;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.repository.LessonRepository;
import apps.sarafrika.elimika.course.service.LessonService;
import apps.sarafrika.elimika.course.util.enums.LessonType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Lesson Service Implementation
 * <p>
 * Implementation of the LessonService interface providing all business logic
 * for lesson management operations in the Sarafrika Elimika system.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since Thursday, June 26, 2025
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LessonServiceImpl implements LessonService {

    private final LessonRepository lessonRepository;
    private final CourseRepository courseRepository;
    private final GenericSpecificationBuilder<Lesson> specificationBuilder;

    @Override
    @Transactional
    public LessonDTO createLesson(LessonDTO lessonDTO) {
        log.debug("Creating new lesson: {} for course: {}", lessonDTO.lessonName(), lessonDTO.courseUuid());

        // Validate that the course exists
        if (courseRepository.findByUuid(lessonDTO.courseUuid()).isEmpty()) {
            throw new ResourceNotFoundException("Course not found for UUID: " + lessonDTO.courseUuid());
        }

        // Check for duplicate lesson number in the course
        if (existsByLessonNoAndCourse(lessonDTO.courseUuid(), lessonDTO.lessonNo())) {
            throw new DuplicateResourceException("Lesson number " + lessonDTO.lessonNo() +
                    " already exists for course: " + lessonDTO.courseUuid());
        }

        try {
            Lesson lesson = LessonFactory.toEntity(lessonDTO);
            Lesson savedLesson = lessonRepository.save(lesson);

            log.info("Successfully created lesson with UUID: {}", savedLesson.getUuid());
            return LessonFactory.toDTO(savedLesson);
        } catch (Exception e) {
            log.error("Failed to create lesson: {}", lessonDTO.lessonName(), e);
            throw new RuntimeException("Failed to create lesson: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public LessonDTO updateLesson(UUID uuid, LessonDTO lessonDTO) {
        log.debug("Updating lesson with UUID: {}", uuid);

        if (lessonDTO == null) {
            throw new IllegalArgumentException("Lesson data cannot be null");
        }

        Lesson existingLesson = findLessonOrThrow(uuid);

        // Check for lesson number duplication if it's being changed
        if (lessonDTO.lessonNo() != null &&
                !existingLesson.getLessonNo().equals(lessonDTO.lessonNo()) &&
                existsByLessonNoAndCourse(existingLesson.getCourseUuid(), lessonDTO.lessonNo())) {
            throw new DuplicateResourceException("Lesson number " + lessonDTO.lessonNo() +
                    " already exists for course: " + existingLesson.getCourseUuid());
        }

        try {
            updateLessonFields(existingLesson, lessonDTO);
            Lesson updatedLesson = lessonRepository.save(existingLesson);

            log.info("Successfully updated lesson with UUID: {}", uuid);
            return LessonFactory.toDTO(updatedLesson);
        } catch (Exception e) {
            log.error("Failed to update lesson with UUID: {}", uuid, e);
            throw new RuntimeException("Failed to update lesson: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LessonDTO> getLessonByUuid(UUID uuid) {
        log.debug("Retrieving lesson by UUID: {}", uuid);
        return lessonRepository.findByUuid(uuid)
                .map(LessonFactory::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LessonDTO> getLessonsByCourse(UUID courseUuid) {
        log.debug("Retrieving lessons for course: {}", courseUuid);

        if (courseRepository.findByUuid(courseUuid).isEmpty()) {
            throw new ResourceNotFoundException("Course not found for UUID: " + courseUuid);
        }

        return lessonRepository.findByCourseUuidOrderByLessonNoAsc(courseUuid)
                .stream()
                .map(LessonFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LessonDTO> getLessonsByCourse(UUID courseUuid, Pageable pageable) {
        log.debug("Retrieving lessons for course: {} with pagination", courseUuid);

        if (courseRepository.findByUuid(courseUuid).isEmpty()) {
            throw new ResourceNotFoundException("Course not found for UUID: " + courseUuid);
        }

        Map<String, String> searchParams = Map.of("courseUuid", courseUuid.toString());
        return search(searchParams, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LessonDTO> getLessonsByType(LessonType lessonType, Pageable pageable) {
        log.debug("Retrieving lessons by type: {}", lessonType);
        Map<String, String> searchParams = Map.of("lessonType", lessonType.toString());
        return search(searchParams, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LessonDTO> search(Map<String, String> searchParams, Pageable pageable) {
        log.debug("Searching lessons with parameters: {}", searchParams);
        Specification<Lesson> spec = specificationBuilder.buildSpecification(Lesson.class, searchParams);
        Page<Lesson> lessons = lessonRepository.findAll(spec, pageable);
        return lessons.map(LessonFactory::toDTO);
    }

    @Override
    @Transactional
    public LessonDTO updateLessonNumber(UUID uuid, Integer newLessonNo) {
        log.debug("Updating lesson number for UUID: {} to {}", uuid, newLessonNo);

        if (newLessonNo == null || newLessonNo <= 0) {
            throw new IllegalArgumentException("Lesson number must be positive");
        }

        Lesson lesson = findLessonOrThrow(uuid);

        if (existsByLessonNoAndCourse(lesson.getCourseUuid(), newLessonNo)) {
            throw new DuplicateResourceException("Lesson number " + newLessonNo +
                    " already exists for course: " + lesson.getCourseUuid());
        }

        try {
            lesson.setLessonNo(newLessonNo);
            Lesson updatedLesson = lessonRepository.save(lesson);

            log.info("Successfully updated lesson number for UUID: {}", uuid);
            return LessonFactory.toDTO(updatedLesson);
        } catch (Exception e) {
            log.error("Failed to update lesson number for UUID: {}", uuid, e);
            throw new RuntimeException("Failed to update lesson number: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void deleteLesson(UUID uuid) {
        log.debug("Deleting lesson with UUID: {}", uuid);
        try {
            Lesson lesson = findLessonOrThrow(uuid);
            lessonRepository.delete(lesson);
            log.info("Successfully deleted lesson with UUID: {}", uuid);

            // Reorder remaining lessons in the course
            reorderLessonsAfterDeletion(lesson.getCourseUuid(), lesson.getLessonNo());
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to delete lesson with UUID: {}", uuid, e);
            throw new RuntimeException("Failed to delete lesson: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void bulkDeleteLessons(List<UUID> lessonUuids) {
        log.debug("Bulk deleting {} lessons", lessonUuids.size());

        try {
            List<Lesson> lessons = lessonRepository.findAllByUuidIn(lessonUuids);

            if (lessons.size() != lessonUuids.size()) {
                List<UUID> foundUuids = lessons.stream().map(Lesson::getUuid).toList();
                List<UUID> notFoundUuids = lessonUuids.stream()
                        .filter(uuid -> !foundUuids.contains(uuid))
                        .toList();
                throw new ResourceNotFoundException("Lessons not found for UUIDs: " + notFoundUuids);
            }

            lessonRepository.deleteAll(lessons);

            // Group by course and reorder each course's lessons
            lessons.stream()
                    .collect(Collectors.groupingBy(Lesson::getCourseUuid))
                    .forEach((courseUuid, courseLessons) -> {
                        reorderAllLessonsInCourse(courseUuid);
                    });

            log.info("Successfully bulk deleted {} lessons", lessons.size());
        } catch (Exception e) {
            log.error("Failed to bulk delete lessons", e);
            throw new RuntimeException("Failed to bulk delete lessons: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUuid(UUID uuid) {
        return lessonRepository.existsByUuid(uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByLessonNoAndCourse(UUID courseUuid, Integer lessonNo) {
        return lessonRepository.existsByCourseUuidAndLessonNo(courseUuid, lessonNo);
    }

    private Lesson findLessonOrThrow(UUID uuid) {
        return lessonRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found for UUID: " + uuid));
    }

    private void updateLessonFields(Lesson lesson, LessonDTO lessonDTO) {
        if (lessonDTO.lessonNo() != null) {
            lesson.setLessonNo(lessonDTO.lessonNo());
        }
        if (lessonDTO.courseUuid() != null) {
            lesson.setCourseUuid(lessonDTO.courseUuid());
        }
        if (lessonDTO.lessonName() != null) {
            lesson.setLessonName(lessonDTO.lessonName());
        }
        if (lessonDTO.lessonDescription() != null) {
            lesson.setLessonDescription(lessonDTO.lessonDescription());
        }
        if (lessonDTO.lessonType() != null) {
            lesson.setLessonType(lessonDTO.lessonType());
        }
        if (lessonDTO.estimatedDurationMinutes() != null) {
            lesson.setEstimatedDurationMinutes(lessonDTO.estimatedDurationMinutes());
        }
    }

    private void reorderLessonsAfterDeletion(UUID courseUuid, Integer deletedLessonNo) {
        log.debug("Reordering lessons after deletion of lesson {} in course {}", deletedLessonNo, courseUuid);

        List<Lesson> lessonsToReorder = lessonRepository.findByCourseUuidAndLessonNoGreaterThanOrderByLessonNoAsc(
                courseUuid, deletedLessonNo);

        lessonsToReorder.forEach(lesson -> {
            lesson.setLessonNo(lesson.getLessonNo() - 1);
        });

        if (!lessonsToReorder.isEmpty()) {
            lessonRepository.saveAll(lessonsToReorder);
            log.info("Reordered {} lessons after deletion", lessonsToReorder.size());
        }
    }

    private void reorderAllLessonsInCourse(UUID courseUuid) {
        log.debug("Reordering all lessons in course {}", courseUuid);

        List<Lesson> lessons = lessonRepository.findByCourseUuidOrderByLessonNoAsc(courseUuid);

        for (int i = 0; i < lessons.size(); i++) {
            lessons.get(i).setLessonNo(i + 1);
        }

        if (!lessons.isEmpty()) {
            lessonRepository.saveAll(lessons);
            log.info("Reordered {} lessons in course {}", lessons.size(), courseUuid);
        }
    }
}