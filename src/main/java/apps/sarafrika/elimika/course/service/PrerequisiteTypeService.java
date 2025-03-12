package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.request.CreatePrerequisiteTypeRequestDTO;
import apps.sarafrika.elimika.course.dto.response.PrerequisiteTypeResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponsePageableDTO;
import org.springframework.data.domain.Pageable;


public interface PrerequisiteTypeService {

    ResponsePageableDTO<PrerequisiteTypeResponseDTO> findAllPrerequisiteTypes(Pageable pageable);

    ResponseDTO<PrerequisiteTypeResponseDTO> findPrerequisiteType(Long id);

    ResponseDTO<Void> createPrerequisiteType(CreatePrerequisiteTypeRequestDTO createPrerequisiteTypeRequestDTO);
}
