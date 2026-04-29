package apps.sarafrika.elimika.timetabling.service.impl;

import apps.sarafrika.elimika.course.spi.CourseInfoService;
import apps.sarafrika.elimika.course.spi.LearnerCourseProgressView;
import apps.sarafrika.elimika.course.spi.LearnerProgressLookupService;
import apps.sarafrika.elimika.shared.exceptions.DuplicateResourceException;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.service.AgeVerificationService;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
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
import apps.sarafrika.elimika.timetabling.spi.SchedulingStatus;
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
import java.util.LinkedHashMap;
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

    private static final String SCHEDULED_INSTANCE_NOT_FOUND_TEMPLATE = "Scheduled instance with UUID %s not found";
    private static final String ENROLLMENT_NOT_FOUND_TEMPLATE = "Enrollment with UUID %s not found";

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
        
        // Get class definition details to populate denormalized fields
        // For now, use default values - in full implementation would call classes module SPI
        entity.setTitle("Class: " + request.classDefinitionUuid().toString().substring(0, 8));
        entity.setLocationType("ONLINE"); // Default for now
        entity.setMaxParticipants(25); // Default capacity
        
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

        try {
            SchedulingStatus status = SchedulingStatus.fromValue(newStatus);
            entity.setStatus(status);
            scheduledInstanceRepository.save(entity);
            
            log.debug("Updated status of scheduled instance: {} to: {}", instanceUuid, newStatus);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid scheduling status: " + newStatus);
        }
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

        Set<UUID> alreadyEnrolledInstanceUuids = enrollmentRepository.findByStudentUuid(studentUuid).stream()
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
            if (!hasCapacityForEnrollment(instance.getUuid())) {
                throw new IllegalArgumentException(
                        String.format("Scheduled instance %s has reached maximum capacity", instance.getUuid()));
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
        }

        List<Enrollment> createdEnrollments = new java.util.ArrayList<>();

        for (ScheduledInstance instance : instancesToEnroll) {
            Enrollment entity = EnrollmentFactory.toEntity(instance.getUuid(), studentUuid);
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

        log.debug("Enrolled student into {} scheduled instances for class definition: {}", createdEnrollments.size(), classDefinitionUuid);
        return EnrollmentFactory.toDTOList(createdEnrollments);
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

        return instances.stream()
            .filter(instance -> !SchedulingStatus.CANCELLED.equals(instance.getStatus()))
            .map(ScheduledInstanceFactory::toDTO)
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

    @Override
    @Transactional(readOnly = true)
    public Page<StudentCourseEnrollmentSummaryDTO> getCourseEnrollmentsForStudent(UUID studentUuid, Pageable pageable) {
        log.debug("Getting course enrollments for student: {}", studentUuid);

        if (studentUuid == null) {
            throw new IllegalArgumentException("Student UUID cannot be null");
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

        return ScheduledInstanceFactory.toDTO(entity);
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

        Specification<Enrollment> specification = enrollmentSpecBuilder.buildSpecification(Enrollment.class, normalizedParams);
        Page<Enrollment> page = specification != null
                ? enrollmentRepository.findAll(specification, pageable)
                : enrollmentRepository.findAll(pageable);

        return page.map(EnrollmentFactory::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnrollmentDTO> getEnrollmentsForClass(UUID classDefinitionUuid) {
        log.debug("Getting enrollments for class definition: {}", classDefinitionUuid);

        if (classDefinitionUuid == null) {
            throw new IllegalArgumentException("Class definition UUID cannot be null");
        }

        List<Enrollment> enrollments = enrollmentRepository.findByClassDefinitionUuid(classDefinitionUuid);
        return EnrollmentFactory.toDTOList(enrollments);
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
        if (studentUuid == null || request == null) {
            throw new IllegalArgumentException("Student UUID and schedule request cannot be null");
        }

        List<Enrollment> conflictingEnrollments = enrollmentRepository
            .findOverlappingEnrollmentsForStudent(studentUuid, request.startTime(), request.endTime());

        return !conflictingEnrollments.isEmpty();
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
        List<String> conflicts = new java.util.ArrayList<>();

        if (!availabilityService.isInstructorAvailable(instructorUuid, request.startTime(), request.endTime())) {
            conflicts.add("Instructor is not available for the requested time range");
        }

        List<ScheduledInstance> overlapping = scheduledInstanceRepository
                .findOverlappingInstancesForInstructor(instructorUuid, request.startTime(), request.endTime());
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
}
