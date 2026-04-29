package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.LessonPracticeActivityDTO;
import apps.sarafrika.elimika.course.factory.LessonPracticeActivityFactory;
import apps.sarafrika.elimika.course.model.Lesson;
import apps.sarafrika.elimika.course.model.LessonPracticeActivity;
import apps.sarafrika.elimika.course.repository.LessonPracticeActivityRepository;
import apps.sarafrika.elimika.course.repository.LessonRepository;
import apps.sarafrika.elimika.course.service.LessonPracticeActivityService;
import apps.sarafrika.elimika.course.util.enums.ContentStatus;
import apps.sarafrika.elimika.course.util.enums.PracticeActivityGrouping;
import apps.sarafrika.elimika.course.util.enums.PracticeActivityType;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class LessonPracticeActivityServiceImpl implements LessonPracticeActivityService {

    private static final String LESSON_NOT_FOUND_TEMPLATE = "Lesson with ID %s not found";
    private static final String ACTIVITY_NOT_FOUND_TEMPLATE = "Practice activity with ID %s not found";

    private final LessonRepository lessonRepository;
    private final LessonPracticeActivityRepository practiceActivityRepository;

    @Override
    public LessonPracticeActivityDTO createPracticeActivity(UUID courseUuid, UUID lessonUuid, LessonPracticeActivityDTO activityDTO) {
        ensureLessonBelongsToCourse(courseUuid, lessonUuid);
        validateRequestLesson(activityDTO.lessonUuid(), lessonUuid);

        LessonPracticeActivity activity = LessonPracticeActivityFactory.toEntity(activityDTO);
        activity.setLessonUuid(lessonUuid);
        applyCreateDefaults(activity);
        validatePublicationState(activity.getStatus(), activity.getActive());

        LessonPracticeActivity savedActivity = practiceActivityRepository.save(activity);
        return LessonPracticeActivityFactory.toDTO(savedActivity);
    }

    @Override
    @Transactional(readOnly = true)
    public LessonPracticeActivityDTO getPracticeActivity(UUID courseUuid, UUID lessonUuid, UUID activityUuid) {
        ensureLessonBelongsToCourse(courseUuid, lessonUuid);
        return practiceActivityRepository.findByUuidAndLessonUuid(activityUuid, lessonUuid)
                .map(LessonPracticeActivityFactory::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ACTIVITY_NOT_FOUND_TEMPLATE, activityUuid)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LessonPracticeActivityDTO> getPracticeActivitiesByLesson(UUID courseUuid, UUID lessonUuid, Pageable pageable) {
        ensureLessonBelongsToCourse(courseUuid, lessonUuid);
        return practiceActivityRepository.findByLessonUuid(lessonUuid, withDefaultSort(pageable))
                .map(LessonPracticeActivityFactory::toDTO);
    }

    @Override
    public LessonPracticeActivityDTO updatePracticeActivity(UUID courseUuid,
                                                           UUID lessonUuid,
                                                           UUID activityUuid,
                                                           LessonPracticeActivityDTO activityDTO) {
        ensureLessonBelongsToCourse(courseUuid, lessonUuid);
        validateRequestLesson(activityDTO.lessonUuid(), lessonUuid);

        LessonPracticeActivity existingActivity = practiceActivityRepository.findByUuidAndLessonUuid(activityUuid, lessonUuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ACTIVITY_NOT_FOUND_TEMPLATE, activityUuid)));

        updatePracticeActivityFields(existingActivity, activityDTO);
        validatePublicationState(existingActivity.getStatus(), existingActivity.getActive());

        LessonPracticeActivity updatedActivity = practiceActivityRepository.save(existingActivity);
        return LessonPracticeActivityFactory.toDTO(updatedActivity);
    }

    @Override
    public void deletePracticeActivity(UUID courseUuid, UUID lessonUuid, UUID activityUuid) {
        ensureLessonBelongsToCourse(courseUuid, lessonUuid);
        LessonPracticeActivity existingActivity = practiceActivityRepository.findByUuidAndLessonUuid(activityUuid, lessonUuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ACTIVITY_NOT_FOUND_TEMPLATE, activityUuid)));

        practiceActivityRepository.delete(existingActivity);
    }

    @Override
    public void reorderPracticeActivities(UUID courseUuid, UUID lessonUuid, List<UUID> activityUuids) {
        ensureLessonBelongsToCourse(courseUuid, lessonUuid);
        if (activityUuids == null) {
            throw new ValidationException("Practice activity UUIDs are required");
        }

        List<LessonPracticeActivity> existingActivities =
                practiceActivityRepository.findByLessonUuidOrderByDisplayOrderAsc(lessonUuid);
        validateFullReorderList(activityUuids, existingActivities);

        for (int i = 0; i < activityUuids.size(); i++) {
            UUID activityUuid = activityUuids.get(i);
            LessonPracticeActivity activity = existingActivities.stream()
                    .filter(candidate -> candidate.getUuid().equals(activityUuid))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException(String.format(ACTIVITY_NOT_FOUND_TEMPLATE, activityUuid)));
            activity.setDisplayOrder(i + 1);
        }

        practiceActivityRepository.saveAll(existingActivities);
    }

    private Lesson ensureLessonBelongsToCourse(UUID courseUuid, UUID lessonUuid) {
        Lesson lesson = lessonRepository.findByUuid(lessonUuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(LESSON_NOT_FOUND_TEMPLATE, lessonUuid)));
        if (!Objects.equals(lesson.getCourseUuid(), courseUuid)) {
            throw new ResourceNotFoundException(
                    String.format("Lesson %s does not belong to course %s", lessonUuid, courseUuid));
        }
        return lesson;
    }

    private void validateRequestLesson(UUID requestLessonUuid, UUID pathLessonUuid) {
        if (requestLessonUuid != null && !requestLessonUuid.equals(pathLessonUuid)) {
            throw new ValidationException("Request lesson_uuid must match the lesson UUID in the path");
        }
    }

    private void applyCreateDefaults(LessonPracticeActivity activity) {
        if (activity.getActivityType() == null) {
            activity.setActivityType(PracticeActivityType.EXERCISE);
        }
        if (activity.getGrouping() == null) {
            activity.setGrouping(PracticeActivityGrouping.INDIVIDUAL);
        }
        if (activity.getStatus() == null) {
            activity.setStatus(ContentStatus.DRAFT);
        }
        if (activity.getActive() == null) {
            activity.setActive(false);
        }
        if (activity.getDisplayOrder() == null) {
            activity.setDisplayOrder(practiceActivityRepository.findMaxDisplayOrderByLessonUuid(activity.getLessonUuid()) + 1);
        }
    }

    private void updatePracticeActivityFields(LessonPracticeActivity existingActivity, LessonPracticeActivityDTO dto) {
        if (dto.title() != null) {
            existingActivity.setTitle(dto.title());
        }
        if (dto.instructions() != null) {
            existingActivity.setInstructions(dto.instructions());
        }
        if (dto.activityType() != null) {
            existingActivity.setActivityType(dto.activityType());
        }
        if (dto.grouping() != null) {
            existingActivity.setGrouping(dto.grouping());
        }
        if (dto.estimatedMinutes() != null) {
            existingActivity.setEstimatedMinutes(dto.estimatedMinutes());
        }
        if (dto.materials() != null) {
            existingActivity.setMaterials(dto.materials());
        }
        if (dto.expectedOutput() != null) {
            existingActivity.setExpectedOutput(dto.expectedOutput());
        }
        if (dto.displayOrder() != null) {
            existingActivity.setDisplayOrder(dto.displayOrder());
        }
        if (dto.status() != null) {
            existingActivity.setStatus(dto.status());
        }
        if (dto.active() != null) {
            existingActivity.setActive(dto.active());
        }
    }

    private void validatePublicationState(ContentStatus status, Boolean active) {
        if (Boolean.TRUE.equals(active) && status != ContentStatus.PUBLISHED) {
            throw new ValidationException("Active practice activities must be published");
        }
    }

    private void validateFullReorderList(List<UUID> requestedUuids, List<LessonPracticeActivity> existingActivities) {
        Set<UUID> requestedSet = new HashSet<>(requestedUuids);
        if (requestedSet.size() != requestedUuids.size()) {
            throw new ValidationException("Practice activity reorder list cannot contain duplicate UUIDs");
        }

        Set<UUID> existingSet = existingActivities.stream()
                .map(LessonPracticeActivity::getUuid)
                .collect(java.util.stream.Collectors.toSet());

        if (requestedSet.size() != existingSet.size() || !requestedSet.equals(existingSet)) {
            throw new ValidationException("Practice activity reorder list must include every activity for the lesson exactly once");
        }
    }

    private Pageable withDefaultSort(Pageable pageable) {
        Pageable requestedPageable = pageable == null ? Pageable.unpaged() : pageable;
        if (requestedPageable.isUnpaged() || requestedPageable.getSort().isSorted()) {
            return requestedPageable;
        }
        return PageRequest.of(
                requestedPageable.getPageNumber(),
                requestedPageable.getPageSize(),
                Sort.by(Sort.Direction.ASC, "displayOrder")
        );
    }
}
