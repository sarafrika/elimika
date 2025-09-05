package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import apps.sarafrika.elimika.course.dto.ContentTypeDTO;
import apps.sarafrika.elimika.course.factory.ContentTypeFactory;
import apps.sarafrika.elimika.course.model.ContentType;
import apps.sarafrika.elimika.course.repository.ContentTypeRepository;
import apps.sarafrika.elimika.course.repository.LessonContentRepository;
import apps.sarafrika.elimika.course.service.ContentTypeService;
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
public class ContentTypeServiceImpl implements ContentTypeService {

    private final ContentTypeRepository contentTypeRepository;
    private final LessonContentRepository lessonContentRepository;

    private final GenericSpecificationBuilder<ContentType> specificationBuilder;

    private static final String CONTENT_TYPE_NOT_FOUND_TEMPLATE = "Content type with ID %s not found";

    @Override
    public ContentTypeDTO createContentType(ContentTypeDTO contentTypeDTO) {
        ContentType contentType = ContentTypeFactory.toEntity(contentTypeDTO);

        ContentType savedContentType = contentTypeRepository.save(contentType);
        return ContentTypeFactory.toDTO(savedContentType);
    }

    @Override
    @Transactional(readOnly = true)
    public ContentTypeDTO getContentTypeByUuid(UUID uuid) {
        return contentTypeRepository.findByUuid(uuid)
                .map(ContentTypeFactory::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(CONTENT_TYPE_NOT_FOUND_TEMPLATE, uuid)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ContentTypeDTO> getAllContentTypes(Pageable pageable) {
        return contentTypeRepository.findAll(pageable).map(ContentTypeFactory::toDTO);
    }

    @Override
    public ContentTypeDTO updateContentType(UUID uuid, ContentTypeDTO contentTypeDTO) {
        ContentType existingContentType = contentTypeRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(CONTENT_TYPE_NOT_FOUND_TEMPLATE, uuid)));

        updateContentTypeFields(existingContentType, contentTypeDTO);

        ContentType updatedContentType = contentTypeRepository.save(existingContentType);
        return ContentTypeFactory.toDTO(updatedContentType);
    }

    @Override
    public void deleteContentType(UUID uuid) {
        if (!contentTypeRepository.existsByUuid(uuid)) {
            throw new ResourceNotFoundException(
                    String.format(CONTENT_TYPE_NOT_FOUND_TEMPLATE, uuid));
        }
        contentTypeRepository.deleteByUuid(uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ContentTypeDTO> search(Map<String, String> searchParams, Pageable pageable) {
        Specification<ContentType> spec = specificationBuilder.buildSpecification(
                ContentType.class, searchParams);
        return contentTypeRepository.findAll(spec, pageable).map(ContentTypeFactory::toDTO);
    }

    // Domain-specific methods leveraging ContentTypeDTO computed properties
    @Transactional(readOnly = true)
    public List<ContentTypeDTO> getMediaContentTypes() {
        return contentTypeRepository.findAll()
                .stream()
                .map(ContentTypeFactory::toDTO)
                .filter(ContentTypeDTO::isMediaType) // Using computed property
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ContentTypeDTO> getContentTypesByCategory(String category) {
        return contentTypeRepository.findAll()
                .stream()
                .map(ContentTypeFactory::toDTO)
                .filter(contentType -> category.equals(contentType.getUploadCategory()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ContentTypeDTO> getVideoContentTypes() {
        return contentTypeRepository.findByMimeTypesContaining("video/")
                .stream()
                .map(ContentTypeFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ContentTypeDTO> getAudioContentTypes() {
        return contentTypeRepository.findByMimeTypesContaining("audio/")
                .stream()
                .map(ContentTypeFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ContentTypeDTO> getImageContentTypes() {
        return contentTypeRepository.findByMimeTypesContaining("image/")
                .stream()
                .map(ContentTypeFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ContentTypeDTO> getDocumentContentTypes() {
        return contentTypeRepository.findByMimeTypesContaining("application/")
                .stream()
                .map(ContentTypeFactory::toDTO)
                .filter(contentType -> "Documents".equals(contentType.getUploadCategory()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ContentTypeDTO> getUnlimitedSizeContentTypes() {
        return contentTypeRepository.findByMaxFileSizeMbIsNull()
                .stream()
                .map(ContentTypeFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ContentTypeDTO> getLargeSizeContentTypes() {
        // Content types with size limit > 100MB
        return contentTypeRepository.findByMaxFileSizeMbGreaterThan(100)
                .stream()
                .map(ContentTypeFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean isMimeTypeSupported(String mimeType) {
        return contentTypeRepository.existsByMimeTypesContaining(mimeType);
    }

    @Transactional(readOnly = true)
    public ContentTypeDTO getContentTypeByMimeType(String mimeType) {
        return contentTypeRepository.findByMimeTypesContaining(mimeType)
                .stream()
                .findFirst()
                .map(ContentTypeFactory::toDTO)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public boolean canDeleteContentType(UUID contentTypeUuid) {
        // Check if any lesson content is using this content type
        return lessonContentRepository.countByContentTypeUuid(contentTypeUuid) == 0;
    }

    private void updateContentTypeFields(ContentType existingContentType, ContentTypeDTO dto) {
        if (dto.name() != null) {
            existingContentType.setName(dto.name());
        }
        if (dto.mimeTypes() != null) {
            existingContentType.setMimeTypes(dto.mimeTypes());
        }
        if (dto.maxFileSizeMb() != null) {
            existingContentType.setMaxFileSizeMb(dto.maxFileSizeMb());
        }
    }
}