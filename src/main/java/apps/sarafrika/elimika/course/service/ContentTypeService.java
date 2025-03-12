package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.request.CreateContentTypeDTO;
import apps.sarafrika.elimika.course.dto.request.UpdateContentTypeDTO;
import apps.sarafrika.elimika.course.dto.response.ContentTypeResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;

import java.util.List;

public interface ContentTypeService {

    ResponseDTO<List<ContentTypeResponseDTO>> findAllContentType();

    ResponseDTO<ContentTypeResponseDTO> findContentType(Long contentTypeId);

    ResponseDTO<ContentTypeResponseDTO> findContentTypeByName(String name);

    ResponseDTO<ContentTypeResponseDTO> createContentType(CreateContentTypeDTO createContentTypeDTO);

    ResponseDTO<ContentTypeResponseDTO> updateContentType(Long contentTypeId, UpdateContentTypeDTO updateContentTypeDTO);

    void deleteContentType(Long contentTypeId);
}
