# Class Module Implementation Guide

## 1. Overview

### Purpose
This module is responsible for the definition and management of class "templates" or "blueprints". It knows everything about a class—its topic, content, format, capacity, and rules for recurrence—but is completely independent of the actual schedule. It answers the question "What is this class?" but not "When is this class?" or "Who is enrolled?".

### Key Features
- **Class Definition**: Create, update, and manage the core information for a class.
- **Recurrence Rules**: Define complex recurrence patterns (e.g., weekly, monthly) that the Timetabling module will use.
- **Resource Management**: Associate resources like documents or links with a class definition.

### Module Boundaries
- **Owns**: Class definitions, recurrence patterns, class resources.
- **Does Not Own**: The actual schedule, enrollments, instructor availability, attendance.

## 2. Spring Modulith Module Architecture

### Module Structure
```
src/main/java/apps/sarafrika/elimika/
└── classDefinition/
    ├── package-info.java                        # @ApplicationModule
    ├── controller/
    │   └── ClassDefinitionController.java
    ├── service/
    │   ├── ClassDefinitionService.java          # Implementation class
    │   └── impl/
    │       └── ClassDefinitionServiceImpl.java
    ├── model/
    │   ├── ClassDefinition.java                 # JPA Entity
    │   ├── RecurrencePattern.java               # JPA Entity
    │   └── ClassResource.java                   # JPA Entity
    ├── repository/
    │   ├── ClassDefinitionRepository.java
    │   └── RecurrencePatternRepository.java
    ├── dto/
    │   ├── ClassDefinitionDTO.java
    │   ├── RecurrencePatternDTO.java
    │   ├── ClassResourceDTO.java
    │   ├── CreateDefinitionRequest.java
    │   └── UpdateDefinitionRequest.java
    ├── spi/                                     # Service Provider Interface
    │   ├── ClassDefinitionService.java          # Public API interface
    │   └── package-info.java                    # @NamedInterface("class-definition-spi")
    └── util/
        ├── enums/
        │   ├── LocationType.java
        │   └── RecurrenceType.java
        └── converter/
            ├── LocationTypeConverter.java
            └── RecurrenceTypeConverter.java
```

## 3. Database Design

### ClassDefinitions Table
*Renamed from `class_sessions` to reflect its new purpose.*

**File**: `V202509061000__create_class_definitions_table.sql`
```sql
CREATE TABLE class_definitions (
    id               BIGSERIAL PRIMARY KEY,
    uuid             UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    
    -- Basic Information
    title            VARCHAR(255) NOT NULL,
    description      TEXT,
    
    -- Ownership & Association
    default_instructor_uuid UUID NOT NULL,
    organisation_uuid UUID,
    course_uuid      UUID,
    
    -- Default Scheduling Information
    duration_minutes INTEGER NOT NULL,
    
    -- Default Format
    location_type    VARCHAR(20) NOT NULL DEFAULT 'online' CHECK (location_type IN ('online', 'in_person', 'hybrid')),
    
    -- Default Capacity
    max_participants INTEGER NOT NULL DEFAULT 50,
    allow_waitlist   BOOLEAN NOT NULL DEFAULT true,
    
    -- Recurrence Pattern
    recurrence_pattern_uuid UUID,
    
    -- Status
    is_active        BOOLEAN NOT NULL DEFAULT true,
    
    -- Audit Fields
    created_date     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_date     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by       VARCHAR(255) NOT NULL,
    updated_by       VARCHAR(255)
);
```

### RecurrencePatterns Table
**File**: `V202509061001__create_recurrence_patterns_table.sql`
```sql
CREATE TABLE recurrence_patterns (
    id               BIGSERIAL PRIMARY KEY,
    uuid             UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    recurrence_type  VARCHAR(20) NOT NULL CHECK (recurrence_type IN ('DAILY', 'WEEKLY', 'MONTHLY')),
    interval_value   INTEGER DEFAULT 1,
    days_of_week     VARCHAR(100), -- e.g., 'MONDAY,WEDNESDAY,FRIDAY'
    day_of_month     INTEGER,
    end_date         DATE,
    occurrence_count INTEGER,
    -- Audit Fields
    created_date     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_date     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
```

## 4. Service Layer & API

### Module Declaration
```java
// In apps.sarafrika.elimika.classDefinition.package-info.java
@ApplicationModule(
    allowedDependencies = {"shared"}
)
package apps.sarafrika.elimika.classDefinition;

import org.springframework.modulith.ApplicationModule;
```

### Public API (SPI)
```java
// In package apps.sarafrika.elimika.classDefinition.spi
public interface ClassDefinitionService {
    ClassDefinitionDTO createClassDefinition(CreateDefinitionRequest request);
    ClassDefinitionDTO updateClassDefinition(UUID definitionUuid, UpdateDefinitionRequest request);
    void deactivateClassDefinition(UUID definitionUuid);
    ClassDefinitionDTO getClassDefinition(UUID definitionUuid);
    List<ClassDefinitionDTO> findClassesForCourse(UUID courseUuid);
}
```

### SPI Package Declaration
```java
// In apps.sarafrika.elimika.classDefinition.spi.package-info.java
@NamedInterface("class-definition-spi")
package apps.sarafrika.elimika.classDefinition.spi;

import org.springframework.modulith.NamedInterface;
```

### Published Events
This module publishes events to notify other modules about changes to class templates.

```java
// In package apps.sarafrika.elimika.classDefinition.dto (event DTOs)
public record ClassDefined(
    @NotNull UUID definitionUuid,
    @NotNull String title,
    @NotNull Integer durationMinutes,
    @NotNull UUID defaultInstructorUuid,
    UUID courseUuid,
    UUID recurrencePatternUuid
) {}

public record ClassDefinitionUpdated(
    @NotNull UUID definitionUuid
) {}
```

### REST Endpoints
```java
@RestController
@RequestMapping("/api/v1/class-definitions")
@PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ORGANIZATION_ADMIN')")
public class ClassDefinitionController {
    
    @PostMapping
    ResponseEntity<ClassDefinitionDTO> createDefinition(@Valid @RequestBody CreateDefinitionRequest request);
    
    @PutMapping("/{uuid}")
    ResponseEntity<ClassDefinitionDTO> updateDefinition(@PathVariable UUID uuid, @Valid @RequestBody UpdateDefinitionRequest request);
    
    @GetMapping("/{uuid}")
    ResponseEntity<ClassDefinitionDTO> getDefinition(@PathVariable UUID uuid);
    
    @DeleteMapping("/{uuid}")
    ResponseEntity<Void> deactivateDefinition(@PathVariable UUID uuid);
}
```

## 5. Enum and Converter Implementation

### LocationType Enum
Following project patterns for enum mapping:

```java
// In apps.sarafrika.elimika.classDefinition.util.enums
public enum LocationType {
    ONLINE("ONLINE", "Online class"),
    IN_PERSON("IN_PERSON", "In-person class"),
    HYBRID("HYBRID", "Hybrid online/in-person class");
    
    private final String value;
    private final String description;
    private static final Map<String, LocationType> VALUE_MAP = new HashMap<>();
    
    static {
        for (LocationType type : LocationType.values()) {
            VALUE_MAP.put(type.value, type);
            VALUE_MAP.put(type.value.toLowerCase(), type);
        }
    }
    
    LocationType(String value, String description) {
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
    public static LocationType fromValue(String value) {
        LocationType type = VALUE_MAP.get(value);
        if (type == null) {
            throw new IllegalArgumentException("Unknown LocationType: " + value);
        }
        return type;
    }
}
```

### RecurrenceType Enum
```java
// In apps.sarafrika.elimika.classDefinition.util.enums
public enum RecurrenceType {
    DAILY("DAILY", "Daily recurrence"),
    WEEKLY("WEEKLY", "Weekly recurrence"),
    MONTHLY("MONTHLY", "Monthly recurrence");
    
    private final String value;
    private final String description;
    private static final Map<String, RecurrenceType> VALUE_MAP = new HashMap<>();
    
    static {
        for (RecurrenceType type : RecurrenceType.values()) {
            VALUE_MAP.put(type.value, type);
            VALUE_MAP.put(type.value.toLowerCase(), type);
        }
    }
    
    RecurrenceType(String value, String description) {
        this.value = value;
        this.description = description;
    }
    
    @JsonValue
    public String getValue() {
        return value;
    }
    
    @JsonCreator
    public static RecurrenceType fromValue(String value) {
        RecurrenceType type = VALUE_MAP.get(value);
        if (type == null) {
            throw new IllegalArgumentException("Unknown RecurrenceType: " + value);
        }
        return type;
    }
}
```

### Converters
```java
// In apps.sarafrika.elimika.classDefinition.util.converter
@Converter(autoApply = true)
public class LocationTypeConverter implements AttributeConverter<LocationType, String> {
    
    @Override
    public String convertToDatabaseColumn(LocationType attribute) {
        return attribute != null ? attribute.getValue() : null;
    }
    
    @Override
    public LocationType convertToEntityAttribute(String dbData) {
        return dbData != null ? LocationType.fromValue(dbData) : null;
    }
}

@Converter(autoApply = true)
public class RecurrenceTypeConverter implements AttributeConverter<RecurrenceType, String> {
    
    @Override
    public String convertToDatabaseColumn(RecurrenceType attribute) {
        return attribute != null ? attribute.getValue() : null;
    }
    
    @Override
    public RecurrenceType convertToEntityAttribute(String dbData) {
        return dbData != null ? RecurrenceType.fromValue(dbData) : null;
    }
}
```
