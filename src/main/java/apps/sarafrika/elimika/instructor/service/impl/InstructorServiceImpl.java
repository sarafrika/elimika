package apps.sarafrika.elimika.instructor.service.impl;

import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import apps.sarafrika.elimika.shared.event.user.UserDomainMappingEvent;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import apps.sarafrika.elimika.instructor.dto.InstructorDTO;
import apps.sarafrika.elimika.instructor.factory.InstructorFactory;
import apps.sarafrika.elimika.instructor.model.Instructor;
import apps.sarafrika.elimika.instructor.repository.InstructorRepository;
import apps.sarafrika.elimika.instructor.service.InstructorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
@Slf4j
public class InstructorServiceImpl implements InstructorService {

    private final InstructorRepository instructorRepository;

    private final ApplicationEventPublisher applicationEventPublisher;

    private final GenericSpecificationBuilder<Instructor> specificationBuilder;

    private static final String INSTRUCTOR_NOT_FOUND_TEMPLATE = "Instructor with ID %s not found";

    @Override
    public InstructorDTO createInstructor(InstructorDTO instructorDTO) {
        Instructor instructor = InstructorFactory.toEntity(instructorDTO);

        instructor.setAdminVerified(false);

        Instructor savedInstructor = instructorRepository.save(instructor);

        applicationEventPublisher.publishEvent(
                new UserDomainMappingEvent(instructor.getUserUuid(), UserDomain.instructor.name())
        );

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

    // ================================
    // INSTRUCTOR VERIFICATION
    // ================================

    @Override
    public InstructorDTO verifyInstructor(UUID instructorUuid, String reason) {
        log.info("Verifying instructor {} for reason: {}", instructorUuid, reason);

        Instructor instructor = instructorRepository.findByUuid(instructorUuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(INSTRUCTOR_NOT_FOUND_TEMPLATE, instructorUuid)));

        instructor.setAdminVerified(true);
        Instructor verifiedInstructor = instructorRepository.save(instructor);

        log.info("Successfully verified instructor {}", instructorUuid);
        return InstructorFactory.toDTO(verifiedInstructor);
    }

    @Override
    public InstructorDTO unverifyInstructor(UUID instructorUuid, String reason) {
        log.info("Removing verification from instructor {} for reason: {}", instructorUuid, reason);

        Instructor instructor = instructorRepository.findByUuid(instructorUuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(INSTRUCTOR_NOT_FOUND_TEMPLATE, instructorUuid)));

        instructor.setAdminVerified(false);
        Instructor unverifiedInstructor = instructorRepository.save(instructor);

        log.info("Successfully removed verification from instructor {}", instructorUuid);
        return InstructorFactory.toDTO(unverifiedInstructor);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isInstructorVerified(UUID instructorUuid) {
        Instructor instructor = instructorRepository.findByUuid(instructorUuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(INSTRUCTOR_NOT_FOUND_TEMPLATE, instructorUuid)));

        return Boolean.TRUE.equals(instructor.getAdminVerified());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InstructorDTO> getVerifiedInstructors(Pageable pageable) {
        log.debug("Getting verified instructors with pagination: {}", pageable);
        return instructorRepository.findByAdminVerified(true, pageable)
                .map(InstructorFactory::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InstructorDTO> getUnverifiedInstructors(Pageable pageable) {
        log.debug("Getting unverified instructors with pagination: {}", pageable);
        return instructorRepository.findByAdminVerified(false, pageable)
                .map(InstructorFactory::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public long countInstructorsByVerificationStatus(boolean verified) {
        return instructorRepository.countByAdminVerified(verified);
    }

    // ================================
    // PRIVATE HELPER METHODS
    // ================================

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