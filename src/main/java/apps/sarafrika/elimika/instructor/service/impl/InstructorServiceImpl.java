package apps.sarafrika.elimika.instructor.service.impl;

import apps.sarafrika.elimika.instructor.config.exception.InstructorNotFoundException;
import apps.sarafrika.elimika.instructor.dto.request.CreateInstructorRequestDTO;
import apps.sarafrika.elimika.instructor.dto.request.UpdateInstructorRequestDTO;
import apps.sarafrika.elimika.instructor.dto.response.InstructorResponseDTO;
import apps.sarafrika.elimika.instructor.persistence.Instructor;
import apps.sarafrika.elimika.instructor.persistence.InstructorFactory;
import apps.sarafrika.elimika.instructor.persistence.InstructorRepository;
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
class InstructorServiceImpl implements InstructorService {

    private static final String INSTRUCTOR_CREATED_SUCCESS = "Instructor persisted successfully";
    private static final String INSTRUCTOR_UPDATED_SUCCESS = "Instructor updated successfully";
    private static final String ERROR_INSTRUCTOR_NOT_FOUND = "Instructor not found";
    private static final String INSTRUCTOR_FOUND_SUCCESS = "Instructor found successfully";
    private static final String INSTRUCTORS_FOUND_SUCCESS = "Instructors found successfully";

    private final InstructorRepository instructorRepository;

    @Transactional
    @Override
    public ResponseDTO<Void> createInstructor(CreateInstructorRequestDTO createInstructorRequestDTO) {

        final Instructor instructor = InstructorFactory.create(createInstructorRequestDTO);

        instructorRepository.save(instructor);

        return new ResponseDTO<>(null, HttpStatus.CREATED.value(), INSTRUCTOR_CREATED_SUCCESS, null, LocalDateTime.now());
    }

    @Transactional
    @Override
    public ResponseDTO<Void> updateInstructor(UpdateInstructorRequestDTO updateInstructorRequestDTO, Long id) {

        final Instructor instructor = findInstructorById(id);

        InstructorFactory.update(instructor, updateInstructorRequestDTO);

        instructorRepository.save(instructor);

        return new ResponseDTO<>(null, HttpStatus.OK.value(), INSTRUCTOR_UPDATED_SUCCESS, null, LocalDateTime.now());
    }


    @Transactional(readOnly = true)
    @Override
    public ResponseDTO<InstructorResponseDTO> findInstructor(Long id) {

        final Instructor instructor = findInstructorById(id);

        InstructorResponseDTO instructorResponseDTO = InstructorResponseDTO.from(instructor);

        return new ResponseDTO<>(instructorResponseDTO, HttpStatus.OK.value(), INSTRUCTOR_FOUND_SUCCESS, null, LocalDateTime.now());
    }

    @Override
    public ResponseDTO<Set<InstructorResponseDTO>> findInstructorsByIds(Set<Long> ids) {

        final Set<Instructor> instructors = instructorRepository.findByIdIn(ids);

        final Set<InstructorResponseDTO> instructorResponseDTOs = instructors.stream()
                .map(InstructorResponseDTO::from)
                .collect(Collectors.toSet());

        return new ResponseDTO<>(instructorResponseDTOs, HttpStatus.OK.value(), INSTRUCTORS_FOUND_SUCCESS, null, LocalDateTime.now());
    }

    private Instructor findInstructorById(Long id) {

        return instructorRepository.findById(id)
                .orElseThrow(() -> new InstructorNotFoundException(ERROR_INSTRUCTOR_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    @Override
    public ResponsePageableDTO<InstructorResponseDTO> findAllInstructors(Pageable pageable) {

        Page<InstructorResponseDTO> instructorsPage = instructorRepository.findAll(pageable)
                .stream()
                .map(InstructorResponseDTO::from)
                .collect(Collectors.collectingAndThen(Collectors.toList(), PageImpl::new));

        return new ResponsePageableDTO<>(instructorsPage.getContent(), instructorsPage.getNumber(), instructorsPage.getSize(),
                instructorsPage.getTotalPages(), instructorsPage.getTotalElements(), HttpStatus.OK.value(), INSTRUCTORS_FOUND_SUCCESS);
    }

    @Transactional
    @Override
    public void deleteInstructor(Long id) {

        Instructor instructor = findInstructorById(id);

        instructorRepository.delete(instructor);
    }

}
