# Student Domain Guide

## Overview

The **Student** domain represents learners who engage with educational content, enroll in courses, and participate in training programs within the Elimika platform. Students can exist as both global users (independent learners) and organization-affiliated members.

## Student Domain Characteristics

### Core Capabilities
- **Course Enrollment**: Access and complete training courses
- **Progress Tracking**: Monitor learning progress and achievements  
- **Assessment Participation**: Take quizzes, submit assignments, receive grades
- **Certificate Earning**: Obtain completion certificates for courses and programs
- **Content Consumption**: View lessons, download materials, interact with content

### Domain Assignment Patterns

```mermaid
graph TD
    subgraph "Student Domain Assignment"
        A[New User Registration] --> B{Registration Context}
        B -->|Direct Platform Signup| C[Global Student Domain]
        B -->|Organization Invitation| D[Organization Student Domain]
        B -->|Both Contexts| E[Combined Assignment]
        
        C --> F[user_domain_mapping]
        D --> G[user_organisation_domain_mapping]
        E --> F
        E --> G
        
        F --> H[System-Wide Student Access]
        G --> I[Organization-Specific Student Access]
    end

    style C fill:#e3f2fd
    style D fill:#f3e5f5
    style E fill:#e8f5e9
```

## Global vs Organization Student Domains

### Global Student Domain

**Assignment**: Direct mapping in `user_domain_mapping` table
**Scope**: Platform-wide access to public courses and content
**Use Cases**:
- Independent learners
- Self-enrolled users
- Open course participation

```sql
-- Example: Global student domain assignment
INSERT INTO user_domain_mapping (user_uuid, domain_uuid)
VALUES ('user-123', 'student-domain-uuid');
```

### Organization Student Domain

**Assignment**: Contextual mapping in `user_organisation_domain_mapping` table
**Scope**: Access to organization-specific courses and programs
**Additional Context**:
- Branch/department assignment
- Enrollment periods (start/end dates)
- Organization-specific policies

```sql
-- Example: Organization student domain assignment
INSERT INTO user_organisation_domain_mapping (
    user_uuid, organisation_uuid, domain_uuid, branch_uuid,
    start_date, active, deleted
) VALUES (
    'user-123', 'university-abc', 'student-domain-uuid', 'cs-department',
    '2024-01-15', true, false
);
```

## Student Lifecycle Management

### Enrollment Process

```mermaid
sequenceDiagram
    participant S as Student
    participant API as Platform API
    participant ORG as Organization
    participant DB as Database

    Note over S,DB: Student Invitation & Enrollment
    ORG->>API: POST /api/v1/invitations
    Note right of ORG: {<br/>"email": "student@university.edu",<br/>"organisationUuid": "uni-123",<br/>"domainName": "student",<br/>"branchUuid": "cs-dept"<br/>}
    
    API->>S: Send Invitation Email
    S->>API: Accept Invitation
    API->>DB: Create/Update user_organisation_domain_mapping
    
    Note over S,DB: Automatic Course Access
    API->>DB: Grant access to org courses
    API-->>S: Welcome & Course Catalog
```

### Progress Tracking

Students accumulate learning data across multiple dimensions:

```mermaid
erDiagram
    STUDENT ||--o{ COURSE_ENROLLMENT : enrolls
    COURSE_ENROLLMENT ||--o{ LESSON_PROGRESS : tracks
    COURSE_ENROLLMENT ||--o{ QUIZ_ATTEMPT : attempts
    COURSE_ENROLLMENT ||--o{ ASSIGNMENT_SUBMISSION : submits
    COURSE_ENROLLMENT ||--|| CERTIFICATE : earns
    
    STUDENT {
        uuid user_uuid PK
        string learning_preferences
        string academic_level
    }
    
    COURSE_ENROLLMENT {
        uuid uuid PK
        uuid user_uuid FK
        uuid course_uuid FK
        enum enrollment_status
        date enrolled_date
        date completion_date
    }
    
    LESSON_PROGRESS {
        uuid uuid PK
        uuid enrollment_uuid FK
        uuid lesson_uuid FK
        enum progress_status
        int completion_percentage
    }
```

## API Reference for Student Management

### Student Profile Management

| Method | Endpoint | Purpose | Student-Specific Fields |
|--------|----------|---------|-------------------------|
| `GET` | `/api/v1/users/{uuid}` | Get student profile | `organisationAffiliations[]` with student roles |
| `PUT` | `/api/v1/users/{uuid}` | Update student info | Academic preferences, learning goals |
| `GET` | `/api/v1/students/{uuid}` | Get extended student data | Course history, achievements |

### Enrollment Management

| Method | Endpoint | Purpose | Use Case |
|--------|----------|---------|----------|
| `POST` | `/api/v1/students/{uuid}/enroll/{courseUuid}` | Enroll in course | Self-enrollment or admin assignment |
| `GET` | `/api/v1/students/{uuid}/enrollments` | List student enrollments | Progress dashboard |
| `GET` | `/api/v1/students/{uuid}/progress` | Get learning progress | Completion tracking |

### Organization Student Management

| Method | Endpoint | Purpose | Organization Context |
|--------|----------|---------|----------------------|
| `GET` | `/api/v1/organizations/{uuid}/students` | List org students | Roster management |
| `POST` | `/api/v1/organizations/{uuid}/students/invite` | Invite student | Batch enrollment |
| `GET` | `/api/v1/organizations/{uuid}/students/{studentUuid}/progress` | Track org student progress | Academic monitoring |

## Student Domain in Organization Context

### Branch-Level Student Management

Students can be assigned to specific organizational branches (departments, divisions):

```json
{
  "studentAffiliation": {
    "organisationUuid": "university-123",
    "organisationName": "State University",
    "domainInOrganisation": "student", 
    "branchUuid": "cs-dept-456",
    "branchName": "Computer Science Department",
    "startDate": "2024-01-15",
    "endDate": null,
    "active": true,
    "academicYear": "2024-2025",
    "studentId": "CS2024001"
  }
}
```

### Academic Periods and Temporal Management

```mermaid
gantt
    title Student Academic Lifecycle
    dateFormat YYYY-MM-DD
    section Semester 1
    Enrollment Period    :active, enroll1, 2024-01-01, 2024-01-15
    Active Learning     :learn1, 2024-01-15, 2024-05-15
    Assessment Period   :assess1, 2024-05-01, 2024-05-30
    
    section Summer Break
    Optional Courses    :summer, 2024-06-01, 2024-08-15
    
    section Semester 2  
    Re-enrollment       :enroll2, 2024-08-01, 2024-08-15
    Active Learning     :learn2, 2024-08-15, 2024-12-15
    Final Assessments   :assess2, 2024-12-01, 2024-12-30
```

## Validation Rules and Business Logic

### Student Domain Assignment Rules

1. **Self-Registration**: Users can self-assign as global students
2. **Organization Invitation**: Must be explicitly invited with student role
3. **Multiple Affiliations**: Can be student in multiple organizations simultaneously
4. **Temporal Constraints**: Organization student memberships respect academic periods

### Permission Validation

```java
// Example permission check for student content access
public boolean canAccessCourse(UUID studentUuid, UUID courseUuid) {
    // Check global student access
    List<String> globalDomains = getUserDomainsFromMappings(studentUuid);
    if (globalDomains.contains("student")) {
        return courseService.isPublicCourse(courseUuid);
    }
    
    // Check organization-specific access
    List<UserOrganisationAffiliationDTO> affiliations = 
        getUserOrganisationAffiliations(studentUuid);
    
    return affiliations.stream()
        .filter(aff -> "student".equals(aff.getDomainInOrganisation()))
        .filter(UserOrganisationAffiliationDTO::isActive)
        .anyMatch(aff -> courseService.isAvailableToOrganization(courseUuid, aff.getOrganisationUuid()));
}
```

## Student Data and Privacy Considerations

### Academic Records

Students generate extensive educational data:

- **Progress Records**: Lesson completion, time spent, attempts
- **Assessment Results**: Quiz scores, assignment grades, rubric evaluations  
- **Behavioral Data**: Login patterns, engagement metrics, help-seeking behavior
- **Achievement Data**: Certificates earned, badges, completion rates

### Data Protection

```mermaid
flowchart TD
    A[Student Data Collection] --> B{Data Type Classification}
    B -->|Academic Records| C[Educational Purpose Only]
    B -->|Personal Info| D[Privacy Protection Required]
    B -->|Behavioral Data| E[Anonymization Required]
    
    C --> F[Instructor/Admin Access]
    D --> G[Student Control Required]
    E --> H[Analytics Only]
    
    F --> I[Academic Monitoring]
    G --> J[Profile Management]
    H --> K[System Improvement]
    
    style D fill:#ffeb3b
    style G fill:#ff5722
    style E fill:#ff5722
```

## Integration with Learning Management

### Course Enrollment Workflow

```bash
# Typical student enrollment process

# 1. Student requests course access
curl -X POST /api/v1/students/{studentUuid}/course-requests \
  -H "Content-Type: application/json" \
  -d '{"courseUuid": "intro-programming-101", "reason": "Required for CS degree"}'

# 2. Organization approves enrollment
curl -X POST /api/v1/organizations/{orgUuid}/enrollments/approve \
  -H "Content-Type: application/json" \
  -d '{"studentUuid": "student-123", "courseUuid": "intro-programming-101"}'

# 3. System creates enrollment record
curl -X GET /api/v1/students/{studentUuid}/enrollments
# Response includes new enrollment with access permissions
```

### Progress Reporting

Organizations can track student progress across multiple dimensions:

```json
{
  "studentProgressSummary": {
    "studentUuid": "student-123",
    "organizationUuid": "university-abc", 
    "branchName": "Computer Science",
    "academicPeriod": "Fall 2024",
    "totalEnrollments": 5,
    "completedCourses": 3,
    "inProgressCourses": 2,
    "averageGrade": 87.5,
    "certificatesEarned": 2,
    "lastActivity": "2024-12-01T14:30:00Z"
  }
}
```

This student domain implementation provides comprehensive support for both independent learners and organization-affiliated students, enabling flexible learning pathways while maintaining appropriate academic oversight and progress tracking.