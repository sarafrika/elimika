package apps.sarafrika.elimika.course.application;

import apps.sarafrika.elimika.course.api.InstructorService;
import apps.sarafrika.elimika.course.api.dto.request.CreateInstructorRequestDTO;
import apps.sarafrika.elimika.course.api.dto.request.UpdateInstructorRequestDTO;
import apps.sarafrika.elimika.course.api.dto.response.InstructorResponseDTO;
import apps.sarafrika.elimika.course.application.exceptions.InstructorNotFoundException;
import apps.sarafrika.elimika.course.domain.Instructor;
import apps.sarafrika.elimika.course.domain.InstructorFactory;
import apps.sarafrika.elimika.course.domain.InstructorRepository;
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

    private static final String ERROR_INSTRUCTOR_NOT_FOUND = "Instructor not found.";
    private static final String INSTRUCTOR_FOUND_SUCCESS = "Instructor retrieved successfully.";
    private static final String INSTRUCTORS_FOUND_SUCCESS = "Instructors retrieved successfully.";
    private static final String INSTRUCTOR_UPDATED_SUCCESS = "Instructor has been updated successfully.";
    private static final String INSTRUCTOR_CREATED_SUCCESS = "Instructor has been persisted successfully.";

    private final InstructorRepository instructorRepository;

    @Override
    @Transactional(readOnly = true)
    public ResponseDTO<InstructorResponseDTO> findById(Long id) {

        final Instructor instructor = findInstructorById(id);

        InstructorResponseDTO instructorResponseDTO = InstructorResponseDTO.from(instructor);

        return new ResponseDTO<>(instructorResponseDTO, HttpStatus.OK.value(), INSTRUCTOR_FOUND_SUCCESS, null, LocalDateTime.now());
    }

    @Override
    public Set<Instructor> findByIds(Set<Long> ids) {
        return instructorRepository.findByIdIn(ids);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponsePageableDTO<InstructorResponseDTO> findAll(Pageable pageable) {

        Page<InstructorResponseDTO> instructorsPage = instructorRepository.findAll(pageable)
                .stream()
                .map(InstructorResponseDTO::from)
                .collect(Collectors.collectingAndThen(Collectors.toList(), PageImpl::new));

        return new ResponsePageableDTO<>(instructorsPage.getContent(), instructorsPage.getNumber(), instructorsPage.getSize(),
                instructorsPage.getTotalPages(), instructorsPage.getTotalElements(), HttpStatus.OK.value(), INSTRUCTORS_FOUND_SUCCESS);
    }

    @Override
    public ResponseDTO<Void> create(CreateInstructorRequestDTO createInstructorRequestDTO) {

        final Instructor instructor = InstructorFactory.create(createInstructorRequestDTO);

        instructorRepository.save(instructor);

        return new ResponseDTO<>(null, HttpStatus.CREATED.value(), INSTRUCTOR_CREATED_SUCCESS, null, LocalDateTime.now());
    }

    @Override
    public ResponseDTO<Void> update(UpdateInstructorRequestDTO updateInstructorRequestDTO, Long id) {

        final Instructor instructorToUpdate = findInstructorById(id);

        InstructorFactory.update(instructorToUpdate, updateInstructorRequestDTO);
        instructorRepository.save(instructorToUpdate);

        return new ResponseDTO<>(null, HttpStatus.OK.value(), INSTRUCTOR_UPDATED_SUCCESS, null, LocalDateTime.now());
    }

    @Override
    public void delete(Long id) {
        final Instructor instructor = findInstructorById(id);

        instructorRepository.delete(instructor);
    }

    private Instructor findInstructorById(Long id) {

        return instructorRepository.findById(id).orElseThrow(() -> new InstructorNotFoundException(ERROR_INSTRUCTOR_NOT_FOUND));
    }
}
