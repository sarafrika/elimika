package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.common.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.common.util.GenericSpecificationBuilder;
import apps.sarafrika.elimika.course.dto.ContentProgressDTO;
import apps.sarafrika.elimika.course.factory.ContentProgressFactory;
import apps.sarafrika.elimika.course.model.ContentProgress;
import apps.sarafrika.elimika.course.repository.ContentProgressRepository;
import apps.sarafrika.elimika.course.service.ContentProgressService;
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
public class ContentProgressServiceImpl implements ContentProgressService {

    private final ContentProgressRepository contentProgressRepository;
    private final GenericSpecificationBuilder<ContentProgress> specificationBuilder;

    private static final String CONTENT_PROGRESS_NOT_FOUND_TEMPLATE = "Content progress with ID %s not found";

    @Override
    public ContentProgressDTO createContentProgress(ContentProgressDTO contentProgressDTO) {
        ContentProgress contentProgress = ContentProgressFactory.toEntity(contentProgressDTO);

        // Set defaults
        if (contentProgress.getIsAccessed() == null) {
            contentProgress.setIsAccessed(false);
        }
        if (contentProgress.getIsCompleted() == null) {
            contentProgress.setIsCompleted(false);
        }
        if (contentProgress.getAccessCount() == null) {
            contentProgress.setAccessCount(0);
        }

        ContentProgress savedContentProgress = contentProgressRepository.save(contentProgress);
        return ContentProgressFactory.toDTO(savedContentProgress);
    }

    @Override
    @Transactional(readOnly = true)
    public ContentProgressDTO getContentProgressByUuid(UUID uuid) {
        return contentProgressRepository.findByUuid(uuid)
                .map(ContentProgressFactory::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(CONTENT_PROGRESS_NOT_FOUND_TEMPLATE, uuid)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ContentProgressDTO> getAllContentProgresses(Pageable pageable) {
        return contentProgressRepository.findAll(pageable).map(ContentProgressFactory::toDTO);
    }

    @Override
    public ContentProgressDTO updateContentProgress(UUID uuid, ContentProgressDTO contentProgressDTO) {
        ContentProgress existingContentProgress = contentProgressRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(CONTENT_PROGRESS_NOT_FOUND_TEMPLATE, uuid)));

        updateContentProgressFields(existingContentProgress, contentProgressDTO);

        ContentProgress updatedContentProgress = contentProgressRepository.save(existingContentProgress);
        return ContentProgressFactory.toDTO(updatedContentProgress);
    }

    @Override
    public void deleteContentProgress(UUID uuid) {
        if (!contentProgressRepository.existsByUuid(uuid)) {
            throw new ResourceNotFoundException(
                    String.format(CONTENT_PROGRESS_NOT_FOUND_TEMPLATE, uuid));
        }
        contentProgressRepository.deleteByUuid(uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ContentProgressDTO> search(Map<String, String> searchParams, Pageable pageable) {
        Specification<ContentProgress> spec = specificationBuilder.buildSpecification(
                ContentProgress.class, searchParams);
        return contentProgressRepository.findAll(spec, pageable).map(ContentProgressFactory::toDTO);
    }

    private void updateContentProgressFields(ContentProgress existingContentProgress, ContentProgressDTO dto) {
        if (dto.enrollmentUuid() != null) {
            existingContentProgress.setEnrollmentUuid(dto.enrollmentUuid());
        }
        if (dto.contentUuid() != null) {
            existingContentProgress.setContentUuid(dto.contentUuid());
        }
        if (dto.isAccessed() != null) {
            existingContentProgress.setIsAccessed(dto.isAccessed());
        }
        if (dto.isCompleted() != null) {
            existingContentProgress.setIsCompleted(dto.isCompleted());
        }
        if (dto.accessCount() != null) {
            existingContentProgress.setAccessCount(dto.accessCount());
        }
        if (dto.firstAccessedAt() != null) {
            existingContentProgress.setFirstAccessedAt(dto.firstAccessedAt());
        }
        if (dto.lastAccessedAt() != null) {
            existingContentProgress.setLastAccessedAt(dto.lastAccessedAt());
        }
        if (dto.completedAt() != null) {
            existingContentProgress.setCompletedAt(dto.completedAt());
        }
    }
}