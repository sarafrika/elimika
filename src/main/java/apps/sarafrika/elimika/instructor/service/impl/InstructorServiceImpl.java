package apps.sarafrika.elimika.instructor.service.impl;

import apps.sarafrika.elimika.common.exceptions.RecordNotFoundException;
import apps.sarafrika.elimika.common.util.GenericSpecificationBuilder;
import apps.sarafrika.elimika.instructor.dto.InstructorDTO;
import apps.sarafrika.elimika.instructor.factory.InstructorFactory;
import apps.sarafrika.elimika.instructor.model.Instructor;
import apps.sarafrika.elimika.instructor.repository.InstructorRepository;
import apps.sarafrika.elimika.instructor.service.InstructorService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InstructorServiceImpl implements InstructorService {

    private final InstructorRepository instructorRepository;
    private final GenericSpecificationBuilder<Instructor> specificationBuilder;

    private static final String INSTRUCTOR_NOT_FOUND_TEMPLATE = "Instructor with ID %s not found";

    @Override
    public InstructorDTO createInstructor(InstructorDTO instructorDTO) {
        Instructor instructor = InstructorFactory.toEntity(instructorDTO);
        instructor.setCreatedDate(LocalDateTime.now());

        Instructor savedInstructor = instructorRepository.save(instructor);
        return InstructorFactory.toDTO(savedInstructor);
    }

    @Override
    public InstructorDTO getInstructorByUuid(UUID uuid) {
        return instructorRepository.findByUuid(uuid)
                .map(InstructorFactory::toDTO)
                .orElseThrow(() -> new RecordNotFoundException(String.format(INSTRUCTOR_NOT_FOUND_TEMPLATE, uuid)));
    }

    @Override
    public Page<InstructorDTO> getAllInstructors(Pageable pageable) {
        return instructorRepository.findAll(pageable).map(InstructorFactory::toDTO);
    }

    @Override
    public InstructorDTO updateInstructor(UUID uuid, InstructorDTO instructorDTO) {
        Instructor existingInstructor = instructorRepository.findByUuid(uuid)
                .orElseThrow(() -> new RecordNotFoundException(String.format(INSTRUCTOR_NOT_FOUND_TEMPLATE, uuid)));

        existingInstructor.setName(instructorDTO.name());
        existingInstructor.setBio(instructorDTO.bio());

        Instructor updatedInstructor = instructorRepository.save(existingInstructor);
        return InstructorFactory.toDTO(updatedInstructor);
    }

    @Override
    public void deleteInstructor(UUID uuid) {
        if (!instructorRepository.existsByUuid(uuid)) {
            throw new RecordNotFoundException(String.format(INSTRUCTOR_NOT_FOUND_TEMPLATE, uuid));
        }
        instructorRepository.deleteByUuid(uuid);
    }

    @Override
    public Page<InstructorDTO> search(Map<String, String> searchParams, Pageable pageable) {
        Specification<Instructor> spec = specificationBuilder.buildSpecification(Instructor.class, searchParams);
        return instructorRepository.findAll(spec, pageable).map(InstructorFactory::toDTO);
    }
}
