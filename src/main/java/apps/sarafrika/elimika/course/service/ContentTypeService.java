package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.ContentTypeDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface ContentTypeService {
    ContentTypeDTO createContentType(ContentTypeDTO contentTypeDTO);
    ContentTypeDTO getContentTypeByUuid(UUID uuid);
    Page<ContentTypeDTO> getAllContentTypes(Pageable pageable);
    ContentTypeDTO updateContentType(UUID uuid, ContentTypeDTO contentTypeDTO);
    void deleteContentType(UUID uuid);
    Page<ContentTypeDTO> search(Map<String, String> searchParams, Pageable pageable);
}