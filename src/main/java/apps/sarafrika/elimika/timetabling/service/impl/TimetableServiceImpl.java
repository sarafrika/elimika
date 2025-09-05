package apps.sarafrika.elimika.timetabling.service.impl;

import apps.sarafrika.elimika.shared.exceptions.DuplicateResourceException;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import apps.sarafrika.elimika.timetabling.dto.*;
import apps.sarafrika.elimika.timetabling.factory.EnrollmentFactory;
import apps.sarafrika.elimika.timetabling.factory.ScheduledInstanceFactory;
import apps.sarafrika.elimika.timetabling.factory.StudentScheduleFactory;
import apps.sarafrika.elimika.timetabling.model.Enrollment;
import apps.sarafrika.elimika.timetabling.model.ScheduledInstance;
import apps.sarafrika.elimika.timetabling.repository.EnrollmentRepository;
import apps.sarafrika.elimika.timetabling.repository.ScheduledInstanceRepository;
import apps.sarafrika.elimika.timetabling.spi.TimetableService;
import apps.sarafrika.elimika.timetabling.util.enums.EnrollmentStatus;
import apps.sarafrika.elimika.timetabling.util.enums.SchedulingStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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

    private static final String SCHEDULED_INSTANCE_NOT_FOUND_TEMPLATE = "Scheduled instance with UUID %s not found";
    private static final String ENROLLMENT_NOT_FOUND_TEMPLATE = "Enrollment with UUID %s not found";

    // ===== Scheduling Operations =====

    @Override
    public ScheduledInstanceDTO scheduleClass(ScheduleRequestDTO request) {
        log.debug("Scheduling class for instructor: {} at time: {}", request.instructorUuid(), request.startTime());
        
        validateScheduleRequest(request);
        
        // Check for instructor conflicts
        if (hasInstructorConflict(request.instructorUuid(), request)) {
            throw new IllegalArgumentException("Instructor has conflicting schedule at the requested time");
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
            enrollmentRepository.save(enrollment);
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

    // ===== Enrollment Operations =====

    @Override
    public EnrollmentDTO enrollStudent(EnrollmentRequestDTO request) {
        log.debug("Enrolling student: {} in scheduled instance: {}", request.studentUuid(), request.scheduledInstanceUuid());
        
        validateEnrollmentRequest(request);

        // Check if student is already enrolled
        if (enrollmentRepository.existsByScheduledInstanceUuidAndStudentUuid(
                request.scheduledInstanceUuid(), request.studentUuid())) {
            throw new DuplicateResourceException("Student is already enrolled in this scheduled instance");
        }

        // Check capacity
        if (!hasCapacityForEnrollment(request.scheduledInstanceUuid())) {
            throw new IllegalArgumentException("Scheduled instance has reached maximum capacity");
        }

        // Check for student conflicts
        ScheduledInstance instance = scheduledInstanceRepository.findByUuid(request.scheduledInstanceUuid())
            .orElseThrow(() -> new ResourceNotFoundException(
                String.format(SCHEDULED_INSTANCE_NOT_FOUND_TEMPLATE, request.scheduledInstanceUuid())));

        ScheduleRequestDTO scheduleRequest = new ScheduleRequestDTO(
            instance.getClassDefinitionUuid(),
            instance.getInstructorUuid(),
            instance.getStartTime(),
            instance.getEndTime(),
            instance.getTimezone()
        );

        if (hasStudentConflict(request.studentUuid(), scheduleRequest)) {
            throw new IllegalArgumentException("Student has conflicting enrollment at the requested time");
        }

        Enrollment entity = EnrollmentFactory.toEntity(request);
        Enrollment savedEntity = enrollmentRepository.save(entity);
        
        // Publish StudentEnrolled event
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
        
        log.debug("Enrolled student with enrollment UUID: {}", savedEntity.getUuid());
        return EnrollmentFactory.toDTO(savedEntity);
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
        enrollmentRepository.save(entity);

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
                            savedEntity.getStatus(),
                            savedEntity.getAttendanceMarkedAt(),
                            instance.getTitle()
                    );
                    eventPublisher.publishEvent(event);
                });

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

        return ScheduledInstanceFactory.toDTOList(instances);
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
            .map(enrollment -> {
                ScheduledInstance instance = scheduledInstanceRepository.findByUuid(enrollment.getScheduledInstanceUuid())
                    .orElse(null);
                return instance != null ? StudentScheduleFactory.toDTO(instance, enrollment) : null;
            })
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
    public boolean hasInstructorConflict(UUID instructorUuid, ScheduleRequestDTO request) {
        if (instructorUuid == null || request == null) {
            throw new IllegalArgumentException("Instructor UUID and schedule request cannot be null");
        }

        List<ScheduledInstance> conflictingInstances = scheduledInstanceRepository
            .findOverlappingInstancesForInstructor(instructorUuid, request.startTime(), request.endTime());

        return !conflictingInstances.isEmpty();
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

        long currentEnrollments = getEnrollmentCount(instanceUuid);
        return currentEnrollments < instance.getMaxParticipants();
    }

    // ===== Validation Helper Methods =====

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

    private void validateEnrollmentRequest(EnrollmentRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Enrollment request cannot be null");
        }
        if (request.scheduledInstanceUuid() == null) {
            throw new IllegalArgumentException("Scheduled instance UUID cannot be null");
        }
        if (request.studentUuid() == null) {
            throw new IllegalArgumentException("Student UUID cannot be null");
        }
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
}