package apps.sarafrika.elimika.timetabling.service.impl;

import apps.sarafrika.elimika.course.spi.CourseInfoService;
import apps.sarafrika.elimika.timetabling.spi.ClassEnrolmentEligibilityDTO;
import apps.sarafrika.elimika.course.spi.LearnerCourseProgressView;
import apps.sarafrika.elimika.course.spi.LearnerProgressLookupService;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.resourcing.spi.ResourceBookingService;
import apps.sarafrika.elimika.shared.event.notification.NotificationRequestedEvent;
import apps.sarafrika.elimika.shared.event.timetabling.ClassSessionCompletedEvent;
import apps.sarafrika.elimika.shared.exceptions.DuplicateResourceException;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.timetabling.security.TimetableSecurityService;
import apps.sarafrika.elimika.shared.service.AgeVerificationService;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import apps.sarafrika.elimika.student.spi.StudentLookupService;
import apps.sarafrika.elimika.commerce.spi.paywall.CommercePaywallService;
import apps.sarafrika.elimika.availability.spi.AvailabilityService;
import apps.sarafrika.elimika.timetabling.dto.ClassScheduledEventDTO;
import apps.sarafrika.elimika.timetabling.spi.EnrollmentDTO;
import apps.sarafrika.elimika.timetabling.spi.EnrollmentRequestDTO;
import apps.sarafrika.elimika.shared.spi.enrollment.AttendanceMarkedEventDTO;
import apps.sarafrika.elimika.shared.spi.enrollment.EnrollmentStatusChangedEventDTO;
import apps.sarafrika.elimika.shared.spi.enrollment.StudentEnrolledEventDTO;
import apps.sarafrika.elimika.timetabling.spi.StudentCourseEnrollmentSummaryDTO;
import apps.sarafrika.elimika.timetabling.spi.StudentClassEnrollmentSummaryDTO;
import apps.sarafrika.elimika.timetabling.spi.StudentScheduleDTO;
import apps.sarafrika.elimika.timetabling.spi.ScheduledInstanceDTO;
import apps.sarafrika.elimika.timetabling.spi.ScheduledInstanceRescheduleRequestDTO;
import apps.sarafrika.elimika.timetabling.spi.ScheduleRequestDTO;
import apps.sarafrika.elimika.timetabling.factory.EnrollmentFactory;
import apps.sarafrika.elimika.timetabling.factory.ScheduledInstanceFactory;
import apps.sarafrika.elimika.timetabling.factory.StudentScheduleFactory;
import apps.sarafrika.elimika.timetabling.internal.SchedulingEventListener.ScheduledInstanceCompletedEvent;
import apps.sarafrika.elimika.timetabling.internal.SchedulingEventListener.ScheduledInstanceStartedEvent;
import apps.sarafrika.elimika.timetabling.model.Enrollment;
import apps.sarafrika.elimika.timetabling.model.ScheduledInstance;
import apps.sarafrika.elimika.timetabling.repository.EnrollmentRepository;
import apps.sarafrika.elimika.timetabling.repository.ScheduledInstanceRepository;
import apps.sarafrika.elimika.timetabling.spi.TimetableService;
import apps.sarafrika.elimika.timetabling.spi.EnrollmentStatus;
import apps.sarafrika.elimika.timetabling.spi.EnrolmentTrendPointDTO;
import apps.sarafrika.elimika.timetabling.spi.TodayGrowthPointDTO;
import apps.sarafrika.elimika.timetabling.spi.WeeklyGrowthPointDTO;
import apps.sarafrika.elimika.timetabling.spi.OrganisationActivityEventDTO;
import apps.sarafrika.elimika.timetabling.spi.ClassEnrolmentCountDTO;
import apps.sarafrika.elimika.timetabling.spi.OrganisationStudentPerformanceDTO;
import apps.sarafrika.elimika.timetabling.spi.StudentEnrolmentSummaryDTO;
import apps.sarafrika.elimika.timetabling.spi.SchedulingStatus;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TimetableServiceImpl implements TimetableService {

    private final ScheduledInstanceRepository scheduledInstanceRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final GenericSpecificationBuilder<ScheduledInstance> scheduledInstanceSpecBuilder;
    private final GenericSpecificationBuilder<Enrollment> enrollmentSpecBuilder;
    private final ClassDefinitionLookupService classDefinitionLookupService;
    private final CourseInfoService courseInfoService;
    private final LearnerProgressLookupService learnerProgressLookupService;
    private final AgeVerificationService ageVerificationService;
    private final CommercePaywallService commercePaywallService;
    private final AvailabilityService availabilityService;
    private final StudentLookupService studentLookupService;
    private final apps.sarafrika.elimika.tenancy.spi.UserLookupService userLookupService;
    private final apps.sarafrika.elimika.tenancy.spi.OrganisationLookupService organisationLookupService;
    private final apps.sarafrika.elimika.tenancy.spi.OrganisationAffiliationService organisationAffiliationService;
    private final InstructorLookupService instructorLookupService;
    private final ResourceBookingService resourceBookingService;
    private final DomainSecurityService domainSecurityService;
    private final TimetableSecurityService timetableSecurityService;

    private static final String SCHEDULED_INSTANCE_NOT_FOUND_TEMPLATE = "Scheduled instance with UUID %s not found";
    private static final String ENROLLMENT_NOT_FOUND_TEMPLATE = "Enrollment with UUID %s not found";
    private static final Set<EnrollmentStatus> START_ELIGIBLE_ENROLLMENT_STATUSES = Set.of(
            EnrollmentStatus.ENROLLED,
            EnrollmentStatus.ATTENDED,
            EnrollmentStatus.ABSENT
    );

    // ===== Scheduling Operations =====

    @Override
    public ScheduledInstanceDTO scheduleClass(ScheduleRequestDTO request) {
        log.debug("Scheduling class for instructor: {} at time: {}", request.instructorUuid(), request.startTime());
        
        validateScheduleRequest(request);
        List<String> conflicts = resolveInstructorConflicts(request.instructorUuid(), request);
        if (!conflicts.isEmpty()) {
            throw new IllegalArgumentException(String.join("; ", conflicts));
        }
        
        ScheduledInstance entity = ScheduledInstanceFactory.toEntity(request);
        
        Optional<ClassDefinitionLookupService.ClassDefinitionSnapshot> classSnapshot =
                classDefinitionLookupService.findByUuid(request.classDefinitionUuid());
        entity.setTitle(classSnapshot
                .map(ClassDefinitionLookupService.ClassDefinitionSnapshot::title)
                .filter(title -> title != null && !title.isBlank())
                .orElse("Class: " + request.classDefinitionUuid().toString().substring(0, 8)));
        entity.setLocationType(classSnapshot
                .map(ClassDefinitionLookupService.ClassDefinitionSnapshot::locationType)
                .map(Enum::name)
                .orElse("ONLINE"));
        entity.setMaxParticipants(classSnapshot
                .map(ClassDefinitionLookupService.ClassDefinitionSnapshot::maxParticipants)
                .orElse(25));
        
        ScheduledInstance savedEntity = scheduledInstanceRepository.save(entity);
        
        // Publish ClassScheduled event
        ClassScheduledEventDTO event = new ClassScheduledEventDTO(
                savedEntity.getUuid(),
                savedEntity.getClassDefinitionUuid(),
                savedEntity.getInstructorUuid(),
                savedEntity.getStartTime(),
                savedEntity.getEndTime(),
                savedEntity.getTitle(),
                savedEntity.getLocationType(),
                savedEntity.getMaxParticipants()
        );
        eventPublisher.publishEvent(event);
        
        log.debug("Scheduled class with UUID: {}", savedEntity.getUuid());
        return ScheduledInstanceFactory.toDTO(savedEntity);
    }

    @Override
    public void cancelScheduledInstance(UUID instanceUuid, String reason) {
        log.debug("Cancelling scheduled instance: {} with reason: {}", instanceUuid, reason);
        
        if (instanceUuid == null) {
            throw new IllegalArgumentException("Instance UUID cannot be null");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Cancellation reason cannot be null or empty");
        }

        ScheduledInstance entity = scheduledInstanceRepository.findByUuid(instanceUuid)
            .orElseThrow(() -> new ResourceNotFoundException(
                String.format(SCHEDULED_INSTANCE_NOT_FOUND_TEMPLATE, instanceUuid)));

        if (!SchedulingStatus.SCHEDULED.equals(entity.getStatus()) && 
            !SchedulingStatus.ONGOING.equals(entity.getStatus())) {
            throw new IllegalArgumentException("Only scheduled or ongoing instances can be cancelled");
        }

        entity.setStatus(SchedulingStatus.CANCELLED);
        entity.setCancellationReason(reason.trim());
        scheduledInstanceRepository.save(entity);

        resourceBookingService.releaseBookingsForInstance(instanceUuid, reason.trim());

        // Cancel all active enrollments for this instance
        List<Enrollment> activeEnrollments = enrollmentRepository.findByScheduledInstanceUuidAndStatus(
            instanceUuid, EnrollmentStatus.ENROLLED);
        
        activeEnrollments.forEach(enrollment -> {
            enrollment.setStatus(EnrollmentStatus.CANCELLED);
            Enrollment savedEnrollment = enrollmentRepository.save(enrollment);
            publishEnrollmentStatusChanged(savedEnrollment, entity);
        });

        log.debug("Cancelled scheduled instance: {} and {} enrollments", instanceUuid, activeEnrollments.size());
    }

    @Override
    public void updateScheduledInstanceStatus(UUID instanceUuid, String newStatus) {
        log.debug("Updating status of scheduled instance: {} to: {}", instanceUuid, newStatus);
        
        if (instanceUuid == null) {
            throw new IllegalArgumentException("Instance UUID cannot be null");
        }
        if (newStatus == null || newStatus.trim().isEmpty()) {
            throw new IllegalArgumentException("New status cannot be null or empty");
        }

        ScheduledInstance entity = scheduledInstanceRepository.findByUuid(instanceUuid)
            .orElseThrow(() -> new ResourceNotFoundException(
                String.format(SCHEDULED_INSTANCE_NOT_FOUND_TEMPLATE, instanceUuid)));

        SchedulingStatus previousStatus = entity.getStatus();
        try {
            SchedulingStatus status = SchedulingStatus.fromValue(newStatus);
            entity.setStatus(status);
            ScheduledInstance saved = scheduledInstanceRepository.save(entity);

            // This route can drive an instance straight to COMPLETED without going through
            // endScheduledInstance, and used to do so silently. A session delivered this way earns
            // the instructor the same as any other, so it has to announce itself too — guarded on
            // the transition so re-asserting COMPLETED does not republish.
            if (SchedulingStatus.COMPLETED.equals(status) && !SchedulingStatus.COMPLETED.equals(previousStatus)) {
                publishSessionCompleted(saved, currentUtcTime());
            }

            log.debug("Updated status of scheduled instance: {} to: {}", instanceUuid, newStatus);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid scheduling status: " + newStatus);
        }
    }

    /**
     * Announces a delivered session on the shared channel, where modules outside timetabling — the
     * payout ledger in particular — can hear it. Every path that lands an instance on
     * {@code COMPLETED} goes through here, because an obligation that depends on which button was
     * pressed is not an obligation.
     */
    private void publishSessionCompleted(ScheduledInstance instance, LocalDateTime completedAt) {
        eventPublisher.publishEvent(new ClassSessionCompletedEvent(
                instance.getUuid(),
                instance.getClassDefinitionUuid(),
                instance.getInstructorUuid(),
                completedAt == null ? currentUtcTime() : completedAt,
                durationMinutes(instance.getStartTime(), instance.getEndTime())
        ));
    }

    @Override
    public ScheduledInstanceDTO rescheduleScheduledInstance(UUID instanceUuid,
                                                           ScheduledInstanceRescheduleRequestDTO request) {
        log.debug("Rescheduling scheduled instance: {}", instanceUuid);

        if (request == null) {
            throw new IllegalArgumentException("Reschedule request cannot be null");
        }

        ScheduledInstance entity = findScheduledInstanceOrThrow(instanceUuid);
        if (!SchedulingStatus.SCHEDULED.equals(entity.getStatus())) {
            throw new IllegalArgumentException("Only scheduled instances can be rescheduled");
        }

        String timezone = request.timezone() == null || request.timezone().isBlank()
                ? Optional.ofNullable(entity.getTimezone()).orElse("UTC")
                : request.timezone().trim();
        ScheduleRequestDTO scheduleRequest = new ScheduleRequestDTO(
                entity.getClassDefinitionUuid(),
                entity.getInstructorUuid(),
                request.startTime(),
                request.endTime(),
                timezone
        );

        validateScheduleRequest(scheduleRequest);
        List<String> conflicts = resolveInstructorConflicts(
                entity.getInstructorUuid(),
                scheduleRequest,
                entity.getUuid());
        if (!conflicts.isEmpty()) {
            throw new IllegalArgumentException(String.join("; ", conflicts));
        }

        resourceBookingService.rescheduleInstanceBookings(entity.getUuid(), request.startTime(), request.endTime());

        entity.setStartTime(request.startTime());
        entity.setEndTime(request.endTime());
        entity.setTimezone(timezone);

        ScheduledInstance savedEntity = scheduledInstanceRepository.save(entity);
        log.debug("Rescheduled instance {} to {} - {}", instanceUuid, request.startTime(), request.endTime());
        return ScheduledInstanceFactory.toDTO(savedEntity);
    }

    @Override
    public ScheduledInstanceDTO startScheduledInstance(UUID instanceUuid) {
        log.debug("Starting scheduled instance: {}", instanceUuid);

        ScheduledInstance entity = findScheduledInstanceOrThrow(instanceUuid);
        SchedulingStatus currentStatus = entity.getStatus();

        if (entity.getConcludedAt() != null || SchedulingStatus.COMPLETED.equals(currentStatus)) {
            throw new IllegalArgumentException("Completed scheduled instances cannot be started");
        }
        if (SchedulingStatus.CANCELLED.equals(currentStatus) || SchedulingStatus.BLOCKED.equals(currentStatus)) {
            throw new IllegalArgumentException("Cancelled or blocked scheduled instances cannot be started");
        }
        if (SchedulingStatus.ONGOING.equals(currentStatus) && entity.getStartedAt() != null) {
            return ScheduledInstanceFactory.toDTO(entity);
        }
        if (!SchedulingStatus.SCHEDULED.equals(currentStatus) && !SchedulingStatus.ONGOING.equals(currentStatus)) {
            throw new IllegalArgumentException("Only scheduled instances can be started");
        }
        if (SchedulingStatus.SCHEDULED.equals(currentStatus)
                && countStartEligibleEnrollments(instanceUuid) <= 0) {
            throw new IllegalArgumentException("At least one enrolled student is required to start this class");
        }

        LocalDateTime transitionTime = currentUtcTime();
        entity.setStatus(SchedulingStatus.ONGOING);
        if (entity.getStartedAt() == null) {
            entity.setStartedAt(transitionTime);
        }

        ScheduledInstance savedEntity = scheduledInstanceRepository.save(entity);
        eventPublisher.publishEvent(new ScheduledInstanceStartedEvent(
                savedEntity.getUuid(),
                savedEntity.getClassDefinitionUuid(),
                savedEntity.getInstructorUuid(),
                savedEntity.getTitle(),
                savedEntity.getStartTime(),
                savedEntity.getEndTime(),
                savedEntity.getStartedAt()
        ));

        log.debug("Started scheduled instance: {}", instanceUuid);
        return ScheduledInstanceFactory.toDTO(savedEntity);
    }

    @Override
    public ScheduledInstanceDTO endScheduledInstance(UUID instanceUuid) {
        log.debug("Ending scheduled instance: {}", instanceUuid);

        ScheduledInstance entity = findScheduledInstanceOrThrow(instanceUuid);
        SchedulingStatus currentStatus = entity.getStatus();

        if (entity.getConcludedAt() != null) {
            return ScheduledInstanceFactory.toDTO(entity);
        }
        if (entity.getStartedAt() == null) {
            throw new IllegalArgumentException("Scheduled instance must be started before it can be ended");
        }
        if (SchedulingStatus.CANCELLED.equals(currentStatus) || SchedulingStatus.BLOCKED.equals(currentStatus)) {
            throw new IllegalArgumentException("Cancelled or blocked scheduled instances cannot be ended");
        }
        if (!SchedulingStatus.ONGOING.equals(currentStatus) && !SchedulingStatus.COMPLETED.equals(currentStatus)) {
            throw new IllegalArgumentException("Only ongoing scheduled instances can be ended");
        }

        LocalDateTime transitionTime = currentUtcTime();
        entity.setStatus(SchedulingStatus.COMPLETED);
        entity.setConcludedAt(transitionTime);

        ScheduledInstance savedEntity = scheduledInstanceRepository.save(entity);
        eventPublisher.publishEvent(new ScheduledInstanceCompletedEvent(
                savedEntity.getUuid(),
                savedEntity.getClassDefinitionUuid(),
                savedEntity.getInstructorUuid(),
                savedEntity.getTitle(),
                savedEntity.getStartTime(),
                savedEntity.getEndTime(),
                savedEntity.getConcludedAt()
        ));
        publishSessionCompleted(savedEntity, savedEntity.getConcludedAt());

        log.debug("Ended scheduled instance: {}", instanceUuid);
        return ScheduledInstanceFactory.toDTO(savedEntity);
    }

    // ===== Enrollment Operations =====

    @Override
    public List<EnrollmentDTO> enrollStudent(EnrollmentRequestDTO request) {
        log.debug("Enrolling student: {} into class definition: {}", request.studentUuid(), request.classDefinitionUuid());

        validateEnrollmentRequest(request);

        UUID classDefinitionUuid = request.classDefinitionUuid();
        UUID studentUuid = request.studentUuid();

        enforceClassContentApproval(classDefinitionUuid);
        enforceClassAgeLimits(studentUuid, classDefinitionUuid);
        commercePaywallService.verifyClassEnrollmentAccess(studentUuid, classDefinitionUuid);

        List<ScheduledInstance> scheduledInstances = scheduledInstanceRepository.findByClassDefinitionUuid(classDefinitionUuid);

        if (scheduledInstances.isEmpty()) {
            throw new ResourceNotFoundException(String.format("No scheduled instances found for class definition with UUID %s", classDefinitionUuid));
        }

        List<Enrollment> existingStudentEnrollments =
                Optional.ofNullable(enrollmentRepository.findByStudentUuid(studentUuid)).orElse(List.of());
        Map<UUID, Enrollment> existingEnrollmentByInstance = existingStudentEnrollments.stream()
                .filter(enrollment -> enrollment.getScheduledInstanceUuid() != null)
                .collect(Collectors.toMap(
                        Enrollment::getScheduledInstanceUuid,
                        enrollment -> enrollment,
                        (first, second) -> second));

        // A seat held at checkout is this student's own reservation, not a duplicate enrolment, so
        // it must be promoted rather than skipped.
        Set<UUID> alreadyEnrolledInstanceUuids = existingStudentEnrollments.stream()
                .filter(this::isStandingEnrollment)
                .map(Enrollment::getScheduledInstanceUuid)
                .collect(Collectors.toSet());

        List<ScheduledInstance> instancesToEnroll = scheduledInstances.stream()
                .filter(instance -> !alreadyEnrolledInstanceUuids.contains(instance.getUuid()))
                .toList();

        if (instancesToEnroll.isEmpty()) {
            throw new DuplicateResourceException("Student is already enrolled in all scheduled instances for this class");
        }

        // Validate constraints before persisting any enrollment
        for (ScheduledInstance instance : instancesToEnroll) {
            Enrollment existing = existingEnrollmentByInstance.get(instance.getUuid());
            boolean ownsActiveHold = existing != null
                    && existing.getStatus() == EnrollmentStatus.RESERVED
                    && !hasLapsed(existing, currentUtcTime());

            if (!ownsActiveHold && !hasCapacityForEnrollment(instance.getUuid())) {
                throw new IllegalArgumentException(
                        String.format("Scheduled instance %s has reached maximum capacity", instance.getUuid()));
            }

            if (hasStudentConflict(studentUuid, toScheduleRequest(instance), Set.of(instance.getUuid()))) {
                throw new IllegalArgumentException(
                        String.format("Student has a scheduling conflict with instance %s starting at %s",
                                instance.getUuid(), instance.getStartTime()));
            }
        }

        List<Enrollment> createdEnrollments = new java.util.ArrayList<>();

        for (ScheduledInstance instance : instancesToEnroll) {
            Enrollment entity = existingEnrollmentByInstance.get(instance.getUuid());
            if (entity == null) {
                entity = EnrollmentFactory.toEntity(instance.getUuid(), studentUuid);
            } else {
                entity.setStatus(EnrollmentStatus.ENROLLED);
                entity.setReservedUntil(null);
            }
            Enrollment savedEntity = enrollmentRepository.save(entity);

            StudentEnrolledEventDTO event = new StudentEnrolledEventDTO(
                    savedEntity.getUuid(),
                    savedEntity.getScheduledInstanceUuid(),
                    savedEntity.getStudentUuid(),
                    instance.getClassDefinitionUuid(),
                    instance.getInstructorUuid(),
                    instance.getStartTime(),
                    instance.getTitle()
            );
            eventPublisher.publishEvent(event);
            publishEnrollmentStatusChanged(savedEntity, instance);

            createdEnrollments.add(savedEntity);
        }

        if (!createdEnrollments.isEmpty()) {
            publishClassEnrollmentNotifications(createdEnrollments.get(0), instancesToEnroll.get(0));
            affiliateStudentWithClassOrganisation(studentUuid, classDefinitionUuid);
        }

        log.debug("Enrolled student into {} scheduled instances for class definition: {}", createdEnrollments.size(), classDefinitionUuid);
        return EnrollmentFactory.toDTOList(createdEnrollments);
    }

    /**
     * When a student enrols in a class, make sure they are affiliated with the class's organisation
     * and its training branch (in the {@code student} domain). Best-effort: a failure here must never
     * roll back or block the enrolment itself.
     */
    private void affiliateStudentWithClassOrganisation(UUID studentUuid, UUID classDefinitionUuid) {
        try {
            UUID organisationUuid = classDefinitionLookupService.findOrganisationUuid(classDefinitionUuid).orElse(null);
            if (organisationUuid == null) {
                return;
            }
            UUID userUuid = studentLookupService.getStudentUserUuid(studentUuid).orElse(null);
            if (userUuid == null) {
                return;
            }
            UUID branchUuid = classDefinitionLookupService.findBranchUuid(classDefinitionUuid).orElse(null);
            organisationAffiliationService.affiliateEnrolledStudent(userUuid, organisationUuid, branchUuid);
        } catch (Exception e) {
            log.warn("Could not affiliate student {} with the organisation of class {}: {}",
                    studentUuid, classDefinitionUuid, e.getMessage());
        }
    }

    @Override
    public EnrollmentDTO enrollStudentInInstance(UUID instanceUuid, UUID studentUuid) {
        log.debug("Enrolling student: {} into scheduled instance: {}", studentUuid, instanceUuid);

        if (instanceUuid == null) {
            throw new IllegalArgumentException("Instance UUID cannot be null");
        }
        if (studentUuid == null) {
            throw new IllegalArgumentException("Student UUID cannot be null");
        }

        ScheduledInstance instance = scheduledInstanceRepository.findByUuid(instanceUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(SCHEDULED_INSTANCE_NOT_FOUND_TEMPLATE, instanceUuid)));

        UUID classDefinitionUuid = instance.getClassDefinitionUuid();
        if (classDefinitionUuid != null) {
            enforceClassContentApproval(classDefinitionUuid);
            enforceClassAgeLimits(studentUuid, classDefinitionUuid);
        }

        Optional<Enrollment> existing = enrollmentRepository.findByScheduledInstanceUuidAndStudentUuid(instanceUuid, studentUuid);
        if (existing.isPresent() && !EnrollmentStatus.CANCELLED.equals(existing.get().getStatus())) {
            throw new DuplicateResourceException("Student is already enrolled for this scheduled instance");
        }

        if (!hasCapacityForEnrollment(instanceUuid)) {
            throw new IllegalArgumentException(
                    String.format("Scheduled instance %s has reached maximum capacity", instanceUuid));
        }

        ScheduleRequestDTO scheduleRequest = new ScheduleRequestDTO(
                instance.getClassDefinitionUuid(),
                instance.getInstructorUuid(),
                instance.getStartTime(),
                instance.getEndTime(),
                instance.getTimezone()
        );

        if (hasStudentConflict(studentUuid, scheduleRequest)) {
            throw new IllegalArgumentException(
                    String.format("Student has a scheduling conflict with instance %s starting at %s",
                            instance.getUuid(), instance.getStartTime()));
        }

        Enrollment enrollment = existing.orElseGet(() -> EnrollmentFactory.toEntity(instanceUuid, studentUuid));
        enrollment.setStatus(EnrollmentStatus.ENROLLED);
        Enrollment savedEnrollment = enrollmentRepository.save(enrollment);

        StudentEnrolledEventDTO event = new StudentEnrolledEventDTO(
                savedEnrollment.getUuid(),
                instance.getUuid(),
                savedEnrollment.getStudentUuid(),
                instance.getClassDefinitionUuid(),
                instance.getInstructorUuid(),
                instance.getStartTime(),
                instance.getTitle()
        );
        eventPublisher.publishEvent(event);
        publishEnrollmentStatusChanged(savedEnrollment, instance);
        publishClassEnrollmentNotifications(savedEnrollment, instance);
        affiliateStudentWithClassOrganisation(studentUuid, instance.getClassDefinitionUuid());

        log.debug("Enrolled student {} into scheduled instance {}", studentUuid, instanceUuid);
        return EnrollmentFactory.toDTO(savedEnrollment);
    }

    @Override
    public List<EnrollmentDTO> joinWaitlist(EnrollmentRequestDTO request) {
        log.debug("Adding student {} to waitlist for class definition {}", request.studentUuid(), request.classDefinitionUuid());

        validateEnrollmentRequest(request);
        UUID classDefinitionUuid = request.classDefinitionUuid();
        UUID studentUuid = request.studentUuid();
        enforceClassContentApproval(classDefinitionUuid);

        List<ScheduledInstance> scheduledInstances = scheduledInstanceRepository.findByClassDefinitionUuid(classDefinitionUuid);
        if (scheduledInstances.isEmpty()) {
            throw new ResourceNotFoundException(String.format("No scheduled instances found for class definition with UUID %s", classDefinitionUuid));
        }

        Boolean waitlistEnabled = classDefinitionLookupService.findByUuid(classDefinitionUuid)
                .map(ClassDefinitionLookupService.ClassDefinitionSnapshot::allowWaitlist)
                .orElse(Boolean.TRUE);
        if (Boolean.FALSE.equals(waitlistEnabled)) {
            throw new IllegalStateException("Waitlisting is disabled for this class");
        }

        boolean hasAvailableSeat = scheduledInstances.stream()
                .filter(this::isInstanceOpenForEnrollment)
                .anyMatch(this::hasCapacityForInstance);
        if (hasAvailableSeat) {
            throw new IllegalStateException("Class has available seats; enroll instead of joining the waitlist");
        }

        List<Enrollment> waitlisted = new ArrayList<>();
        for (ScheduledInstance instance : scheduledInstances) {
            Optional<Enrollment> existing = enrollmentRepository.findByScheduledInstanceUuidAndStudentUuid(instance.getUuid(), studentUuid);
            if (existing.isPresent() && !EnrollmentStatus.CANCELLED.equals(existing.get().getStatus())) {
                throw new DuplicateResourceException("Student is already enrolled or waitlisted for this class");
            }

            Enrollment enrollment = existing.orElseGet(() -> EnrollmentFactory.toEntity(instance.getUuid(), studentUuid));
            enrollment.setStatus(EnrollmentStatus.WAITLISTED);
            Enrollment savedEnrollment = enrollmentRepository.save(enrollment);
            publishEnrollmentStatusChanged(savedEnrollment, instance);
            waitlisted.add(savedEnrollment);
        }

        log.info("Student {} added to waitlist for class definition {}", studentUuid, classDefinitionUuid);
        return EnrollmentFactory.toDTOList(waitlisted);
    }

    @Override
    public void cancelEnrollment(UUID enrollmentUuid, String reason) {
        log.debug("Cancelling enrollment: {} with reason: {}", enrollmentUuid, reason);
        
        if (enrollmentUuid == null) {
            throw new IllegalArgumentException("Enrollment UUID cannot be null");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Cancellation reason cannot be null or empty");
        }

        Enrollment entity = enrollmentRepository.findByUuid(enrollmentUuid)
            .orElseThrow(() -> new ResourceNotFoundException(
                String.format(ENROLLMENT_NOT_FOUND_TEMPLATE, enrollmentUuid)));

        if (!EnrollmentStatus.ENROLLED.equals(entity.getStatus())) {
            throw new IllegalArgumentException("Only enrolled students can have their enrollment cancelled");
        }

        entity.setStatus(EnrollmentStatus.CANCELLED);
        Enrollment savedEntity = enrollmentRepository.save(entity);
        scheduledInstanceRepository.findByUuid(entity.getScheduledInstanceUuid())
                .ifPresent(instance -> publishEnrollmentStatusChanged(savedEntity, instance));

        log.debug("Cancelled enrollment: {}", enrollmentUuid);
    }

    @Override
    public void markAttendance(UUID enrollmentUuid, boolean attended) {
        log.debug("Marking attendance for enrollment: {} as: {}", enrollmentUuid, attended ? "ATTENDED" : "ABSENT");
        
        if (enrollmentUuid == null) {
            throw new IllegalArgumentException("Enrollment UUID cannot be null");
        }

        Enrollment entity = enrollmentRepository.findByUuid(enrollmentUuid)
            .orElseThrow(() -> new ResourceNotFoundException(
                String.format(ENROLLMENT_NOT_FOUND_TEMPLATE, enrollmentUuid)));

        if (entity.getAttendanceMarkedAt() != null) {
            throw new IllegalArgumentException("Attendance has already been marked for this enrollment");
        }

        entity.setStatus(attended ? EnrollmentStatus.ATTENDED : EnrollmentStatus.ABSENT);
        entity.setAttendanceMarkedAt(LocalDateTime.now());
        Enrollment savedEntity = enrollmentRepository.save(entity);

        // Get scheduled instance details for event
        scheduledInstanceRepository.findByUuid(entity.getScheduledInstanceUuid())
                .ifPresent(instance -> {
                    // Publish AttendanceMarked event
                    AttendanceMarkedEventDTO event = new AttendanceMarkedEventDTO(
                            savedEntity.getUuid(),
                            instance.getUuid(),
                            savedEntity.getStudentUuid(),
                            instance.getClassDefinitionUuid(),
                            instance.getInstructorUuid(),
                            savedEntity.getStatus().getValue(),
                            savedEntity.getAttendanceMarkedAt(),
                            instance.getTitle()
                    );
                    eventPublisher.publishEvent(event);
                });
        scheduledInstanceRepository.findByUuid(entity.getScheduledInstanceUuid())
                .ifPresent(instance -> publishEnrollmentStatusChanged(savedEntity, instance));

        log.debug("Marked attendance for enrollment: {} as: {}", enrollmentUuid, entity.getStatus());
    }

    // ===== Query Operations =====

    @Override
    public List<ScheduledInstanceDTO> getScheduleForInstructor(UUID instructorUuid, LocalDate start, LocalDate end) {
        log.debug("Getting schedule for instructor: {} from {} to {}", instructorUuid, start, end);
        
        validateDateRange(instructorUuid, start, end);

        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.plusDays(1).atStartOfDay();

        List<ScheduledInstance> instances = scheduledInstanceRepository.findByInstructorAndTimeRange(
            instructorUuid, startDateTime, endDateTime);

        List<ScheduledInstanceDTO> schedule = instances.stream()
            .filter(instance -> !SchedulingStatus.CANCELLED.equals(instance.getStatus()))
            .map(ScheduledInstanceFactory::toDTO)
            .toList();

        return attributeToOrganisations(schedule);
    }

    /**
     * Stamps each session with the organisation whose class it delivers.
     * <p>
     * An instructor hired by an organisation gets sessions on their calendar that they never
     * created. Without this the booking is just an unexplained block of taken time; with it the
     * calendar can say whose work it is. Resolved in two batched lookups - classes, then
     * organisations - rather than per session, since a term's schedule is many sessions of few
     * classes.
     *
     * @param schedule sessions to attribute
     * @return the same sessions, organisation-owned ones carrying their organisation
     */
    private List<ScheduledInstanceDTO> attributeToOrganisations(List<ScheduledInstanceDTO> schedule) {
        Set<UUID> classDefinitionUuids = schedule.stream()
                .map(ScheduledInstanceDTO::classDefinitionUuid)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (classDefinitionUuids.isEmpty()) {
            return schedule;
        }

        Map<UUID, UUID> organisationByClass = classDefinitionLookupService
                .findOrganisationUuids(classDefinitionUuids);
        if (organisationByClass.isEmpty()) {
            return schedule;
        }

        Map<UUID, String> organisationNames = organisationLookupService
                .findOrganisationNames(organisationByClass.values());

        return schedule.stream()
                .map(instance -> {
                    UUID organisationUuid = instance.classDefinitionUuid() == null
                            ? null
                            : organisationByClass.get(instance.classDefinitionUuid());
                    return organisationUuid == null
                            ? instance
                            : instance.withOrganisation(organisationUuid, organisationNames.get(organisationUuid));
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EnrollmentDTO> getEnrollmentsForStudent(UUID studentUuid, Pageable pageable) {
        log.debug("Getting enrollments for student: {}", studentUuid);

        if (studentUuid == null) {
            throw new IllegalArgumentException("Student UUID cannot be null");
        }

        return enrollmentRepository.findPageByStudentUuidOrderByScheduledInstanceStartTime(studentUuid, pageable)
                .map(EnrollmentFactory::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StudentClassEnrollmentSummaryDTO> getClassEnrollmentsForStudent(UUID studentUuid, Pageable pageable) {
        log.debug("Getting class enrollments for student: {}", studentUuid);

        if (studentUuid == null) {
            throw new IllegalArgumentException("Student UUID cannot be null");
        }

        Page<UUID> classDefinitionUuids =
                enrollmentRepository.findClassDefinitionUuidsByStudentUuid(studentUuid, pageable);
        if (classDefinitionUuids.isEmpty()) {
            return Page.empty(pageable);
        }

        List<Enrollment> enrollments = enrollmentRepository.findByStudentUuidAndClassDefinitionUuidIn(
                studentUuid,
                classDefinitionUuids.getContent());
        List<StudentClassEnrollmentSummaryDTO> summaries =
                buildClassEnrollmentSummaries(enrollments, classDefinitionUuids.getContent());
        return new PageImpl<>(summaries, pageable, classDefinitionUuids.getTotalElements());
    }

    /**
     * A learner's course progress is their whole record on the platform, gathered from wherever they
     * study — so it belongs to them, and to platform administrators supporting them. An institution
     * that wants to know how one of its learners is doing has the organisation-scoped performance
     * view, which cannot see past its own classes.
     * <p>
     * The rule lives here rather than only on the route because the enrolment overview composes this
     * view with another that a wider audience may read; a composite must not hand out more than the
     * views it is made of. Callers who may not see it get an empty page rather than a refusal, so the
     * half of the overview they are entitled to still renders.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<StudentCourseEnrollmentSummaryDTO> getCourseEnrollmentsForStudent(UUID studentUuid, Pageable pageable) {
        log.debug("Getting course enrollments for student: {}", studentUuid);

        if (studentUuid == null) {
            throw new IllegalArgumentException("Student UUID cannot be null");
        }

        if (!studentUuid.equals(domainSecurityService.getCurrentStudentUuid())
                && !domainSecurityService.isPlatformAdmin()) {
            log.debug("Withholding platform-wide course progress of student {} from the current caller", studentUuid);
            return Page.empty(pageable);
        }

        return learnerProgressLookupService.findCourseProgress(studentUuid, pageable)
                .map(this::toCourseEnrollmentSummary);
    }

    @Override
    public List<StudentScheduleDTO> getScheduleForStudent(UUID studentUuid, LocalDate start, LocalDate end) {
        log.debug("Getting schedule for student: {} from {} to {}", studentUuid, start, end);
        
        validateDateRange(studentUuid, start, end);

        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.plusDays(1).atStartOfDay();

        List<Enrollment> enrollments = enrollmentRepository.findByStudentAndTimeRange(
            studentUuid, startDateTime, endDateTime);

        return enrollments.stream()
            .filter(enrollment -> !EnrollmentStatus.CANCELLED.equals(enrollment.getStatus()))
            .map(enrollment -> scheduledInstanceRepository.findByUuid(enrollment.getScheduledInstanceUuid())
                    .filter(instance -> !SchedulingStatus.CANCELLED.equals(instance.getStatus()))
                    .map(instance -> StudentScheduleFactory.toDTO(instance, enrollment))
                    .orElse(null))
            .filter(dto -> dto != null)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ScheduledInstanceDTO getScheduledInstance(UUID instanceUuid) {
        log.debug("Getting scheduled instance: {}", instanceUuid);
        
        if (instanceUuid == null) {
            throw new IllegalArgumentException("Instance UUID cannot be null");
        }

        ScheduledInstance entity = scheduledInstanceRepository.findByUuid(instanceUuid)
            .orElseThrow(() -> new ResourceNotFoundException(
                String.format(SCHEDULED_INSTANCE_NOT_FOUND_TEMPLATE, instanceUuid)));

        return attributeToOrganisations(List.of(ScheduledInstanceFactory.toDTO(entity))).getFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public EnrollmentDTO getEnrollment(UUID enrollmentUuid) {
        log.debug("Getting enrollment: {}", enrollmentUuid);
        
        if (enrollmentUuid == null) {
            throw new IllegalArgumentException("Enrollment UUID cannot be null");
        }

        Enrollment entity = enrollmentRepository.findByUuid(enrollmentUuid)
            .orElseThrow(() -> new ResourceNotFoundException(
                String.format(ENROLLMENT_NOT_FOUND_TEMPLATE, enrollmentUuid)));

        return EnrollmentFactory.toDTO(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnrollmentDTO> getEnrollmentsForInstance(UUID instanceUuid) {
        log.debug("Getting enrollments for scheduled instance: {}", instanceUuid);
        
        if (instanceUuid == null) {
            throw new IllegalArgumentException("Instance UUID cannot be null");
        }

        List<Enrollment> enrollments = enrollmentRepository.findByScheduledInstanceUuid(instanceUuid);
        return EnrollmentFactory.toDTOList(enrollments);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EnrollmentDTO> searchEnrollments(Map<String, String> searchParams, Pageable pageable) {
        Map<String, String> normalizedParams = searchParams == null ? new HashMap<>() : new HashMap<>(searchParams);
        normalizedParams.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue().isBlank());

        String classDefinitionParam = extractSearchParam(normalizedParams, "class_definition_uuid", "classDefinitionUuid");
        if (classDefinitionParam != null) {
            try {
                UUID classDefinitionUuid = UUID.fromString(classDefinitionParam.trim());
                List<ScheduledInstance> instances = scheduledInstanceRepository.findByClassDefinitionUuid(classDefinitionUuid);
                if (instances.isEmpty()) {
                    return Page.empty(pageable);
                }

                String instanceUuidList = instances.stream()
                        .map(ScheduledInstance::getUuid)
                        .map(UUID::toString)
                        .collect(Collectors.joining(","));
                normalizedParams.put("scheduled_instance_uuid_in", instanceUuidList);
            } catch (IllegalArgumentException ex) {
                log.warn("Invalid class_definition_uuid value: {}", classDefinitionParam);
            }
        }

        enrollmentSpecBuilder.validateSortProperties(Enrollment.class, pageable);
        Specification<Enrollment> specification = enrollmentSpecBuilder.buildSpecification(Enrollment.class, normalizedParams);
        Specification<Enrollment> reach = enrolmentVisibilityScope();
        Specification<Enrollment> combined = reach == null
                ? specification
                : specification == null ? reach : specification.and(reach);
        Page<Enrollment> page = combined != null
                ? enrollmentRepository.findAll(combined, pageable)
                : enrollmentRepository.findAll(pageable);

        return page.map(EnrollmentFactory::toDTO);
    }

    /**
     * Confines an enrolment search to what the caller is entitled to see, or {@code null} when the
     * caller is a platform administrator and nothing is withheld.
     * <p>
     * Everyone else reaches enrolments along three legs, OR-ed together: every class of an
     * organisation they manage, every session they instruct (as the class's default instructor or as
     * the instructor scheduled on the session itself), and their own enrolments as a learner. A caller
     * with none of those gets an empty page rather than a 403 — the search is a filter over a
     * collection, not a request for one named resource, and an empty result reveals nothing.
     * <p>
     * The reachable classes are resolved up front and bound as an IN-list — an organisation has
     * classes in the tens, and the list is the caller's own — but the sessions of those classes are
     * left to a subquery, since a busy organisation's timetable runs to thousands of rows and binding
     * every one of them would be the part that hurts.
     */
    private Specification<Enrollment> enrolmentVisibilityScope() {
        if (domainSecurityService.isPlatformAdmin()) {
            return null;
        }
        UUID callerUuid = domainSecurityService.getCurrentUserUuid();
        if (callerUuid == null) {
            return (root, query, criteriaBuilder) -> criteriaBuilder.disjunction();
        }

        Set<UUID> visibleClassUuids = new HashSet<>();
        for (UUID organisationUuid : userLookupService.getUserOrganizations(callerUuid)) {
            if (domainSecurityService.managesOrganisation(organisationUuid)) {
                visibleClassUuids.addAll(
                        classDefinitionLookupService.findClassDefinitionUuidsByOrganisationUuid(organisationUuid));
            }
        }
        UUID instructorUuid = instructorLookupService.findInstructorUuidByUserUuid(callerUuid).orElse(null);
        if (instructorUuid != null) {
            visibleClassUuids.addAll(
                    classDefinitionLookupService.findClassDefinitionUuidsByInstructorUuid(instructorUuid));
        }
        UUID studentUuid = domainSecurityService.getCurrentStudentUuid();

        return (root, query, criteriaBuilder) -> {
            List<Predicate> legs = new ArrayList<>();
            if (!visibleClassUuids.isEmpty() || instructorUuid != null) {
                Subquery<UUID> visibleInstances = query.subquery(UUID.class);
                Root<ScheduledInstance> instance = visibleInstances.from(ScheduledInstance.class);
                List<Predicate> instanceLegs = new ArrayList<>();
                if (!visibleClassUuids.isEmpty()) {
                    instanceLegs.add(instance.get("classDefinitionUuid").in(visibleClassUuids));
                }
                if (instructorUuid != null) {
                    instanceLegs.add(criteriaBuilder.equal(instance.get("instructorUuid"), instructorUuid));
                }
                visibleInstances.select(instance.<UUID>get("uuid"))
                        .where(criteriaBuilder.or(instanceLegs.toArray(new Predicate[0])));
                legs.add(root.get("scheduledInstanceUuid").in(visibleInstances));
            }
            if (studentUuid != null) {
                legs.add(criteriaBuilder.equal(root.get("studentUuid"), studentUuid));
            }
            return legs.isEmpty()
                    ? criteriaBuilder.disjunction()
                    : criteriaBuilder.or(legs.toArray(new Predicate[0]));
        };
    }

    /**
     * A class's enrolment list is every learner across every sitting the class has run — the same
     * roster the per-session route serves, only wider — so it is confined to the people who hold the
     * class: its default instructor, an instructor scheduled on one of its sessions, a manager of the
     * owning organisation, or a platform administrator.
     * <p>
     * A learner is narrowed to their own rows rather than refused. The pages a student reaches this
     * from ask the list one question — "which enrolment of mine is this?" — and answering it does not
     * require handing them the identifiers of everyone sitting beside them. Anyone else sees nothing.
     * The rule lives here rather than on the route because the route is in another module and the
     * guarantee belongs to the data.
     */
    @Override
    @Transactional(readOnly = true)
    public List<EnrollmentDTO> getEnrollmentsForClass(UUID classDefinitionUuid) {
        log.debug("Getting enrollments for class definition: {}", classDefinitionUuid);

        if (classDefinitionUuid == null) {
            throw new IllegalArgumentException("Class definition UUID cannot be null");
        }

        List<Enrollment> enrollments = enrollmentRepository.findByClassDefinitionUuid(classDefinitionUuid);
        if (timetableSecurityService.canReadClassRoster(classDefinitionUuid)) {
            return EnrollmentFactory.toDTOList(enrollments);
        }

        UUID callerStudentUuid = domainSecurityService.getCurrentStudentUuid();
        if (callerStudentUuid == null) {
            log.debug("Withholding the roster of class {} from a caller who does not hold it", classDefinitionUuid);
            return List.of();
        }
        return EnrollmentFactory.toDTOList(enrollments.stream()
                .filter(enrollment -> callerStudentUuid.equals(enrollment.getStudentUuid()))
                .toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> getActiveStudentUuidsForClass(UUID classDefinitionUuid) {
        log.debug("Getting active student UUIDs for class definition: {}", classDefinitionUuid);

        if (classDefinitionUuid == null) {
            throw new IllegalArgumentException("Class definition UUID cannot be null");
        }

        return enrollmentRepository.findByClassDefinitionUuid(classDefinitionUuid)
                .stream()
                .filter(enrollment -> EnrollmentStatus.ENROLLED.equals(enrollment.getStatus()))
                .map(Enrollment::getStudentUuid)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduledInstanceDTO> getScheduledInstancesForClassDefinition(UUID classDefinitionUuid) {
        log.debug("Getting scheduled instances for class definition: {}", classDefinitionUuid);

        if (classDefinitionUuid == null) {
            throw new IllegalArgumentException("Class definition UUID cannot be null");
        }

        return scheduledInstanceRepository.findByClassDefinitionUuid(classDefinitionUuid)
                .stream()
                .map(ScheduledInstanceFactory::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ScheduledInstanceDTO> getScheduledInstancesForClassDefinition(UUID classDefinitionUuid, Pageable pageable) {
        log.debug("Getting scheduled instances page for class definition: {}", classDefinitionUuid);

        if (classDefinitionUuid == null) {
            throw new IllegalArgumentException("Class definition UUID cannot be null");
        }

        Page<ScheduledInstance> page = scheduledInstanceRepository.findByClassDefinitionUuid(classDefinitionUuid, pageable);
        return page.map(ScheduledInstanceFactory::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public long countScheduledInstancesForClassDefinition(UUID classDefinitionUuid) {
        if (classDefinitionUuid == null) {
            throw new IllegalArgumentException("Class definition UUID cannot be null");
        }
        return scheduledInstanceRepository.countByClassDefinitionUuid(classDefinitionUuid);
    }

    private List<StudentClassEnrollmentSummaryDTO> buildClassEnrollmentSummaries(
            List<Enrollment> enrollments,
            List<UUID> classDefinitionOrder) {
        if (enrollments == null || enrollments.isEmpty()) {
            return List.of();
        }

        Map<UUID, ScheduledInstance> scheduledInstanceByUuid = scheduledInstanceRepository.findByUuidIn(
                        enrollments.stream()
                                .map(Enrollment::getScheduledInstanceUuid)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(ScheduledInstance::getUuid, instance -> instance));

        Map<UUID, AggregatedClassEnrollment> grouped = new LinkedHashMap<>();

        for (Enrollment enrollment : enrollments) {
            ScheduledInstance scheduledInstance = scheduledInstanceByUuid.get(enrollment.getScheduledInstanceUuid());
            if (scheduledInstance == null || scheduledInstance.getClassDefinitionUuid() == null) {
                continue;
            }

            UUID classDefinitionUuid = scheduledInstance.getClassDefinitionUuid();
            grouped.computeIfAbsent(classDefinitionUuid, ignored -> new AggregatedClassEnrollment())
                    .include(enrollment, scheduledInstance);
        }

        return classDefinitionOrder.stream()
                .map(classDefinitionUuid -> toClassEnrollmentSummary(
                        classDefinitionUuid,
                        grouped.get(classDefinitionUuid)))
                .filter(Objects::nonNull)
                .toList();
    }

    private StudentClassEnrollmentSummaryDTO toClassEnrollmentSummary(
            UUID classDefinitionUuid,
            AggregatedClassEnrollment aggregate) {
        if (aggregate == null) {
            return null;
        }
        return new StudentClassEnrollmentSummaryDTO(
                classDefinitionUuid,
                aggregate.classTitle,
                aggregate.latestEnrollmentUuid,
                aggregate.latestEnrollmentStatus,
                aggregate.scheduledInstanceCount,
                aggregate.latestScheduledInstanceStartTime,
                aggregate.latestActivityDate
        );
    }

    private StudentCourseEnrollmentSummaryDTO toCourseEnrollmentSummary(LearnerCourseProgressView view) {
        return new StudentCourseEnrollmentSummaryDTO(
                view.enrollmentUuid(),
                view.courseUuid(),
                view.courseName(),
                view.status(),
                view.progressPercentage(),
                view.updatedDate()
        );
    }

    private static LocalDateTime resolveEnrollmentActivityAt(Enrollment enrollment) {
        if (enrollment == null) {
            return null;
        }
        return enrollment.getLastModifiedDate() != null
                ? enrollment.getLastModifiedDate()
                : enrollment.getCreatedDate();
    }

    private static final class AggregatedClassEnrollment {
        private String classTitle;
        private UUID latestEnrollmentUuid;
        private EnrollmentStatus latestEnrollmentStatus;
        private LocalDateTime latestScheduledInstanceStartTime;
        private LocalDateTime latestActivityDate;
        private int scheduledInstanceCount;

        private void include(Enrollment enrollment, ScheduledInstance scheduledInstance) {
            scheduledInstanceCount++;
            if (classTitle == null) {
                classTitle = scheduledInstance.getTitle();
            }

            LocalDateTime candidateActivityDate = resolveEnrollmentActivityAt(enrollment);
            LocalDateTime candidateStartTime = scheduledInstance.getStartTime();
            if (isMoreRecent(candidateActivityDate, candidateStartTime)) {
                latestEnrollmentUuid = enrollment.getUuid();
                latestEnrollmentStatus = enrollment.getStatus();
                latestScheduledInstanceStartTime = candidateStartTime;
                latestActivityDate = candidateActivityDate;
                if (scheduledInstance.getTitle() != null && !scheduledInstance.getTitle().isBlank()) {
                    classTitle = scheduledInstance.getTitle();
                }
            }
        }

        private boolean isMoreRecent(LocalDateTime candidateActivityDate, LocalDateTime candidateStartTime) {
            if (latestActivityDate == null && latestScheduledInstanceStartTime == null) {
                return true;
            }
            if (candidateActivityDate != null && (latestActivityDate == null || candidateActivityDate.isAfter(latestActivityDate))) {
                return true;
            }
            if (candidateActivityDate != null && candidateActivityDate.equals(latestActivityDate)) {
                return candidateStartTime != null
                        && (latestScheduledInstanceStartTime == null || candidateStartTime.isAfter(latestScheduledInstanceStartTime));
            }
            return candidateActivityDate == null
                    && latestActivityDate == null
                    && candidateStartTime != null
                    && (latestScheduledInstanceStartTime == null || candidateStartTime.isAfter(latestScheduledInstanceStartTime));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasInstructorConflict(UUID instructorUuid, ScheduleRequestDTO request) {
        if (instructorUuid == null || request == null) {
            throw new IllegalArgumentException("Instructor UUID and schedule request cannot be null");
        }

        return !resolveInstructorConflicts(instructorUuid, request).isEmpty();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasStudentConflict(UUID studentUuid, ScheduleRequestDTO request) {
        return hasStudentConflict(studentUuid, request, Set.of());
    }

    private boolean hasStudentConflict(UUID studentUuid, ScheduleRequestDTO request, Set<UUID> ignoredInstanceUuids) {
        if (studentUuid == null || request == null) {
            throw new IllegalArgumentException("Student UUID and schedule request cannot be null");
        }

        List<Enrollment> conflictingEnrollments = enrollmentRepository
            .findOverlappingEnrollmentsForStudent(studentUuid, request.startTime(), request.endTime());

        LocalDateTime now = currentUtcTime();
        return conflictingEnrollments.stream()
                .filter(enrollment -> enrollment.getStatus() != EnrollmentStatus.WAITLISTED)
                .filter(enrollment -> !hasLapsed(enrollment, now))
                .filter(enrollment -> enrollment.getScheduledInstanceUuid() == null
                        || ignoredInstanceUuids == null
                        || !ignoredInstanceUuids.contains(enrollment.getScheduledInstanceUuid()))
                .findAny()
                .isPresent();
    }

    @Override
    public ScheduledInstanceDTO blockInstructorTime(UUID instructorUuid, apps.sarafrika.elimika.timetabling.dto.BlockInstructorTimeRequest request) {
        if (instructorUuid == null) {
            throw new IllegalArgumentException("Instructor UUID cannot be null");
        }
        if (request == null || request.periods() == null || request.periods().isEmpty()) {
            throw new IllegalArgumentException("At least one block period is required");
        }

        ScheduledInstance lastSaved = null;
        for (apps.sarafrika.elimika.timetabling.dto.BlockInstructorTimeRequest.Period period : request.periods()) {
            if (period == null || period.startTime() == null || period.endTime() == null) {
                throw new IllegalArgumentException("start_time and end_time are required");
            }
            if (!period.startTime().isBefore(period.endTime())) {
                throw new IllegalArgumentException("start_time must be before end_time");
            }

            ScheduledInstance block = new ScheduledInstance();
            block.setInstructorUuid(instructorUuid);
            block.setClassDefinitionUuid(null);
            block.setStartTime(period.startTime());
            block.setEndTime(period.endTime());
            block.setTimezone("UTC");
            block.setTitle(period.reason() != null && !period.reason().isBlank()
                    ? "Blocked: " + period.reason()
                    : "Instructor blocked");
            block.setLocationType("ONLINE");
            block.setMaxParticipants(0);
            block.setStatus(SchedulingStatus.BLOCKED);
            block.setCancellationReason(period.reason());

            lastSaved = scheduledInstanceRepository.save(block);
        }

        return ScheduledInstanceFactory.toDTO(lastSaved);
    }

    @Override
    @Transactional(readOnly = true)
    public long getEnrollmentCount(UUID instanceUuid) {
        if (instanceUuid == null) {
            throw new IllegalArgumentException("Instance UUID cannot be null");
        }

        return enrollmentRepository.countActiveEnrollmentsByScheduledInstance(instanceUuid);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasCapacityForEnrollment(UUID instanceUuid) {
        if (instanceUuid == null) {
            throw new IllegalArgumentException("Instance UUID cannot be null");
        }

        ScheduledInstance instance = scheduledInstanceRepository.findByUuid(instanceUuid)
            .orElseThrow(() -> new ResourceNotFoundException(
                String.format(SCHEDULED_INSTANCE_NOT_FOUND_TEMPLATE, instanceUuid)));

        if (!isInstanceOpenForEnrollment(instance)) {
            return false;
        }

        return hasCapacityForInstance(instance);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasCapacityForClassDefinition(UUID classDefinitionUuid) {
        if (classDefinitionUuid == null) {
            throw new IllegalArgumentException("Class definition UUID cannot be null");
        }

        List<ScheduledInstance> scheduledInstances = scheduledInstanceRepository.findByClassDefinitionUuid(classDefinitionUuid);
        if (scheduledInstances.isEmpty()) {
            return false;
        }

        return scheduledInstances.stream()
                .filter(this::isInstanceOpenForEnrollment)
                .anyMatch(this::hasCapacityForInstance);
    }

    private boolean hasCapacityForInstance(ScheduledInstance instance) {
        if (instance == null || instance.getUuid() == null) {
            return false;
        }
        Integer maxParticipants = instance.getMaxParticipants();
        if (maxParticipants == null || maxParticipants <= 0) {
            return true;
        }
        long currentEnrollments = enrollmentRepository.countActiveEnrollmentsByScheduledInstance(instance.getUuid());
        return currentEnrollments < maxParticipants;
    }

    private boolean isInstanceOpenForEnrollment(ScheduledInstance instance) {
        if (instance == null || instance.getStatus() == null) {
            return false;
        }
        return SchedulingStatus.SCHEDULED.equals(instance.getStatus()) || SchedulingStatus.ONGOING.equals(instance.getStatus());
    }

    // ===== Validation Helper Methods =====

    private ScheduledInstance findScheduledInstanceOrThrow(UUID instanceUuid) {
        if (instanceUuid == null) {
            throw new IllegalArgumentException("Instance UUID cannot be null");
        }
        return scheduledInstanceRepository.findByUuid(instanceUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(SCHEDULED_INSTANCE_NOT_FOUND_TEMPLATE, instanceUuid)));
    }

    private LocalDateTime currentUtcTime() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private void validateScheduleRequest(ScheduleRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Schedule request cannot be null");
        }
        if (request.classDefinitionUuid() == null) {
            throw new IllegalArgumentException("Class definition UUID cannot be null");
        }
        if (request.instructorUuid() == null) {
            throw new IllegalArgumentException("Instructor UUID cannot be null");
        }
        if (request.startTime() == null) {
            throw new IllegalArgumentException("Start time cannot be null");
        }
        if (request.endTime() == null) {
            throw new IllegalArgumentException("End time cannot be null");
        }
        if (request.startTime().isAfter(request.endTime()) || request.startTime().equals(request.endTime())) {
            throw new IllegalArgumentException("Start time must be before end time");
        }
    }

    private List<String> resolveInstructorConflicts(UUID instructorUuid, ScheduleRequestDTO request) {
        return resolveInstructorConflicts(instructorUuid, request, null);
    }

    private List<String> resolveInstructorConflicts(UUID instructorUuid,
                                                    ScheduleRequestDTO request,
                                                    UUID excludedInstanceUuid) {
        List<String> conflicts = new java.util.ArrayList<>();

        if (!availabilityService.isInstructorAvailable(instructorUuid, request.startTime(), request.endTime())) {
            conflicts.add("Instructor is not available for the requested time range");
        }

        List<ScheduledInstance> overlapping = scheduledInstanceRepository
                .findOverlappingInstancesForInstructor(instructorUuid, request.startTime(), request.endTime())
                .stream()
                .filter(instance -> excludedInstanceUuid == null || !Objects.equals(instance.getUuid(), excludedInstanceUuid))
                .toList();
        if (!overlapping.isEmpty()) {
            conflicts.add("Instructor has existing scheduled instances that overlap this time");
        }

        return conflicts;
    }

    private void validateEnrollmentRequest(EnrollmentRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Enrollment request cannot be null");
        }
        if (request.classDefinitionUuid() == null) {
            throw new IllegalArgumentException("Class definition UUID cannot be null");
        }
        if (request.studentUuid() == null) {
            throw new IllegalArgumentException("Student UUID cannot be null");
        }
    }


    /**
     * Same rules as {@link #enrollStudent}, decided from our own records and reported instead of
     * thrown. A learner should be told they are too young before they are charged, not after.
     */
    @Override
    @Transactional(readOnly = true)
    public ClassEnrolmentEligibilityDTO getClassEnrolmentEligibility(UUID classDefinitionUuid, UUID studentUuid) {
        Integer minAge = null;
        Integer maxAge = null;
        Integer studentAge = null;
        boolean dateOfBirthOnFile = true;
        boolean ageOk = true;

        Optional<ClassDefinitionLookupService.ClassDefinitionSnapshot> snapshot =
                classDefinitionLookupService.findByUuid(classDefinitionUuid);
        UUID courseUuid = snapshot.map(ClassDefinitionLookupService.ClassDefinitionSnapshot::courseUuid).orElse(null);

        Optional<CourseInfoService.AgeLimits> limits = courseUuid == null
                ? Optional.empty()
                : courseInfoService.getAgeLimits(courseUuid);

        if (limits.isPresent()) {
            minAge = limits.get().minAge();
            maxAge = limits.get().maxAge();

            java.time.LocalDate dateOfBirth = studentLookupService.getStudentUserUuid(studentUuid)
                    .flatMap(userLookupService::getUserDateOfBirth)
                    .orElse(null);

            if (dateOfBirth == null) {
                dateOfBirthOnFile = false;
                ageOk = false;
            } else {
                studentAge = java.time.Period.between(dateOfBirth, java.time.LocalDate.now(ZoneOffset.UTC)).getYears();
                ageOk = (minAge == null || studentAge >= minAge) && (maxAge == null || studentAge <= maxAge);
            }
        }

        List<ScheduledInstance> instances = scheduledInstanceRepository.findByClassDefinitionUuid(classDefinitionUuid);
        List<Enrollment> studentEnrollments = enrollmentRepository.findByStudentUuid(studentUuid);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        // The seat this buyer is holding at checkout is their own, so it must not be read back as an
        // enrolment: doing so refuses the very purchase that took the hold. A cancelled seat is not
        // an enrolment either. Only rows that mean "this student has this seat" count here.
        Set<UUID> enrolledInstances = studentEnrollments.stream()
                .filter(enrollment -> enrollment.getStatus() != EnrollmentStatus.RESERVED
                        && enrollment.getStatus() != EnrollmentStatus.CANCELLED)
                .map(Enrollment::getScheduledInstanceUuid)
                .collect(Collectors.toSet());

        // Seats this student occupies and therefore does not need to buy again. A live hold counts;
        // one that has lapsed does not, or a buyer whose payment never resolved would be locked out
        // of the class for good.
        Set<UUID> occupiedInstances = studentEnrollments.stream()
                .filter(enrollment -> enrollment.getStatus() != EnrollmentStatus.CANCELLED)
                .filter(enrollment -> !hasLapsed(enrollment, now))
                .map(Enrollment::getScheduledInstanceUuid)
                .collect(Collectors.toSet());

        boolean alreadyEnrolled = !instances.isEmpty()
                && instances.stream().allMatch(instance -> enrolledInstances.contains(instance.getUuid()));

        List<ScheduledInstance> instancesNeedingSeat = instances.stream()
                .filter(instance -> !occupiedInstances.contains(instance.getUuid()))
                .toList();
        // Holding every seat already is not the same as the class being full: there is nothing left
        // to take, so reporting "full" here would block a purchase that needs no new seat.
        boolean seatsAvailable = !instances.isEmpty()
                && (instancesNeedingSeat.isEmpty()
                        || instancesNeedingSeat.stream()
                                .anyMatch(instance -> hasCapacityForEnrollment(instance.getUuid())));
        Optional<ScheduledInstance> conflictingInstance =
                findFirstStudentConflict(studentUuid, instancesNeedingSeat);

        String reason = null;
        if (!dateOfBirthOnFile) {
            reason = "Add your date of birth to your profile so we can check this class's age requirement.";
        } else if (!ageOk && minAge != null && studentAge != null && studentAge < minAge) {
            reason = String.format("This class is for ages %d and over. Your profile says you are %d.", minAge, studentAge);
        } else if (!ageOk && maxAge != null && studentAge != null && studentAge > maxAge) {
            reason = String.format("This class is for ages %d and under. Your profile says you are %d.", maxAge, studentAge);
        } else if (alreadyEnrolled) {
            reason = "You are already enrolled in this class.";
        } else if (instances.isEmpty()) {
            reason = "This class has no scheduled sessions yet.";
        } else if (conflictingInstance.isPresent()) {
            ScheduledInstance instance = conflictingInstance.get();
            reason = String.format("This class overlaps with another class on your schedule at %s.",
                    instance.getStartTime());
        } else if (!seatsAvailable) {
            reason = "This class is full.";
        }

        return new ClassEnrolmentEligibilityDTO(
                reason == null,
                studentAge,
                minAge,
                maxAge,
                dateOfBirthOnFile,
                ageOk,
                seatsAvailable,
                alreadyEnrolled,
                reason);
    }

    /**
     * True when this row is a seat hold whose window has closed. A hold with no expiry is open ended
     * and never lapses; anything that is not a hold cannot lapse either.
     */
    private boolean hasLapsed(Enrollment enrollment, LocalDateTime now) {
        return enrollment.getStatus() == EnrollmentStatus.RESERVED
                && enrollment.getReservedUntil() != null
                && enrollment.getReservedUntil().isBefore(now);
    }

    private boolean isStandingEnrollment(Enrollment enrollment) {
        if (enrollment == null || enrollment.getStatus() == null) {
            return false;
        }
        return enrollment.getStatus() != EnrollmentStatus.RESERVED
                && enrollment.getStatus() != EnrollmentStatus.CANCELLED
                && enrollment.getStatus() != EnrollmentStatus.WAITLISTED;
    }

    private Optional<ScheduledInstance> findFirstStudentConflict(UUID studentUuid, List<ScheduledInstance> instances) {
        if (studentUuid == null || instances == null || instances.isEmpty()) {
            return Optional.empty();
        }

        return instances.stream()
                .filter(instance -> hasStudentConflict(
                        studentUuid,
                        toScheduleRequest(instance),
                        instance.getUuid() == null ? Set.of() : Set.of(instance.getUuid())))
                .findFirst();
    }

    private ScheduleRequestDTO toScheduleRequest(ScheduledInstance instance) {
        return new ScheduleRequestDTO(
                instance.getClassDefinitionUuid(),
                instance.getInstructorUuid(),
                instance.getStartTime(),
                instance.getEndTime(),
                instance.getTimezone()
        );
    }

    private void enforceClassAgeLimits(UUID studentUuid, UUID classDefinitionUuid) {
        if (studentUuid == null || classDefinitionUuid == null) {
            return;
        }

        Optional<ClassDefinitionLookupService.ClassDefinitionSnapshot> snapshotOpt =
                classDefinitionLookupService.findByUuid(classDefinitionUuid);
        if (snapshotOpt.isEmpty()) {
            return;
        }
        ClassDefinitionLookupService.ClassDefinitionSnapshot snapshot = snapshotOpt.get();
        UUID courseUuid = snapshot.courseUuid();
        if (courseUuid == null) {
            return;
        }

        courseInfoService.getAgeLimits(courseUuid)
                .ifPresent(ageLimits -> ageVerificationService.verifyStudentAge(
                        studentUuid,
                        ageLimits.minAge(),
                        ageLimits.maxAge(),
                        resolveCourseContext(snapshot)
                ));
    }

    private String resolveCourseContext(ClassDefinitionLookupService.ClassDefinitionSnapshot snapshot) {
        if (snapshot.title() != null && !snapshot.title().isBlank()) {
            return "course \"" + snapshot.title().trim() + "\"";
        }
        UUID courseUuid = snapshot.courseUuid();
        if (courseUuid == null) {
            return "the selected course";
        }
        return courseInfoService.getCourseName(courseUuid)
                .filter(name -> !name.isBlank())
                .map(name -> "course \"" + name.trim() + "\"")
                .orElse("course " + courseUuid);
    }


    @Override
    public boolean reserveSeatsForClass(UUID classDefinitionUuid, UUID studentUuid, LocalDateTime reservedUntil) {
        if (classDefinitionUuid == null || studentUuid == null) {
            return false;
        }
        List<ScheduledInstance> instances = scheduledInstanceRepository.findByClassDefinitionUuid(classDefinitionUuid);
        if (instances.isEmpty()) {
            return false;
        }

        List<Enrollment> toSave = new ArrayList<>();
        for (ScheduledInstance instance : instances) {
            Optional<Enrollment> existing = enrollmentRepository
                    .findByScheduledInstanceUuidAndStudentUuid(instance.getUuid(), studentUuid);

            if (existing.isPresent()) {
                Enrollment held = existing.get();
                // Already holding or holding a lapsed seat: refresh it rather than doubling up.
                if (held.getStatus() == EnrollmentStatus.RESERVED
                        || held.getStatus() == EnrollmentStatus.CANCELLED) {
                    held.setStatus(EnrollmentStatus.RESERVED);
                    held.setReservedUntil(reservedUntil);
                    toSave.add(held);
                    continue;
                }
                // Anything else means they already have this seat; nothing to reserve.
                continue;
            }

            if (!hasCapacityForEnrollment(instance.getUuid())) {
                return false;
            }
            Enrollment reservation = EnrollmentFactory.toEntity(instance.getUuid(), studentUuid);
            reservation.setStatus(EnrollmentStatus.RESERVED);
            reservation.setReservedUntil(reservedUntil);
            toSave.add(reservation);
        }

        enrollmentRepository.saveAll(toSave);
        return true;
    }

    @Override
    public void releaseSeatsForClass(UUID classDefinitionUuid, UUID studentUuid) {
        if (classDefinitionUuid == null || studentUuid == null) {
            return;
        }
        List<ScheduledInstance> instances = scheduledInstanceRepository.findByClassDefinitionUuid(classDefinitionUuid);
        List<Enrollment> released = new ArrayList<>();
        for (ScheduledInstance instance : instances) {
            enrollmentRepository.findByScheduledInstanceUuidAndStudentUuid(instance.getUuid(), studentUuid)
                    .filter(enrollment -> enrollment.getStatus() == EnrollmentStatus.RESERVED)
                    .ifPresent(enrollment -> {
                        enrollment.setStatus(EnrollmentStatus.CANCELLED);
                        enrollment.setReservedUntil(null);
                        released.add(enrollment);
                    });
        }
        if (!released.isEmpty()) {
            enrollmentRepository.saveAll(released);
            log.info("Released {} held seats on class {} for student {}",
                    released.size(), classDefinitionUuid, studentUuid);
        }
    }

    private void enforceClassContentApproval(UUID classDefinitionUuid) {
        if (classDefinitionUuid == null) {
            return;
        }

        ClassDefinitionLookupService.ClassDefinitionSnapshot snapshot = classDefinitionLookupService.findByUuid(classDefinitionUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Class definition with UUID " + classDefinitionUuid + " not found"));

        UUID courseUuid = snapshot.courseUuid();
        if (courseUuid != null && !courseInfoService.isCourseApproved(courseUuid)) {
            throw new IllegalStateException(
                    "Course " + courseUuid + " is not approved for enrollment. Please wait for admin approval.");
        }

        UUID programUuid = snapshot.programUuid();
        if (programUuid != null && !courseInfoService.isTrainingProgramApproved(programUuid)) {
            throw new IllegalStateException(
                    "Training program " + programUuid + " is not approved for enrollment. Please wait for admin approval.");
        }
    }

    private void publishEnrollmentStatusChanged(Enrollment enrollment, ScheduledInstance instance) {
        if (enrollment == null || instance == null) {
            return;
        }
        EnrollmentStatusChangedEventDTO event = new EnrollmentStatusChangedEventDTO(
                enrollment.getUuid(),
                instance.getUuid(),
                enrollment.getStudentUuid(),
                instance.getClassDefinitionUuid(),
                enrollment.getStatus().getValue(),
                LocalDateTime.now()
        );
        eventPublisher.publishEvent(event);
    }

    private void publishClassEnrollmentNotifications(Enrollment enrollment, ScheduledInstance instance) {
        if (enrollment == null || instance == null || enrollment.getStudentUuid() == null) {
            return;
        }

        ClassDefinitionLookupService.ClassDefinitionSnapshot snapshot = instance.getClassDefinitionUuid() == null
                ? null
                : classDefinitionLookupService.findByUuid(instance.getClassDefinitionUuid()).orElse(null);
        String classTitle = resolveClassTitle(instance, snapshot);
        long enrollmentCount = resolveClassEnrollmentCount(instance);
        boolean milestone = isEnrollmentMilestone(enrollmentCount);

        publishStudentEnrollmentNotification(enrollment, instance, classTitle);
        publishInstructorEnrollmentNotification(enrollment, instance, classTitle, enrollmentCount, milestone);
        publishCourseCreatorEnrollmentNotification(enrollment, instance, snapshot, classTitle, enrollmentCount, milestone);
    }

    private void publishStudentEnrollmentNotification(Enrollment enrollment, ScheduledInstance instance, String classTitle) {
        UUID recipientUserUuid = studentLookupService.getStudentUserUuid(enrollment.getStudentUuid())
                .orElse(null);
        if (recipientUserUuid == null) {
            return;
        }

        eventPublisher.publishEvent(NotificationRequestedEvent.inApp(
                recipientUserUuid,
                "CLASS_ENROLLMENT_CONFIRMED",
                "POPUP",
                "Class enrollment confirmed",
                "You have been enrolled in " + classTitle + ".",
                resolveClassActionUrl(instance),
                enrollmentMetadata(enrollment, instance, 0),
                "class-enrollment-student:" + classEnrollmentScope(instance) + ":" + enrollment.getStudentUuid()
        ));
    }

    private void publishInstructorEnrollmentNotification(Enrollment enrollment,
                                                         ScheduledInstance instance,
                                                         String classTitle,
                                                         long enrollmentCount,
                                                         boolean milestone) {
        if (instance.getInstructorUuid() == null) {
            return;
        }
        UUID recipientUserUuid = instructorLookupService.getInstructorUserUuid(instance.getInstructorUuid())
                .orElse(null);
        if (recipientUserUuid == null) {
            return;
        }

        String type = milestone
                ? "INSTRUCTOR_CLASS_ENROLLMENT_MILESTONE"
                : "INSTRUCTOR_CLASS_ENROLLMENT_NOTICE";
        eventPublisher.publishEvent(NotificationRequestedEvent.inApp(
                recipientUserUuid,
                type,
                milestone ? "POPUP" : "INBOX",
                milestone ? "Enrollment milestone reached" : "New class enrollment",
                enrollmentNoticeBody(classTitle, enrollmentCount, milestone),
                resolveClassActionUrl(instance),
                enrollmentMetadata(enrollment, instance, enrollmentCount),
                "class-enrollment-instructor:" + classEnrollmentScope(instance) + ":" + type + ":" + enrollmentCount
        ));
    }

    private void publishCourseCreatorEnrollmentNotification(Enrollment enrollment,
                                                            ScheduledInstance instance,
                                                            ClassDefinitionLookupService.ClassDefinitionSnapshot snapshot,
                                                            String classTitle,
                                                            long enrollmentCount,
                                                            boolean milestone) {
        UUID recipientUserUuid = resolveCourseCreatorUserUuid(snapshot);
        if (recipientUserUuid == null) {
            return;
        }

        String type = milestone
                ? "COURSE_ENROLLMENT_MILESTONE"
                : "COURSE_ENROLLMENT_NOTICE";
        eventPublisher.publishEvent(NotificationRequestedEvent.inApp(
                recipientUserUuid,
                type,
                milestone ? "POPUP" : "INBOX",
                milestone ? "Enrollment milestone reached" : "New course enrollment",
                enrollmentNoticeBody(classTitle, enrollmentCount, milestone),
                resolveClassActionUrl(instance),
                enrollmentMetadata(enrollment, instance, enrollmentCount),
                "class-enrollment-creator:" + classEnrollmentScope(instance) + ":" + type + ":" + enrollmentCount
        ));
    }

    private UUID resolveCourseCreatorUserUuid(ClassDefinitionLookupService.ClassDefinitionSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        if (snapshot.courseUuid() != null) {
            return courseInfoService.getCourseCreatorUserUuid(snapshot.courseUuid()).orElse(null);
        }
        if (snapshot.programUuid() != null) {
            return courseInfoService.getTrainingProgramCreatorUserUuid(snapshot.programUuid()).orElse(null);
        }
        return null;
    }

    private long resolveClassEnrollmentCount(ScheduledInstance instance) {
        if (instance.getClassDefinitionUuid() != null) {
            return enrollmentRepository.countDistinctStudentsByClassDefinitionUuidAndStatus(
                    instance.getClassDefinitionUuid(),
                    EnrollmentStatus.ENROLLED
            );
        }
        return enrollmentRepository.countActiveEnrollmentsByScheduledInstance(instance.getUuid());
    }

    private boolean isEnrollmentMilestone(long enrollmentCount) {
        return enrollmentCount == 1 || (enrollmentCount > 0 && enrollmentCount % 10 == 0);
    }

    private long countStartEligibleEnrollments(UUID instanceUuid) {
        Long count = enrollmentRepository.countEnrollmentsByScheduledInstanceAndStatusIn(
                instanceUuid,
                START_ELIGIBLE_ENROLLMENT_STATUSES
        );
        return count == null ? 0 : count;
    }

    private String enrollmentNoticeBody(String classTitle, long enrollmentCount, boolean milestone) {
        if (!milestone) {
            return "A new student enrolled in " + classTitle + ".";
        }
        if (enrollmentCount == 1) {
            return "The first student has enrolled in " + classTitle + ".";
        }
        return enrollmentCount + " students have enrolled in " + classTitle + ".";
    }

    private Map<String, Object> enrollmentMetadata(Enrollment enrollment, ScheduledInstance instance, long enrollmentCount) {
        return Map.of(
                "enrollment_uuid", enrollment.getUuid() == null ? "" : enrollment.getUuid().toString(),
                "scheduled_instance_uuid", instance.getUuid() == null ? "" : instance.getUuid().toString(),
                "class_definition_uuid", instance.getClassDefinitionUuid() == null ? "" : instance.getClassDefinitionUuid().toString(),
                "student_uuid", enrollment.getStudentUuid() == null ? "" : enrollment.getStudentUuid().toString(),
                "instructor_uuid", instance.getInstructorUuid() == null ? "" : instance.getInstructorUuid().toString(),
                "enrollment_count", enrollmentCount
        );
    }

    private String resolveClassTitle(ScheduledInstance instance, ClassDefinitionLookupService.ClassDefinitionSnapshot snapshot) {
        if (snapshot != null && snapshot.title() != null && !snapshot.title().isBlank()) {
            return snapshot.title();
        }
        if (instance.getTitle() != null && !instance.getTitle().isBlank()) {
            return instance.getTitle();
        }
        return "your class";
    }

    private String resolveClassActionUrl(ScheduledInstance instance) {
        if (instance.getClassDefinitionUuid() != null) {
            return "/dashboard/classes/" + instance.getClassDefinitionUuid();
        }
        return "/dashboard/classes/schedule/" + instance.getUuid();
    }

    private String classEnrollmentScope(ScheduledInstance instance) {
        if (instance.getClassDefinitionUuid() != null) {
            return instance.getClassDefinitionUuid().toString();
        }
        return instance.getUuid() == null ? "unknown" : instance.getUuid().toString();
    }

    private void validateDateRange(UUID uuid, LocalDate start, LocalDate end) {
        if (uuid == null) {
            throw new IllegalArgumentException("UUID cannot be null");
        }
        if (start == null) {
            throw new IllegalArgumentException("Start date cannot be null");
        }
        if (end == null) {
            throw new IllegalArgumentException("End date cannot be null");
        }
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }
    }

    private String extractSearchParam(Map<String, String> searchParams, String... keys) {
        String extractedValue = null;
        var iterator = searchParams.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            for (String key : keys) {
                if (matchesSearchKey(entry.getKey(), key)) {
                    if (extractedValue == null && entry.getValue() != null && !entry.getValue().isBlank()) {
                        extractedValue = entry.getValue();
                    }
                    iterator.remove();
                    break;
                }
            }
        }
        return extractedValue;
    }

    private boolean matchesSearchKey(String candidate, String key) {
        return candidate.equals(key) || candidate.startsWith(key + "_");
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnrolmentTrendPointDTO> getEnrolmentTrendsForOrganisation(UUID organisationUuid, int months) {
        int span = Math.max(1, months);
        java.time.LocalDateTime since = java.time.LocalDate.now()
                .minusMonths(span - 1L)
                .withDayOfMonth(1)
                .atStartOfDay();
        return enrollmentRepository.findEnrolmentTrendsForOrganisation(organisationUuid, since).stream()
                .map(row -> new EnrolmentTrendPointDTO((String) row[0], ((Number) row[1]).longValue()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TodayGrowthPointDTO> getTodayGrowthForOrganisation(UUID organisationUuid) {
        java.time.LocalDateTime startOfDay = java.time.LocalDate.now().atStartOfDay();
        return enrollmentRepository.findEnrolmentsByHourTodayForOrganisation(organisationUuid, startOfDay).stream()
                .map(row -> new TodayGrowthPointDTO((String) row[0], ((Number) row[1]).longValue()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WeeklyGrowthPointDTO> getWeeklyGrowthForOrganisation(UUID organisationUuid, int weeks) {
        int span = Math.max(1, weeks);
        java.time.LocalDateTime since = java.time.LocalDate.now()
                .minusWeeks(span - 1L)
                .with(java.time.DayOfWeek.MONDAY)
                .atStartOfDay();
        return enrollmentRepository.findWeeklyEnrolmentGrowthForOrganisation(organisationUuid, since).stream()
                .map(row -> new WeeklyGrowthPointDTO((String) row[0], ((Number) row[1]).longValue()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassEnrolmentCountDTO> getClassEnrolmentCountsForOrganisation(UUID organisationUuid) {
        return enrollmentRepository.findClassEnrolmentCountsForOrganisation(organisationUuid).stream()
                .map(row -> new ClassEnrolmentCountDTO((UUID) row[0], ((Number) row[1]).longValue()))
                .toList();
    }

    /**
     * The feed's {@code PAYOUT} rows carry what the organisation settled with a named instructor — a
     * private figure between those two parties — so the amount and its currency are filled in only
     * for the people who run the organisation, or a platform admin. The route that serves this feed
     * demands exactly that, so the condition always holds there today; it is kept because the
     * guarantee belongs to the data rather than to one route, and the next caller of this service
     * inherits it instead of having to remember it. Anyone reaching the feed another way still sees
     * that an instructor was paid for a class, just not how much.
     */
    @Override
    @Transactional(readOnly = true)
    public List<OrganisationActivityEventDTO> getActivityFeedForOrganisation(UUID organisationUuid, int limit) {
        int capped = Math.max(1, Math.min(limit, 100));
        boolean discloseSettlements = domainSecurityService.isPlatformAdmin()
                || domainSecurityService.managesOrganisation(organisationUuid);
        return enrollmentRepository
                .findActivityFeedForOrganisation(organisationUuid, org.springframework.data.domain.PageRequest.of(0, capped))
                .stream()
                .map(row -> new OrganisationActivityEventDTO(
                        (String) row[0],
                        toLocalDateTime(row[1]),
                        (String) row[2],
                        toUuid(row[3]),
                        discloseSettlements ? toBigDecimal(row[4]) : null,
                        discloseSettlements ? (String) row[5] : null))
                .toList();
    }

    /** Native-query timestamp columns come back as one of several JDBC types depending on driver
     * and column type ({@code timestamptz} in particular); normalise them all to {@link java.time.LocalDateTime}. */
    private static java.time.LocalDateTime toLocalDateTime(Object value) {
        return switch (value) {
            case null -> null;
            case java.sql.Timestamp ts -> ts.toLocalDateTime();
            case java.time.OffsetDateTime odt -> odt.toLocalDateTime();
            case java.time.Instant instant -> java.time.LocalDateTime.ofInstant(instant, java.time.ZoneOffset.UTC);
            case java.time.LocalDateTime ldt -> ldt;
            default -> null;
        };
    }

    private static UUID toUuid(Object value) {
        return switch (value) {
            case null -> null;
            case UUID u -> u;
            default -> UUID.fromString(value.toString());
        };
    }

    private static java.math.BigDecimal toBigDecimal(Object value) {
        return switch (value) {
            case null -> null;
            case java.math.BigDecimal bd -> bd;
            case Number n -> java.math.BigDecimal.valueOf(n.doubleValue());
            default -> new java.math.BigDecimal(value.toString());
        };
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentEnrolmentSummaryDTO> getStudentEnrolmentSummariesForOrganisation(UUID organisationUuid) {
        return enrollmentRepository.findStudentEnrolmentSummariesForOrganisation(organisationUuid).stream()
                .map(row -> new StudentEnrolmentSummaryDTO(
                        (UUID) row[0], ((Number) row[1]).longValue(), ((Number) row[2]).longValue()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganisationStudentPerformanceDTO> getStudentPerformanceForOrganisation(UUID organisationUuid,
                                                                                         UUID studentUuid) {
        return enrollmentRepository.findStudentPerformanceForOrganisation(organisationUuid, studentUuid).stream()
                .map(row -> {
                    long total = ((Number) row[2]).longValue();
                    long attended = ((Number) row[3]).longValue();
                    return new OrganisationStudentPerformanceDTO(
                            (UUID) row[0],
                            (String) row[1],
                            total,
                            attended,
                            ((Number) row[4]).longValue(),
                            total == 0 ? 0d : Math.round((attended * 1000d) / total) / 10d,
                            row[5] == null ? null : ((java.sql.Timestamp) row[5]).toLocalDateTime());
                })
                .toList();
    }

    /**
     * Scheduled length of a session, used to price an instructor's per-hour rate.
     */
    private static Integer durationMinutes(java.time.LocalDateTime start, java.time.LocalDateTime end) {
        if (start == null || end == null || !end.isAfter(start)) {
            return null;
        }
        return (int) java.time.Duration.between(start, end).toMinutes();
    }
}
