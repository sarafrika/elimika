package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.common.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.common.util.GenericSpecificationBuilder;
import apps.sarafrika.elimika.course.dto.ProgramEnrollmentDTO;
import apps.sarafrika.elimika.course.dto.RubricScoringDTO;
import apps.sarafrika.elimika.course.factory.ProgramEnrollmentFactory;
import apps.sarafrika.elimika.course.factory.RubricScoringFactory;
import apps.sarafrika.elimika.course.model.ProgramEnrollment;
import apps.sarafrika.elimika.course.model.RubricScoring;
import apps.sarafrika.elimika.course.repository.ProgramEnrollmentRepository;
import apps.sarafrika.elimika.course.service.ProgramEnrollmentService;
import apps.sarafrika.elimika.course.util.enums.EnrollmentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ProgramEnrollmentServiceImpl implements ProgramEnrollmentService {

    private final ProgramEnrollmentRepository programEnrollmentRepository;
    private final GenericSpecificationBuilder<ProgramEnrollment> specificationBuilder;

    private static final String PROGRAM_ENROLLMENT_NOT_FOUND_TEMPLATE = "Program enrollment with ID %s not found";

    @Override
    public ProgramEnrollmentDTO createProgramEnrollment(ProgramEnrollmentDTO programEnrollmentDTO) {
        ProgramEnrollment programEnrollment = ProgramEnrollmentFactory.toEntity(programEnrollmentDTO);

        // Set defaults based on ProgramEnrollmentDTO business logic
        if (programEnrollment.getEnrollmentDate() == null) {
            programEnrollment.setEnrollmentDate(LocalDateTime.now());
        }
        if (programEnrollment.getStatus() == null) {
            programEnrollment.setStatus(EnrollmentStatus.ACTIVE);
        }
        if (programEnrollment.getProgressPercentage() == null) {
            programEnrollment.setProgressPercentage(BigDecimal.ZERO);
        }

        ProgramEnrollment savedProgramEnrollment = programEnrollmentRepository.save(programEnrollment);
        return ProgramEnrollmentFactory.toDTO(savedProgramEnrollment);
    }

    @Override
    @Transactional(readOnly = true)
    public ProgramEnrollmentDTO getProgramEnrollmentByUuid(UUID uuid) {
        return programEnrollmentRepository.findByUuid(uuid)
                .map(ProgramEnrollmentFactory::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(PROGRAM_ENROLLMENT_NOT_FOUND_TEMPLATE, uuid)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProgramEnrollmentDTO> getAllProgramEnrollments(Pageable pageable) {
        return programEnrollmentRepository.findAll(pageable).map(ProgramEnrollmentFactory::toDTO);
    }

    @Override
    public ProgramEnrollmentDTO updateProgramEnrollment(UUID uuid, ProgramEnrollmentDTO programEnrollmentDTO) {
        ProgramEnrollment existingProgramEnrollment = programEnrollmentRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(PROGRAM_ENROLLMENT_NOT_FOUND_TEMPLATE, uuid)));

        updateProgramEnrollmentFields(existingProgramEnrollment, programEnrollmentDTO);

        ProgramEnrollment updatedProgramEnrollment = programEnrollmentRepository.save(existingProgramEnrollment);
        return ProgramEnrollmentFactory.toDTO(updatedProgramEnrollment);
    }

    @Override
    public void deleteProgramEnrollment(UUID uuid) {
        if (!programEnrollmentRepository.existsByUuid(uuid)) {
            throw new ResourceNotFoundException(
                    String.format(PROGRAM_ENROLLMENT_NOT_FOUND_TEMPLATE, uuid));
        }
        programEnrollmentRepository.deleteByUuid(uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProgramEnrollmentDTO> search(Map<String, String> searchParams, Pageable pageable) {
        Specification<ProgramEnrollment> spec = specificationBuilder.buildSpecification(
                ProgramEnrollment.class, searchParams);
        return programEnrollmentRepository.findAll(spec, pageable).map(ProgramEnrollmentFactory::toDTO);
    }

    private void updateProgramEnrollmentFields(ProgramEnrollment existingProgramEnrollment, ProgramEnrollmentDTO dto) {
        if (dto.studentUuid() != null) {
            existingProgramEnrollment.setStudentUuid(dto.studentUuid());
        }
        if (dto.programUuid() != null) {
            existingProgramEnrollment.setProgramUuid(dto.programUuid());
        }
        if (dto.enrollmentDate() != null) {
            existingProgramEnrollment.setEnrollmentDate(dto.enrollmentDate());
        }
        if (dto.completionDate() != null) {
            existingProgramEnrollment.setCompletionDate(dto.completionDate());
        }
        if (dto.status() != null) {
            existingProgramEnrollment.setStatus(dto.status());
        }
        if (dto.progressPercentage() != null) {
            existingProgramEnrollment.setProgressPercentage(dto.progressPercentage());
        }
        if (dto.finalGrade() != null) {
            existingProgramEnrollment.setFinalGrade(dto.finalGrade());
        }
    }
}