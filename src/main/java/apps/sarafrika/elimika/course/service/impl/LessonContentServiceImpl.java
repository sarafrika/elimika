package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.common.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.common.util.GenericSpecificationBuilder;
import apps.sarafrika.elimika.course.dto.LessonContentDTO;
import apps.sarafrika.elimika.course.factory.LessonContentFactory;
import apps.sarafrika.elimika.course.model.LessonContent;
import apps.sarafrika.elimika.course.repository.LessonContentRepository;
import apps.sarafrika.elimika.course.service.LessonContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class LessonContentServiceImpl implements LessonContentService {

    private final LessonContentRepository lessonContentRepository;
    private final GenericSpecificationBuilder<LessonContent> specificationBuilder;

    private static final String LESSON_CONTENT_NOT_FOUND_TEMPLATE = "Lesson content with ID %s not found";

    @Override
    public LessonContentDTO createLessonContent(LessonContentDTO lessonContentDTO) {
        LessonContent lessonContent = LessonContentFactory.toEntity(lessonContentDTO);

        // Set defaults
        if (lessonContent.getIsRequired() == null) {
            lessonContent.setIsRequired(false);
        }

        LessonContent savedLessonContent = lessonContentRepository.save(lessonContent);
        return LessonContentFactory.toDTO(savedLessonContent);
    }

    @Override
    @Transactional(readOnly = true)
    public LessonContentDTO getLessonContentByUuid(UUID uuid) {
        return lessonContentRepository.findByUuid(uuid)
                .map(LessonContentFactory::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(LESSON_CONTENT_NOT_FOUND_TEMPLATE, uuid)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LessonContentDTO> getAllLessonContents(Pageable pageable) {
        return lessonContentRepository.findAll(pageable).map(LessonContentFactory::toDTO);
    }

    @Override
    public LessonContentDTO updateLessonContent(UUID uuid, LessonContentDTO lessonContentDTO) {
        LessonContent existingLessonContent = lessonContentRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(LESSON_CONTENT_NOT_FOUND_TEMPLATE, uuid)));

        updateLessonContentFields(existingLessonContent, lessonContentDTO);

        LessonContent updatedLessonContent = lessonContentRepository.save(existingLessonContent);
        return LessonContentFactory.toDTO(updatedLessonContent);
    }

    @Override
    public void deleteLessonContent(UUID uuid) {
        if (!lessonContentRepository.existsByUuid(uuid)) {
            throw new ResourceNotFoundException(
                    String.format(LESSON_CONTENT_NOT_FOUND_TEMPLATE, uuid));
        }
        lessonContentRepository.deleteByUuid(uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LessonContentDTO> search(Map<String, String> searchParams, Pageable pageable) {
        Specification<LessonContent> spec = specificationBuilder.buildSpecification(
                LessonContent.class, searchParams);
        return lessonContentRepository.findAll(spec, pageable).map(LessonContentFactory::toDTO);
    }

    // Domain-specific methods leveraging LessonContentDTO computed properties
    @Transactional(readOnly = true)
    public List<LessonContentDTO> getContentByLesson(UUID lessonUuid) {
        return lessonContentRepository.findByLessonUuidOrderByDisplayOrderAsc(lessonUuid)
                .stream()
                .map(LessonContentFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LessonContentDTO> getRequiredContent(UUID lessonUuid) {
        return lessonContentRepository.findByLessonUuidAndIsRequiredTrueOrderByDisplayOrderAsc(lessonUuid)
                .stream()
                .map(LessonContentFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LessonContentDTO> getOptionalContent(UUID lessonUuid) {
        return lessonContentRepository.findByLessonUuidAndIsRequiredFalseOrderByDisplayOrderAsc(lessonUuid)
                .stream()
                .map(LessonContentFactory::toDTO)
                .collect(Collectors.toList());
    }

    // Content filtering by computed properties
    @Transactional(readOnly = true)
    public List<LessonContentDTO> getVideoContent(UUID lessonUuid) {
        return lessonContentRepository.findByLessonUuid(lessonUuid)
                .stream()
                .map(LessonContentFactory::toDTO)
                .filter(content -> "Video Content".equals(content.getContentCategory()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LessonContentDTO> getTextContent(UUID lessonUuid) {
        return lessonContentRepository.findByLessonUuid(lessonUuid)
                .stream()
                .map(LessonContentFactory::toDTO)
                .filter(content -> "Text Content".equals(content.getContentCategory()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LessonContentDTO> getFileContent(UUID lessonUuid) {
        return lessonContentRepository.findByLessonUuid(lessonUuid)
                .stream()
                .map(LessonContentFactory::toDTO)
                .filter(content -> content.getContentCategory().contains("Content") &&
                        !"Text Content".equals(content.getContentCategory()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getContentCategoryDistribution(UUID lessonUuid) {
        return lessonContentRepository.findByLessonUuid(lessonUuid)
                .stream()
                .map(LessonContentFactory::toDTO)
                .collect(Collectors.groupingBy(
                        LessonContentDTO::getContentCategory, // Using computed property
                        Collectors.counting()
                ));
    }

    public void reorderContent(UUID lessonUuid, List<UUID> contentUuids) {
        for (int i = 0; i < contentUuids.size(); i++) {
            int finalI = i;
            LessonContent content = lessonContentRepository.findByUuid(contentUuids.get(i))
                    .orElseThrow(() -> new ResourceNotFoundException(
                            String.format(LESSON_CONTENT_NOT_FOUND_TEMPLATE, contentUuids.get(finalI))));

            content.setDisplayOrder(i + 1);
            lessonContentRepository.save(content);
        }
    }

    @Transactional(readOnly = true)
    public int getNextDisplayOrder(UUID lessonUuid) {
        int maxOrder = lessonContentRepository.findMaxDisplayOrderByLessonUuid(lessonUuid);
        return maxOrder + 1;
    }

    @Transactional(readOnly = true)
    public long getTotalContentSize(UUID lessonUuid) {
        return lessonContentRepository.findByLessonUuid(lessonUuid)
                .stream()
                .filter(content -> content.getFileSizeBytes() != null)
                .mapToLong(LessonContent::getFileSizeBytes)
                .sum();
    }

    @Transactional(readOnly = true)
    public List<LessonContentDTO> getLargeFiles(UUID lessonUuid, long sizeThresholdBytes) {
        return lessonContentRepository.findByLessonUuidAndFileSizeBytesGreaterThan(lessonUuid, sizeThresholdBytes)
                .stream()
                .map(LessonContentFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean hasContent(UUID lessonUuid) {
        return lessonContentRepository.countByLessonUuid(lessonUuid) > 0;
    }

    @Transactional(readOnly = true)
    public boolean hasRequiredContent(UUID lessonUuid) {
        return lessonContentRepository.countByLessonUuidAndIsRequiredTrue(lessonUuid) > 0;
    }

    private void updateLessonContentFields(LessonContent existingLessonContent, LessonContentDTO dto) {
        if (dto.lessonUuid() != null) {
            existingLessonContent.setLessonUuid(dto.lessonUuid());
        }
        if (dto.contentTypeUuid() != null) {
            existingLessonContent.setContentTypeUuid(dto.contentTypeUuid());
        }
        if (dto.title() != null) {
            existingLessonContent.setTitle(dto.title());
        }
        if (dto.description() != null) {
            existingLessonContent.setDescription(dto.description());
        }
        if (dto.contentText() != null) {
            existingLessonContent.setContentText(dto.contentText());
        }
        if (dto.fileUrl() != null) {
            existingLessonContent.setFileUrl(dto.fileUrl());
        }
        if (dto.displayOrder() != null) {
            existingLessonContent.setDisplayOrder(dto.displayOrder());
        }
        if (dto.isRequired() != null) {
            existingLessonContent.setIsRequired(dto.isRequired());
        }
    }
}