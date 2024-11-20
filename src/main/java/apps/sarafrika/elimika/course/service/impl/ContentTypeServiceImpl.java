package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.config.exception.ContentTypeNotFoundException;
import apps.sarafrika.elimika.course.dto.request.CreateContentTypeDTO;
import apps.sarafrika.elimika.course.dto.request.UpdateContentTypeDTO;
import apps.sarafrika.elimika.course.dto.response.ContentTypeResponseDTO;
import apps.sarafrika.elimika.course.persistence.ContentType;
import apps.sarafrika.elimika.course.persistence.ContentTypeFactory;
import apps.sarafrika.elimika.course.persistence.ContentTypeRepository;
import apps.sarafrika.elimika.course.service.ContentTypeService;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContentTypeServiceImpl implements ContentTypeService {
    private static final String ERROR_CONTENT_TYPE_NOT_FOUND = "Content type not found";
    private static final String CONTENT_TYPE_FOUND_SUCCESS = "Content type has been retrieved successfully";
    private static final String CONTENT_TYPE_CREATED_SUCCESS = "Content type has been created successfully";
    private static final String CONTENT_TYPE_UPDATED_SUCCESS = "Content type has been updated successfully";

    private final ContentTypeRepository contentTypeRepository;

    @Override
    public ResponseDTO<List<ContentTypeResponseDTO>> findAllContentType() {

        List<ContentTypeResponseDTO> contentTypeResponseDTOList = contentTypeRepository.findAll().stream()
                .map(ContentTypeResponseDTO::from)
                .toList();

        return new ResponseDTO<>(contentTypeResponseDTOList, HttpStatus.OK.value(), CONTENT_TYPE_FOUND_SUCCESS, null, LocalDateTime.now());
    }

    @Override
    public ResponseDTO<ContentTypeResponseDTO> findContentType(Long contentTypeId) {

        ContentType contentType = findContentTypeById(contentTypeId);

        return new ResponseDTO<>(ContentTypeResponseDTO.from(contentType), HttpStatus.OK.value(), CONTENT_TYPE_FOUND_SUCCESS, null, LocalDateTime.now());
    }

    @Override
    public ResponseDTO<ContentTypeResponseDTO> findContentTypeByName(String name) {

        ContentType contentType = contentTypeRepository.findByName(name.trim().toLowerCase()).orElseThrow(() -> new ContentTypeNotFoundException(ERROR_CONTENT_TYPE_NOT_FOUND));

        return new ResponseDTO<>(ContentTypeResponseDTO.from(contentType), HttpStatus.OK.value(), CONTENT_TYPE_FOUND_SUCCESS, null, LocalDateTime.now());
    }

    private ContentType findContentTypeById(Long contentTypeId) {

        return contentTypeRepository.findById(contentTypeId).orElseThrow(() -> new ContentTypeNotFoundException(ERROR_CONTENT_TYPE_NOT_FOUND));
    }

    @Override
    public ResponseDTO<ContentTypeResponseDTO> createContentType(CreateContentTypeDTO createContentTypeDTO) {

        ContentType contentType = ContentTypeFactory.create(createContentTypeDTO);

        contentTypeRepository.save(contentType);

        return new ResponseDTO<>(ContentTypeResponseDTO.from(contentType), HttpStatus.CREATED.value(), CONTENT_TYPE_CREATED_SUCCESS, null, LocalDateTime.now());
    }

    @Override
    public ResponseDTO<ContentTypeResponseDTO> updateContentType(Long contentTypeId, UpdateContentTypeDTO updateContentTypeDTO) {

        ContentType contentType = findContentTypeById(contentTypeId);

        ContentTypeFactory.update(contentType, updateContentTypeDTO);

        contentTypeRepository.save(contentType);

        return new ResponseDTO<>(ContentTypeResponseDTO.from(contentType), HttpStatus.OK.value(), CONTENT_TYPE_UPDATED_SUCCESS, null, LocalDateTime.now());
    }

    @Override
    public void deleteContentType(Long contentTypeId) {

        ContentType contentType = findContentTypeById(contentTypeId);

        contentTypeRepository.delete(contentType);
    }
}
