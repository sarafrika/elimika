package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.config.exception.LessonContentNotFoundException;
import apps.sarafrika.elimika.course.config.exception.ValidationException;
import apps.sarafrika.elimika.course.dto.request.CreateLessonContentDTO;
import apps.sarafrika.elimika.course.dto.request.LessonContentRequestDTO;
import apps.sarafrika.elimika.course.dto.request.UpdateLessonContentDTO;
import apps.sarafrika.elimika.course.dto.response.ContentTypeResponseDTO;
import apps.sarafrika.elimika.course.dto.response.LessonContentResponseDTO;
import apps.sarafrika.elimika.course.persistence.LessonContent;
import apps.sarafrika.elimika.course.persistence.LessonContentRepository;
import apps.sarafrika.elimika.course.persistence.LessonContentSpecification;
import apps.sarafrika.elimika.course.service.ContentTypeService;
import apps.sarafrika.elimika.course.service.LessonContentService;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import apps.sarafrika.elimika.shared.storage.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.hibernate.service.spi.ServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LessonContentServiceImpl implements LessonContentService {

    private static final String ERROR_LESSON_CONTENT_NOT_FOUND = "Lesson content not found.";
    private static final String ERROR_INVALID_CONTENT_TYPE = "Unsupported content type.";
    private static final String ERROR_EXTRA_FILES_PROVIDED = "More files provided than expected.";
    private static final String ERROR_FILE_MISSING = "Missing file for content type.";
    private static final String LESSON_CONTENT_CREATED_SUCCESS = "Lesson content has been persisted successfully.";
    private static final String LESSON_CONTENT_FOUND_SUCCESS = "Lesson content retrieved successfully.";
    private static final String ERROR_FILE_MIME_TYPE_MISSING = "Could not determine file mime type.";
    private static final String ERROR_FILE_MIME_TYPE_INVALID = "Invalid file type for content type.";

    private final StorageService storageService;
    private final ContentTypeService contentTypeService;
    private final LessonContentRepository lessonContentRepository;

    @Override
    public ResponseDTO<List<LessonContentResponseDTO>> findAllLessonContent(LessonContentRequestDTO lessonContentRequestDTO) {

        List<LessonContent> lessonContent = lessonContentRepository.findAll(new LessonContentSpecification(lessonContentRequestDTO));

        List<LessonContentResponseDTO> lessonContentResponseDTOS = lessonContent.stream()
                .map(content -> {
                    ContentTypeResponseDTO contentType = contentTypeService.findContentType(content.getContentTypeId()).data();

                    return LessonContentResponseDTO.from(content, contentType.name());
                })
                .toList();

        return new ResponseDTO<>(lessonContentResponseDTOS, HttpStatus.OK.value(), LESSON_CONTENT_FOUND_SUCCESS, null, LocalDateTime.now());
    }

    @Override
    public ResponseDTO<List<LessonContentResponseDTO>> createLessonContent(Long lessonId, List<CreateLessonContentDTO> metadata, List<MultipartFile> files) {

        List<LessonContent> contentToCreate = new ArrayList<>();
        int fileIndex = 0;

        for (CreateLessonContentDTO contentMetadata : metadata) {

            ResponseDTO<ContentTypeResponseDTO> contentType = contentTypeService.findContentTypeByName(contentMetadata.contentType());

            LessonContent lessonContent = LessonContent.builder()
                    .title(contentMetadata.title())
                    .displayOrder(contentMetadata.displayOrder())
                    .duration(contentMetadata.duration())
                    .contentTypeId(contentType.data().id())
                    .lessonId(lessonId)
                    .build();

            switch (contentType.data().name()) {
                case "text":
                    lessonContent.setContent(contentMetadata.contentText());
                    break;

                case "video":
                case "image":
                case "pdf":
                    if (fileIndex >= files.size()) {

                        throw new ValidationException(ERROR_FILE_MISSING + ": " + contentType.data().name());
                    }

                    MultipartFile file = files.get(fileIndex++);

                    validateFileType(file, contentType.data().name());

                    String fileUrl = storageService.store(file);

                    lessonContent.setContent(fileUrl);
                    break;

                default:
                    throw new ValidationException(ERROR_INVALID_CONTENT_TYPE + ": " + contentType.data().name());
            }

            contentToCreate.add(lessonContent);
        }

        lessonContentRepository.saveAll(contentToCreate);

        if (fileIndex < files.size()) {

            throw new ValidationException(ERROR_EXTRA_FILES_PROVIDED);
        }

        List<LessonContentResponseDTO> lessonContentDTOS = contentToCreate.stream()
                .map(lessonContent -> LessonContentResponseDTO.from(lessonContent, contentTypeService.findContentType(lessonContent.getContentTypeId()).data().name()))
                .toList();

        return new ResponseDTO<>(lessonContentDTOS, HttpStatus.CREATED.value(), LESSON_CONTENT_CREATED_SUCCESS, null, LocalDateTime.now());
    }

    private void validateFileType(MultipartFile file, String contentType) {
        String mimeType = file.getContentType();

        if (mimeType == null) {
            throw new ServiceException(ERROR_FILE_MIME_TYPE_MISSING);
        }

        boolean isValid = switch (contentType.trim().toLowerCase()) {
            case "video" -> mimeType.startsWith("video/");
            case "image" -> mimeType.startsWith("image/");
            case "pdf" -> mimeType.startsWith("application/pdf");
            default -> false;
        };

        if (!isValid) {
            throw new ServiceException(ERROR_FILE_MIME_TYPE_INVALID);
        }

    }

    @Override
    public ResponseDTO<LessonContentResponseDTO> updateLessonContent(Long lessonContentId, UpdateLessonContentDTO updateLessonContentDTO) {
        return null;
    }

    @Override
    public void deleteLessonContent(Long lessonContentId) {

        LessonContent lessonContent = findLessonContentById(lessonContentId);

        lessonContentRepository.delete(lessonContent);
    }

    private LessonContent findLessonContentById(Long lessonContentId) {

        return lessonContentRepository.findById(lessonContentId).orElseThrow(() -> new LessonContentNotFoundException(ERROR_LESSON_CONTENT_NOT_FOUND));
    }
}
