package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.request.CreateClassRequestDTO;
import apps.sarafrika.elimika.course.dto.request.UpdateClassRequestDTO;
import apps.sarafrika.elimika.course.dto.response.ClassResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponsePageableDTO;
import org.springframework.data.domain.Pageable;

public interface ClassService {

    ResponsePageableDTO<ClassResponseDTO> findAllClasses(Pageable pageable);

    ResponseDTO<ClassResponseDTO> findClass(Long id);

    ResponseDTO<Void> createClass(CreateClassRequestDTO createClassRequestDTO);

    ResponseDTO<Void> updateClass(UpdateClassRequestDTO updateClassRequestDTO, Long id);

    void deleteClass(Long id);
}
