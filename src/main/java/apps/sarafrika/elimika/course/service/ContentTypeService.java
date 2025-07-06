package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.ContentTypeDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ContentTypeService {
    ContentTypeDTO createContentType(ContentTypeDTO contentTypeDTO);

    ContentTypeDTO getContentTypeByUuid(UUID uuid);

    Page<ContentTypeDTO> getAllContentTypes(Pageable pageable);

    ContentTypeDTO updateContentType(UUID uuid, ContentTypeDTO contentTypeDTO);

    void deleteContentType(UUID uuid);

    Page<ContentTypeDTO> search(Map<String, String> searchParams, Pageable pageable);

    // Domain-specific methods
    List<ContentTypeDTO> getMediaContentTypes();

    List<ContentTypeDTO> getContentTypesByCategory(String category);

    List<ContentTypeDTO> getVideoContentTypes();

    List<ContentTypeDTO> getAudioContentTypes();

    List<ContentTypeDTO> getImageContentTypes();

    List<ContentTypeDTO> getDocumentContentTypes();

    List<ContentTypeDTO> getUnlimitedSizeContentTypes();

    List<ContentTypeDTO> getLargeSizeContentTypes();

    boolean isMimeTypeSupported(String mimeType);

    ContentTypeDTO getContentTypeByMimeType(String mimeType);

    boolean canDeleteContentType(UUID contentTypeUuid);
}