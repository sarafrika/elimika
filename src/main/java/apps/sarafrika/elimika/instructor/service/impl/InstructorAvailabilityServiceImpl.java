package apps.sarafrika.elimika.instructor.service.impl;

import apps.sarafrika.elimika.instructor.config.exception.InstructorAvailabilityNotFoundException;
import apps.sarafrika.elimika.instructor.dto.request.CreateInstructorAvailabilityRequestDTO;
import apps.sarafrika.elimika.instructor.dto.request.UpdateInstructorAvailabilityRequestDTO;
import apps.sarafrika.elimika.instructor.dto.response.InstructorAvailabilityResponseDTO;
import apps.sarafrika.elimika.instructor.dto.response.InstructorResponseDTO;
import apps.sarafrika.elimika.instructor.persistence.InstructorAvailability;
import apps.sarafrika.elimika.instructor.persistence.InstructorAvailabilityFactory;
import apps.sarafrika.elimika.instructor.persistence.InstructorAvailabilityRepository;
import apps.sarafrika.elimika.instructor.service.InstructorAvailabilityService;
import apps.sarafrika.elimika.instructor.service.InstructorService;
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
class InstructorAvailabilityServiceImpl implements InstructorAvailabilityService {

    private static final String ERROR_INSTRUCTOR_AVAILABILITY_NOT_FOUND = "Instructor availability not found for the given instructor.";
    private static final String INSTRUCTOR_AVAILABILITY_FOUND_SUCCESS = "Instructor availability found successfully.";
    private static final String INSTRUCTOR_AVAILABILITY_CREATED_SUCCESS = "Instructor availability persisted successfully.";
    private static final String INSTRUCTOR_AVAILABILITY_UPDATED_SUCCESS = "Instructor availability updated successfully.";

    private final InstructorAvailabilityRepository instructorAvailabilityRepository;
    private final InstructorService instructorService;

    @Transactional(readOnly = true)
    @Override
    public ResponseDTO<InstructorAvailabilityResponseDTO> findInstructorAvailabilitySlot(Long instructorId, Long id) {

        final InstructorAvailability instructorAvailability = findInstructorAvailabilityByIdAndInstructorId(id, instructorId);

        InstructorAvailabilityResponseDTO instructorAvailabilityResponseDTO = InstructorAvailabilityResponseDTO.from(instructorAvailability);

        return new ResponseDTO<>(instructorAvailabilityResponseDTO, HttpStatus.OK.value(), INSTRUCTOR_AVAILABILITY_FOUND_SUCCESS, null, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    @Override
    public ResponsePageableDTO<InstructorAvailabilityResponseDTO> findAllInstructorAvailabilitySlots(Long instructorId, Pageable pageable) {

        final ResponseDTO<InstructorResponseDTO> instructor = instructorService.findInstructor(instructorId);

        Page<InstructorAvailabilityResponseDTO> instructorAvailabilitySlotsPage = instructorAvailabilityRepository.findAllByInstructorId(instructor.data().id(), pageable)
                .stream()
                .map(InstructorAvailabilityResponseDTO::from)
                .collect(Collectors.collectingAndThen(Collectors.toList(), PageImpl::new));

        return new ResponsePageableDTO<>(instructorAvailabilitySlotsPage.getContent(), instructorAvailabilitySlotsPage.getNumber(), instructorAvailabilitySlotsPage.getSize(),
                instructorAvailabilitySlotsPage.getTotalPages(), instructorAvailabilitySlotsPage.getTotalElements(), HttpStatus.OK.value(), INSTRUCTOR_AVAILABILITY_FOUND_SUCCESS);
    }

    @Transactional
    @Override
    public ResponseDTO<Void> addInstructorAvailabilitySlot(CreateInstructorAvailabilityRequestDTO createInstructorAvailabilityRequestDTO, Long instructorId) {

        final ResponseDTO<InstructorResponseDTO> instructor = instructorService.findInstructor(instructorId);

        final InstructorAvailability instructorAvailability = InstructorAvailabilityFactory.create(createInstructorAvailabilityRequestDTO);

        instructorAvailability.setInstructorId(instructor.data().id());

        instructorAvailabilityRepository.save(instructorAvailability);

        return new ResponseDTO<>(null, HttpStatus.CREATED.value(), INSTRUCTOR_AVAILABILITY_CREATED_SUCCESS, null, LocalDateTime.now());
    }

    @Transactional
    @Override
    public ResponseDTO<Void> addInstructorAvailabilitySlotBatch(Set<CreateInstructorAvailabilityRequestDTO> createInstructorAvailabilityRequestDTOS, Long instructorId) {

        ResponseDTO<InstructorResponseDTO> instructor = instructorService.findInstructor(instructorId);

        final Set<InstructorAvailability> instructorAvailabilitySlots = createInstructorAvailabilityRequestDTOS.stream()
                .map((createInstructorAvailabilityRequestDTO -> {

                    InstructorAvailability instructorAvailability = InstructorAvailabilityFactory.create(createInstructorAvailabilityRequestDTO);

                    instructorAvailability.setInstructorId(instructor.data().id());

                    return instructorAvailability;
                }))
                .collect(Collectors.toSet());

        instructorAvailabilityRepository.saveAll(instructorAvailabilitySlots);

        return new ResponseDTO<>(null, HttpStatus.CREATED.value(), INSTRUCTOR_AVAILABILITY_CREATED_SUCCESS, null, LocalDateTime.now());
    }

    @Transactional
    @Override
    public ResponseDTO<Void> updateInstructorAvailabilitySlot(UpdateInstructorAvailabilityRequestDTO updateInstructorAvailabilityRequestDTO, Long instructorId, Long id) {

        final InstructorAvailability instructorAvailability = findInstructorAvailabilityByIdAndInstructorId(id, instructorId);

        InstructorAvailabilityFactory.update(updateInstructorAvailabilityRequestDTO, instructorAvailability);

        instructorAvailabilityRepository.save(instructorAvailability);

        return new ResponseDTO<>(null, HttpStatus.OK.value(), INSTRUCTOR_AVAILABILITY_UPDATED_SUCCESS, null, LocalDateTime.now());
    }

    @Transactional
    @Override
    public void deleteInstructorAvailabilitySlot(Long instructorId, Long id) {

        final InstructorAvailability instructorAvailability = findInstructorAvailabilityByIdAndInstructorId(id, instructorId);

        instructorAvailabilityRepository.delete(instructorAvailability);
    }

    private InstructorAvailability findInstructorAvailabilityByIdAndInstructorId(Long id, Long instructorId) {

        return instructorAvailabilityRepository.findByIdAndInstructorId(id, instructorId).orElseThrow(() -> new InstructorAvailabilityNotFoundException(ERROR_INSTRUCTOR_AVAILABILITY_NOT_FOUND));
    }
}
