package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.ContentTypeDTO;
import apps.sarafrika.elimika.course.repository.ContentTypeRepository;
import apps.sarafrika.elimika.course.service.ContentTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContentTypeServiceImpl implements ContentTypeService {
    private static final String ERROR_CONTENT_TYPE_NOT_FOUND = "Content type not found";
    private static final String CONTENT_TYPE_FOUND_SUCCESS = "Content type has been retrieved successfully";
    private static final String CONTENT_TYPE_CREATED_SUCCESS = "Content type has been created successfully";
    private static final String CONTENT_TYPE_UPDATED_SUCCESS = "Content type has been updated successfully";

    private final ContentTypeRepository contentTypeRepository;

    @Override
    public ContentTypeDTO createContentType(ContentTypeDTO contentTypeDTO) {
        return null;
    }

    @Override
    public ContentTypeDTO getContentTypeByUuid(UUID uuid) {
        return null;
    }

    @Override
    public Page<ContentTypeDTO> getAllContentTypes(Pageable pageable) {
        return null;
    }

    @Override
    public ContentTypeDTO updateContentType(UUID uuid, ContentTypeDTO contentTypeDTO) {
        return null;
    }

    @Override
    public void deleteContentType(UUID uuid) {

    }

    @Override
    public Page<ContentTypeDTO> searchContentTypes(Map<String, String> searchParams, Pageable pageable) {
        return null;
    }
}
