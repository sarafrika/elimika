package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.common.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.common.util.GenericSpecificationBuilder;
import apps.sarafrika.elimika.course.dto.LessonProgressDTO;
import apps.sarafrika.elimika.course.factory.LessonProgressFactory;
import apps.sarafrika.elimika.course.model.LessonProgress;
import apps.sarafrika.elimika.course.repository.LessonProgressRepository;
import apps.sarafrika.elimika.course.service.LessonProgressService;
import apps.sarafrika.elimika.course.util.enums.ProgressStatus;
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
public class LessonProgressServiceImpl implements LessonProgressService {

    private final LessonProgressRepository lessonProgressRepository;
    private final GenericSpecificationBuilder<LessonProgress> specificationBuilder;

    private static final String PROGRESS_NOT_FOUND_TEMPLATE = "Lesson progress with ID %s not found";

    @Override
    public LessonProgressDTO createLessonProgress(LessonProgressDTO lessonProgressDTO) {
        LessonProgress progress = LessonProgressFactory.toEntity(lessonProgressDTO);

        // Set defaults based on LessonProgressDTO business logic
        if (progress.getStatus() == null) {
            progress.setStatus(ProgressStatus.NOT_STARTED);
        }

        LessonProgress savedProgress = lessonProgressRepository.save(progress);
        return LessonProgressFactory.toDTO(savedProgress);
    }

    @Override
    @Transactional(readOnly = true)
    public LessonProgressDTO getLessonProgressByUuid(UUID uuid) {
        return lessonProgressRepository.findByUuid(uuid)
                .map(LessonProgressFactory::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(PROGRESS_NOT_FOUND_TEMPLATE, uuid)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LessonProgressDTO> getAllLessonProgresses(Pageable pageable) {
        return lessonProgressRepository.findAll(pageable).map(LessonProgressFactory::toDTO);
    }

    @Override
    public LessonProgressDTO updateLessonProgress(UUID uuid, LessonProgressDTO lessonProgressDTO) {
        LessonProgress existingProgress = lessonProgressRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(PROGRESS_NOT_FOUND_TEMPLATE, uuid)));

        updateProgressFields(existingProgress, lessonProgressDTO);

        LessonProgress updatedProgress = lessonProgressRepository.save(existingProgress);
        return LessonProgressFactory.toDTO(updatedProgress);
    }

    @Override
    public void deleteLessonProgress(UUID uuid) {
        if (!lessonProgressRepository.existsByUuid(uuid)) {
            throw new ResourceNotFoundException(
                    String.format(PROGRESS_NOT_FOUND_TEMPLATE, uuid));
        }
        lessonProgressRepository.deleteByUuid(uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LessonProgressDTO> search(Map<String, String> searchParams, Pageable pageable) {
        Specification<LessonProgress> spec = specificationBuilder.buildSpecification(
                LessonProgress.class, searchParams);
        return lessonProgressRepository.findAll(spec, pageable).map(LessonProgressFactory::toDTO);
    }


    private LessonProgress getOrCreateProgress(UUID enrollmentUuid, UUID lessonUuid) {
        return lessonProgressRepository.findByEnrollmentUuidAndLessonUuid(enrollmentUuid, lessonUuid)
                .orElseGet(() -> {
                    LessonProgress newProgress = new LessonProgress();
                    newProgress.setEnrollmentUuid(enrollmentUuid);
                    newProgress.setLessonUuid(lessonUuid);
                    newProgress.setStatus(ProgressStatus.NOT_STARTED);
                    newProgress.setCreatedDate(LocalDateTime.now());
                    return newProgress;
                });
    }

    private void updateProgressFields(LessonProgress existingProgress, LessonProgressDTO dto) {
        if (dto.enrollmentUuid() != null) {
            existingProgress.setEnrollmentUuid(dto.enrollmentUuid());
        }
        if (dto.lessonUuid() != null) {
            existingProgress.setLessonUuid(dto.lessonUuid());
        }
        if (dto.status() != null) {
            existingProgress.setStatus(dto.status());
        }
        if (dto.startedAt() != null) {
            existingProgress.setStartedAt(dto.startedAt());
        }
        if (dto.completedAt() != null) {
            existingProgress.setCompletedAt(dto.completedAt());
        }
        if (dto.timeSpentMinutes() != null) {
            existingProgress.setTimeSpentMinutes(dto.timeSpentMinutes());
        }
    }
}