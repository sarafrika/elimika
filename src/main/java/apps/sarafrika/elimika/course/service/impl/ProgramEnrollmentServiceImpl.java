package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import apps.sarafrika.elimika.course.internal.CourseEnrollmentSyncService;
import apps.sarafrika.elimika.course.dto.ProgramEnrollmentDTO;
import apps.sarafrika.elimika.course.factory.ProgramEnrollmentFactory;
import apps.sarafrika.elimika.course.internal.security.CourseFootingCap;
import apps.sarafrika.elimika.course.internal.security.CourseSecurityServiceImpl;
import apps.sarafrika.elimika.course.model.ProgramEnrollment;
import apps.sarafrika.elimika.course.model.TrainingProgram;
import apps.sarafrika.elimika.course.repository.ProgramEnrollmentRepository;
import apps.sarafrika.elimika.course.repository.TrainingProgramRepository;
import apps.sarafrika.elimika.course.service.ProgramEnrollmentService;
import apps.sarafrika.elimika.course.util.enums.CourseContentAccess;
import apps.sarafrika.elimika.course.util.enums.EnrollmentStatus;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ProgramEnrollmentServiceImpl implements ProgramEnrollmentService {

    private final ProgramEnrollmentRepository programEnrollmentRepository;
    private final CourseEnrollmentSyncService courseEnrollmentSyncService;
    private final GenericSpecificationBuilder<ProgramEnrollment> specificationBuilder;
    private final TrainingProgramRepository trainingProgramRepository;
    private final DomainSecurityService domainSecurityService;
    private final CourseSecurityServiceImpl courseSecurityService;
    private final CourseFootingCap courseFootingCap;

    private static final String PROGRAM_ENROLLMENT_NOT_FOUND_TEMPLATE = "Program enrollment with ID %s not found";

    @Override
    public ProgramEnrollmentDTO createProgramEnrollment(ProgramEnrollmentDTO programEnrollmentDTO) {
        enforceProgramApproval(programEnrollmentDTO.programUuid());
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
        courseEnrollmentSyncService.syncFromProgramEnrollment(
                savedProgramEnrollment.getStudentUuid(),
                savedProgramEnrollment.getProgramUuid(),
                savedProgramEnrollment.getStatus()
        );
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
        specificationBuilder.validateSortProperties(ProgramEnrollment.class, pageable);
        return programEnrollmentRepository.findAll(pageable).map(ProgramEnrollmentFactory::toDTO);
    }

    @Override
    public ProgramEnrollmentDTO updateProgramEnrollment(UUID uuid, ProgramEnrollmentDTO programEnrollmentDTO) {
        ProgramEnrollment existingProgramEnrollment = programEnrollmentRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(PROGRAM_ENROLLMENT_NOT_FOUND_TEMPLATE, uuid)));

        updateProgramEnrollmentFields(existingProgramEnrollment, programEnrollmentDTO);
        enforceProgramApproval(existingProgramEnrollment.getProgramUuid());

        ProgramEnrollment updatedProgramEnrollment = programEnrollmentRepository.save(existingProgramEnrollment);
        courseEnrollmentSyncService.syncFromProgramEnrollment(
                updatedProgramEnrollment.getStudentUuid(),
                updatedProgramEnrollment.getProgramUuid(),
                updatedProgramEnrollment.getStatus()
        );
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
        specificationBuilder.validateSortProperties(ProgramEnrollment.class, pageable);
        Specification<ProgramEnrollment> spec = specificationBuilder.buildSpecification(
                ProgramEnrollment.class, searchParams);
        return programEnrollmentRepository.findAll(spec, pageable).map(ProgramEnrollmentFactory::toDTO);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Mirrors {@code CourseEnrollmentServiceImpl#getCourseEnrollmentsForCaller}: the footings are
     * capped by the dashboard the caller is acting from, so an administrator browsing as a learner
     * gets a learner's view.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ProgramEnrollmentDTO> getProgramEnrollmentsForCaller(UUID programUuid, Pageable pageable) {
        Map<String, String> filters = new HashMap<>();
        filters.put("programUuid", programUuid.toString());

        if (readsEveryRoster() || readsRosterOf(programUuid)) {
            return search(filters, pageable);
        }

        UUID studentUuid = domainSecurityService.getCurrentStudentUuid();
        if (studentUuid != null && programEnrollmentRepository.existsByStudentUuidAndProgramUuidAndStatusIn(
                studentUuid, programUuid, List.of(EnrollmentStatus.values()))) {
            filters.put("studentUuid", studentUuid.toString());
            return search(filters, pageable);
        }

        return search(filters, pageable).map(ProgramEnrollmentServiceImpl::toEnrolmentTally);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProgramEnrollmentDTO> searchForCaller(Map<String, String> searchParams, Pageable pageable) {
        if (readsEveryRoster()) {
            return search(searchParams, pageable);
        }
        specificationBuilder.validateSortProperties(ProgramEnrollment.class, pageable);
        Specification<ProgramEnrollment> spec = specificationBuilder.buildSpecification(
                ProgramEnrollment.class, searchParams);
        Specification<ProgramEnrollment> scope = callerScope();
        spec = spec == null ? scope : spec.and(scope);
        return programEnrollmentRepository.findAll(spec, pageable).map(ProgramEnrollmentFactory::toDTO);
    }

    private boolean readsEveryRoster() {
        return courseFootingCap.permits(CourseContentAccess.ADMIN) && domainSecurityService.isPlatformAdmin();
    }

    private boolean readsRosterOf(UUID programUuid) {
        return courseFootingCap.permitsAnyStaffFooting() && courseSecurityService.canReadProgramRoster(programUuid);
    }

    /**
     * Rows of programs whose roster the caller may read, plus the caller's own rows as a learner.
     * A caller with neither sees nothing.
     */
    private Specification<ProgramEnrollment> callerScope() {
        Set<UUID> rosterPrograms = courseFootingCap.permitsAnyStaffFooting()
                ? courseSecurityService.rosterReadableProgramUuids()
                : Set.of();
        UUID studentUuid = domainSecurityService.getCurrentStudentUuid();
        return (root, query, cb) -> {
            List<Predicate> allowed = new ArrayList<>();
            if (!rosterPrograms.isEmpty()) {
                allowed.add(root.get("programUuid").in(rosterPrograms));
            }
            if (studentUuid != null) {
                allowed.add(cb.equal(root.get("studentUuid"), studentUuid));
            }
            return allowed.isEmpty() ? cb.disjunction() : cb.or(allowed.toArray(new Predicate[0]));
        };
    }

    /**
     * A row that still counts towards the page total but names nobody: the program and the status,
     * and nothing else.
     */
    private static ProgramEnrollmentDTO toEnrolmentTally(ProgramEnrollmentDTO enrollment) {
        return new ProgramEnrollmentDTO(
                null,
                null,
                enrollment.programUuid(),
                null,
                null,
                enrollment.status(),
                null,
                null,
                null,
                null,
                null,
                null
        );
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

    private void enforceProgramApproval(UUID programUuid) {
        if (programUuid == null) {
            throw new IllegalArgumentException("Program UUID is required for enrollment");
        }

        TrainingProgram program = trainingProgramRepository.findByUuid(programUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Training program with UUID " + programUuid + " not found"));

        if (!Boolean.TRUE.equals(program.getAdminApproved())) {
            throw new IllegalStateException(
                    "Training program " + programUuid + " is pending admin approval and cannot accept enrollments.");
        }
    }

}
