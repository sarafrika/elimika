package apps.sarafrika.elimika.course.api;

import apps.sarafrika.elimika.course.api.dto.request.CreateClassRequestDTO;
import apps.sarafrika.elimika.course.api.dto.request.UpdateClassRequestDTO;
import apps.sarafrika.elimika.course.api.dto.response.ClassResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponsePageableDTO;
import org.springframework.data.domain.Pageable;

public interface ClassService {
    ResponseDTO<ClassResponseDTO> findById(final Long id);

    ResponsePageableDTO<ClassResponseDTO> findAll(final Pageable pageable);

    ResponseDTO<Void> create(CreateClassRequestDTO createClassRequestDTO);

    ResponseDTO<Void> update(UpdateClassRequestDTO updateClassRequestDTO, final Long id);

    void delete(final Long id);
}
