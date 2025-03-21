package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.LessonContentDTO;
import apps.sarafrika.elimika.course.exception.LessonContentNotFoundException;
import apps.sarafrika.elimika.course.exception.ValidationException;
import apps.sarafrika.elimika.course.dto.request.LessonContentRequestDTO;
import apps.sarafrika.elimika.course.dto.request.UpdateLessonContentDTO;
import apps.sarafrika.elimika.course.dto.response.ContentTypeResponseDTO;
import apps.sarafrika.elimika.course.dto.response.LessonContentResponseDTO;
import apps.sarafrika.elimika.course.model.LessonContent;
import apps.sarafrika.elimika.course.repository.LessonContentRepository;
import apps.sarafrika.elimika.course.service.ContentTypeService;
import apps.sarafrika.elimika.course.service.LessonContentService;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import apps.sarafrika.elimika.common.storage.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.hibernate.service.spi.ServiceException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
    public LessonContentDTO createLessonContent(LessonContentDTO lessonContentDTO) {
        return null;
    }

    @Override
    public LessonContentDTO getLessonContentByUuid(UUID uuid) {
        return null;
    }

    @Override
    public Page<LessonContentDTO> getAllLessonContents(Pageable pageable) {
        return null;
    }

    @Override
    public LessonContentDTO updateLessonContent(UUID uuid, LessonContentDTO lessonContentDTO) {
        return null;
    }

    @Override
    public void deleteLessonContent(UUID uuid) {

    }

    @Override
    public Page<LessonContentDTO> searchLessonContents(Map<String, String> searchParams, Pageable pageable) {
        return null;
    }
}
