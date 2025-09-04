# Calendar-Based Class Scheduling System Implementation Guide
*Spring Modulith Architecture*

## Table of Contents
1. [Overview](#overview)
2. [Spring Modulith Module Architecture](#spring-modulith-module-architecture)
3. [Module Boundaries & SPI Events](#module-boundaries--spi-events)
4. [Database Design](#database-design)
5. [Service Layer Architecture](#service-layer-architecture)
6. [API Design](#api-design)
7. [Inter-Module Communication](#inter-module-communication)
8. [Security & Authorization](#security--authorization)
9. [Business Logic & Validation](#business-logic--validation)
10. [Integration with Existing System](#integration-with-existing-system)
11. [Implementation Phases](#implementation-phases)
12. [Technical Considerations](#technical-considerations)
13. [Testing Strategy](#testing-strategy)
14. [Deployment & Migration](#deployment--migration)

## Overview

### Purpose
Implement a calendar-based class scheduling system that allows organizations and instructors to create interactive scheduled sessions, while enabling students to enroll in these time-based classes. This extends the current self-paced learning system with live, interactive educational experiences.

### Key Features
- **Class Creation**: Organizations and instructors can create scheduled classes
- **Student Enrollment**: Students can browse and enroll in available classes
- **Conflict Detection**: Automatic prevention of scheduling conflicts
- **Recurring Classes**: Support for repeating class schedules
- **Multiple Formats**: Online, in-person, and hybrid class support
- **Capacity Management**: Class size limits and enrollment tracking
- **Attendance Tracking**: Mark and track student attendance

### Business Rules
1. Only **Organizations** and **Instructors** can create classes
2. Only **Students** can enroll in classes
3. Instructors cannot have overlapping class schedules
4. Students cannot enroll in conflicting time slots
5. Classes have maximum capacity limits
6. Classes can be linked to existing courses/programs (optional)

## Spring Modulith Module Architecture

### Module Structure
Following Spring Modulith patterns, the class scheduling system will be implemented as a dedicated module with clear boundaries and defined interfaces.

```
src/main/java/apps/sarafrika/elimika/
├── scheduling/                                    # Main scheduling module
│   ├── ClassSchedulingModule.java                # Module configuration
│   ├── api/                                       # Public API (SPI)
│   │   ├── ClassSchedulingService.java           # Public service interface
│   │   ├── events/                                # Published events
│   │   │   ├── ClassCreatedEvent.java
│   │   │   ├── ClassCancelledEvent.java
│   │   │   ├── StudentEnrolledEvent.java
│   │   │   ├── StudentUnenrolledEvent.java
│   │   │   ├── ClassCompletedEvent.java
│   │   │   └── AttendanceMarkedEvent.java
│   │   └── dto/                                   # Public DTOs
│   │       ├── ClassSessionDTO.java
│   │       ├── ClassEnrollmentDTO.java
│   │       └── SchedulingStatisticsDTO.java
│   │
│   ├── internal/                                  # Internal implementation
│   │   ├── domain/                                # Domain layer
│   │   │   ├── model/
│   │   │   │   ├── ClassSession.java
│   │   │   │   ├── ClassEnrollment.java
│   │   │   │   ├── ClassRecurrencePattern.java
│   │   │   │   ├── InstructorAvailability.java
│   │   │   │   └── ClassWaitlist.java
│   │   │   ├── repository/
│   │   │   │   ├── ClassSessionRepository.java
│   │   │   │   ├── ClassEnrollmentRepository.java
│   │   │   │   └── InstructorAvailabilityRepository.java
│   │   │   └── service/
│   │   │       ├── ClassSessionServiceImpl.java
│   │   │       ├── EnrollmentServiceImpl.java
│   │   │       ├── AvailabilityServiceImpl.java
│   │   │       └── ConflictDetectionService.java
│   │   │
│   │   ├── web/                                   # Web layer
│   │   │   ├── ClassSessionController.java
│   │   │   ├── EnrollmentController.java
│   │   │   └── AvailabilityController.java
│   │   │
│   │   └── infrastructure/                        # Infrastructure layer
│   │       ├── events/
│   │       │   └── SchedulingEventHandler.java
│   │       ├── integration/
│   │       │   ├── CourseModuleEventHandler.java
│   │       │   └── NotificationModuleEventHandler.java
│   │       └── security/
│   │           └── SchedulingSecurityConfig.java
│   │
│   └── config/                                    # Module configuration
│       ├── SchedulingModuleConfig.java
│       └── SchedulingProperties.java
```

### Module Configuration
```java
@Configuration
@EnableConfigurationProperties(SchedulingProperties.class)
@EnableJpaRepositories(basePackages = "apps.sarafrika.elimika.scheduling.internal.domain.repository")
@EntityScan(basePackages = "apps.sarafrika.elimika.scheduling.internal.domain.model")
@ComponentScan(basePackages = "apps.sarafrika.elimika.scheduling")
@ApplicationModule("scheduling")
public class ClassSchedulingModule {
    
    @Bean
    @ConditionalOnProperty(prefix = "elimika.scheduling", name = "enabled", havingValue = "true", matchIfMissing = true)
    public ClassSchedulingService classSchedulingService(
            ClassSessionServiceImpl classSessionService,
            EnrollmentServiceImpl enrollmentService,
            AvailabilityServiceImpl availabilityService) {
        return new ClassSchedulingServiceImpl(classSessionService, enrollmentService, availabilityService);
    }
    
    @Bean
    public ApplicationModuleListener schedulingModuleListener() {
        return ApplicationModuleListener.of("scheduling", () -> {
            log.info("Class Scheduling Module initialized");
        });
    }
}
```

### Module Properties
```java
@ConfigurationProperties(prefix = "elimika.scheduling")
@Data
public class SchedulingProperties {
    private boolean enabled = true;
    private BusinessRules businessRules = new BusinessRules();
    private Notifications notifications = new Notifications();
    private Performance performance = new Performance();
    
    @Data
    public static class BusinessRules {
        private Duration advanceBookingMinimum = Duration.ofHours(2);
        private Duration advanceBookingMaximum = Duration.ofDays(90);
        private Duration classMinimumDuration = Duration.ofMinutes(15);
        private Duration classMaximumDuration = Duration.ofHours(8);
        private Integer defaultMaxCapacity = 50;
        private Integer maximumAllowedCapacity = 500;
    }
    
    @Data 
    public static class Notifications {
        private List<Duration> reminderHoursBefore = List.of(Duration.ofHours(24), Duration.ofHours(2));
        private boolean digestEnabled = true;
        private LocalTime digestTime = LocalTime.of(8, 0);
    }
    
    @Data
    public static class Performance {
        private int batchSize = 100;
        private boolean asyncEnabled = true;
        private boolean cacheEnabled = true;
        private Duration cacheTimeout = Duration.ofMinutes(15);
    }
}
```

## Module Boundaries & SPI Events

### Public API (SPI) Interface
The scheduling module exposes its functionality through a well-defined SPI interface:

```java
@API(status = API.Status.STABLE)
public interface ClassSchedulingService {
    
    // Core Operations
    ClassSessionDTO createClassSession(CreateClassSessionRequest request);
    ClassSessionDTO updateClassSession(UUID uuid, UpdateClassSessionRequest request);
    void cancelClassSession(UUID uuid, String reason);
    
    // Enrollment Operations
    ClassEnrollmentDTO enrollStudent(UUID studentUuid, UUID classSessionUuid);
    void cancelEnrollment(UUID enrollmentUuid, String reason);
    
    // Query Operations
    Page<ClassSessionDTO> findClassesByInstructor(UUID instructorUuid, Pageable pageable);
    Page<ClassSessionDTO> findClassesByCourse(UUID courseUuid, Pageable pageable);
    Page<ClassSessionDTO> findUpcomingClasses(ClassSearchCriteria criteria, Pageable pageable);
    
    // Integration Operations
    List<ClassSessionDTO> getScheduledClassesForCourse(UUID courseUuid);
    boolean hasScheduledClasses(UUID courseUuid);
    SchedulingStatisticsDTO getStatistics(UUID entityUuid, EntityType entityType);
}
```

### Published Events
The scheduling module publishes domain events that other modules can subscribe to:

#### ClassCreatedEvent
```java
@DomainEvent
public record ClassCreatedEvent(
    @NotNull UUID classSessionUuid,
    @NotNull String title,
    @NotNull UUID instructorUuid,
    UUID organisationUuid,
    UUID courseUuid,
    UUID trainingProgramUuid,
    @NotNull LocalDateTime startDateTime,
    @NotNull LocalDateTime endDateTime,
    @NotNull SessionType sessionType,
    @NotNull String location,
    String meetingLink,
    @NotNull Integer maxCapacity,
    @NotNull String createdBy,
    @NotNull Instant timestamp
) implements Serializable {
    
    public static ClassCreatedEvent from(ClassSession classSession) {
        return new ClassCreatedEvent(
            classSession.getUuid(),
            classSession.getTitle(),
            classSession.getInstructorUuid(),
            classSession.getOrganisationUuid(),
            classSession.getCourseUuid(),
            classSession.getTrainingProgramUuid(),
            classSession.getStartDateTime(),
            classSession.getEndDateTime(),
            classSession.getSessionType(),
            classSession.getLocation(),
            classSession.getMeetingLink(),
            classSession.getMaxCapacity(),
            classSession.getCreatedBy(),
            Instant.now()
        );
    }
}
```

#### StudentEnrolledEvent
```java
@DomainEvent
public record StudentEnrolledEvent(
    @NotNull UUID enrollmentUuid,
    @NotNull UUID classSessionUuid,
    @NotNull UUID studentUuid,
    @NotNull String classTitle,
    @NotNull LocalDateTime classStartTime,
    @NotNull LocalDateTime enrollmentTime,
    @NotNull String enrolledBy,
    @NotNull Instant timestamp
) implements Serializable {
    
    public static StudentEnrolledEvent from(ClassEnrollment enrollment, ClassSession classSession) {
        return new StudentEnrolledEvent(
            enrollment.getUuid(),
            enrollment.getClassSessionUuid(),
            enrollment.getStudentUuid(),
            classSession.getTitle(),
            classSession.getStartDateTime(),
            enrollment.getEnrollmentDate(),
            enrollment.getCreatedBy(),
            Instant.now()
        );
    }
}
```

#### ClassCompletedEvent
```java
@DomainEvent
public record ClassCompletedEvent(
    @NotNull UUID classSessionUuid,
    @NotNull String title,
    @NotNull UUID instructorUuid,
    UUID courseUuid,
    UUID trainingProgramUuid,
    @NotNull LocalDateTime completedDateTime,
    @NotNull Integer totalEnrollments,
    @NotNull Integer attendedCount,
    @NotNull List<UUID> attendedStudents,
    @NotNull String completedBy,
    @NotNull Instant timestamp
) implements Serializable {
    
    public static ClassCompletedEvent from(ClassSession classSession, List<UUID> attendedStudents, String completedBy) {
        return new ClassCompletedEvent(
            classSession.getUuid(),
            classSession.getTitle(),
            classSession.getInstructorUuid(),
            classSession.getCourseUuid(),
            classSession.getTrainingProgramUuid(),
            classSession.getEndDateTime(),
            classSession.getCurrentEnrollments(),
            attendedStudents.size(),
            attendedStudents,
            completedBy,
            Instant.now()
        );
    }
}
```

#### AttendanceMarkedEvent
```java
@DomainEvent
public record AttendanceMarkedEvent(
    @NotNull UUID enrollmentUuid,
    @NotNull UUID classSessionUuid,
    @NotNull UUID studentUuid,
    @NotNull String classTitle,
    @NotNull Boolean attended,
    LocalDateTime joinTime,
    LocalDateTime leaveTime,
    String attendanceNotes,
    @NotNull UUID markedBy,
    @NotNull LocalDateTime markedAt,
    @NotNull Instant timestamp
) implements Serializable {
    
    public static AttendanceMarkedEvent from(ClassEnrollment enrollment, ClassSession classSession, UUID markedBy) {
        return new AttendanceMarkedEvent(
            enrollment.getUuid(),
            enrollment.getClassSessionUuid(),
            enrollment.getStudentUuid(),
            classSession.getTitle(),
            EnrollmentStatus.ATTENDED.equals(enrollment.getEnrollmentStatus()),
            enrollment.getJoinTime(),
            enrollment.getLeaveTime(),
            enrollment.getAttendanceNotes(),
            markedBy,
            enrollment.getAttendanceMarkedDate(),
            Instant.now()
        );
    }
}
```

### Event Publishing
```java
@Service
@RequiredArgsConstructor
public class SchedulingDomainEventPublisher {
    
    private final ApplicationEventPublisher eventPublisher;
    
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleClassSessionCreated(ClassSession classSession) {
        ClassCreatedEvent event = ClassCreatedEvent.from(classSession);
        eventPublisher.publishEvent(event);
    }
    
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleStudentEnrolled(ClassEnrollment enrollment, ClassSession classSession) {
        StudentEnrolledEvent event = StudentEnrolledEvent.from(enrollment, classSession);
        eventPublisher.publishEvent(event);
    }
    
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleClassCompleted(ClassSession classSession, List<UUID> attendedStudents, String completedBy) {
        ClassCompletedEvent event = ClassCompletedEvent.from(classSession, attendedStudents, completedBy);
        eventPublisher.publishEvent(event);
    }
    
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleAttendanceMarked(ClassEnrollment enrollment, ClassSession classSession, UUID markedBy) {
        AttendanceMarkedEvent event = AttendanceMarkedEvent.from(enrollment, classSession, markedBy);
        eventPublisher.publishEvent(event);
    }
}
```

### Consumed Events
The scheduling module also listens to events from other modules:

#### Course Module Events
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class CourseModuleEventHandler {
    
    private final ClassSessionService classSessionService;
    
    @EventListener
    @Async
    public void handleCourseDeleted(CourseDeletedEvent event) {
        log.info("Handling course deleted event for course: {}", event.courseUuid());
        
        // Cancel all scheduled classes for the deleted course
        List<ClassSessionDTO> scheduledClasses = classSessionService.getScheduledClassesForCourse(event.courseUuid());
        
        for (ClassSessionDTO scheduledClass : scheduledClasses) {
            try {
                classSessionService.cancelClassSession(
                    scheduledClass.uuid(), 
                    "Course was deleted: " + event.reason()
                );
                log.info("Cancelled class session {} due to course deletion", scheduledClass.uuid());
            } catch (Exception e) {
                log.error("Failed to cancel class session {} for deleted course {}", 
                    scheduledClass.uuid(), event.courseUuid(), e);
            }
        }
    }
    
    @EventListener
    @Async
    public void handleCourseInstructorChanged(CourseInstructorChangedEvent event) {
        log.info("Handling course instructor changed event for course: {}", event.courseUuid());
        
        // Update instructor for all future classes linked to this course
        // Implementation depends on business requirements
    }
    
    @EventListener
    public void handleStudentEnrolledInCourse(StudentEnrolledInCourseEvent event) {
        log.info("Student {} enrolled in course {}", event.studentUuid(), event.courseUuid());
        
        // Optionally notify about available classes for this course
        List<ClassSessionDTO> upcomingClasses = classSessionService.getUpcomingClassesByCourse(event.courseUuid());
        if (!upcomingClasses.isEmpty()) {
            // Publish notification event for available classes
            // This would be handled by the notification module
        }
    }
}
```

#### Instructor Module Events
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class InstructorModuleEventHandler {
    
    private final ClassSessionService classSessionService;
    private final InstructorAvailabilityService availabilityService;
    
    @EventListener
    @Async
    public void handleInstructorDeactivated(InstructorDeactivatedEvent event) {
        log.info("Handling instructor deactivated event for instructor: {}", event.instructorUuid());
        
        // Cancel all future classes for this instructor
        Page<ClassSessionDTO> futureClasses = classSessionService.findFutureClassesByInstructor(
            event.instructorUuid(), Pageable.unpaged());
            
        for (ClassSessionDTO classSession : futureClasses.getContent()) {
            try {
                classSessionService.cancelClassSession(
                    classSession.uuid(),
                    "Instructor account was deactivated"
                );
            } catch (Exception e) {
                log.error("Failed to cancel class {} for deactivated instructor {}", 
                    classSession.uuid(), event.instructorUuid(), e);
            }
        }
        
        // Clear instructor availability
        availabilityService.clearAllAvailability(event.instructorUuid());
    }
    
    @EventListener
    public void handleInstructorProfileUpdated(InstructorProfileUpdatedEvent event) {
        // Update cached instructor information if needed
        log.info("Instructor profile updated for: {}", event.instructorUuid());
    }
}
```

#### Student Module Events  
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class StudentModuleEventHandler {
    
    private final ClassEnrollmentService enrollmentService;
    
    @EventListener
    @Async
    public void handleStudentDeactivated(StudentDeactivatedEvent event) {
        log.info("Handling student deactivated event for student: {}", event.studentUuid());
        
        // Cancel all future enrollments for this student
        Page<ClassEnrollmentDTO> futureEnrollments = enrollmentService.getFutureEnrollments(
            event.studentUuid(), Pageable.unpaged());
            
        for (ClassEnrollmentDTO enrollment : futureEnrollments.getContent()) {
            try {
                enrollmentService.cancelEnrollment(
                    enrollment.uuid(),
                    "Student account was deactivated"
                );
            } catch (Exception e) {
                log.error("Failed to cancel enrollment {} for deactivated student {}", 
                    enrollment.uuid(), event.studentUuid(), e);
            }
        }
    }
}
```

## System Requirements

### Functional Requirements
- **FR1**: Create, update, and cancel scheduled classes
- **FR2**: Student enrollment and cancellation in classes
- **FR3**: Instructor availability management
- **FR4**: Automatic conflict detection and prevention
- **FR5**: Recurring class pattern support
- **FR6**: Class capacity and waitlist management
- **FR7**: Attendance tracking and reporting
- **FR8**: Integration with existing course/program system
- **FR9**: Multi-timezone support
- **FR10**: Email notifications for class events

### Non-Functional Requirements
- **NFR1**: System should handle 10,000+ concurrent class sessions
- **NFR2**: Class booking response time < 2 seconds
- **NFR3**: 99.9% availability during peak hours
- **NFR4**: Support for multiple timezones
- **NFR5**: Audit trail for all scheduling operations

## Database Design

### Flyway Migration Structure

The class scheduling module follows the established Flyway migration patterns used throughout the Elimika project. All migrations must:

1. Follow the naming convention: `V{YYYYMMDDHHMI}__{Description}.sql`
2. Include all BaseEntity fields (id, uuid, created_date, updated_date, created_by, updated_by)
3. Use proper UUID generation: `UUID NOT NULL UNIQUE DEFAULT gen_random_uuid()`
4. Include comprehensive indexing for performance
5. Add proper constraints and foreign key relationships

### Migration Files

#### 1. Class Sessions Table Migration
**File**: `V202509041325__create_class_sessions_table.sql`

```sql
-- 202509041325__create_class_sessions_table.sql
-- Create class_sessions table for calendar-based scheduling

CREATE TABLE class_sessions (
    id               BIGSERIAL PRIMARY KEY,
    uuid             UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    
    -- Basic Information
    title            VARCHAR(255)             NOT NULL,
    description      TEXT,
    
    -- Ownership & Organization  
    instructor_uuid  UUID                     NOT NULL,
    organisation_uuid UUID,
    training_branch_uuid UUID,
    
    -- Associated Content
    course_uuid      UUID,
    training_program_uuid UUID,
    
    -- Scheduling Information
    start_time       TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time         TIMESTAMP WITH TIME ZONE NOT NULL,
    timezone         VARCHAR(50)              NOT NULL DEFAULT 'UTC',
    
    -- Location & Format
    location_type    VARCHAR(20)              NOT NULL DEFAULT 'online' 
                     CHECK (location_type IN ('online', 'in_person', 'hybrid')),
    physical_location VARCHAR(500),
    meeting_url      VARCHAR(500),
    meeting_password VARCHAR(100),
    
    -- Capacity & Enrollment
    max_participants INTEGER                  NOT NULL DEFAULT 50,
    min_participants INTEGER                  NOT NULL DEFAULT 1,
    allow_waitlist   BOOLEAN                  NOT NULL DEFAULT true,
    
    -- Recurrence Pattern
    is_recurring     BOOLEAN                  NOT NULL DEFAULT false,
    recurrence_pattern VARCHAR(20)            CHECK (recurrence_pattern IN ('daily', 'weekly', 'monthly', 'custom')),
    recurrence_end_date TIMESTAMP WITH TIME ZONE,
    recurrence_count INTEGER,
    custom_recurrence_rule TEXT,
    
    -- Status Management
    status           VARCHAR(20)              NOT NULL DEFAULT 'scheduled' 
                     CHECK (status IN ('scheduled', 'ongoing', 'completed', 'cancelled')),
    cancellation_reason TEXT,
    cancelled_by     VARCHAR(255),
    
    -- Audit Fields (BaseEntity pattern)
    created_date     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    updated_date     TIMESTAMP WITH TIME ZONE DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    created_by       VARCHAR(255)             NOT NULL,
    updated_by       VARCHAR(255),
    
    -- Constraints
    CONSTRAINT check_time_validity CHECK (start_time < end_time),
    CONSTRAINT check_capacity CHECK (min_participants <= max_participants),
    CONSTRAINT check_recurrence_data CHECK (
        (is_recurring = false AND recurrence_pattern IS NULL) OR
        (is_recurring = true AND recurrence_pattern IS NOT NULL)
    )
);

-- Performance Indexes
CREATE INDEX idx_class_sessions_uuid ON class_sessions (uuid);
CREATE INDEX idx_class_sessions_instructor_uuid ON class_sessions (instructor_uuid);
CREATE INDEX idx_class_sessions_organisation_uuid ON class_sessions (organisation_uuid);
CREATE INDEX idx_class_sessions_training_branch_uuid ON class_sessions (training_branch_uuid);
CREATE INDEX idx_class_sessions_course_uuid ON class_sessions (course_uuid);
CREATE INDEX idx_class_sessions_training_program_uuid ON class_sessions (training_program_uuid);
CREATE INDEX idx_class_sessions_start_time ON class_sessions (start_time);
CREATE INDEX idx_class_sessions_end_time ON class_sessions (end_time);
CREATE INDEX idx_class_sessions_status ON class_sessions (status);
CREATE INDEX idx_class_sessions_location_type ON class_sessions (location_type);
CREATE INDEX idx_class_sessions_is_recurring ON class_sessions (is_recurring);
CREATE INDEX idx_class_sessions_created_date ON class_sessions (created_date);

-- Composite indexes for common queries
CREATE INDEX idx_class_sessions_instructor_time ON class_sessions (instructor_uuid, start_time, end_time);
CREATE INDEX idx_class_sessions_org_time ON class_sessions (organisation_uuid, start_time, end_time);
CREATE INDEX idx_class_sessions_status_time ON class_sessions (status, start_time);
```

#### 2. Class Enrollments Table Migration
**File**: `V202509041326__create_class_enrollments_table.sql`

```sql
-- 202509041326__create_class_enrollments_table.sql
-- Create class_enrollments table for student enrollment in scheduled classes

CREATE TABLE class_enrollments (
    id                    BIGSERIAL PRIMARY KEY,
    uuid                  UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    
    -- Relationships
    class_session_uuid    UUID                     NOT NULL,
    student_uuid          UUID                     NOT NULL,
    
    -- Enrollment Information
    enrollment_date       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    enrollment_method     VARCHAR(20)              NOT NULL DEFAULT 'self_service'
                          CHECK (enrollment_method IN ('self_service', 'admin_enrolled', 'waitlist_promoted')),
    
    -- Status Management
    status                VARCHAR(20)              NOT NULL DEFAULT 'enrolled' 
                          CHECK (status IN ('enrolled', 'attended', 'absent', 'cancelled', 'transferred')),
    
    -- Cancellation Information
    cancellation_date     TIMESTAMP WITH TIME ZONE,
    cancellation_reason   TEXT,
    cancelled_by          VARCHAR(255),
    
    -- Transfer Information
    transferred_to_uuid   UUID,
    transfer_date         TIMESTAMP WITH TIME ZONE,
    transfer_reason       TEXT,
    
    -- Attendance Tracking
    attendance_status     VARCHAR(20)              CHECK (attendance_status IN ('present', 'absent', 'late', 'excused')),
    attendance_marked_at  TIMESTAMP WITH TIME ZONE,
    attendance_marked_by  VARCHAR(255),
    attendance_notes      TEXT,
    
    -- Audit Fields (BaseEntity pattern)
    created_date          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    updated_date          TIMESTAMP WITH TIME ZONE DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    created_by            VARCHAR(255)             NOT NULL,
    updated_by            VARCHAR(255),
    
    -- Constraints
    CONSTRAINT unique_student_class_enrollment UNIQUE (class_session_uuid, student_uuid),
    CONSTRAINT check_cancellation_data CHECK (
        (status != 'cancelled') OR 
        (status = 'cancelled' AND cancellation_date IS NOT NULL AND cancelled_by IS NOT NULL)
    ),
    CONSTRAINT check_transfer_data CHECK (
        (status != 'transferred') OR 
        (status = 'transferred' AND transferred_to_uuid IS NOT NULL AND transfer_date IS NOT NULL)
    )
);

-- Performance Indexes  
CREATE INDEX idx_class_enrollments_uuid ON class_enrollments (uuid);
CREATE INDEX idx_class_enrollments_class_session_uuid ON class_enrollments (class_session_uuid);
CREATE INDEX idx_class_enrollments_student_uuid ON class_enrollments (student_uuid);
CREATE INDEX idx_class_enrollments_status ON class_enrollments (status);
CREATE INDEX idx_class_enrollments_enrollment_date ON class_enrollments (enrollment_date);
CREATE INDEX idx_class_enrollments_attendance_status ON class_enrollments (attendance_status);
CREATE INDEX idx_class_enrollments_created_date ON class_enrollments (created_date);

-- Composite indexes for common queries
CREATE INDEX idx_class_enrollments_student_status ON class_enrollments (student_uuid, status);
CREATE INDEX idx_class_enrollments_class_status ON class_enrollments (class_session_uuid, status);
CREATE INDEX idx_class_enrollments_attendance ON class_enrollments (class_session_uuid, attendance_status);
```

#### 3. Instructor Availability Table Migration  
**File**: `V202509041327__create_instructor_availability_table.sql`

```sql
-- 202509041327__create_instructor_availability_table.sql
-- Create instructor_availability table for managing instructor schedule availability

CREATE TABLE instructor_availability (
    id                BIGSERIAL PRIMARY KEY,
    uuid              UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    
    -- Relationships
    instructor_uuid   UUID                     NOT NULL,
    
    -- Availability Type
    availability_type VARCHAR(20)              NOT NULL DEFAULT 'weekly'
                      CHECK (availability_type IN ('weekly', 'specific_date', 'date_range')),
    
    -- Time Information
    day_of_week       INTEGER                  CHECK (day_of_week >= 1 AND day_of_week <= 7),
    specific_date     DATE,
    start_date        DATE,
    end_date          DATE,
    start_time        TIME                     NOT NULL,
    end_time          TIME                     NOT NULL,
    timezone          VARCHAR(50)              NOT NULL DEFAULT 'UTC',
    
    -- Status
    is_active         BOOLEAN                  NOT NULL DEFAULT true,
    notes             TEXT,
    
    -- Audit Fields (BaseEntity pattern)
    created_date      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    updated_date      TIMESTAMP WITH TIME ZONE DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    created_by        VARCHAR(255)             NOT NULL,
    updated_by        VARCHAR(255),
    
    -- Constraints
    CONSTRAINT check_time_validity CHECK (start_time < end_time),
    CONSTRAINT check_weekly_availability CHECK (
        (availability_type != 'weekly') OR 
        (availability_type = 'weekly' AND day_of_week IS NOT NULL)
    ),
    CONSTRAINT check_specific_date_availability CHECK (
        (availability_type != 'specific_date') OR 
        (availability_type = 'specific_date' AND specific_date IS NOT NULL)
    ),
    CONSTRAINT check_date_range_availability CHECK (
        (availability_type != 'date_range') OR 
        (availability_type = 'date_range' AND start_date IS NOT NULL AND end_date IS NOT NULL AND start_date <= end_date)
    )
);

-- Performance Indexes
CREATE INDEX idx_instructor_availability_uuid ON instructor_availability (uuid);
CREATE INDEX idx_instructor_availability_instructor_uuid ON instructor_availability (instructor_uuid);
CREATE INDEX idx_instructor_availability_type ON instructor_availability (availability_type);
CREATE INDEX idx_instructor_availability_day_of_week ON instructor_availability (day_of_week);
CREATE INDEX idx_instructor_availability_specific_date ON instructor_availability (specific_date);
CREATE INDEX idx_instructor_availability_start_date ON instructor_availability (start_date);
CREATE INDEX idx_instructor_availability_end_date ON instructor_availability (end_date);
CREATE INDEX idx_instructor_availability_active ON instructor_availability (is_active);
CREATE INDEX idx_instructor_availability_created_date ON instructor_availability (created_date);

-- Composite indexes for common queries  
CREATE INDEX idx_instructor_availability_instructor_active ON instructor_availability (instructor_uuid, is_active);
CREATE INDEX idx_instructor_availability_instructor_type ON instructor_availability (instructor_uuid, availability_type);
```

#### 4. Class Waitlist Table Migration
**File**: `V202509041328__create_class_waitlist_table.sql`

```sql
-- 202509041328__create_class_waitlist_table.sql  
-- Create class_waitlist table for managing waiting lists when classes are full

CREATE TABLE class_waitlist (
    id                  BIGSERIAL PRIMARY KEY,
    uuid                UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    
    -- Relationships
    class_session_uuid  UUID                     NOT NULL,
    student_uuid        UUID                     NOT NULL,
    
    -- Waitlist Information
    joined_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    position            INTEGER                  NOT NULL,
    
    -- Status Management
    status              VARCHAR(20)              NOT NULL DEFAULT 'waiting'
                        CHECK (status IN ('waiting', 'promoted', 'expired', 'cancelled')),
    
    -- Promotion Information  
    promoted_at         TIMESTAMP WITH TIME ZONE,
    promotion_expires_at TIMESTAMP WITH TIME ZONE,
    
    -- Cancellation Information
    cancelled_at        TIMESTAMP WITH TIME ZONE,
    cancellation_reason TEXT,
    
    -- Notification Preferences
    notify_on_availability BOOLEAN              NOT NULL DEFAULT true,
    notification_method VARCHAR(20)              NOT NULL DEFAULT 'email'
                        CHECK (notification_method IN ('email', 'sms', 'push', 'none')),
    
    -- Audit Fields (BaseEntity pattern)
    created_date        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    updated_date        TIMESTAMP WITH TIME ZONE DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    created_by          VARCHAR(255)             NOT NULL,
    updated_by          VARCHAR(255),
    
    -- Constraints
    CONSTRAINT unique_student_class_waitlist UNIQUE (class_session_uuid, student_uuid),
    CONSTRAINT check_position_positive CHECK (position > 0),
    CONSTRAINT check_promotion_data CHECK (
        (status != 'promoted') OR 
        (status = 'promoted' AND promoted_at IS NOT NULL)
    )
);

-- Performance Indexes
CREATE INDEX idx_class_waitlist_uuid ON class_waitlist (uuid);
CREATE INDEX idx_class_waitlist_class_session_uuid ON class_waitlist (class_session_uuid);
CREATE INDEX idx_class_waitlist_student_uuid ON class_waitlist (student_uuid);
CREATE INDEX idx_class_waitlist_status ON class_waitlist (status);
CREATE INDEX idx_class_waitlist_position ON class_waitlist (position);
CREATE INDEX idx_class_waitlist_joined_at ON class_waitlist (joined_at);
CREATE INDEX idx_class_waitlist_promoted_at ON class_waitlist (promoted_at);
CREATE INDEX idx_class_waitlist_created_date ON class_waitlist (created_date);

-- Composite indexes for common queries
CREATE INDEX idx_class_waitlist_class_status_position ON class_waitlist (class_session_uuid, status, position);
CREATE INDEX idx_class_waitlist_student_status ON class_waitlist (student_uuid, status);
```

#### 5. Class Resources Table Migration
**File**: `V202509041329__create_class_resources_table.sql`

```sql
-- 202509041329__create_class_resources_table.sql
-- Create class_resources table for managing resources and materials associated with classes

CREATE TABLE class_resources (
    id                 BIGSERIAL PRIMARY KEY,
    uuid               UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    
    -- Relationships
    class_session_uuid UUID                     NOT NULL,
    
    -- Resource Information
    resource_name      VARCHAR(255)             NOT NULL,
    resource_type      VARCHAR(50)              NOT NULL 
                       CHECK (resource_type IN ('document', 'video', 'audio', 'image', 'link', 'file')),
    resource_url       VARCHAR(1000)            NOT NULL,
    file_size_bytes    BIGINT,
    mime_type          VARCHAR(100),
    
    -- Access Control
    is_public          BOOLEAN                  NOT NULL DEFAULT false,
    requires_enrollment BOOLEAN                 NOT NULL DEFAULT true,
    
    -- Organization
    display_order      INTEGER                  NOT NULL DEFAULT 0,
    description        TEXT,
    
    -- Audit Fields (BaseEntity pattern)
    created_date       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    updated_date       TIMESTAMP WITH TIME ZONE DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC' + INTERVAL '3 hours'),
    created_by         VARCHAR(255)             NOT NULL,
    updated_by         VARCHAR(255)
);

-- Performance Indexes
CREATE INDEX idx_class_resources_uuid ON class_resources (uuid);
CREATE INDEX idx_class_resources_class_session_uuid ON class_resources (class_session_uuid);
CREATE INDEX idx_class_resources_resource_type ON class_resources (resource_type);
CREATE INDEX idx_class_resources_is_public ON class_resources (is_public);
CREATE INDEX idx_class_resources_display_order ON class_resources (display_order);
CREATE INDEX idx_class_resources_created_date ON class_resources (created_date);

-- Composite indexes for common queries
CREATE INDEX idx_class_resources_class_order ON class_resources (class_session_uuid, display_order);
CREATE INDEX idx_class_resources_class_public ON class_resources (class_session_uuid, is_public);
```

### Core Entities

After running the migrations above, the following entities will be created following the established BaseEntity pattern:
    
    -- Content Association (Optional)
    course_uuid VARCHAR(36),
    training_program_uuid VARCHAR(36),
    lesson_uuid VARCHAR(36),
    
    -- Scheduling
    start_date_time DATETIME NOT NULL,
    end_date_time DATETIME NOT NULL,
    duration_minutes INTEGER NOT NULL,
    timezone VARCHAR(50) DEFAULT 'UTC',
    
    -- Session Details
    session_type ENUM('ONLINE', 'IN_PERSON', 'HYBRID') NOT NULL,
    location VARCHAR(500),
    meeting_link VARCHAR(500),
    meeting_password VARCHAR(100),
    
    -- Capacity Management
    max_capacity INTEGER DEFAULT 50,
    current_enrollments INTEGER DEFAULT 0,
    allow_waitlist BOOLEAN DEFAULT false,
    
    -- Status
    is_active BOOLEAN DEFAULT true,
    status ENUM('SCHEDULED', 'ONGOING', 'COMPLETED', 'CANCELLED') DEFAULT 'SCHEDULED',
    cancellation_reason TEXT,
    
    -- Recurrence
    is_recurring BOOLEAN DEFAULT false,
    recurrence_pattern_uuid VARCHAR(36),
    parent_session_uuid VARCHAR(36), -- For recurring instances
    
    -- Audit Fields
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_date DATETIME ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    
    -- Indexes
    INDEX idx_instructor_datetime (instructor_uuid, start_date_time),
    INDEX idx_org_datetime (organisation_uuid, start_date_time),
    INDEX idx_course_datetime (course_uuid, start_date_time),
    INDEX idx_status_datetime (status, start_date_time),
    
    -- Foreign Keys
    FOREIGN KEY (instructor_uuid) REFERENCES instructors(uuid),
    FOREIGN KEY (organisation_uuid) REFERENCES organisations(uuid),
    FOREIGN KEY (course_uuid) REFERENCES courses(uuid),
    FOREIGN KEY (training_program_uuid) REFERENCES training_programs(uuid)
);
```

#### ClassRecurrencePattern Entity
```sql
CREATE TABLE class_recurrence_patterns (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    
    -- Pattern Definition
    recurrence_type ENUM('DAILY', 'WEEKLY', 'MONTHLY') NOT NULL,
    interval_value INTEGER DEFAULT 1, -- Every X days/weeks/months
    
    -- Weekly Pattern Details
    days_of_week SET('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'),
    
    -- Monthly Pattern Details
    day_of_month INTEGER, -- 1-31
    week_of_month INTEGER, -- 1-4 (first week, second week, etc.)
    day_of_week_monthly ENUM('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'),
    
    -- End Conditions
    end_date DATE,
    occurrence_count INTEGER,
    
    -- Audit Fields
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_date DATETIME ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);
```

#### ClassEnrollment Entity
```sql
CREATE TABLE class_enrollments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    
    -- References
    class_session_uuid VARCHAR(36) NOT NULL,
    student_uuid VARCHAR(36) NOT NULL,
    
    -- Enrollment Details
    enrollment_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    enrollment_status ENUM('ENROLLED', 'ATTENDED', 'MISSED', 'CANCELLED', 'WAITLISTED') DEFAULT 'ENROLLED',
    
    -- Cancellation
    cancellation_date DATETIME,
    cancellation_reason TEXT,
    cancelled_by VARCHAR(36), -- User UUID who cancelled
    
    -- Attendance
    attendance_marked_date DATETIME,
    attendance_marked_by VARCHAR(36), -- Instructor UUID
    join_time DATETIME,
    leave_time DATETIME,
    
    -- Waitlist
    waitlist_position INTEGER,
    waitlist_notification_sent BOOLEAN DEFAULT false,
    
    -- Audit Fields
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_date DATETIME ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    
    -- Constraints
    UNIQUE KEY unique_student_class (class_session_uuid, student_uuid),
    
    -- Indexes
    INDEX idx_student_enrollments (student_uuid, enrollment_status),
    INDEX idx_class_enrollments (class_session_uuid, enrollment_status),
    INDEX idx_enrollment_date (enrollment_date),
    
    -- Foreign Keys
    FOREIGN KEY (class_session_uuid) REFERENCES class_sessions(uuid),
    FOREIGN KEY (student_uuid) REFERENCES students(uuid)
);
```

#### InstructorAvailability Entity
```sql
CREATE TABLE instructor_availability (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    
    -- Reference
    instructor_uuid VARCHAR(36) NOT NULL,
    
    -- Time Slot
    day_of_week ENUM('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'),
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    timezone VARCHAR(50) DEFAULT 'UTC',
    
    -- Date Range (for temporary availability)
    effective_date DATE,
    expiry_date DATE,
    
    -- Recurrence
    is_recurring BOOLEAN DEFAULT true, -- Weekly recurring
    
    -- Status
    is_active BOOLEAN DEFAULT true,
    
    -- Audit Fields
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_date DATETIME ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    
    -- Indexes
    INDEX idx_instructor_day (instructor_uuid, day_of_week),
    INDEX idx_instructor_active (instructor_uuid, is_active),
    
    -- Foreign Keys
    FOREIGN KEY (instructor_uuid) REFERENCES instructors(uuid)
);
```

#### ClassWaitlist Entity
```sql
CREATE TABLE class_waitlists (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    
    -- References
    class_session_uuid VARCHAR(36) NOT NULL,
    student_uuid VARCHAR(36) NOT NULL,
    
    -- Waitlist Details
    position INTEGER NOT NULL,
    join_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    notification_sent BOOLEAN DEFAULT false,
    notification_date DATETIME,
    
    -- Status
    status ENUM('WAITING', 'ENROLLED', 'EXPIRED', 'CANCELLED') DEFAULT 'WAITING',
    
    -- Audit Fields
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_date DATETIME ON UPDATE CURRENT_TIMESTAMP,
    
    -- Constraints
    UNIQUE KEY unique_student_waitlist (class_session_uuid, student_uuid),
    
    -- Indexes
    INDEX idx_class_position (class_session_uuid, position),
    INDEX idx_student_waitlist (student_uuid, status),
    
    -- Foreign Keys
    FOREIGN KEY (class_session_uuid) REFERENCES class_sessions(uuid),
    FOREIGN KEY (student_uuid) REFERENCES students(uuid)
);
```

### Entity Relationships
```
ClassSession 1:N ClassEnrollment
ClassSession 1:1 ClassRecurrencePattern (optional)
ClassSession N:1 Instructor
ClassSession N:1 Organisation
ClassSession N:1 Course (optional)
ClassSession N:1 TrainingProgram (optional)
ClassSession 1:N ClassWaitlist

Instructor 1:N InstructorAvailability
Student 1:N ClassEnrollment
Student 1:N ClassWaitlist
```

## Service Layer Architecture

### Core Services

#### ClassSessionService
```java
public interface ClassSessionService {
    // CRUD Operations
    ClassSessionDTO createClassSession(CreateClassSessionRequest request);
    ClassSessionDTO updateClassSession(UUID uuid, UpdateClassSessionRequest request);
    ClassSessionDTO getClassSession(UUID uuid);
    Page<ClassSessionDTO> searchClasses(ClassSearchCriteria criteria, Pageable pageable);
    void cancelClassSession(UUID uuid, String reason);
    
    // Scheduling Operations
    List<AvailableTimeSlotDTO> getAvailableTimeSlots(UUID instructorUuid, LocalDate date);
    boolean isTimeSlotAvailable(UUID instructorUuid, LocalDateTime start, LocalDateTime end);
    List<ConflictDTO> checkSchedulingConflicts(ClassSessionDTO classSession);
    
    // Instructor/Organization Operations
    Page<ClassSessionDTO> getInstructorClasses(UUID instructorUuid, ClassFilterDTO filter, Pageable pageable);
    Page<ClassSessionDTO> getOrganisationClasses(UUID orgUuid, ClassFilterDTO filter, Pageable pageable);
    
    // Recurring Classes
    List<ClassSessionDTO> createRecurringClasses(CreateRecurringClassRequest request);
    void updateRecurringClassSeries(UUID parentUuid, UpdateRecurringClassRequest request);
    void cancelRecurringClassSeries(UUID parentUuid, String reason, boolean cancelFutureOnly);
    
    // Status Management
    void markClassAsOngoing(UUID uuid);
    void markClassAsCompleted(UUID uuid);
    
    // Analytics
    ClassStatisticsDTO getClassStatistics(UUID classUuid);
    InstructorScheduleDTO getInstructorSchedule(UUID instructorUuid, LocalDate startDate, LocalDate endDate);
}
```

#### ClassEnrollmentService
```java
public interface ClassEnrollmentService {
    // Enrollment Operations
    ClassEnrollmentDTO enrollStudent(UUID studentUuid, UUID classSessionUuid);
    ClassEnrollmentDTO cancelEnrollment(UUID enrollmentUuid, String reason);
    ClassEnrollmentDTO transferEnrollment(UUID enrollmentUuid, UUID newClassSessionUuid);
    
    // Validation
    EnrollmentEligibilityDTO checkEnrollmentEligibility(UUID studentUuid, UUID classSessionUuid);
    boolean canStudentEnroll(UUID studentUuid, UUID classSessionUuid);
    List<ConflictDTO> findEnrollmentConflicts(UUID studentUuid, UUID classSessionUuid);
    
    // Attendance Management
    void markAttendance(UUID enrollmentUuid, AttendanceDTO attendance);
    void markBulkAttendance(UUID classSessionUuid, List<AttendanceDTO> attendanceList);
    AttendanceReportDTO generateAttendanceReport(UUID classSessionUuid);
    
    // Student Operations
    Page<ClassEnrollmentDTO> getStudentEnrollments(UUID studentUuid, EnrollmentFilterDTO filter, Pageable pageable);
    List<ClassSessionDTO> getUpcomingClasses(UUID studentUuid);
    StudentScheduleDTO getStudentSchedule(UUID studentUuid, LocalDate startDate, LocalDate endDate);
    
    // Class Management
    Page<ClassEnrollmentDTO> getClassEnrollments(UUID classSessionUuid, Pageable pageable);
    EnrollmentSummaryDTO getEnrollmentSummary(UUID classSessionUuid);
    
    // Waitlist Operations
    ClassWaitlistDTO joinWaitlist(UUID studentUuid, UUID classSessionUuid);
    void processWaitlist(UUID classSessionUuid);
    List<ClassWaitlistDTO> getWaitlistForClass(UUID classSessionUuid);
}
```

#### InstructorAvailabilityService
```java
public interface InstructorAvailabilityService {
    // Availability Management
    void setWeeklyAvailability(UUID instructorUuid, List<WeeklyAvailabilitySlotDTO> slots);
    void setTemporaryAvailability(UUID instructorUuid, List<TemporaryAvailabilitySlotDTO> slots);
    void removeAvailability(UUID instructorUuid, UUID availabilityUuid);
    
    // Availability Queries
    List<AvailabilitySlotDTO> getInstructorAvailability(UUID instructorUuid, LocalDate date);
    List<AvailabilitySlotDTO> getAvailabilityRange(UUID instructorUuid, LocalDate startDate, LocalDate endDate);
    boolean isInstructorAvailable(UUID instructorUuid, LocalDateTime start, LocalDateTime end);
    
    // Conflict Detection
    List<ConflictDTO> findAvailabilityConflicts(UUID instructorUuid, LocalDateTime start, LocalDateTime end);
    List<AvailableTimeSlotDTO> findAvailableSlots(UUID instructorUuid, LocalDate date, Integer durationMinutes);
}
```

#### SchedulingConflictService
```java
public interface SchedulingConflictService {
    // Conflict Detection
    List<ConflictDTO> findInstructorConflicts(UUID instructorUuid, LocalDateTime start, LocalDateTime end);
    List<ConflictDTO> findStudentConflicts(UUID studentUuid, LocalDateTime start, LocalDateTime end);
    List<ConflictDTO> findResourceConflicts(String location, LocalDateTime start, LocalDateTime end);
    
    // Validation
    boolean hasSchedulingConflicts(ScheduleValidationRequest request);
    ScheduleValidationResultDTO validateSchedule(ScheduleValidationRequest request);
    
    // Resolution
    List<AlternativeTimeSlotDTO> suggestAlternativeSlots(ConflictResolutionRequest request);
    boolean canResolveConflicts(List<ConflictDTO> conflicts);
}
```

## Inter-Module Communication

### Spring Modulith Communication Patterns
The scheduling module follows Spring Modulith communication patterns using domain events for loose coupling between modules.

#### Event-Driven Communication
```java
// Scheduling module publishes events that other modules can consume
@Service
@RequiredArgsConstructor
public class SchedulingServiceImpl implements ClassSchedulingService {
    
    private final ApplicationEventPublisher eventPublisher;
    
    @Override
    @Transactional
    public ClassSessionDTO createClassSession(CreateClassSessionRequest request) {
        // Create class session
        ClassSession classSession = classSessionService.createClassSession(request);
        
        // Publish domain event
        ClassCreatedEvent event = ClassCreatedEvent.from(classSession);
        eventPublisher.publishEvent(event);
        
        return ClassSessionDTO.from(classSession);
    }
    
    @Override
    @Transactional
    public ClassEnrollmentDTO enrollStudent(UUID studentUuid, UUID classSessionUuid) {
        // Enroll student
        ClassEnrollment enrollment = enrollmentService.enrollStudent(studentUuid, classSessionUuid);
        ClassSession classSession = classSessionService.findByUuid(classSessionUuid);
        
        // Publish domain event
        StudentEnrolledEvent event = StudentEnrolledEvent.from(enrollment, classSession);
        eventPublisher.publishEvent(event);
        
        return ClassEnrollmentDTO.from(enrollment);
    }
}
```

#### Module Integration Points
```java
// Other modules can integrate with scheduling through SPI
@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {
    
    private final ClassSchedulingService schedulingService; // SPI injection
    
    @Override
    public CourseDTO getCourseWithScheduledClasses(UUID courseUuid) {
        CourseDTO course = findCourseByUuid(courseUuid);
        
        // Use scheduling module's public API
        List<ClassSessionDTO> scheduledClasses = schedulingService.getScheduledClassesForCourse(courseUuid);
        
        return course.withScheduledClasses(scheduledClasses);
    }
    
    @Override
    @Transactional
    public void deleteCourse(UUID courseUuid, String reason) {
        // Check if course has scheduled classes
        boolean hasScheduledClasses = schedulingService.hasScheduledClasses(courseUuid);
        
        if (hasScheduledClasses) {
            // Business rule: Cannot delete course with scheduled classes
            throw new CourseHasScheduledClassesException(
                "Cannot delete course with scheduled classes. Cancel classes first."
            );
        }
        
        // Proceed with course deletion
        courseRepository.deleteByUuid(courseUuid);
        
        // Publish event that scheduling module will consume
        eventPublisher.publishEvent(CourseDeletedEvent.of(courseUuid, reason));
    }
}
```

### Asynchronous Event Processing
```java
// Scheduling module handles events asynchronously to avoid blocking other modules
@Component
@RequiredArgsConstructor
@Slf4j
public class ExternalModuleEventHandler {
    
    private final ClassSessionService classSessionService;
    private final ClassEnrollmentService enrollmentService;
    
    @EventListener
    @Async
    @Transactional
    public void handleCourseDeleted(CourseDeletedEvent event) {
        log.info("Processing course deletion for scheduling module: {}", event.courseUuid());
        
        try {
            // Cancel all scheduled classes for the deleted course
            List<ClassSessionDTO> scheduledClasses = classSessionService.getScheduledClassesForCourse(event.courseUuid());
            
            for (ClassSessionDTO scheduledClass : scheduledClasses) {
                classSessionService.cancelClassSession(
                    scheduledClass.uuid(), 
                    "Course was deleted: " + event.reason()
                );
            }
            
            log.info("Cancelled {} scheduled classes for deleted course {}", 
                scheduledClasses.size(), event.courseUuid());
                
        } catch (Exception e) {
            log.error("Failed to process course deletion event for course {}", event.courseUuid(), e);
            // Could implement retry logic or dead letter queue here
        }
    }
    
    @EventListener
    @Async
    @Transactional
    public void handleInstructorDeactivated(InstructorDeactivatedEvent event) {
        log.info("Processing instructor deactivation for scheduling module: {}", event.instructorUuid());
        
        try {
            // Cancel all future classes for deactivated instructor
            Page<ClassSessionDTO> futureClasses = classSessionService.findFutureClassesByInstructor(
                event.instructorUuid(), Pageable.unpaged());
                
            for (ClassSessionDTO classSession : futureClasses.getContent()) {
                classSessionService.cancelClassSession(
                    classSession.uuid(),
                    "Instructor account was deactivated"
                );
            }
            
            log.info("Cancelled {} future classes for deactivated instructor {}", 
                futureClasses.getTotalElements(), event.instructorUuid());
                
        } catch (Exception e) {
            log.error("Failed to process instructor deactivation event for instructor {}", 
                event.instructorUuid(), e);
        }
    }
    
    @EventListener
    @Async
    @Transactional
    public void handleStudentDeactivated(StudentDeactivatedEvent event) {
        log.info("Processing student deactivation for scheduling module: {}", event.studentUuid());
        
        try {
            // Cancel all future enrollments for deactivated student
            Page<ClassEnrollmentDTO> futureEnrollments = enrollmentService.getFutureEnrollments(
                event.studentUuid(), Pageable.unpaged());
                
            for (ClassEnrollmentDTO enrollment : futureEnrollments.getContent()) {
                enrollmentService.cancelEnrollment(
                    enrollment.uuid(),
                    "Student account was deactivated"
                );
            }
            
            log.info("Cancelled {} future enrollments for deactivated student {}", 
                futureEnrollments.getTotalElements(), event.studentUuid());
                
        } catch (Exception e) {
            log.error("Failed to process student deactivation event for student {}", 
                event.studentUuid(), e);
        }
    }
}
```

### Module Boundaries Enforcement
```java
// ArchUnit tests to enforce module boundaries
@AnalyzeClasses(packagesOf = ElimikaApplication.class)
public class SchedulingModuleBoundariesTest {
    
    @Test
    void schedulingModuleShouldNotAccessOtherModulesInternals() {
        noClasses()
            .that().resideInAPackage("..scheduling..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "..course.internal..", 
                "..instructor.internal..",
                "..student.internal..",
                "..notifications.internal.."
            )
            .check(importedClasses);
    }
    
    @Test
    void otherModulesShouldNotAccessSchedulingInternals() {
        noClasses()
            .that().resideOutsideOfPackage("..scheduling..")
            .should().dependOnClassesThat().resideInAPackage("..scheduling.internal..")
            .check(importedClasses);
    }
    
    @Test
    void schedulingModuleShouldOnlyExposePublicAPI() {
        classes()
            .that().resideInAPackage("..scheduling.api..")
            .should().bePublic()
            .andShould().haveSimpleNameNotContaining("Impl")
            .check(importedClasses);
    }
    
    @Test
    void schedulingEventsShouldBeSerializable() {
        classes()
            .that().resideInAPackage("..scheduling.api.events..")
            .and().areAnnotatedWith(DomainEvent.class)
            .should().implement(Serializable.class)
            .check(importedClasses);
    }
}
```

### Integration Testing with Modulith
```java
@SpringBootTest
@DirtiesContext
public class SchedulingModuleIntegrationTest {
    
    @Autowired
    private ApplicationModuleTest moduleTest;
    
    @Test
    void shouldVerifyModuleStructure() {
        // Verify module structure and dependencies
        moduleTest.verify();
    }
    
    @Test
    void shouldPublishAndConsumeEvents() {
        moduleTest.publishEvent(CourseDeletedEvent.of(UUID.randomUUID(), "Test deletion"))
            .andWaitForEventOfType(ClassCancelledEvent.class)
            .toArrive();
    }
    
    @Test
    void shouldRespectModuleBoundaries() {
        // Test that scheduling module cannot access internals of other modules
        assertThat(moduleTest.getModule("scheduling"))
            .doesNotDependOn("course.internal")
            .doesNotDependOn("instructor.internal")
            .doesNotDependOn("student.internal");
    }
}
```

### Failure Handling and Resilience
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class SchedulingEventFailureHandler {
    
    private final SchedulingProperties properties;
    private final RetryTemplate retryTemplate;
    
    @EventListener
    @RetryableTopic(
        attempts = "3",
        backoff = @Backoff(delay = 1000, multiplier = 2.0),
        include = {DataAccessException.class, TransientException.class}
    )
    public void handleEventWithRetry(ExternalModuleEvent event) {
        try {
            processEvent(event);
        } catch (Exception e) {
            log.error("Failed to process event: {}", event, e);
            
            if (properties.getFailureHandling().isDeadLetterEnabled()) {
                // Send to dead letter queue for manual processing
                deadLetterService.sendToDeadLetterQueue(event, e);
            }
            
            throw e; // Rethrow for retry mechanism
        }
    }
    
    @DltHandler
    public void handleFailedEvent(ExternalModuleEvent event, Exception exception) {
        log.error("Event processing failed after all retries: {}", event, exception);
        
        // Could implement:
        // 1. Alert administrators
        // 2. Store in failure audit log
        // 3. Schedule for manual review
        
        auditService.logEventFailure(event, exception, "Dead letter queue");
    }
}
```

## API Design

### REST Endpoints

#### ClassSessionController
```java
@RestController
@RequestMapping("/api/v1/classes")
@PreAuthorize("hasRole('USER')")
public class ClassSessionController {
    
    // Create Class (Instructors/Orgs only)
    @PostMapping
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ORGANIZATION_ADMIN')")
    ResponseEntity<ClassSessionDTO> createClass(@Valid @RequestBody CreateClassSessionRequest request);
    
    // Update Class
    @PutMapping("/{uuid}")
    @PreAuthorize("@classAuthorizationService.canManageClass(#uuid, authentication)")
    ResponseEntity<ClassSessionDTO> updateClass(@PathVariable UUID uuid, 
                                               @Valid @RequestBody UpdateClassSessionRequest request);
    
    // Get Class Details
    @GetMapping("/{uuid}")
    @PreAuthorize("@classAuthorizationService.canViewClass(#uuid, authentication)")
    ResponseEntity<ClassSessionDTO> getClass(@PathVariable UUID uuid);
    
    // Search Classes
    @GetMapping("/search")
    ResponseEntity<Page<ClassSessionDTO>> searchClasses(
        @Valid @ModelAttribute ClassSearchCriteria criteria, 
        Pageable pageable);
    
    // Cancel Class
    @DeleteMapping("/{uuid}")
    @PreAuthorize("@classAuthorizationService.canManageClass(#uuid, authentication)")
    ResponseEntity<Void> cancelClass(@PathVariable UUID uuid, 
                                   @RequestParam String reason);
    
    // Get Available Time Slots
    @GetMapping("/available-slots")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ORGANIZATION_ADMIN')")
    ResponseEntity<List<AvailableTimeSlotDTO>> getAvailableSlots(
        @RequestParam UUID instructorUuid,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date);
    
    // Recurring Classes
    @PostMapping("/recurring")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ORGANIZATION_ADMIN')")
    ResponseEntity<List<ClassSessionDTO>> createRecurringClasses(
        @Valid @RequestBody CreateRecurringClassRequest request);
}
```

#### ClassEnrollmentController
```java
@RestController
@RequestMapping("/api/v1/enrollments")
@PreAuthorize("hasRole('USER')")
public class ClassEnrollmentController {
    
    // Enroll in Class (Students only)
    @PostMapping("/enroll")
    @PreAuthorize("hasRole('STUDENT')")
    ResponseEntity<ClassEnrollmentDTO> enrollInClass(@Valid @RequestBody EnrollmentRequest request);
    
    // Cancel Enrollment
    @PutMapping("/{uuid}/cancel")
    @PreAuthorize("@enrollmentAuthorizationService.canCancelEnrollment(#uuid, authentication)")
    ResponseEntity<ClassEnrollmentDTO> cancelEnrollment(@PathVariable UUID uuid,
                                                       @RequestParam String reason);
    
    // Get Student's Enrollments
    @GetMapping("/my-enrollments")
    @PreAuthorize("hasRole('STUDENT')")
    ResponseEntity<Page<ClassEnrollmentDTO>> getMyEnrollments(
        @Valid @ModelAttribute EnrollmentFilterDTO filter,
        Pageable pageable);
    
    // Get Class Enrollments (Instructor/Admin view)
    @GetMapping("/class/{classUuid}")
    @PreAuthorize("@classAuthorizationService.canViewClassEnrollments(#classUuid, authentication)")
    ResponseEntity<Page<ClassEnrollmentDTO>> getClassEnrollments(@PathVariable UUID classUuid,
                                                                Pageable pageable);
    
    // Mark Attendance
    @PostMapping("/{uuid}/attendance")
    @PreAuthorize("@enrollmentAuthorizationService.canMarkAttendance(#uuid, authentication)")
    ResponseEntity<Void> markAttendance(@PathVariable UUID uuid,
                                       @Valid @RequestBody AttendanceDTO attendance);
}
```

### Request/Response DTOs

#### Request DTOs
```java
public record CreateClassSessionRequest(
    @NotBlank String title,
    String description,
    UUID instructorUuid,
    UUID organisationUuid,
    UUID trainingBranchUuid,
    UUID courseUuid,
    UUID trainingProgramUuid,
    UUID lessonUuid,
    
    @NotNull @Future LocalDateTime startDateTime,
    @NotNull @Future LocalDateTime endDateTime,
    @NotBlank String timezone,
    
    @NotNull SessionType sessionType,
    String location,
    String meetingLink,
    String meetingPassword,
    
    @Min(1) @Max(1000) Integer maxCapacity,
    Boolean allowWaitlist,
    
    Boolean isRecurring,
    ClassRecurrencePatternDTO recurrencePattern
) {}

public record CreateRecurringClassRequest(
    @Valid CreateClassSessionRequest classSession,
    @Valid @NotNull ClassRecurrencePatternDTO recurrencePattern
) {}

public record EnrollmentRequest(
    @NotNull UUID classSessionUuid
) {}
```

#### Response DTOs
```java
public record ClassSessionDTO(
    UUID uuid,
    String title,
    String description,
    UUID instructorUuid,
    String instructorName,
    UUID organisationUuid,
    String organisationName,
    
    LocalDateTime startDateTime,
    LocalDateTime endDateTime,
    Integer durationMinutes,
    String timezone,
    
    SessionType sessionType,
    String location,
    String meetingLink,
    Boolean hasMeetingPassword,
    
    Integer maxCapacity,
    Integer currentEnrollments,
    Integer availableSpots,
    Boolean allowWaitlist,
    Integer waitlistCount,
    
    Boolean isActive,
    SessionStatus status,
    String cancellationReason,
    
    Boolean isRecurring,
    UUID recurrencePatternUuid,
    UUID parentSessionUuid,
    
    LocalDateTime createdDate,
    LocalDateTime lastModifiedDate,
    String createdBy,
    String lastModifiedBy,
    
    // Computed fields
    Boolean canEnroll,
    Boolean isUpcoming,
    Boolean isPast,
    Boolean isOngoing,
    Long timeUntilStart,
    String formattedSchedule
) {}

public record ClassEnrollmentDTO(
    UUID uuid,
    UUID classSessionUuid,
    ClassSessionDTO classSession,
    UUID studentUuid,
    StudentDTO student,
    
    LocalDateTime enrollmentDate,
    EnrollmentStatus enrollmentStatus,
    
    LocalDateTime cancellationDate,
    String cancellationReason,
    UUID cancelledBy,
    
    LocalDateTime attendanceMarkedDate,
    UUID attendanceMarkedBy,
    LocalDateTime joinTime,
    LocalDateTime leaveTime,
    Boolean attended,
    String attendanceNotes,
    
    Integer waitlistPosition,
    Boolean waitlistNotificationSent,
    
    LocalDateTime createdDate,
    LocalDateTime lastModifiedDate
) {}
```

## Security & Authorization

### Role-Based Access Control

#### Roles and Permissions
```yaml
ROLES:
  SUPER_ADMIN:
    - Can manage all classes across all organizations
    - Can view all statistics and reports
    - Can manage system-wide scheduling settings
    
  ORGANIZATION_ADMIN:
    - Can create classes for their organization
    - Can manage classes created by their organization
    - Can view enrollment statistics for their classes
    - Can assign instructors to classes
    
  INSTRUCTOR:
    - Can create classes they will teach
    - Can manage their own classes
    - Can set their availability
    - Can mark attendance for their classes
    - Can view enrollment lists for their classes
    
  STUDENT:
    - Can enroll in available classes
    - Can view their enrolled classes
    - Can cancel their enrollments
    - Can join waitlists
    - Can view their schedule
    
  GUEST:
    - Can browse public class listings
    - Cannot enroll or create classes
```

#### Authorization Service
```java
@Service
public class ClassAuthorizationService {
    
    public boolean canCreateClass(Authentication auth, UUID instructorUuid, UUID organisationUuid) {
        String role = extractRole(auth);
        UUID currentUserUuid = extractUserUuid(auth);
        
        return switch (role) {
            case "SUPER_ADMIN" -> true;
            case "ORGANIZATION_ADMIN" -> userBelongsToOrganisation(currentUserUuid, organisationUuid);
            case "INSTRUCTOR" -> currentUserUuid.equals(instructorUuid) && 
                               instructorBelongsToOrganisation(instructorUuid, organisationUuid);
            default -> false;
        };
    }
    
    public boolean canManageClass(UUID classSessionUuid, Authentication auth) {
        ClassSession classSession = classSessionRepository.findByUuid(classSessionUuid);
        String role = extractRole(auth);
        UUID currentUserUuid = extractUserUuid(auth);
        
        return switch (role) {
            case "SUPER_ADMIN" -> true;
            case "ORGANIZATION_ADMIN" -> 
                classSession.getOrganisationUuid() != null && 
                userBelongsToOrganisation(currentUserUuid, classSession.getOrganisationUuid());
            case "INSTRUCTOR" -> 
                currentUserUuid.equals(classSession.getInstructorUuid());
            default -> false;
        };
    }
}
```

## Business Logic & Validation

### Scheduling Validation Rules

#### Core Business Rules
```java
@Component
public class SchedulingBusinessRules {
    
    // Rule 1: No overlapping classes for instructors
    @Rule("INSTRUCTOR_NO_OVERLAP")
    public ValidationResult validateInstructorOverlap(UUID instructorUuid, LocalDateTime start, LocalDateTime end) {
        boolean hasOverlap = classSessionRepository.existsByInstructorUuidAndTimeOverlap(
            instructorUuid, start, end);
            
        return ValidationResult.builder()
            .valid(!hasOverlap)
            .errorCode("INSTRUCTOR_SCHEDULE_CONFLICT")
            .message("Instructor has conflicting class scheduled")
            .build();
    }
    
    // Rule 2: No overlapping enrollments for students
    @Rule("STUDENT_NO_OVERLAP")
    public ValidationResult validateStudentOverlap(UUID studentUuid, LocalDateTime start, LocalDateTime end) {
        boolean hasOverlap = classEnrollmentRepository.existsByStudentUuidAndTimeOverlap(
            studentUuid, start, end);
            
        return ValidationResult.builder()
            .valid(!hasOverlap)
            .errorCode("STUDENT_SCHEDULE_CONFLICT")
            .message("Student has conflicting class enrollment")
            .build();
    }
    
    // Rule 3: Class capacity limits
    @Rule("CAPACITY_LIMIT")
    public ValidationResult validateCapacity(UUID classSessionUuid) {
        ClassSession session = classSessionRepository.findByUuid(classSessionUuid);
        int currentEnrollments = classEnrollmentRepository.countActiveEnrollments(classSessionUuid);
        
        return ValidationResult.builder()
            .valid(currentEnrollments < session.getMaxCapacity())
            .errorCode("CLASS_FULL")
            .message("Class has reached maximum capacity")
            .build();
    }
    
    // Rule 4: Minimum advance booking
    @Rule("ADVANCE_BOOKING")
    public ValidationResult validateAdvanceBooking(LocalDateTime classStart, Integer minHours) {
        LocalDateTime minBookingTime = LocalDateTime.now().plusHours(minHours);
        
        return ValidationResult.builder()
            .valid(classStart.isAfter(minBookingTime))
            .errorCode("INSUFFICIENT_ADVANCE_BOOKING")
            .message(String.format("Class must be booked at least %d hours in advance", minHours))
            .build();
    }
    
    // Rule 5: Instructor availability
    @Rule("INSTRUCTOR_AVAILABILITY")
    public ValidationResult validateInstructorAvailability(UUID instructorUuid, LocalDateTime start, LocalDateTime end) {
        boolean isAvailable = instructorAvailabilityService.isInstructorAvailable(instructorUuid, start, end);
        
        return ValidationResult.builder()
            .valid(isAvailable)
            .errorCode("INSTRUCTOR_NOT_AVAILABLE")
            .message("Instructor is not available during the requested time")
            .build();
    }
}
```

#### Validation Orchestrator
```java
@Service
public class ClassValidationService {
    
    private final List<BusinessRule> businessRules;
    
    public ValidationResult validateClassCreation(CreateClassSessionRequest request) {
        ValidationContext context = ValidationContext.builder()
            .operation("CREATE_CLASS")
            .instructorUuid(request.instructorUuid())
            .startDateTime(request.startDateTime())
            .endDateTime(request.endDateTime())
            .timezone(request.timezone())
            .build();
            
        return executeValidationRules(context, Arrays.asList(
            "INSTRUCTOR_NO_OVERLAP",
            "INSTRUCTOR_AVAILABILITY", 
            "DURATION_LIMITS",
            "WORKING_HOURS",
            "ADVANCE_BOOKING"
        ));
    }
    
    public ValidationResult validateEnrollment(UUID studentUuid, UUID classSessionUuid) {
        ClassSession classSession = classSessionRepository.findByUuid(classSessionUuid);
        
        ValidationContext context = ValidationContext.builder()
            .operation("STUDENT_ENROLLMENT")
            .studentUuid(studentUuid)
            .classSessionUuid(classSessionUuid)
            .startDateTime(classSession.getStartDateTime())
            .endDateTime(classSession.getEndDateTime())
            .build();
            
        return executeValidationRules(context, Arrays.asList(
            "STUDENT_NO_OVERLAP",
            "CAPACITY_LIMIT",
            "ADVANCE_BOOKING"
        ));
    }
}
```

## Integration with Existing System

### Course/Program Integration

#### Enhanced Course Service
```java
@Service
public class CourseServiceImpl implements CourseService {
    
    // Existing methods...
    
    // New methods for class integration
    public List<ClassSessionDTO> getScheduledClassesForCourse(UUID courseUuid) {
        return classSessionService.getClassesByCourse(courseUuid);
    }
    
    public List<ClassSessionDTO> getUpcomingClassesForCourse(UUID courseUuid) {
        return classSessionService.getUpcomingClassesByCourse(courseUuid);
    }
    
    // Enhanced enrollment method
    @Override
    public CourseEnrollmentDTO enrollInCourse(UUID studentUuid, UUID courseUuid) {
        CourseEnrollmentDTO enrollment = super.enrollInCourse(studentUuid, courseUuid);
        
        // Send notification about available classes
        List<ClassSessionDTO> availableClasses = getUpcomingClassesForCourse(courseUuid);
        if (!availableClasses.isEmpty()) {
            notificationService.sendAvailableClassesNotification(studentUuid, courseUuid, availableClasses);
        }
        
        return enrollment;
    }
}
```

### Database Migration Scripts

#### Add Class Scheduling Tables
```sql
-- Migration: V2.0.1__Add_Class_Scheduling_Tables.sql

-- Create class_sessions table
CREATE TABLE class_sessions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    
    -- Basic Information
    title VARCHAR(255) NOT NULL,
    description TEXT,
    
    -- Ownership
    instructor_uuid VARCHAR(36),
    organisation_uuid VARCHAR(36),
    training_branch_uuid VARCHAR(36),
    
    -- Content Association (Optional)
    course_uuid VARCHAR(36),
    training_program_uuid VARCHAR(36),
    lesson_uuid VARCHAR(36),
    
    -- Scheduling
    start_date_time DATETIME NOT NULL,
    end_date_time DATETIME NOT NULL,
    duration_minutes INTEGER NOT NULL,
    timezone VARCHAR(50) DEFAULT 'UTC',
    
    -- Session Details
    session_type ENUM('ONLINE', 'IN_PERSON', 'HYBRID') NOT NULL,
    location VARCHAR(500),
    meeting_link VARCHAR(500),
    meeting_password VARCHAR(100),
    
    -- Capacity Management
    max_capacity INTEGER DEFAULT 50,
    current_enrollments INTEGER DEFAULT 0,
    allow_waitlist BOOLEAN DEFAULT false,
    
    -- Status
    is_active BOOLEAN DEFAULT true,
    status ENUM('SCHEDULED', 'ONGOING', 'COMPLETED', 'CANCELLED') DEFAULT 'SCHEDULED',
    cancellation_reason TEXT,
    
    -- Recurrence
    is_recurring BOOLEAN DEFAULT false,
    recurrence_pattern_uuid VARCHAR(36),
    parent_session_uuid VARCHAR(36),
    
    -- Audit Fields
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_date DATETIME ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    
    -- Indexes
    INDEX idx_instructor_datetime (instructor_uuid, start_date_time),
    INDEX idx_org_datetime (organisation_uuid, start_date_time),
    INDEX idx_course_datetime (course_uuid, start_date_time),
    INDEX idx_status_datetime (status, start_date_time),
    
    -- Foreign Keys
    FOREIGN KEY (instructor_uuid) REFERENCES instructors(uuid) ON DELETE SET NULL,
    FOREIGN KEY (organisation_uuid) REFERENCES organisations(uuid) ON DELETE SET NULL,
    FOREIGN KEY (course_uuid) REFERENCES courses(uuid) ON DELETE SET NULL,
    FOREIGN KEY (training_program_uuid) REFERENCES training_programs(uuid) ON DELETE SET NULL,
    FOREIGN KEY (training_branch_uuid) REFERENCES training_branches(uuid) ON DELETE SET NULL
);

-- Additional tables follow the same pattern...
```

## Implementation Phases

### Phase 1: Foundation (Weeks 1-3) ⭐
**Priority**: Critical
**Dependencies**: None

#### Week 1: Database Setup
- [ ] Create database migration scripts
- [ ] Run migrations in development environment
- [ ] Set up basic entity classes
- [ ] Configure JPA relationships
- [ ] Create basic repository interfaces

#### Week 2: Core Entities & Services  
- [ ] Implement `ClassSession` entity and repository
- [ ] Create `ClassSessionService` interface and implementation
- [ ] Implement basic CRUD operations
- [ ] Add `ClassSessionController` with basic endpoints
- [ ] Create DTOs and Factory classes

#### Week 3: Basic Enrollment
- [ ] Implement `ClassEnrollment` entity and repository
- [ ] Create `ClassEnrollmentService` interface and implementation
- [ ] Add enrollment and cancellation APIs
- [ ] Implement basic capacity validation
- [ ] Add enrollment status tracking

**Deliverables**:
- Working class creation and basic enrollment
- Basic REST APIs functional
- Database schema established

### Phase 2: Conflict Detection & Validation (Weeks 4-5) ⭐
**Priority**: High
**Dependencies**: Phase 1

#### Week 4: Business Rules Engine
- [ ] Implement `SchedulingBusinessRules` component
- [ ] Create validation framework
- [ ] Add instructor conflict detection
- [ ] Add student conflict detection
- [ ] Implement capacity validation

#### Week 5: Advanced Validation
- [ ] Add advance booking validation
- [ ] Implement working hours validation
- [ ] Add duration limits validation
- [ ] Create conflict resolution service
- [ ] Add alternative time slot suggestions

**Deliverables**:
- Comprehensive validation system
- Conflict detection working
- Business rules enforceable

### Phase 3: Instructor Availability (Weeks 6-7) ⭐  
**Priority**: High
**Dependencies**: Phase 2

#### Week 6: Availability Management
- [ ] Implement `InstructorAvailability` entity
- [ ] Create availability management service
- [ ] Add availability CRUD APIs
- [ ] Implement recurring availability patterns
- [ ] Add timezone support

#### Week 7: Availability Integration
- [ ] Integrate availability with conflict detection
- [ ] Add availability-based scheduling
- [ ] Implement availability override functionality
- [ ] Add bulk availability management
- [ ] Create availability calendar views

**Deliverables**:
- Instructor availability system functional
- Scheduling respects availability constraints
- Availability management UI ready

### Phase 4: Recurring Classes (Weeks 8-9) ⭐
**Priority**: Medium
**Dependencies**: Phase 3

#### Week 8: Recurrence Patterns
- [ ] Implement `ClassRecurrencePattern` entity
- [ ] Create recurrence pattern service
- [ ] Add daily/weekly/monthly patterns
- [ ] Implement pattern validation
- [ ] Add recurrence conflict detection

#### Week 9: Recurring Class Management
- [ ] Add recurring class creation APIs
- [ ] Implement series management
- [ ] Add bulk update/cancel functionality
- [ ] Create recurring class calendar views
- [ ] Add exception handling for recurring series

**Deliverables**:
- Recurring class system working
- Pattern-based scheduling functional
- Series management operational

### Phase 5: Waitlist & Advanced Features (Weeks 10-11)
**Priority**: Medium
**Dependencies**: Phase 4

#### Week 10: Waitlist System
- [ ] Implement `ClassWaitlist` entity
- [ ] Create waitlist management service
- [ ] Add automatic enrollment from waitlist
- [ ] Implement waitlist position tracking
- [ ] Add waitlist notification system

#### Week 11: Advanced Features
- [ ] Add attendance tracking
- [ ] Implement class status management
- [ ] Add class statistics and reporting
- [ ] Create batch operations
- [ ] Add data export functionality

**Deliverables**:
- Waitlist system operational
- Advanced class management features
- Comprehensive reporting available

### Phase 6: Timetable Views & Calendar Interface (Weeks 12-14) ⭐
**Priority**: High  
**Dependencies**: Phase 5

#### Week 12: Calendar Grid Components
- [ ] Create weekly timetable grid component
- [ ] Implement daily schedule view
- [ ] Add monthly calendar overview
- [ ] Build time slot grid system
- [ ] Create responsive calendar layouts

#### Week 13: Interactive Timetable Features
- [ ] Add drag-and-drop scheduling interface
- [ ] Implement click-to-schedule functionality
- [ ] Create color-coded class type visualization
- [ ] Add conflict highlighting in calendar views
- [ ] Build quick scheduling modals

#### Week 14: Advanced Timetable Operations
- [ ] Implement bulk scheduling operations
- [ ] Add template-based scheduling (copy schedules)
- [ ] Create auto-scheduling suggestions
- [ ] Add resource allocation views (rooms/equipment)
- [ ] Implement academic calendar integration

**Deliverables**:
- Full interactive timetable interface
- Visual calendar-based scheduling
- Drag-and-drop class management
- Bulk scheduling capabilities

### Phase 7: Integration & Enhancement (Weeks 15-16)
**Priority**: Medium
**Dependencies**: Phase 6

#### Week 15: Course/Program Integration
- [ ] Link classes to existing courses
- [ ] Add program-based scheduling
- [ ] Implement hybrid completion tracking
- [ ] Add class-based progress tracking
- [ ] Create integrated reporting

#### Week 16: Notifications & Polish
- [ ] Implement email notification system
- [ ] Add class reminder notifications
- [ ] Create scheduling digest emails
- [ ] Add mobile push notifications
- [ ] Implement notification preferences

**Deliverables**:
- Full integration with existing system
- Comprehensive notification system
- Production-ready scheduling platform

## Technical Considerations

### Performance Optimization

#### Database Optimization
```sql
-- Indexing strategy for common queries
CREATE INDEX idx_upcoming_classes 
ON class_sessions (start_date_time, status, is_active) 
WHERE start_date_time > NOW() AND status = 'SCHEDULED';

-- Materialized view for instructor schedules
CREATE MATERIALIZED VIEW instructor_schedule_summary AS
SELECT 
    instructor_uuid,
    DATE(start_date_time) as class_date,
    COUNT(*) as total_classes,
    SUM(duration_minutes) as total_minutes,
    MIN(start_date_time) as first_class,
    MAX(end_date_time) as last_class
FROM class_sessions 
WHERE status = 'SCHEDULED' AND is_active = true
GROUP BY instructor_uuid, DATE(start_date_time);
```

#### Caching Strategy
```java
@Service
@CacheConfig(cacheNames = "scheduling")
public class ClassSessionServiceImpl implements ClassSessionService {
    
    @Cacheable(key = "#instructorUuid + '_' + #date")
    public List<AvailableTimeSlotDTO> getAvailableTimeSlots(UUID instructorUuid, LocalDate date) {
        // Implementation with expensive calculations
    }
    
    @Cacheable(key = "#studentUuid + '_upcoming'", unless = "#result.isEmpty()")
    public List<ClassSessionDTO> getUpcomingClasses(UUID studentUuid) {
        // Cache upcoming classes for frequent student requests
    }
    
    @CacheEvict(allEntries = true)
    public ClassSessionDTO createClassSession(CreateClassSessionRequest request) {
        // Clear cache when new classes are created
    }
}
```

### Scalability Considerations

#### Async Processing
```java
@Service
public class AsyncSchedulingService {
    
    @Async("schedulingTaskExecutor")
    public CompletableFuture<Void> processRecurringClassCreation(CreateRecurringClassRequest request) {
        // Process large recurring series in background
        List<ClassSessionDTO> createdClasses = new ArrayList<>();
        
        for (LocalDateTime classTime : generateRecurrenceDates(request)) {
            ClassSessionDTO classSession = classSessionService.createClassSession(
                buildClassSessionRequest(request, classTime));
            createdClasses.add(classSession);
        }
        
        // Send completion notification
        notificationService.sendRecurringClassCreationComplete(
            request.getCreatedBy(), createdClasses.size());
            
        return CompletableFuture.completedFuture(null);
    }
}
```

#### Event-Driven Architecture
```java
@Component
public class SchedulingEventPublisher {
    
    private final ApplicationEventPublisher eventPublisher;
    
    public void publishClassCreated(ClassSession classSession) {
        eventPublisher.publishEvent(ClassCreatedEvent.builder()
            .classSessionUuid(classSession.getUuid())
            .instructorUuid(classSession.getInstructorUuid())
            .organisationUuid(classSession.getOrganisationUuid())
            .startDateTime(classSession.getStartDateTime())
            .createdBy(classSession.getCreatedBy())
            .timestamp(LocalDateTime.now())
            .build());
    }
}
```

## Testing Strategy

### Unit Testing
```java
@ExtendWith(MockitoExtension.class)
class ClassSessionServiceTest {
    
    @Mock
    private ClassSessionRepository classSessionRepository;
    
    @Mock
    private SchedulingConflictService conflictService;
    
    @InjectMocks
    private ClassSessionServiceImpl classSessionService;
    
    @Test
    @DisplayName("Should create class successfully when no conflicts")
    void createClass_NoConflicts_Success() {
        // Given
        CreateClassSessionRequest request = createValidRequest();
        when(conflictService.hasSchedulingConflicts(any())).thenReturn(false);
        when(classSessionRepository.save(any())).thenReturn(createClassSession());
        
        // When
        ClassSessionDTO result = classSessionService.createClassSession(request);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.title()).isEqualTo(request.title());
        verify(classSessionRepository).save(any(ClassSession.class));
    }
    
    @Test
    @DisplayName("Should throw exception when instructor has conflict")
    void createClass_InstructorConflict_ThrowsException() {
        // Given
        CreateClassSessionRequest request = createValidRequest();
        when(conflictService.hasSchedulingConflicts(any())).thenReturn(true);
        when(conflictService.findInstructorConflicts(any(), any(), any()))
            .thenReturn(List.of(createConflict()));
        
        // When & Then
        assertThatThrownBy(() -> classSessionService.createClassSession(request))
            .isInstanceOf(SchedulingConflictException.class)
            .hasMessageContaining("Instructor has conflicting class");
    }
}
```

### Integration Testing
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ClassSessionControllerIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private ClassSessionRepository classSessionRepository;
    
    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    @DisplayName("Should create class successfully via API")
    void createClass_ValidRequest_ReturnsCreatedClass() {
        // Given
        CreateClassSessionRequest request = CreateClassSessionRequest.builder()
            .title("Java Programming Basics")
            .description("Introduction to Java programming")
            .instructorUuid(TestDataFactory.INSTRUCTOR_UUID)
            .startDateTime(LocalDateTime.now().plusDays(1))
            .endDateTime(LocalDateTime.now().plusDays(1).plusHours(2))
            .timezone("UTC")
            .sessionType(SessionType.ONLINE)
            .maxCapacity(25)
            .build();
        
        // When
        ResponseEntity<ClassSessionDTO> response = restTemplate.postForEntity(
            "/api/v1/classes", request, ClassSessionDTO.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().title()).isEqualTo(request.title());
    }
}
```

## Deployment & Migration

### Database Migration Strategy
```yaml
# Deployment Strategy
deployment:
  strategy: blue-green
  
  migration:
    phase-1: # Create new tables
      - V2.0.1__Add_Class_Scheduling_Tables.sql
      - V2.0.2__Add_Class_Scheduling_Indexes.sql
      
    phase-2: # Add foreign key constraints after data migration
      - V2.0.3__Add_Class_Scheduling_Constraints.sql
      
    phase-3: # Optimize after full deployment
      - V2.0.4__Optimize_Class_Scheduling_Performance.sql

  rollback:
    enabled: true
    scripts:
      - R__Remove_Class_Scheduling_Tables.sql
```

### Configuration Updates

#### Application Properties
```yaml
# application.yml additions
elimika:
  scheduling:
    # Business rules configuration
    advance-booking:
      minimum-hours: 2
      maximum-days: 90
    
    class-duration:
      minimum-minutes: 15
      maximum-minutes: 480
      
    working-hours:
      start-time: "06:00"
      end-time: "23:00"
      timezone: "UTC"
    
    capacity:
      default-max-capacity: 50
      maximum-allowed-capacity: 500
      
    waitlist:
      enabled: true
      automatic-enrollment: true
      notification-timeout-hours: 24
    
    notifications:
      reminder-hours-before: [24, 2]
      digest-enabled: true
      digest-time: "08:00"
```

### Monitoring & Observability
```java
@Component
public class SchedulingMetrics {
    
    private final MeterRegistry meterRegistry;
    private final Counter classCreatedCounter;
    private final Counter enrollmentCounter;
    private final Timer conflictDetectionTimer;
    
    public SchedulingMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.classCreatedCounter = Counter.builder("classes.created")
            .description("Number of classes created")
            .tag("type", "scheduled")
            .register(meterRegistry);
            
        this.enrollmentCounter = Counter.builder("enrollments.created")
            .description("Number of student enrollments")
            .register(meterRegistry);
            
        this.conflictDetectionTimer = Timer.builder("scheduling.conflict_detection")
            .description("Time taken for conflict detection")
            .register(meterRegistry);
    }
    
    public void incrementClassCreated(SessionType sessionType) {
        classCreatedCounter.increment(Tags.of("session_type", sessionType.name()));
    }
    
    public void recordConflictDetectionTime(Duration duration) {
        conflictDetectionTimer.record(duration);
    }
}
```

---

## Summary

This implementation guide provides a comprehensive roadmap for adding calendar-based class scheduling to the Elimika educational platform. The system will support:

- **Complete Class Lifecycle**: Creation, enrollment, attendance, and completion tracking
- **Robust Conflict Detection**: Preventing scheduling conflicts for both instructors and students  
- **Flexible Scheduling**: Support for one-time and recurring classes with various patterns
- **Scalable Architecture**: Event-driven design supporting high concurrent usage
- **Comprehensive Security**: Role-based access control and audit trails
- **Integration Ready**: Seamless integration with existing course and training program systems

The phased implementation approach ensures gradual rollout with early value delivery, while the detailed technical specifications provide clear guidance for development teams.

### Key Benefits

✅ **Role-Based Access**: Only instructors/orgs create classes, students enroll  
✅ **Conflict Prevention**: Automatic detection of scheduling overlaps  
✅ **Flexible Integration**: Links to existing courses/programs or standalone  
✅ **Scalable Architecture**: Follows existing patterns in the codebase  
✅ **Comprehensive Coverage**: Online, in-person, and recurring class support

This plan extends your current educational platform with interactive, scheduled learning sessions while maintaining the existing self-paced course structure.

## Timetable Implementation Details

### Calendar Views & Components

#### Weekly Timetable Grid
```java
@Component
public class WeeklyTimetableComponent {
    
    @GetMapping("/api/v1/timetable/weekly")
    public ResponseEntity<WeeklyTimetableDTO> getWeeklyTimetable(
        @RequestParam(required = false) UUID instructorUuid,
        @RequestParam(required = false) UUID organisationUuid,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
        @RequestParam(defaultValue = "UTC") String timezone
    ) {
        WeeklyTimetableDTO timetable = timetableService.getWeeklyTimetable(
            instructorUuid, organisationUuid, weekStart, timezone);
        return ResponseEntity.ok(timetable);
    }
}

public record WeeklyTimetableDTO(
    LocalDate weekStartDate,
    LocalDate weekEndDate,
    String timezone,
    List<DayScheduleDTO> days,
    List<TimeSlotDTO> timeSlots,
    Map<String, List<ClassSessionDTO>> classSchedule // "MONDAY_09:00" -> List<ClassSessionDTO>
) {}

public record DayScheduleDTO(
    DayOfWeek dayOfWeek,
    LocalDate date,
    List<ClassSessionDTO> classes,
    Map<String, Integer> timeSlotOccupancy // "09:00" -> count of classes
) {}

public record TimeSlotDTO(
    LocalTime startTime,
    LocalTime endTime,
    String displayLabel, // "09:00 - 10:00"
    Boolean isStandardSlot,
    Integer durationMinutes
) {}
```

#### Monthly Calendar View
```java
@Component
public class MonthlyCalendarComponent {
    
    @GetMapping("/api/v1/timetable/monthly")
    public ResponseEntity<MonthlyCalendarDTO> getMonthlyCalendar(
        @RequestParam(required = false) UUID instructorUuid,
        @RequestParam(required = false) UUID organisationUuid,
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
        @RequestParam(defaultValue = "UTC") String timezone
    ) {
        MonthlyCalendarDTO calendar = timetableService.getMonthlyCalendar(
            instructorUuid, organisationUuid, month, timezone);
        return ResponseEntity.ok(calendar);
    }
}

public record MonthlyCalendarDTO(
    YearMonth month,
    String timezone,
    List<CalendarDayDTO> days,
    CalendarStatisticsDTO statistics
) {}

public record CalendarDayDTO(
    LocalDate date,
    List<ClassSessionDTO> classes,
    Integer totalClasses,
    Integer totalEnrollments,
    Boolean hasConflicts,
    DayType dayType // WEEKDAY, WEEKEND, HOLIDAY
) {}
```

#### Interactive Scheduling Interface
```java
@Component
public class InteractiveSchedulingComponent {
    
    // Drag and drop scheduling
    @PostMapping("/api/v1/timetable/move-class")
    @PreAuthorize("@classAuthorizationService.canManageClass(#request.classSessionUuid(), authentication)")
    public ResponseEntity<ClassSchedulingResultDTO> moveClass(
        @Valid @RequestBody MoveClassRequest request
    ) {
        ClassSchedulingResultDTO result = timetableService.moveClass(request);
        return ResponseEntity.ok(result);
    }
    
    // Quick scheduling modal
    @PostMapping("/api/v1/timetable/quick-schedule")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ORGANIZATION_ADMIN')")
    public ResponseEntity<ClassSchedulingResultDTO> quickScheduleClass(
        @Valid @RequestBody QuickScheduleRequest request
    ) {
        ClassSchedulingResultDTO result = timetableService.quickScheduleClass(request);
        return ResponseEntity.ok(result);
    }
    
    // Bulk scheduling
    @PostMapping("/api/v1/timetable/bulk-schedule")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ORGANIZATION_ADMIN')")
    public ResponseEntity<List<ClassSchedulingResultDTO>> bulkScheduleClasses(
        @Valid @RequestBody BulkScheduleRequest request
    ) {
        List<ClassSchedulingResultDTO> results = timetableService.bulkScheduleClasses(request);
        return ResponseEntity.ok(results);
    }
}

public record MoveClassRequest(
    @NotNull UUID classSessionUuid,
    @NotNull LocalDateTime newStartTime,
    @NotNull LocalDateTime newEndTime,
    String reason
) {}

public record QuickScheduleRequest(
    @NotBlank String title,
    @NotNull UUID instructorUuid,
    UUID courseUuid,
    @NotNull LocalDateTime startTime,
    @NotNull Integer durationMinutes,
    @NotNull SessionType sessionType,
    @Min(1) Integer maxCapacity
) {}

public record ClassSchedulingResultDTO(
    Boolean success,
    UUID classSessionUuid,
    List<ConflictDTO> conflicts,
    List<String> warnings,
    String message,
    ClassSessionDTO scheduledClass
) {}
```

### Time Slot Management System

#### Standard Time Slots Configuration
```java
@Service
public class TimeSlotManagementService {
    
    private final TimeSlotRepository timeSlotRepository;
    private final SchedulingProperties schedulingProperties;
    
    // Create standard institutional time slots
    @PostConstruct
    public void initializeStandardTimeSlots() {
        if (timeSlotRepository.count() == 0) {
            createStandardTimeSlots();
        }
    }
    
    private void createStandardTimeSlots() {
        // Standard academic time slots
        List<TimeSlot> standardSlots = Arrays.asList(
            createTimeSlot("08:00", "09:00", "Morning Slot 1"),
            createTimeSlot("09:00", "10:00", "Morning Slot 2"),
            createTimeSlot("10:00", "11:00", "Morning Slot 3"),
            createTimeSlot("11:00", "12:00", "Morning Slot 4"),
            createTimeSlot("13:00", "14:00", "Afternoon Slot 1"),
            createTimeSlot("14:00", "15:00", "Afternoon Slot 2"),
            createTimeSlot("15:00", "16:00", "Afternoon Slot 3"),
            createTimeSlot("16:00", "17:00", "Afternoon Slot 4"),
            createTimeSlot("18:00", "19:00", "Evening Slot 1"),
            createTimeSlot("19:00", "20:00", "Evening Slot 2")
        );
        
        timeSlotRepository.saveAll(standardSlots);
    }
    
    public List<AvailableTimeSlotDTO> getAvailableTimeSlots(
        UUID instructorUuid, LocalDate date, Integer durationMinutes
    ) {
        List<TimeSlot> allSlots = timeSlotRepository.findAllActiveSlots();
        List<ClassSession> existingClasses = classSessionRepository
            .findByInstructorUuidAndDate(instructorUuid, date);
            
        return allSlots.stream()
            .filter(slot -> isSlotAvailable(slot, existingClasses, date, durationMinutes))
            .map(this::toAvailableTimeSlotDTO)
            .collect(Collectors.toList());
    }
}

// Time slot entity
@Entity
@Table(name = "time_slots")
public class TimeSlot extends BaseEntity {
    
    @Column(name = "start_time")
    private LocalTime startTime;
    
    @Column(name = "end_time")
    private LocalTime endTime;
    
    @Column(name = "display_label")
    private String displayLabel;
    
    @Column(name = "duration_minutes")
    private Integer durationMinutes;
    
    @Column(name = "is_standard_slot")
    private Boolean isStandardSlot;
    
    @Column(name = "is_active")
    private Boolean isActive;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "slot_type")
    private SlotType slotType; // MORNING, AFTERNOON, EVENING
}
```

### Resource Allocation & Room Management

#### Room Scheduling Integration
```java
@Service
public class ResourceSchedulingService {
    
    // Check room/resource availability
    public boolean isResourceAvailable(String resourceId, LocalDateTime start, LocalDateTime end) {
        return !classSessionRepository.existsByResourceAndTimeOverlap(resourceId, start, end);
    }
    
    // Get available rooms for a time slot
    public List<RoomDTO> getAvailableRooms(LocalDateTime start, LocalDateTime end, Integer capacity) {
        List<Room> allRooms = roomRepository.findByCapacityGreaterThanEqual(capacity);
        List<String> occupiedRoomIds = classSessionRepository
            .findOccupiedResourcesByTimeRange(start, end);
            
        return allRooms.stream()
            .filter(room -> !occupiedRoomIds.contains(room.getId()))
            .map(RoomFactory::toDTO)
            .collect(Collectors.toList());
    }
    
    // Resource allocation for classes
    @PostMapping("/api/v1/timetable/allocate-resource")
    @PreAuthorize("hasRole('ORGANIZATION_ADMIN')")
    public ResponseEntity<ResourceAllocationDTO> allocateResource(
        @Valid @RequestBody ResourceAllocationRequest request
    ) {
        ResourceAllocationDTO allocation = resourceSchedulingService.allocateResource(request);
        return ResponseEntity.ok(allocation);
    }
}

public record ResourceAllocationRequest(
    @NotNull UUID classSessionUuid,
    @NotNull String resourceType, // "ROOM", "EQUIPMENT", "VEHICLE"
    @NotNull String resourceId,
    String notes
) {}

public record RoomDTO(
    String id,
    String name,
    String building,
    String floor,
    Integer capacity,
    List<String> amenities,
    Boolean hasProjector,
    Boolean hasWhiteboard,
    String location
) {}
```

### Template-Based Scheduling

#### Schedule Templates
```java
@Service
public class ScheduleTemplateService {
    
    // Create schedule template from existing classes
    @PostMapping("/api/v1/timetable/templates")
    @PreAuthorize("hasRole('ORGANIZATION_ADMIN')")
    public ResponseEntity<ScheduleTemplateDTO> createTemplate(
        @Valid @RequestBody CreateTemplateRequest request
    ) {
        ScheduleTemplateDTO template = scheduleTemplateService.createTemplate(request);
        return ResponseEntity.ok(template);
    }
    
    // Apply template to create new schedule
    @PostMapping("/api/v1/timetable/templates/{templateId}/apply")
    @PreAuthorize("hasRole('ORGANIZATION_ADMIN')")
    public ResponseEntity<BulkSchedulingResultDTO> applyTemplate(
        @PathVariable UUID templateId,
        @Valid @RequestBody ApplyTemplateRequest request
    ) {
        BulkSchedulingResultDTO result = scheduleTemplateService.applyTemplate(templateId, request);
        return ResponseEntity.ok(result);
    }
    
    // Copy schedule from previous period
    @PostMapping("/api/v1/timetable/copy-schedule")
    @PreAuthorize("hasRole('ORGANIZATION_ADMIN')")
    public ResponseEntity<BulkSchedulingResultDTO> copyScheduleFromPeriod(
        @Valid @RequestBody CopyScheduleRequest request
    ) {
        BulkSchedulingResultDTO result = scheduleTemplateService.copySchedule(request);
        return ResponseEntity.ok(result);
    }
}

public record CreateTemplateRequest(
    @NotBlank String templateName,
    String description,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    UUID instructorUuid,
    UUID organisationUuid,
    List<UUID> classSessionUuids
) {}

public record ApplyTemplateRequest(
    @NotNull LocalDate targetStartDate,
    @NotNull LocalDate targetEndDate,
    UUID targetInstructorUuid,
    Map<UUID, UUID> courseMapping, // Old course UUID -> New course UUID
    Boolean adjustForConflicts
) {}

@Entity
@Table(name = "schedule_templates")
public class ScheduleTemplate extends BaseEntity {
    
    @Column(name = "template_name")
    private String templateName;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "created_by_uuid")
    private UUID createdByUuid;
    
    @Column(name = "organisation_uuid")
    private UUID organisationUuid;
    
    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL)
    private List<TemplateClassSession> classSessions;
}

@Entity
@Table(name = "template_class_sessions")
public class TemplateClassSession extends BaseEntity {
    
    @ManyToOne
    @JoinColumn(name = "template_id")
    private ScheduleTemplate template;
    
    @Column(name = "day_of_week")
    private DayOfWeek dayOfWeek;
    
    @Column(name = "start_time")
    private LocalTime startTime;
    
    @Column(name = "end_time")
    private LocalTime endTime;
    
    @Column(name = "title")
    private String title;
    
    @Column(name = "course_uuid")
    private UUID courseUuid;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "session_type")
    private SessionType sessionType;
}
```

### Auto-Scheduling Intelligence

#### Smart Scheduling Suggestions
```java
@Service
public class SmartSchedulingService {
    
    // AI-powered scheduling suggestions
    @GetMapping("/api/v1/timetable/suggest-schedule")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ORGANIZATION_ADMIN')")
    public ResponseEntity<List<SchedulingSuggestionDTO>> getSchedulingSuggestions(
        @RequestParam UUID instructorUuid,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam Integer classCount,
        @RequestParam Integer classLengthMinutes
    ) {
        List<SchedulingSuggestionDTO> suggestions = smartSchedulingService
            .generateSchedulingSuggestions(instructorUuid, startDate, endDate, classCount, classLengthMinutes);
        return ResponseEntity.ok(suggestions);
    }
    
    // Optimize existing schedule
    @PostMapping("/api/v1/timetable/optimize")
    @PreAuthorize("hasRole('ORGANIZATION_ADMIN')")
    public ResponseEntity<ScheduleOptimizationResultDTO> optimizeSchedule(
        @Valid @RequestBody OptimizeScheduleRequest request
    ) {
        ScheduleOptimizationResultDTO result = smartSchedulingService.optimizeSchedule(request);
        return ResponseEntity.ok(result);
    }
    
    // Auto-schedule based on preferences
    @PostMapping("/api/v1/timetable/auto-schedule")
    @PreAuthorize("hasRole('ORGANIZATION_ADMIN')")
    public ResponseEntity<AutoScheduleResultDTO> autoScheduleClasses(
        @Valid @RequestBody AutoScheduleRequest request
    ) {
        AutoScheduleResultDTO result = smartSchedulingService.autoSchedule(request);
        return ResponseEntity.ok(result);
    }
}

public record SchedulingSuggestionDTO(
    LocalDateTime suggestedStartTime,
    LocalDateTime suggestedEndTime,
    String rationale,
    Double confidenceScore,
    List<String> pros,
    List<String> cons,
    Boolean hasConflicts
) {}

public record AutoScheduleRequest(
    List<UUID> courseUuids,
    List<UUID> instructorUuids,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    SchedulingPreferencesDTO preferences
) {}

public record SchedulingPreferencesDTO(
    List<DayOfWeek> preferredDays,
    LocalTime earliestStartTime,
    LocalTime latestEndTime,
    Integer minimumBreakMinutes,
    Integer maxClassesPerDay,
    Boolean avoidBackToBackClasses,
    List<String> preferredRooms
) {}
```

### Academic Calendar Integration

#### Term and Holiday Management
```java
@Service
public class AcademicCalendarService {
    
    // Get academic calendar for scheduling
    @GetMapping("/api/v1/timetable/academic-calendar")
    public ResponseEntity<AcademicCalendarDTO> getAcademicCalendar(
        @RequestParam @DateTimeFormat(pattern = "yyyy") Integer year,
        @RequestParam(required = false) UUID organisationUuid
    ) {
        AcademicCalendarDTO calendar = academicCalendarService.getCalendar(year, organisationUuid);
        return ResponseEntity.ok(calendar);
    }
    
    // Check if date is a teaching day
    public boolean isTeachingDay(LocalDate date, UUID organisationUuid) {
        return !holidayRepository.existsByDateAndOrganisation(date, organisationUuid) &&
               !date.getDayOfWeek().equals(DayOfWeek.SATURDAY) &&
               !date.getDayOfWeek().equals(DayOfWeek.SUNDAY);
    }
    
    // Get available teaching dates in range
    public List<LocalDate> getAvailableTeachingDates(
        LocalDate startDate, LocalDate endDate, UUID organisationUuid
    ) {
        return startDate.datesUntil(endDate.plusDays(1))
            .filter(date -> isTeachingDay(date, organisationUuid))
            .collect(Collectors.toList());
    }
}

public record AcademicCalendarDTO(
    Integer year,
    UUID organisationUuid,
    List<AcademicTermDTO> terms,
    List<HolidayDTO> holidays,
    List<ImportantDateDTO> importantDates
) {}

public record AcademicTermDTO(
    String termName,
    LocalDate startDate,
    LocalDate endDate,
    Integer teachingWeeks,
    List<LocalDate> breakPeriods
) {}

@Entity
@Table(name = "academic_holidays")
public class AcademicHoliday extends BaseEntity {
    
    @Column(name = "holiday_name")
    private String holidayName;
    
    @Column(name = "holiday_date")
    private LocalDate holidayDate;
    
    @Column(name = "organisation_uuid")
    private UUID organisationUuid;
    
    @Column(name = "is_recurring")
    private Boolean isRecurring;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "holiday_type")
    private HolidayType holidayType; // PUBLIC, INSTITUTIONAL, RELIGIOUS
}
```

### Timetable Analytics & Reporting

#### Comprehensive Timetable Analytics
```java
@Service
public class TimetableAnalyticsService {
    
    @GetMapping("/api/v1/timetable/analytics/utilization")
    @PreAuthorize("hasRole('ORGANIZATION_ADMIN')")
    public ResponseEntity<UtilizationAnalyticsDTO> getUtilizationAnalytics(
        @RequestParam UUID organisationUuid,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        UtilizationAnalyticsDTO analytics = timetableAnalyticsService
            .getUtilizationAnalytics(organisationUuid, startDate, endDate);
        return ResponseEntity.ok(analytics);
    }
    
    @GetMapping("/api/v1/timetable/analytics/conflicts")
    @PreAuthorize("hasRole('ORGANIZATION_ADMIN')")
    public ResponseEntity<ConflictAnalyticsDTO> getConflictAnalytics(
        @RequestParam UUID organisationUuid,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        ConflictAnalyticsDTO analytics = timetableAnalyticsService
            .getConflictAnalytics(organisationUuid, startDate, endDate);
        return ResponseEntity.ok(analytics);
    }
}

public record UtilizationAnalyticsDTO(
    Double overallUtilization,
    Map<DayOfWeek, Double> dailyUtilization,
    Map<String, Double> timeSlotUtilization, // "09:00-10:00" -> 0.75
    List<InstructorUtilizationDTO> instructorUtilization,
    List<RoomUtilizationDTO> roomUtilization,
    List<PeakHoursDTO> peakHours
) {}

public record ConflictAnalyticsDTO(
    Integer totalConflicts,
    Integer resolvedConflicts,
    Map<String, Integer> conflictsByType,
    List<ConflictTrendDTO> conflictTrends,
    List<String> commonConflictCauses
) {}
```

This comprehensive timetable implementation provides:

✅ **Visual Calendar Interface**: Weekly, monthly, and daily views  
✅ **Interactive Scheduling**: Drag-and-drop and click-to-schedule  
✅ **Smart Time Management**: Standard time slots and resource allocation  
✅ **Template System**: Copy schedules and create reusable templates  
✅ **Auto-Scheduling**: AI-powered suggestions and optimization  
✅ **Academic Integration**: Term management and holiday handling  
✅ **Advanced Analytics**: Utilization reports and conflict analysis  

The timetable system integrates seamlessly with the existing class scheduling foundation, providing a complete calendar-based educational management solution.