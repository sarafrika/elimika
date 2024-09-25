package apps.sarafrika.elimika.instructor.service;

import apps.sarafrika.elimika.instructor.dto.request.CreateInstructorAvailabilityRequestDTO;
import apps.sarafrika.elimika.instructor.dto.request.UpdateInstructorAvailabilityRequestDTO;
import apps.sarafrika.elimika.instructor.dto.response.InstructorAvailabilityResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponsePageableDTO;
import org.springframework.data.domain.Pageable;

import java.util.Set;

public interface InstructorAvailabilityService {

    ResponseDTO<InstructorAvailabilityResponseDTO> findInstructorAvailabilitySlot(Long instructorId, Long id);

    ResponsePageableDTO<InstructorAvailabilityResponseDTO> findAllInstructorAvailabilitySlots(Long instructorId, Pageable pageable);

    ResponseDTO<Void> addInstructorAvailabilitySlot(CreateInstructorAvailabilityRequestDTO createInstructorAvailabilityRequestDTO, Long instructorId);

    ResponseDTO<Void> addInstructorAvailabilitySlotBatch(Set<CreateInstructorAvailabilityRequestDTO> createInstructorAvailabilityRequestDTOS, Long instructorId);

    ResponseDTO<Void> updateInstructorAvailabilitySlot(UpdateInstructorAvailabilityRequestDTO updateInstructorAvailabilityRequestDTO, Long instructorId, Long id);

    void deleteInstructorAvailabilitySlot(Long instructorId, Long id);
}
