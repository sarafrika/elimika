package apps.sarafrika.elimika.instructor.service.impl;

import apps.sarafrika.elimika.common.exceptions.ResourceNotFoundException;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class InstructorServiceImpl implements InstructorService {

    private final InstructorRepository instructorRepository;
    private final GenericSpecificationBuilder<Instructor> specificationBuilder;

    private static final String INSTRUCTOR_NOT_FOUND_TEMPLATE = "Instructor with ID %s not found";

    @Override
    public InstructorDTO createInstructor(InstructorDTO instructorDTO) {
        Instructor instructor = InstructorFactory.toEntity(instructorDTO);

        Instructor savedInstructor = instructorRepository.save(instructor);
        return InstructorFactory.toDTO(savedInstructor);
    }

    @Override
    @Transactional(readOnly = true)
    public InstructorDTO getInstructorByUuid(UUID uuid) {
        return instructorRepository.findByUuid(uuid)
                .map(InstructorFactory::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(INSTRUCTOR_NOT_FOUND_TEMPLATE, uuid)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InstructorDTO> getAllInstructors(Pageable pageable) {
        return instructorRepository.findAll(pageable).map(InstructorFactory::toDTO);
    }

    @Override
    public InstructorDTO updateInstructor(UUID uuid, InstructorDTO instructorDTO) {
        Instructor existingInstructor = instructorRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(INSTRUCTOR_NOT_FOUND_TEMPLATE, uuid)));

        updateInstructorFields(existingInstructor, instructorDTO);

        Instructor updatedInstructor = instructorRepository.save(existingInstructor);
        return InstructorFactory.toDTO(updatedInstructor);
    }

    @Override
    public void deleteInstructor(UUID uuid) {
        Instructor instructor = instructorRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(INSTRUCTOR_NOT_FOUND_TEMPLATE, uuid)));

        instructorRepository.delete(instructor);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InstructorDTO> search(Map<String, String> searchParams, Pageable pageable) {
        Specification<Instructor> spec = specificationBuilder.buildSpecification(Instructor.class, searchParams);
        return instructorRepository.findAll(spec, pageable).map(InstructorFactory::toDTO);
    }

    /**
     * Updates the fields of the existing instructor entity with values from the DTO.
     * Only updates non-null values from the DTO to support partial updates.
     * Note: Read-only fields like uuid, createdDate, createdBy, fullName, verified, etc. are not updated.
     */
    private void updateInstructorFields(Instructor existingInstructor, InstructorDTO instructorDTO) {
        if (instructorDTO.latitude() != null) {
            existingInstructor.setLatitude(instructorDTO.latitude());
        }
        if (instructorDTO.longitude() != null) {
            existingInstructor.setLongitude(instructorDTO.longitude());
        }
        if (instructorDTO.website() != null) {
            existingInstructor.setWebsite(instructorDTO.website());
        }
        if (instructorDTO.bio() != null) {
            existingInstructor.setBio(instructorDTO.bio());
        }
        if (instructorDTO.professionalHeadline() != null) {
            existingInstructor.setProfessionalHeadline(instructorDTO.professionalHeadline());
        }
    }
}