package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.request.*;
import apps.sarafrika.elimika.course.dto.response.PrerequisiteGroupResponseDTO;
import apps.sarafrika.elimika.course.dto.response.PrerequisiteResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponsePageableDTO;
import org.springframework.data.domain.Pageable;

public interface PrerequisiteService {

    ResponsePageableDTO<PrerequisiteResponseDTO> findAllPrerequisites(PrerequisiteRequestDTO prerequisiteRequestDTO, Pageable pageable);

    ResponseDTO<PrerequisiteResponseDTO> findPrerequisite(Long prerequisiteId);

    ResponseDTO<Void> createPrerequisite(CreatePrerequisiteRequestDTO createPrerequisiteRequestDTO);

    ResponseDTO<Void> updatePrerequisite(UpdatePrerequisiteRequestDTO updatePrerequisiteRequestDTO, Long prerequisiteId);

    void deletePrerequisite(Long prerequisiteId);

    ResponseDTO<Void> createPrerequisiteGroup(CreatePrerequisiteGroupRequestDTO createPrerequisiteGroupRequestDTO);

    ResponseDTO<Void> updatePrerequisiteGroup(UpdatePrerequisiteGroupRequestDTO updatePrerequisiteGroupRequestDTO, Long prerequisiteGroupId);

    void deletePrerequisiteGroup(Long prerequisiteGroupId);

    ResponseDTO<PrerequisiteGroupResponseDTO> findPrerequisiteGroup(Long prerequisiteGroupId);
}
