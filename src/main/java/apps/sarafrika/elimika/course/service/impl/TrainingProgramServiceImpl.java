package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import apps.sarafrika.elimika.course.dto.CourseDTO;
import apps.sarafrika.elimika.course.dto.TrainingProgramDTO;
import apps.sarafrika.elimika.course.factory.CourseFactory;
import apps.sarafrika.elimika.course.factory.TrainingProgramFactory;
import apps.sarafrika.elimika.course.model.TrainingProgram;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.repository.ProgramCourseRepository;
import apps.sarafrika.elimika.course.repository.ProgramEnrollmentRepository;
import apps.sarafrika.elimika.course.repository.TrainingProgramRepository;
import apps.sarafrika.elimika.course.service.ContentModerationHistoryService;
import apps.sarafrika.elimika.course.service.TrainingProgramService;
import apps.sarafrika.elimika.course.spi.CourseSecuritySpi;
import apps.sarafrika.elimika.course.util.enums.ContentStatus;
import apps.sarafrika.elimika.course.util.enums.EnrollmentStatus;
import apps.sarafrika.elimika.course.util.enums.ModerationAction;
import apps.sarafrika.elimika.course.util.enums.ModerationContentType;
import apps.sarafrika.elimika.coursecreator.spi.CourseCreatorLookupService;
import apps.sarafrika.elimika.shared.event.notification.NotificationRequestedEvent;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TrainingProgramServiceImpl implements TrainingProgramService {

    private final TrainingProgramRepository trainingProgramRepository;
    private final GenericSpecificationBuilder<TrainingProgram> specificationBuilder;
    private final ProgramEnrollmentRepository programEnrollmentRepository;
    private final ProgramCourseRepository programCourseRepository;
    private final CourseRepository courseRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final CourseCreatorLookupService courseCreatorLookupService;
    private final ContentModerationHistoryService contentModerationHistoryService;
    private final DomainSecurityService domainSecurityService;
    private final CourseSecuritySpi courseSecurityService;

    private static final String PROGRAM_NOT_FOUND_TEMPLATE = "Training program with ID %s not found";

    @Override
    public TrainingProgramDTO createTrainingProgram(TrainingProgramDTO trainingProgramDTO) {
        TrainingProgram program = TrainingProgramFactory.toEntity(trainingProgramDTO);

        // A new program always starts as an unapproved draft. Lifecycle fields sent by the client
        // are ignored: publishing goes through publishProgram and approval through admin moderation.
        program.setStatus(ContentStatus.DRAFT);
        program.setIsPublished(false);
        program.setActive(false);
        program.setAdminApproved(false);

        TrainingProgram savedProgram = trainingProgramRepository.save(program);
        return TrainingProgramFactory.toDTO(savedProgram);
    }

    @Override
    @Transactional(readOnly = true)
    public TrainingProgramDTO getTrainingProgramByUuid(UUID uuid) {
        return trainingProgramRepository.findByUuid(uuid)
                .map(TrainingProgramFactory::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(PROGRAM_NOT_FOUND_TEMPLATE, uuid)));
    }

    @Override
    @Transactional(readOnly = true)
    public TrainingProgramDTO getVisibleTrainingProgramByUuid(UUID uuid) {
        return trainingProgramRepository.findByUuid(uuid)
                .filter(this::isVisibleToCaller)
                .map(TrainingProgramFactory::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(PROGRAM_NOT_FOUND_TEMPLATE, uuid)));
    }

    /**
     * The single-program counterpart of {@code CourseServiceImpl#isVisibleToCaller}. A live program
     * (published and admin-approved) and an archived one are readable by anyone: learners and
     * classes keep referencing a program after it is retired. A draft, in-review or not-yet-approved
     * program is readable only by a platform admin, its author, a learner enrolled on it (an
     * unpublished program keeps its enrolments) or someone approved to deliver it.
     */
    private boolean isVisibleToCaller(TrainingProgram program) {
        boolean live = program.getStatus() == ContentStatus.PUBLISHED
                && Boolean.TRUE.equals(program.getAdminApproved());
        if (live || program.getStatus() == ContentStatus.ARCHIVED) {
            return true;
        }
        if (domainSecurityService.isPlatformAdmin()
                || courseSecurityService.canReadProgramRoster(program.getUuid())) {
            return true;
        }
        UUID studentUuid = domainSecurityService.getCurrentStudentUuid();
        return studentUuid != null && programEnrollmentRepository.existsByStudentUuidAndProgramUuidAndStatusIn(
                studentUuid, program.getUuid(), EnrollmentStatus.ACCESS_ALLOWING);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TrainingProgramDTO> getAllTrainingPrograms(Pageable pageable) {
        specificationBuilder.validateSortProperties(TrainingProgram.class, pageable);
        return trainingProgramRepository.findAll(pageable).map(TrainingProgramFactory::toDTO);
    }

    @Override
    public TrainingProgramDTO updateTrainingProgram(UUID uuid, TrainingProgramDTO trainingProgramDTO) {
        TrainingProgram existingProgram = trainingProgramRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(PROGRAM_NOT_FOUND_TEMPLATE, uuid)));

        updateProgramFields(existingProgram, trainingProgramDTO);

        TrainingProgram updatedProgram = trainingProgramRepository.save(existingProgram);
        return TrainingProgramFactory.toDTO(updatedProgram);
    }

    @Override
    public void deleteTrainingProgram(UUID uuid) {
        if (!trainingProgramRepository.existsByUuid(uuid)) {
            throw new ResourceNotFoundException(
                    String.format(PROGRAM_NOT_FOUND_TEMPLATE, uuid));
        }
        trainingProgramRepository.deleteByUuid(uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TrainingProgramDTO> search(Map<String, String> searchParams, Pageable pageable) {
        specificationBuilder.validateSortProperties(TrainingProgram.class, pageable);
        Specification<TrainingProgram> spec = specificationBuilder.buildSpecification(
                TrainingProgram.class, searchParams);
        return trainingProgramRepository.findAll(spec, pageable).map(TrainingProgramFactory::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TrainingProgramDTO> searchForCaller(Map<String, String> searchParams, Pageable pageable) {
        specificationBuilder.validateSortProperties(TrainingProgram.class, pageable);
        Specification<TrainingProgram> spec = specificationBuilder.buildSpecification(
                TrainingProgram.class, searchParams);
        Specification<TrainingProgram> visible = visibleToCaller();
        if (visible != null) {
            spec = spec == null ? visible : spec.and(visible);
        }
        return spec == null
                ? trainingProgramRepository.findAll(pageable).map(TrainingProgramFactory::toDTO)
                : trainingProgramRepository.findAll(spec, pageable).map(TrainingProgramFactory::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TrainingProgramDTO> getFreeProgramsForCaller(Pageable pageable) {
        specificationBuilder.validateSortProperties(TrainingProgram.class, pageable);
        Specification<TrainingProgram> visible = visibleToCaller();
        Specification<TrainingProgram> spec = visible == null ? isFree() : isFree().and(visible);
        return trainingProgramRepository.findAll(spec, pageable).map(TrainingProgramFactory::toDTO);
    }

    /**
     * The programs the current caller may discover, or {@code null} for a platform admin, who is
     * unrestricted.
     * <p>
     * A program is live once it is published, active and approved by an admin - the same state
     * {@code GET /programs/published} and the admin pending queue key off. Its author - on either
     * the course-creator or the legacy instructor identity, as {@code isProgramOwner} accepts - also
     * sees their own drafts.
     */
    private Specification<TrainingProgram> visibleToCaller() {
        if (domainSecurityService.isPlatformAdmin()) {
            return null;
        }
        Set<UUID> ownIdentities = new HashSet<>();
        UUID courseCreatorUuid = domainSecurityService.getCurrentCourseCreatorUuid();
        if (courseCreatorUuid != null) {
            ownIdentities.add(courseCreatorUuid);
        }
        UUID instructorUuid = domainSecurityService.getCurrentInstructorUuid();
        if (instructorUuid != null) {
            ownIdentities.add(instructorUuid);
        }
        return (root, query, cb) -> {
            var live = cb.and(
                    cb.isTrue(root.get("adminApproved")),
                    cb.isTrue(root.get("active")),
                    cb.equal(root.get("status"), ContentStatus.PUBLISHED));
            return ownIdentities.isEmpty()
                    ? live
                    : cb.or(live, root.get("courseCreatorUuid").in(ownIdentities));
        };
    }

    /**
     * A program is free when it carries no price or a zero price.
     */
    private static Specification<TrainingProgram> isFree() {
        return (root, query, cb) -> cb.or(
                cb.isNull(root.get("price")),
                cb.equal(root.get("price"), BigDecimal.ZERO));
    }

    // Domain-specific methods leveraging TrainingProgramDTO computed properties
    @Transactional(readOnly = true)
    public List<TrainingProgramDTO> getActivePrograms() {
        return trainingProgramRepository.findByActiveTrue()
                .stream()
                .map(TrainingProgramFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TrainingProgramDTO> getProgramsByCategory(UUID categoryUuid) {
        return trainingProgramRepository.findByCategoryUuid(categoryUuid)
                .stream()
                .map(TrainingProgramFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TrainingProgramDTO> getProgramsByCourseCreator(UUID courseCreatorUuid) {
        return trainingProgramRepository.findByCourseCreatorUuid(courseCreatorUuid)
                .stream()
                .map(TrainingProgramFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TrainingProgramDTO> getPublishedPrograms() {
        return trainingProgramRepository.findByStatus(ContentStatus.PUBLISHED)
                .stream()
                .map(TrainingProgramFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public double getProgramCompletionRate(UUID programUuid) {
        long totalEnrollments = programEnrollmentRepository.countByProgramUuid(programUuid);
        long completedEnrollments = programEnrollmentRepository
                .countByProgramUuidAndStatus(programUuid, EnrollmentStatus.COMPLETED);

        return totalEnrollments > 0 ? (double) completedEnrollments / totalEnrollments * 100 : 0.0;
    }

    @Transactional(readOnly = true)
    public boolean isProgramComplete(UUID studentUuid, UUID programUuid) {
        return programEnrollmentRepository.existsByStudentUuidAndProgramUuidAndStatus(
                studentUuid, programUuid, EnrollmentStatus.COMPLETED);
    }

    @Transactional(readOnly = true)
    public List<CourseDTO> getRequiredCourses(UUID programUuid) {
        return programCourseRepository.findByProgramUuidAndIsRequiredTrue(programUuid)
                .stream()
                .map(pc -> courseRepository.findByUuid(pc.getCourseUuid()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(CourseFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CourseDTO> getOptionalCourses(UUID programUuid) {
        return programCourseRepository.findByProgramUuidAndIsRequiredFalse(programUuid)
                .stream()
                .map(pc -> courseRepository.findByUuid(pc.getCourseUuid()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(CourseFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CourseDTO> getAllProgramCourses(UUID programUuid) {
        return programCourseRepository.findByProgramUuidOrderBySequenceOrderAsc(programUuid)
                .stream()
                .map(pc -> courseRepository.findByUuid(pc.getCourseUuid()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(CourseFactory::toDTO)
                .collect(Collectors.toList());
    }

    // Leveraging TrainingProgramDTO computed properties for analytics
    @Transactional(readOnly = true)
    public List<TrainingProgramDTO> getProgramsByType(String programType) {
        return trainingProgramRepository.findAll()
                .stream()
                .map(TrainingProgramFactory::toDTO)
                .filter(program -> programType.equals(program.getProgramType()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TrainingProgramDTO> getFreePrograms() {
        return trainingProgramRepository.findByPriceIsNullOrPrice(BigDecimal.ZERO)
                .stream()
                .map(TrainingProgramFactory::toDTO)
                .filter(dto -> dto.price() == null || BigDecimal.ZERO.compareTo(dto.price()) >= 0)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TrainingProgramDTO> getExtendedPrograms() {
        // Programs with 100+ hours (using computed property logic)
        return trainingProgramRepository.findByTotalDurationHoursGreaterThanEqual(100)
                .stream()
                .map(TrainingProgramFactory::toDTO)
                .filter(program -> "Extended Program".equals(program.getProgramType()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TrainingProgramDTO> getIntensivePrograms() {
        // Programs with 50-99 hours
        return trainingProgramRepository.findByTotalDurationHoursBetween(50, 99)
                .stream()
                .map(TrainingProgramFactory::toDTO)
                .filter(program -> "Intensive Program".equals(program.getProgramType()))
                .collect(Collectors.toList());
    }

    /**
     * Publishes a program the way {@code CourseServiceImpl#publishCourse} publishes a course: it
     * becomes PUBLISHED and active. Admin approval is untouched, so an unapproved program lands in
     * the admin pending queue ({@code admin_approved = false}, status PUBLISHED) and goes live once
     * approved.
     */
    @Override
    public TrainingProgramDTO publishProgram(UUID programUuid) {
        TrainingProgram program = findProgram(programUuid);

        program.setStatus(ContentStatus.PUBLISHED);
        program.setIsPublished(true);
        program.setActive(true);

        return TrainingProgramFactory.toDTO(trainingProgramRepository.save(program));
    }

    /**
     * Returns a program to draft. As with courses, it stays active while learners are still
     * enrolled so their access is not cut off, but it leaves the catalogue.
     */
    @Override
    public TrainingProgramDTO unpublishProgram(UUID programUuid) {
        TrainingProgram program = findProgram(programUuid);

        program.setStatus(ContentStatus.DRAFT);
        program.setIsPublished(false);
        program.setActive(programEnrollmentRepository
                .countByProgramUuidAndStatus(programUuid, EnrollmentStatus.ACTIVE) > 0);

        return TrainingProgramFactory.toDTO(trainingProgramRepository.save(program));
    }

    @Override
    public TrainingProgramDTO archiveProgram(UUID programUuid) {
        TrainingProgram program = findProgram(programUuid);

        program.setStatus(ContentStatus.ARCHIVED);
        program.setIsPublished(false);
        program.setActive(false);

        return TrainingProgramFactory.toDTO(trainingProgramRepository.save(program));
    }

    private TrainingProgram findProgram(UUID programUuid) {
        return trainingProgramRepository.findByUuid(programUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(PROGRAM_NOT_FOUND_TEMPLATE, programUuid)));
    }

    @Override
    public TrainingProgramDTO approveProgram(UUID programUuid, String reason) {
        TrainingProgram program = trainingProgramRepository.findByUuid(programUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(PROGRAM_NOT_FOUND_TEMPLATE, programUuid)));

        if (!Boolean.TRUE.equals(program.getAdminApproved())) {
            program.setAdminApproved(true);
            trainingProgramRepository.save(program);
            contentModerationHistoryService.record(ModerationContentType.TRAINING_PROGRAM, programUuid,
                    ModerationAction.APPROVED, reason);
            publishProgramModerationNotification(program, true, reason);
        }

        return TrainingProgramFactory.toDTO(program);
    }

    @Override
    public TrainingProgramDTO unapproveProgram(UUID programUuid, String reason, ModerationAction action) {
        TrainingProgram program = trainingProgramRepository.findByUuid(programUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(PROGRAM_NOT_FOUND_TEMPLATE, programUuid)));

        boolean wasApproved = !Boolean.FALSE.equals(program.getAdminApproved());
        if (wasApproved) {
            program.setAdminApproved(false);
            trainingProgramRepository.save(program);
        }

        // A rejection of a still-pending program changes no state but must still be recorded and communicated
        if (wasApproved || action == ModerationAction.REJECTED) {
            contentModerationHistoryService.record(ModerationContentType.TRAINING_PROGRAM, programUuid, action, reason);
            publishProgramModerationNotification(program, false, reason);
        }

        return TrainingProgramFactory.toDTO(program);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isProgramApproved(UUID programUuid) {
        TrainingProgram program = trainingProgramRepository.findByUuid(programUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(PROGRAM_NOT_FOUND_TEMPLATE, programUuid)));
        return Boolean.TRUE.equals(program.getAdminApproved());
    }

    @Transactional(readOnly = true)
    public boolean isProgramReadyForPublishing(UUID programUuid) {
        TrainingProgramDTO program = getTrainingProgramByUuid(programUuid);

        // Business logic: Program needs courses and basic info
        return program.title() != null &&
                program.description() != null &&
                programCourseRepository.countByProgramUuid(programUuid) > 0;
    }

    @Transactional(readOnly = true)
    public int getTotalProgramCourses(UUID programUuid) {
        return (int) programCourseRepository.countByProgramUuid(programUuid);
    }

    @Transactional(readOnly = true)
    public int getTotalRequiredCourses(UUID programUuid) {
        return (int) programCourseRepository.countByProgramUuidAndIsRequiredTrue(programUuid);
    }

    private void updateProgramFields(TrainingProgram existingProgram, TrainingProgramDTO dto) {
        if (dto.title() != null) {
            existingProgram.setTitle(dto.title());
        }
        // course_creator_uuid is deliberately not settable: ownership never changes through an update.
        if (dto.categoryUuid() != null) {
            existingProgram.setCategoryUuid(dto.categoryUuid());
        }
        if (dto.description() != null) {
            existingProgram.setDescription(dto.description());
        }
        if (dto.objectives() != null) {
            existingProgram.setObjectives(dto.objectives());
        }
        if (dto.prerequisites() != null) {
            existingProgram.setPrerequisites(dto.prerequisites());
        }
        if (dto.totalDurationHours() != null) {
            existingProgram.setTotalDurationHours(dto.totalDurationHours());
        }
        if (dto.totalDurationMinutes() != null) {
            existingProgram.setTotalDurationMinutes(dto.totalDurationMinutes());
        }
        if (dto.classLimit() != null) {
            existingProgram.setClassLimit(dto.classLimit());
        }
        if (dto.price() != null) {
            existingProgram.setPrice(dto.price());
        }
        // status, published and active are deliberately not settable here. Lifecycle changes go
        // through publishProgram/unpublishProgram/archiveProgram, as they do for courses.
    }

    private void publishProgramModerationNotification(TrainingProgram program, boolean approved, String reason) {
        if (program.getCourseCreatorUuid() == null) {
            return;
        }
        UUID recipientUserUuid = courseCreatorLookupService.getCourseCreatorUserUuid(program.getCourseCreatorUuid())
                .orElse(null);
        if (recipientUserUuid == null) {
            return;
        }
        String type = approved ? "PROGRAM_CONTENT_APPROVED" : "PROGRAM_CONTENT_REJECTED";
        String programTitle = program.getTitle() == null ? "Your program" : program.getTitle();
        String body = approved
                ? programTitle + " has been approved by admin."
                : programTitle + " was rejected by admin.";
        eventPublisher.publishEvent(NotificationRequestedEvent.inApp(
                recipientUserUuid,
                type,
                "INBOX",
                approved ? "Program approved" : "Program rejected",
                body,
                "/dashboard/programs/" + program.getUuid(),
                Map.of(
                        "program_uuid", program.getUuid(),
                        "program_title", programTitle,
                        "reason", reason == null ? "" : reason
                ),
                "program-moderation:" + program.getUuid() + ":" + type
        ));
    }
}
