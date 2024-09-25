package apps.sarafrika.elimika.instructor.service;

import apps.sarafrika.elimika.instructor.dto.request.CreateInstructorRequestDTO;
import apps.sarafrika.elimika.instructor.dto.request.UpdateInstructorRequestDTO;
import apps.sarafrika.elimika.instructor.dto.response.InstructorResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponsePageableDTO;
import org.springframework.data.domain.Pageable;

import java.util.Set;

public interface InstructorService {

    ResponseDTO<Void> createInstructor(CreateInstructorRequestDTO createInstructorRequestDTO);

    ResponseDTO<Void> updateInstructor(UpdateInstructorRequestDTO updateInstructorRequestDTO, Long id);

    ResponseDTO<InstructorResponseDTO> findInstructor(Long id);

    ResponseDTO<Set<InstructorResponseDTO>> findInstructorsByIds(Set<Long> ids);

    ResponsePageableDTO<InstructorResponseDTO> findAllInstructors(Pageable pageable);

    void deleteInstructor(Long id);
}
