# Timetabling Module Implementation Guide

## 1. Overview

### Purpose
This module is the "scheduling engine" of the system. It is responsible for placing class definitions from the `class` module into available time slots provided by the `availability` module. It manages the concrete, final schedule, student enrollments, waitlists, and attendance. It answers the questions "When is this class actually happening?" and "Who is attending?".

### Key Features
- **Scheduling**: Creates concrete `ScheduledInstance`s on the calendar.
- **Conflict Detection**: Validates that new instances do not conflict with existing schedules for instructors or students.
- **Enrollment Management**: Handles student enrollment, cancellation, and waitlists for scheduled instances.
- **Attendance Tracking**: Manages attendance for each session.

### Module Boundaries
- **Owns**: The final schedule, enrollments, waitlists, attendance records.
- **Does Not Own**: Class definitions, instructor availability patterns.

## 2. Spring Modulith Module Architecture

### Module Structure
```
src/main/java/apps/sarafrika/elimika/
└── timetabling/
    ├── package-info.java                        # @ApplicationModule
    ├── controller/
    │   ├── TimetableController.java
    │   └── EnrollmentController.java
    ├── service/
    │   ├── TimetableService.java                 # Implementation class
    │   ├── EnrollmentService.java                # Implementation class
    │   └── impl/
    │       ├── TimetableServiceImpl.java
    │       ├── EnrollmentServiceImpl.java
    │       └── ConflictDetectionService.java
    ├── model/
    │   ├── ScheduledInstance.java                # JPA Entity
    │   ├── Enrollment.java                       # JPA Entity
    │   └── Waitlist.java                         # JPA Entity
    ├── repository/
    │   ├── ScheduledInstanceRepository.java
    │   ├── EnrollmentRepository.java
    │   └── WaitlistRepository.java
    ├── dto/
    │   ├── ScheduledInstanceDTO.java
    │   ├── EnrollmentDTO.java
    │   ├── StudentScheduleDTO.java
    │   ├── ScheduleRequest.java
    │   └── EnrollmentRequest.java
    ├── spi/                                      # Service Provider Interface
    │   ├── TimetableService.java                 # Public API interface
    │   └── package-info.java                     # @NamedInterface("timetabling-spi")
    ├── internal/                                 # Event listeners
    │   └── SchedulingEventListener.java
    └── util/
        ├── enums/
        │   ├── SchedulingStatus.java
        │   └── EnrollmentStatus.java
        └── converter/
            ├── SchedulingStatusConverter.java
            └── EnrollmentStatusConverter.java
```

## 3. Database Design

### ScheduledInstances Table
*This is a new table, central to this module.*

**File**: `V202509061010__create_scheduled_instances_table.sql`
```sql
CREATE TABLE scheduled_instances (
    id                      BIGSERIAL PRIMARY KEY,
    uuid                    UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    
    -- Links to other modules
    class_definition_uuid   UUID NOT NULL, -- Foreign key to class_definitions
    instructor_uuid         UUID NOT NULL,
    
    -- Scheduling Information
    start_time              TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time                TIMESTAMP WITH TIME ZONE NOT NULL,
    timezone                VARCHAR(50) NOT NULL DEFAULT 'UTC',
    
    -- Denormalized/Cached Information from ClassDefinition
    title                   VARCHAR(255) NOT NULL,
    location_type           VARCHAR(20) NOT NULL,
    max_participants        INTEGER NOT NULL,

    -- Live Status
    status                  VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED' CHECK (status IN ('SCHEDULED', 'ONGOING', 'COMPLETED', 'CANCELLED')),
    cancellation_reason     TEXT,
    
    -- Audit Fields
    created_date            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_date            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by              VARCHAR(255) NOT NULL,
    updated_by              VARCHAR(255)
);
CREATE INDEX idx_scheduled_instances_instructor_time ON scheduled_instances (instructor_uuid, start_time, end_time);
CREATE INDEX idx_scheduled_instances_definition_uuid ON scheduled_instances (class_definition_uuid);
```

### Enrollments Table
*This table now links to `scheduled_instances`.*

**File**: `V202509061011__create_enrollments_table.sql`
```sql
CREATE TABLE enrollments (
    id                      BIGSERIAL PRIMARY KEY,
    uuid                    UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    
    -- Relationships
    scheduled_instance_uuid UUID NOT NULL, -- Foreign key to scheduled_instances
    student_uuid            UUID NOT NULL,
    
    -- Status & Attendance
    status                  VARCHAR(20) NOT NULL DEFAULT 'ENROLLED' CHECK (status IN ('ENROLLED', 'ATTENDED', 'ABSENT', 'CANCELLED')),
    attendance_marked_at    TIMESTAMP WITH TIME ZONE,
    
    -- Audit Fields
    created_date            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_date            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT unique_student_instance_enrollment UNIQUE (scheduled_instance_uuid, student_uuid)
);
CREATE INDEX idx_enrollments_student_instance ON enrollments (student_uuid, scheduled_instance_uuid);
```

## 4. Service Layer & API

### Module Declaration
```java
// In apps.sarafrika.elimika.timetabling.package-info.java
@ApplicationModule(
    allowedDependencies = {"shared", "availability :: availability-spi", "classDefinition :: class-definition-spi"}
)
package apps.sarafrika.elimika.timetabling;

import org.springframework.modulith.ApplicationModule;
```

### Public API (SPI)
```java
// In package apps.sarafrika.elimika.timetabling.spi
public interface TimetableService {
    // Scheduling actions
    ScheduledInstanceDTO scheduleClass(ScheduleRequest request);
    void cancelScheduledInstance(UUID instanceUuid, String reason);

    // Enrollment actions
    EnrollmentDTO enrollStudent(EnrollmentRequest request);
    void cancelEnrollment(UUID enrollmentUuid, String reason);

    // Query actions
    List<ScheduledInstanceDTO> getScheduleForInstructor(UUID instructorUuid, LocalDate start, LocalDate end);
    List<StudentScheduleDTO> getScheduleForStudent(UUID studentUuid, LocalDate start, LocalDate end);
}
```

### SPI Package Declaration
```java
// In apps.sarafrika.elimika.timetabling.spi.package-info.java
@NamedInterface("timetabling-spi")
package apps.sarafrika.elimika.timetabling.spi;

import org.springframework.modulith.NamedInterface;
```

### Consumed Events
This module listens to events from other modules to react to changes.
```java
// In package apps.sarafrika.elimika.timetabling.internal
@Component
public class SchedulingEventListener {

    @EventListener
    public void handleClassDefined(ClassDefined event) {
        // Potentially pre-validate or cache class definition data
    }

    @EventListener
    public void handleInstructorAvailabilityChanged(InstructorAvailabilityChanged event) {
        // Check for conflicts with already scheduled classes for that instructor and date
        // and publish a `PotentialConflictDetected` event if necessary.
    }

    @EventListener
    public void handleStudentDeactivated(StudentDeactivatedEvent event) {
        // Cancel all future enrollments for this student.
    }
}
```

### Published Events
```java
// In package apps.sarafrika.elimika.timetabling.dto (event DTOs)
public record ClassScheduled(UUID instanceUuid, UUID definitionUuid, UUID instructorUuid, LocalDateTime startTime) {}

public record StudentEnrolled(UUID enrollmentUuid, UUID instanceUuid, UUID studentUuid) {}

public record AttendanceMarked(UUID enrollmentUuid, UUID instanceUuid, UUID studentUuid, String status) {}
```

### REST Endpoints
```java
@RestController
@RequestMapping("/api/v1/timetable")
public class TimetableController {
    
    @PostMapping("/schedule")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    ResponseEntity<ScheduledInstanceDTO> scheduleClass(@RequestBody ScheduleRequest request);

    @DeleteMapping("/schedule/{instanceUuid}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    ResponseEntity<Void> cancelScheduledClass(@PathVariable UUID instanceUuid, @RequestParam String reason);

    @GetMapping("/instructor/{instructorUuid}")
    ResponseEntity<List<ScheduledInstanceDTO>> getInstructorSchedule(@PathVariable UUID instructorUuid, @RequestParam LocalDate start, @RequestParam LocalDate end);
}

@RestController
@RequestMapping("/api/v1/enrollment")
public class EnrollmentController {

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    ResponseEntity<EnrollmentDTO> enroll(@RequestBody EnrollmentRequest request);

    @DeleteMapping("/{enrollmentUuid}")
    @PreAuthorize("isOwner(#enrollmentUuid) or hasRole('INSTRUCTOR')")
    ResponseEntity<Void> cancel(@PathVariable UUID enrollmentUuid);

    @GetMapping("/student/{studentUuid}")
    ResponseEntity<List<StudentScheduleDTO>> getStudentSchedule(@PathVariable UUID studentUuid, @RequestParam LocalDate start, @RequestParam LocalDate end);
}
```

## 5. Enum and Converter Implementation

### SchedulingStatus Enum
Following project patterns for enum mapping:

```java
// In apps.sarafrika.elimika.timetabling.util.enums
public enum SchedulingStatus {
    SCHEDULED("SCHEDULED", "Class is scheduled"),
    ONGOING("ONGOING", "Class is currently in progress"),
    COMPLETED("COMPLETED", "Class has been completed"),
    CANCELLED("CANCELLED", "Class has been cancelled");
    
    private final String value;
    private final String description;
    private static final Map<String, SchedulingStatus> VALUE_MAP = new HashMap<>();
    
    static {
        for (SchedulingStatus status : SchedulingStatus.values()) {
            VALUE_MAP.put(status.value, status);
            VALUE_MAP.put(status.value.toLowerCase(), status);
        }
    }
    
    SchedulingStatus(String value, String description) {
        this.value = value;
        this.description = description;
    }
    
    @JsonValue
    public String getValue() {
        return value;
    }
    
    public String getDescription() {
        return description;
    }
    
    @JsonCreator
    public static SchedulingStatus fromValue(String value) {
        SchedulingStatus status = VALUE_MAP.get(value);
        if (status == null) {
            throw new IllegalArgumentException("Unknown SchedulingStatus: " + value);
        }
        return status;
    }
}
```

### EnrollmentStatus Enum
```java
// In apps.sarafrika.elimika.timetabling.util.enums
public enum EnrollmentStatus {
    ENROLLED("ENROLLED", "Student is enrolled"),
    ATTENDED("ATTENDED", "Student attended the class"),
    ABSENT("ABSENT", "Student was absent"),
    CANCELLED("CANCELLED", "Enrollment was cancelled");
    
    private final String value;
    private final String description;
    private static final Map<String, EnrollmentStatus> VALUE_MAP = new HashMap<>();
    
    static {
        for (EnrollmentStatus status : EnrollmentStatus.values()) {
            VALUE_MAP.put(status.value, status);
            VALUE_MAP.put(status.value.toLowerCase(), status);
        }
    }
    
    EnrollmentStatus(String value, String description) {
        this.value = value;
        this.description = description;
    }
    
    @JsonValue
    public String getValue() {
        return value;
    }
    
    @JsonCreator
    public static EnrollmentStatus fromValue(String value) {
        EnrollmentStatus status = VALUE_MAP.get(value);
        if (status == null) {
            throw new IllegalArgumentException("Unknown EnrollmentStatus: " + value);
        }
        return status;
    }
}
```

### Converters
```java
// In apps.sarafrika.elimika.timetabling.util.converter
@Converter(autoApply = true)
public class SchedulingStatusConverter implements AttributeConverter<SchedulingStatus, String> {
    
    @Override
    public String convertToDatabaseColumn(SchedulingStatus attribute) {
        return attribute != null ? attribute.getValue() : null;
    }
    
    @Override
    public SchedulingStatus convertToEntityAttribute(String dbData) {
        return dbData != null ? SchedulingStatus.fromValue(dbData) : null;
    }
}

@Converter(autoApply = true)
public class EnrollmentStatusConverter implements AttributeConverter<EnrollmentStatus, String> {
    
    @Override
    public String convertToDatabaseColumn(EnrollmentStatus attribute) {
        return attribute != null ? attribute.getValue() : null;
    }
    
    @Override
    public EnrollmentStatus convertToEntityAttribute(String dbData) {
        return dbData != null ? EnrollmentStatus.fromValue(dbData) : null;
    }
}
```
