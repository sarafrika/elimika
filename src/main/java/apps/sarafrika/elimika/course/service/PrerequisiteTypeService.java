package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.request.CreatePrerequisiteTypeRequestDTO;
import apps.sarafrika.elimika.course.dto.response.PrerequisiteTypeResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;

public interface PrerequisiteTypeService {

    ResponseDTO<PrerequisiteTypeResponseDTO> findPrerequisiteType(Long id);

    ResponseDTO<Void> createPrerequisiteType(CreatePrerequisiteTypeRequestDTO createPrerequisiteTypeRequestDTO);
}
