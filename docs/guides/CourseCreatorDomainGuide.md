# Course Creator Domain Guide

## Overview

The **Course Creator** domain represents content authors who design, build, and publish educational courses within the Elimika platform. Course creators focus on content authorship and intellectual property, distinct from instructors who deliver instruction. Course creators can operate as independent content publishers (global domain) or as content developers affiliated with educational organizations.

## Course Creator Domain Characteristics

### Core Capabilities
- **Content Authorship**: Design and structure courses, lessons, and learning materials
- **Intellectual Property Management**: Own and manage course content rights
- **Course Publishing**: Control course visibility, pricing, and distribution
- **Content Monetization**: Earn revenue from course sales and enrollments
- **Quality Control**: Update and maintain course content over time

### Domain Assignment Patterns

```mermaid
graph TD
    subgraph "Course Creator Domain Assignment"
        A[Course Creator Registration] --> B{Professional Context}
        B -->|Independent Publisher| C[Global Course Creator Domain]
        B -->|Organization Content Developer| D[Organization Course Creator Domain]
        B -->|Freelance + Institutional| E[Hybrid Assignment]

        C --> F[user_domain_mapping]
        D --> G[user_organisation_domain_mapping]
        E --> F
        E --> G

        F --> H[Platform-Wide Publishing Rights]
        G --> I[Organization-Specific Content Development]
    end

    subgraph "Course Creator Profile"
        J[CourseCreator Entity]
        K[Professional Headline]
        L[Bio & Portfolio]
        M[Website & Links]
        N[Verification Status]

        J --> K
        J --> L
        J --> M
        J --> N
    end

    style C fill:#e3f2fd
    style D fill:#f3e5f5
    style E fill:#e8f5e9
```

## Global vs Organization Course Creator Domains

### Global Course Creator Domain

**Assignment**: Direct mapping in `user_domain_mapping` table
**Scope**: Platform-wide content creation and publishing authority
**Use Cases**:
- Independent content creators
- Subject matter experts
- Educational content entrepreneurs
- Freelance curriculum designers

Assignment is done by adding an entry to the `user_domain_mapping` table linking the user's UUID with the course creator domain's UUID.

**Capabilities**:
- Create and publish courses to platform marketplace
- Set course pricing and licensing terms
- Retain full intellectual property rights
- Build creator reputation and portfolio
- Earn revenue from course enrollments

### Organization Course Creator Domain

**Assignment**: Contextual mapping in `user_organisation_domain_mapping` table
**Scope**: Institution-specific content development responsibilities
**Additional Context**:
- Department/division assignment
- Content development contracts
- Organizational IP agreements
- Institution-specific curriculum standards

Assignment is done by adding a detailed entry to the `user_organisation_domain_mapping` table that links the user to the organization, the course creator domain, and optionally a specific branch, along with start dates and status.

## Course Creator Profile System

### Course Creator Entity Relationships

```mermaid
erDiagram
    USER ||--|| COURSE_CREATOR : extends
    COURSE_CREATOR ||--o{ COURSE : creates
    COURSE ||--o{ LESSON : contains
    COURSE ||--o{ ASSESSMENT : includes

    USER {
        uuid uuid PK
        string first_name
        string last_name
        string email
        string profile_image_url
    }

    COURSE_CREATOR {
        uuid uuid PK
        uuid user_uuid FK
        string full_name
        text bio
        string professional_headline
        string website
        boolean admin_verified
    }

    COURSE {
        uuid uuid PK
        uuid course_creator_uuid FK
        string name
        text description
        decimal price
        enum status
        boolean active
    }
```

### Professional Profile Management

```json
{
  "courseCreatorProfile": {
    "uuid": "creator-123",
    "userUuid": "user-456",
    "fullName": "Dr. Jane Smith",
    "professionalHeadline": "Expert in Machine Learning & AI Education",
    "bio": "PhD in Computer Science with 10 years of experience creating online courses. Passionate about making complex topics accessible to learners worldwide.",
    "website": "https://janesmithlearning.com",
    "adminVerified": true,

    "publishedCourses": 15,
    "totalEnrollments": 12500,
    "averageRating": 4.8,
    "activeStudents": 3200
  }
}
```

## Course Creator Lifecycle Management

### Registration and Verification Workflow

```mermaid
sequenceDiagram
    participant CC as Course Creator
    participant API as Platform API
    participant ADMIN as Admin
    participant DB as Database

    Note over CC,DB: Course Creator Registration Process
    CC->>API: POST /api/v1/course-creators
    Note right of CC: {<br/>"userUuid": "user-123",<br/>"fullName": "Dr. Jane Smith",<br/>"bio": "...",<br/>"professionalHeadline": "..."<br/>}

    API->>DB: Create course_creator record
    API->>DB: Publish UserDomainMappingEvent
    API->>DB: Assign course_creator domain
    API-->>CC: Profile Created (unverified)

    Note over CC,ADMIN: Verification Process
    CC->>API: Upload credentials & portfolio
    ADMIN->>API: GET /api/v1/course-creators/unverified
    ADMIN->>API: Review creator profile
    ADMIN->>API: POST /api/v1/course-creators/{uuid}/verify

    API->>DB: Update admin_verified = true
    API-->>CC: Verification Complete Notice
    CC->>API: Full publishing rights granted
```

### Course Publishing Workflow

```mermaid
flowchart TD
    A[Course Creator Dashboard] --> B[Create New Course]
    B --> C{Course Type}

    C -->|Global Course| D[Public Marketplace]
    C -->|Organization Course| E[Institution Catalog]

    D --> F[Set Pricing & Visibility]
    E --> G[Organization Approval]

    F --> H[Content Development]
    G --> H

    H --> I[Add Lessons & Assessments]
    I --> J[Content Review]

    J --> K{Quality Check}
    K -->|Approved| L[Publish Course]
    K -->|Needs Revision| H

    L --> M[Course Live]
    M --> N[Student Enrollments]
    M --> O[Performance Analytics]

    style D fill:#4caf50
    style E fill:#2196f3
    style L fill:#ff9800
```

## API Reference for Course Creator Management

### Course Creator Profile Management

| Method | Endpoint | Purpose | Course Creator-Specific Fields |
|--------|----------|---------|-------------------------------|
| `POST` | `/api/v1/course-creators` | Create new course creator | `fullName`, `bio`, `professionalHeadline`, `website` |
| `GET` | `/api/v1/course-creators/{uuid}` | Get course creator profile | Complete creator profile with stats |
| `PUT` | `/api/v1/course-creators/{uuid}` | Update course creator info | Bio, headline, website updates |
| `DELETE` | `/api/v1/course-creators/{uuid}` | Delete course creator | Remove creator and cascade data |
| `GET` | `/api/v1/course-creators` | Get all course creators (paginated) | `PagedDTO<CourseCreatorDTO>` |

### Course Creator Search and Discovery

| Method | Endpoint | Purpose | Use Case |
|--------|----------|---------|----------|
| `GET` | `/api/v1/course-creators/search` | Search course creators with filters | Advanced search with operators |
| `GET` | `/api/v1/course-creators/verified` | Get verified course creators | Trusted creator list |
| `GET` | `/api/v1/course-creators/unverified` | Get unverified course creators | Admin review queue |

**Search Operators Supported**:
- `fullName_like=value` - Contains search (case-insensitive)
- `fullName_startswith=value` - Prefix search
- `fullName_endswith=value` - Suffix search
- `adminVerified=true/false` - Filter by verification status
- `createdDate_gte=2024-01-01` - Date range filtering
- `professionalHeadline_like=content` - Search by expertise

### Course Creator Verification Management

| Method | Endpoint | Purpose | Admin Context |
|--------|----------|---------|---------------|
| `POST` | `/api/v1/course-creators/{uuid}/verify` | Verify course creator | Admin approval with optional reason |
| `POST` | `/api/v1/course-creators/{uuid}/unverify` | Revoke verification | Admin action with reason |
| `GET` | `/api/v1/course-creators/{uuid}/verification-status` | Check verification status | Returns boolean verification state |
| `GET` | `/api/v1/course-creators/count?verified=true` | Count creators by status | Statistics for dashboard |

## Course Ownership and Security

### Course Ownership Model

```mermaid
flowchart TD
    A[User Authentication] --> B[JWT Token with Claims]
    B --> C[Extract Keycloak ID]
    C --> D[Find User Record]
    D --> E[Find Course Creator Profile]
    E --> F[Course Creator UUID]

    F --> G{Course Access Check}

    G -->|Create Course| H[Set course_creator_uuid]
    G -->|Edit Course| I[Verify Ownership]
    G -->|Delete Course| J[Verify Ownership]

    I --> K{course.courseCreatorUuid == creator.uuid}
    J --> K

    K -->|Match| L[Access Granted]
    K -->|No Match| M[Access Denied]

    style L fill:#4caf50
    style M fill:#f44336
```

### Authorization Service Integration

The `CourseSecurityService` validates course ownership:

```java
// JWT (keycloakId) → User → CourseCreator → Course
public boolean isCourseOwner(UUID courseUuid) {
    // Extract keycloakId from JWT
    // Find User by keycloakId
    // Find CourseCreator by User UUID
    // Get Course and verify course.courseCreatorUuid == courseCreator.uuid
}
```

**Domain Security Service Methods**:
- `isCourseCreator()` - Check if user has course_creator domain
- `hasAnyDomain(UserDomain.course_creator, UserDomain.admin)` - Multi-domain check
- `getCurrentUser()` - Get authenticated user context

## Verification Process and Quality Assurance

### Admin Verification Workflow

```mermaid
stateDiagram-v2
    [*] --> Unverified: Course Creator Registers

    Unverified --> UnderReview: Admin Reviews Profile
    UnderReview --> Verified: Credentials Approved
    UnderReview --> Rejected: Credentials Insufficient

    Verified --> Publishing: Full Access Granted
    Rejected --> Unverified: Resubmit with Updates

    Verified --> Suspended: Policy Violation
    Suspended --> Verified: Issue Resolved
    Suspended --> [*]: Permanent Removal

    Publishing --> [*]: Active Creator
```

### Verification Criteria

Course creators are verified based on:

1. **Professional Credentials**: Educational background, certifications
2. **Content Portfolio**: Sample courses, teaching materials
3. **Subject Matter Expertise**: Demonstrated knowledge in teaching domain
4. **Quality Standards**: Adherence to platform content guidelines
5. **Legal Compliance**: Copyright, licensing, and terms acceptance

## Organization-Specific Course Creator Features

### Institutional Content Development

```json
{
  "organizationCourseCreator": {
    "courseCreatorUuid": "creator-789",
    "organizationAffiliation": {
      "organisationUuid": "university-abc",
      "organisationName": "State University",
      "domainInOrganisation": "course_creator",
      "branchUuid": "curriculum-dev",
      "branchName": "Curriculum Development Office",
      "contractType": "Full-time Content Developer",
      "startDate": "2023-01-15",
      "active": true,
      "ipAgreement": "University owns institutional course IP"
    },

    "institutionalResponsibilities": {
      "contentQuota": "4 courses per semester",
      "qualityReviewRequired": true,
      "collaborationExpected": true,
      "brandingGuidelines": "Must follow university visual identity",
      "approvalWorkflow": "Department Head → Dean → Publishing"
    }
  }
}
```

### Multi-Organization Content Development

Course creators can develop content for multiple organizations. This is achieved by creating multiple `user_organisation_domain_mapping` entries for the same user with different organizations, each with its own content development terms.

## Course Creator Analytics and Performance

### Creator Dashboard Metrics

```mermaid
pie title Course Portfolio Distribution
    "Published & Active" : 12
    "Draft" : 5
    "Under Review" : 2
    "Archived" : 3
```

### Performance Analytics

```json
{
  "courseCreatorAnalytics": {
    "creatorUuid": "creator-123",
    "period": "2024",

    "courseMetrics": {
      "totalCoursesCreated": 22,
      "publishedCourses": 18,
      "activeCourses": 15,
      "averageCourseRating": 4.7,
      "totalEnrollments": 15420
    },

    "revenueMetrics": {
      "totalRevenue": 127500.00,
      "averageRevenuePerCourse": 7083.33,
      "topPerformingCourse": {
        "courseUuid": "course-456",
        "courseName": "Advanced Machine Learning",
        "enrollments": 2150,
        "revenue": 43000.00
      }
    },

    "engagementMetrics": {
      "averageCompletionRate": 78.5,
      "studentSatisfaction": 4.6,
      "certificatesIssued": 9850,
      "activeDiscussions": 342
    }
  }
}
```

## Integration with Course Management

### Course Creation Flow

```mermaid
sequenceDiagram
    participant CC as Course Creator
    participant API as Course API
    participant SEC as Security Service
    participant DB as Database

    CC->>SEC: Authenticate (JWT)
    SEC->>SEC: Verify course_creator domain
    SEC-->>CC: Authentication confirmed

    CC->>API: POST /api/v1/courses
    Note right of CC: {<br/>"name": "Advanced Python",<br/>"description": "...",<br/>"price": 99.99<br/>}

    API->>SEC: Get current course creator
    SEC-->>API: courseCreator.uuid

    API->>DB: Create course with courseCreatorUuid
    DB-->>API: Course created

    API-->>CC: Course created successfully

    Note over CC,DB: Content Development Phase
    CC->>API: POST /api/v1/courses/{uuid}/lessons
    CC->>API: POST /api/v1/courses/{uuid}/assessments
    CC->>API: PUT /api/v1/courses/{uuid}/publish

    API->>SEC: Verify course ownership
    SEC-->>API: Ownership confirmed
    API->>DB: Update course status = PUBLISHED
    API-->>CC: Course published to marketplace
```

## Course Creator vs Instructor Distinction

### Key Differences

| Aspect | Course Creator | Instructor |
|--------|---------------|------------|
| **Primary Role** | Content authorship & IP ownership | Content delivery & teaching |
| **Focus** | Course structure, curriculum design | Student engagement, instruction |
| **Ownership** | Owns course intellectual property | Delivers others' courses or own |
| **Revenue Model** | Per-enrollment or licensing fees | Salary, hourly, or per-class |
| **Responsibilities** | Content updates, quality maintenance | Grading, feedback, office hours |
| **Relationship to Content** | Creator and rights holder | Facilitator and educator |

### Dual Role Support

Users can have both `course_creator` and `instructor` domains:

```json
{
  "dualRoleUser": {
    "userUuid": "user-999",
    "domains": ["course_creator", "instructor"],

    "asCreator": {
      "createsContent": true,
      "ownsCourseIP": true,
      "earnsRoyalties": true
    },

    "asInstructor": {
      "teachesCourses": true,
      "gradeStudents": true,
      "provideFeedback": true
    }
  }
}
```

## Validation Rules and Business Logic

### Course Creator Domain Assignment Rules

1. **Self-Registration**: Users can register as course creators (subject to verification)
2. **Organization Invitation**: Institutions can invite users as content developers
3. **Multiple Affiliations**: Can create content for multiple organizations
4. **Verification Requirement**: Admin verification recommended for marketplace publishing
5. **IP Agreements**: Clear ownership terms for organization-affiliated creators

### Course Ownership Validation

The system validates course ownership through the `CourseSecurityService`:

1. Extract JWT token from request
2. Get Keycloak ID from token claims
3. Find User record by Keycloak ID
4. Find CourseCreator record by User UUID
5. Verify course's `courseCreatorUuid` matches the CourseCreator's UUID
6. Grant or deny access based on ownership match

## Data Model Reference

### Course Creator Entity

```sql
CREATE TABLE course_creators (
    uuid UUID PRIMARY KEY,
    user_uuid UUID NOT NULL REFERENCES users(uuid),
    full_name VARCHAR(255),
    bio TEXT,
    professional_headline VARCHAR(255),
    website VARCHAR(255),
    admin_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

### Course Creator Domain Mapping

```sql
-- Global course creator assignment
INSERT INTO user_domain_mapping (user_uuid, user_domain_uuid)
VALUES ('user-123', (SELECT uuid FROM user_domains WHERE domain_name = 'course_creator'));

-- Organization course creator assignment
INSERT INTO user_organisation_domain_mapping (
    user_uuid,
    organisation_uuid,
    user_domain_uuid,
    branch_uuid,
    start_date,
    active
)
VALUES (
    'user-123',
    'org-456',
    (SELECT uuid FROM user_domains WHERE domain_name = 'course_creator'),
    'branch-789',
    '2024-01-15',
    true
);
```

## Best Practices for UI Integration

### Course Creator Dashboard Components

1. **Profile Management**
   - Full name, bio, headline, website editing
   - Verification status display
   - Portfolio showcase

2. **Course Management**
   - Create/edit/delete courses
   - Ownership-based access control
   - Publishing workflow

3. **Analytics Dashboard**
   - Revenue tracking
   - Enrollment statistics
   - Student feedback metrics

4. **Verification Tracking**
   - Verification status indicator
   - Admin review progress
   - Resubmission workflow

### Authentication & Authorization Patterns

```javascript
// Example: Check if user is course creator
const isCourseCreator = user.domains?.includes('course_creator');

// Example: Check course ownership before edit
const canEditCourse = async (courseUuid) => {
  const response = await api.get(`/api/v1/courses/${courseUuid}`);
  return response.data.courseCreatorUuid === currentUser.courseCreatorUuid;
};

// Example: Verify before publishing
const publishCourse = async (courseUuid) => {
  if (!isCourseCreator) {
    throw new Error('Only course creators can publish courses');
  }
  // Proceed with publish
};
```

### UI State Management

```javascript
// Course Creator Profile State
{
  courseCreator: {
    uuid: 'creator-123',
    fullName: 'Dr. Jane Smith',
    professionalHeadline: 'ML & AI Expert',
    adminVerified: true,
    bio: '...',
    website: 'https://...'
  },

  ownedCourses: [
    { uuid: 'course-1', name: 'Advanced Python', status: 'published' },
    { uuid: 'course-2', name: 'ML Basics', status: 'draft' }
  ],

  analytics: {
    totalEnrollments: 5200,
    averageRating: 4.8,
    monthlyRevenue: 12500
  }
}
```

This course creator domain implementation provides comprehensive support for content authorship, intellectual property management, and course monetization, enabling both independent creators and institutional content developers to build and publish high-quality educational content while maintaining appropriate ownership rights and quality standards.
