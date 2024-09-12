package apps.sarafrika.elimika.course.api;

import apps.sarafrika.elimika.course.api.dto.request.CreateInstructorAvailabilityRequestDTO;
import apps.sarafrika.elimika.course.api.dto.request.UpdateInstructorAvailabilityRequestDTO;
import apps.sarafrika.elimika.course.api.dto.response.InstructorAvailabilityResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponsePageableDTO;
import org.springframework.data.domain.Pageable;

import java.util.Set;

public interface InstructorAvailabilityService {
    ResponseDTO<InstructorAvailabilityResponseDTO> findById(final Long id);

    ResponsePageableDTO<InstructorAvailabilityResponseDTO> findAllByInstructor(final Pageable pageable, final Long instructorId);

    ResponseDTO<Void> create(CreateInstructorAvailabilityRequestDTO createInstructorAvailabilityRequestDTO, final Long instructorId);

    ResponseDTO<Void> createBatch(Set<CreateInstructorAvailabilityRequestDTO> createInstructorAvailabilityRequestDTOS, final Long instructorId);

    ResponseDTO<Void> update(UpdateInstructorAvailabilityRequestDTO updateInstructorAvailabilityRequestDTO, Long id);

    void delete(final Long id);
}
