package apps.sarafrika.elimika.course.api;

import apps.sarafrika.elimika.course.api.dto.request.CreateInstructorRequestDTO;
import apps.sarafrika.elimika.course.api.dto.request.UpdateInstructorRequestDTO;
import apps.sarafrika.elimika.course.api.dto.response.InstructorResponseDTO;
import apps.sarafrika.elimika.course.domain.Instructor;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponsePageableDTO;
import org.springframework.data.domain.Pageable;

import java.util.Set;

public interface InstructorService {
    ResponseDTO<InstructorResponseDTO> findById(final Long id);

    Set<Instructor> findByIds(final Set<Long> ids);

    ResponsePageableDTO<InstructorResponseDTO> findAll(final Pageable pageable);

    ResponseDTO<Void> create(CreateInstructorRequestDTO createInstructorRequestDTO);

    ResponseDTO<Void> update(UpdateInstructorRequestDTO updateInstructorRequestDTO, Long id);

    void delete(final Long id);
}
