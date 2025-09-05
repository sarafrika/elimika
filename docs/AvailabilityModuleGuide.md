# Availability Module Implementation Guide

## 1. Overview

### Purpose
This module is responsible for managing when an instructor is available to teach. It has no knowledge of classes or enrollments; it simply manages blocks of time. It answers the question "When is this instructor available?"

### Key Features
- **Weekly Availability**: Define recurring weekly time slots.
- **Specific Dates**: Set availability or unavailability for specific dates (e.g., for holidays or special events).
- **Availability Queries**: Provide a clear API for other modules to query for open time slots.

### Module Boundaries
- **Owns**: Instructor availability slots.
- **Does Not Own**: Class information, schedules, enrollments.

## 2. Spring Modulith Module Architecture

### Module Structure
```
src/main/java/apps/sarafrika/elimika/
└── availability/
    ├── package-info.java                    # @ApplicationModule
    ├── controller/
    │   └── AvailabilityController.java
    ├── service/
    │   ├── AvailabilityService.java         # Implementation class
    │   └── impl/
    │       └── AvailabilityServiceImpl.java
    ├── model/
    │   └── InstructorAvailability.java     # JPA Entity
    ├── repository/
    │   └── AvailabilityRepository.java
    ├── dto/
    │   ├── AvailabilitySlotDTO.java
    │   ├── WeeklyAvailabilitySlotDTO.java
    │   └── DateSpecificSlotDTO.java
    ├── spi/                                 # Service Provider Interface
    │   ├── AvailabilityService.java         # Public API interface
    │   └── package-info.java                # @NamedInterface("availability-spi")
    └── util/
        ├── enums/
        │   └── AvailabilityType.java
        └── converter/
            └── AvailabilityTypeConverter.java
```

## 3. Database Design

### InstructorAvailability Table

**File**: `V202509061005__create_instructor_availability_table.sql`
```sql
CREATE TABLE instructor_availability (
    id                BIGSERIAL PRIMARY KEY,
    uuid              UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    
    -- Relationships
    instructor_uuid   UUID NOT NULL,
    
    -- Availability Type
    availability_type VARCHAR(20) NOT NULL DEFAULT 'weekly' CHECK (availability_type IN ('weekly', 'specific_date')),
    
    -- Time Information
    day_of_week       INTEGER, -- 1 (Monday) to 7 (Sunday), for weekly availability
    specific_date     DATE,    -- For specific_date availability
    start_time        TIME NOT NULL,
    end_time          TIME NOT NULL,
    
    -- Status
    is_available      BOOLEAN NOT NULL DEFAULT true, -- true for available, false for blocked out
    
    -- Audit Fields
    created_date      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_date      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by        VARCHAR(255) NOT NULL,
    updated_by        VARCHAR(255),

    CONSTRAINT check_time_validity CHECK (start_time < end_time)
);

CREATE INDEX idx_instructor_availability_instructor_uuid ON instructor_availability (instructor_uuid);
```

## 4. Service Layer & API

### Module Declaration
```java
// In apps.sarafrika.elimika.availability.package-info.java
@ApplicationModule(
    allowedDependencies = {"shared"}
)
package apps.sarafrika.elimika.availability;

import org.springframework.modulith.ApplicationModule;
```

### Public API (SPI)
```java
// In package apps.sarafrika.elimika.availability.spi
public interface AvailabilityService {
    void setWeeklyAvailability(UUID instructorUuid, List<WeeklyAvailabilitySlotDTO> slots);
    void setDateSpecificAvailability(UUID instructorUuid, List<DateSpecificSlotDTO> slots);
    List<AvailabilitySlotDTO> getAvailabilityForDate(UUID instructorUuid, LocalDate date);
    boolean isInstructorAvailable(UUID instructorUuid, LocalDateTime start, LocalDateTime end);
}
```

### SPI Package Declaration
```java
// In apps.sarafrika.elimika.availability.spi.package-info.java
@NamedInterface("availability-spi")
package apps.sarafrika.elimika.availability.spi;

import org.springframework.modulith.NamedInterface;
```

### Published Events
This module publishes events to notify other modules when availability changes.

```java
// In package apps.sarafrika.elimika.availability.dto (event DTOs)
public record InstructorAvailabilityChanged(
    @NotNull UUID instructorUuid,
    @NotNull LocalDate effectiveDate
) {}
```

### REST Endpoints
```java
@RestController
@RequestMapping("/api/v1/availability")
@PreAuthorize("hasRole('INSTRUCTOR')")
public class AvailabilityController {
    
    @GetMapping("/{instructorUuid}")
    ResponseEntity<List<AvailabilitySlotDTO>> getAvailability(@PathVariable UUID instructorUuid, @RequestParam LocalDate date);

    @PostMapping("/{instructorUuid}/weekly")
    ResponseEntity<Void> setWeeklyAvailability(@PathVariable UUID instructorUuid, @RequestBody List<WeeklyAvailabilitySlotDTO> slots);

    @PostMapping("/{instructorUuid}/specific-date")
    ResponseEntity<Void> setDateSpecificAvailability(@PathVariable UUID instructorUuid, @RequestBody List<DateSpecificSlotDTO> slots);
}
```

## 5. Enum and Converter Implementation

### AvailabilityType Enum
Following project patterns for enum mapping:

```java
// In apps.sarafrika.elimika.availability.util.enums
public enum AvailabilityType {
    WEEKLY("WEEKLY", "Weekly recurring availability"),
    SPECIFIC_DATE("SPECIFIC_DATE", "Specific date availability");
    
    private final String value;
    private final String description;
    private static final Map<String, AvailabilityType> VALUE_MAP = new HashMap<>();
    
    static {
        for (AvailabilityType type : AvailabilityType.values()) {
            VALUE_MAP.put(type.value, type);
            VALUE_MAP.put(type.value.toLowerCase(), type);
        }
    }
    
    AvailabilityType(String value, String description) {
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
    public static AvailabilityType fromValue(String value) {
        AvailabilityType type = VALUE_MAP.get(value);
        if (type == null) {
            throw new IllegalArgumentException("Unknown AvailabilityType: " + value);
        }
        return type;
    }
}
```

### AvailabilityType Converter
```java
// In apps.sarafrika.elimika.availability.util.converter
@Converter(autoApply = true)
public class AvailabilityTypeConverter implements AttributeConverter<AvailabilityType, String> {
    
    @Override
    public String convertToDatabaseColumn(AvailabilityType attribute) {
        return attribute != null ? attribute.getValue() : null;
    }
    
    @Override
    public AvailabilityType convertToEntityAttribute(String dbData) {
        return dbData != null ? AvailabilityType.fromValue(dbData) : null;
    }
}
```

### Entity with Converter
```java
// In apps.sarafrika.elimika.availability.model
@Entity
@Table(name = "instructor_availability")
@Getter @Setter @ToString
@SuperBuilder
@NoArgsConstructor @AllArgsConstructor
public class InstructorAvailability extends BaseEntity {
    
    @Column(name = "instructor_uuid", nullable = false)
    private UUID instructorUuid;
    
    @Column(name = "availability_type", nullable = false)
    @Convert(converter = AvailabilityTypeConverter.class)
    private AvailabilityType availabilityType;
    
    // Other fields...
}
```
