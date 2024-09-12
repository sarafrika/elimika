package apps.sarafrika.elimika.course.application;

import apps.sarafrika.elimika.course.api.InstructorAvailabilityService;
import apps.sarafrika.elimika.course.api.dto.request.CreateInstructorAvailabilityRequestDTO;
import apps.sarafrika.elimika.course.api.dto.request.UpdateInstructorAvailabilityRequestDTO;
import apps.sarafrika.elimika.course.api.dto.response.InstructorAvailabilityResponseDTO;
import apps.sarafrika.elimika.course.application.exceptions.InstructorAvailabilityNotFoundException;
import apps.sarafrika.elimika.course.application.exceptions.InstructorNotFoundException;
import apps.sarafrika.elimika.course.domain.*;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponsePageableDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InstructorAvailabilityServiceImpl implements InstructorAvailabilityService {

    private static final String ERROR_INSTRUCTOR_AVAILABILITY_NOT_FOUND = "Instructor availability not found";
    private static final String ERROR_INSTRUCTOR_NOT_FOUND = "Instructor not found.";
    private static final String INSTRUCTOR_AVAILABILITY_FOUND_SUCCESS = "Instructor availability retrieved successfully";
    private static final String INSTRUCTOR_AVAILABILITY_CREATED_SUCCESS = "Instructor availability created successfully";
    private static final String INSTRUCTOR_AVAILABILITY_UPDATED_SUCCESS = "Instructor availability updated successfully";

    private final InstructorAvailabilityRepository instructorAvailabilityRepository;
    private final InstructorRepository instructorRepository;

    @Override
    @Transactional(readOnly = true)
    public ResponseDTO<InstructorAvailabilityResponseDTO> findById(Long id) {

        final InstructorAvailability instructorAvailability = findInstructorAvailabilityById(id);

        InstructorAvailabilityResponseDTO instructorAvailabilityResponseDTO = InstructorAvailabilityResponseDTO.from(instructorAvailability);

        return new ResponseDTO<>(instructorAvailabilityResponseDTO, HttpStatus.OK.value(), INSTRUCTOR_AVAILABILITY_FOUND_SUCCESS, null, LocalDateTime.now());
    }

    @Override
    public ResponsePageableDTO<InstructorAvailabilityResponseDTO> findAllByInstructor(Pageable pageable, Long instructorId) {

        final Instructor instructor = findInstructorById(instructorId);

        Page<InstructorAvailabilityResponseDTO> instructorAvailabilyPage = instructorAvailabilityRepository.findAllByInstructor(pageable, instructor)
                .stream()
                .map(InstructorAvailabilityResponseDTO::from)
                .collect(Collectors.collectingAndThen(Collectors.toList(), PageImpl::new));

        return new ResponsePageableDTO<>(instructorAvailabilyPage.getContent(), instructorAvailabilyPage.getNumber(), instructorAvailabilyPage.getSize(),
                instructorAvailabilyPage.getTotalPages(), instructorAvailabilyPage.getTotalElements(), HttpStatus.OK.value(), INSTRUCTOR_AVAILABILITY_FOUND_SUCCESS);
    }

    @Override
    public ResponseDTO<Void> create(CreateInstructorAvailabilityRequestDTO createInstructorAvailabilityRequestDTO, final Long instructorId) {

        Instructor instructor = findInstructorById(instructorId);

        final InstructorAvailability instructorAvailability = InstructorAvailabilityFactory.create(createInstructorAvailabilityRequestDTO, instructor);

        instructorAvailabilityRepository.save(instructorAvailability);

        return new ResponseDTO<>(null, HttpStatus.CREATED.value(), INSTRUCTOR_AVAILABILITY_CREATED_SUCCESS, null, LocalDateTime.now());
    }

    @Override
    public ResponseDTO<Void> createBatch(Set<CreateInstructorAvailabilityRequestDTO> createInstructorAvailabilityRequestDTOS, Long instructorId) {

        Instructor instructor = findInstructorById(instructorId);

        final Set<InstructorAvailability> availabilitySlots = createInstructorAvailabilityRequestDTOS.stream()
                .map((createInstructorAvailabilityRequestDTO -> InstructorAvailabilityFactory.create(createInstructorAvailabilityRequestDTO, instructor)))
                .collect(Collectors.toSet());

        instructorAvailabilityRepository.saveAll(availabilitySlots);

        return new ResponseDTO<>(null, HttpStatus.CREATED.value(), INSTRUCTOR_AVAILABILITY_CREATED_SUCCESS, null, LocalDateTime.now());
    }

    @Override
    public ResponseDTO<Void> update(UpdateInstructorAvailabilityRequestDTO updateInstructorAvailabilityRequestDTO, Long id) {

        final InstructorAvailability instructorAvailabilityToUpdate = findInstructorAvailabilityById(id);

        InstructorAvailabilityFactory.update(updateInstructorAvailabilityRequestDTO, instructorAvailabilityToUpdate);

        instructorAvailabilityRepository.save(instructorAvailabilityToUpdate);

        return new ResponseDTO<>(null, HttpStatus.OK.value(), INSTRUCTOR_AVAILABILITY_UPDATED_SUCCESS, null, LocalDateTime.now());
    }

    @Override
    public void delete(Long id) {

        final InstructorAvailability instructorAvailability = findInstructorAvailabilityById(id);

        instructorAvailabilityRepository.delete(instructorAvailability);

    }

    private InstructorAvailability findInstructorAvailabilityById(Long id) {

        return instructorAvailabilityRepository.findById(id).orElseThrow(() -> new InstructorAvailabilityNotFoundException(ERROR_INSTRUCTOR_AVAILABILITY_NOT_FOUND));
    }

    private Instructor findInstructorById(Long id) {

        return instructorRepository.findById(id).orElseThrow(() -> new InstructorNotFoundException(ERROR_INSTRUCTOR_NOT_FOUND));
    }
}
