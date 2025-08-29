# Organization-Specific User Domains Guide

## Overview

Organization-specific domains enable contextual role-based access control within educational institutions. Unlike global domains that apply platform-wide, organization domains provide granular control over user permissions, responsibilities, and access rights within specific institutional contexts.

## Organization Domain Architecture

### Multi-Dimensional Mapping System

Organization domains use a sophisticated mapping system that considers multiple contextual factors:

```mermaid
graph TB
    subgraph "Organization Domain Context"
        A[User] --> B[User-Organization Domain Mapping]
        C[Organization] --> B
        D[Domain/Role] --> B
        E[Training Branch] --> B
        F[Time Period] --> B
        
        B --> G[Contextual Permissions]
        
        H[Active Status] --> B
        I[Audit Trail] --> B
    end
    
    subgraph "Mapping Dimensions"
        J[WHO: User Identity]
        K[WHERE: Organization Context]  
        L[WHAT: Role/Domain]
        M[WHICH: Branch/Department]
        N[WHEN: Temporal Validity]
        O[STATUS: Active/Inactive]
        
        J -.-> A
        K -.-> C
        L -.-> D
        M -.-> E
        N -.-> F
        O -.-> H
    end

    style B fill:#e1f5fe
    style G fill:#4caf50
```

## Organization Domain Entity Structure

### Core Entity: UserOrganisationDomainMapping

```mermaid
erDiagram
    USER ||--o{ USER_ORG_DOMAIN_MAPPING : participates
    ORGANISATION ||--o{ USER_ORG_DOMAIN_MAPPING : contains
    USER_DOMAIN ||--o{ USER_ORG_DOMAIN_MAPPING : defines
    TRAINING_BRANCH ||--o{ USER_ORG_DOMAIN_MAPPING : assigns
    
    USER_ORG_DOMAIN_MAPPING {
        uuid uuid PK
        uuid user_uuid FK "User Identity"
        uuid organisation_uuid FK "Organization Context"  
        uuid domain_uuid FK "Role Definition"
        uuid branch_uuid FK "Department Assignment (nullable)"
        date start_date "Affiliation Start"
        date end_date "Affiliation End (nullable)"
        boolean active "Current Status"
        boolean deleted "Soft Delete Flag"
        timestamp created_date "Record Creation"
        timestamp last_modified_date "Record Updates"
        string created_by "Audit: Creator"
        string last_modified_by "Audit: Modifier"
    }
    
    ORGANISATION {
        uuid uuid PK
        string name
        string description
        boolean active
    }
    
    TRAINING_BRANCH {
        uuid uuid PK
        string name
        uuid organisation_uuid FK
        string description
        boolean active
    }
    
    USER_DOMAIN {
        uuid uuid PK
        string domain_name
    }
```

### Organization Affiliation DTO

The system provides rich contextual information through the `UserOrganisationAffiliationDTO`:

```json
{
  "organizationAffiliation": {
    "organisationUuid": "university-abc-123",
    "organisationName": "ABC State University",
    "domainInOrganisation": "instructor",
    "branchUuid": "cs-department-456", 
    "branchName": "Computer Science Department",
    "startDate": "2024-01-15",
    "endDate": null,
    "active": true,
    "affiliatedDate": "2024-01-15T09:00:00Z"
  }
}
```

## Organization Domain Lifecycle Management

### User Invitation and Enrollment

```mermaid
sequenceDiagram
    participant ORG as Organization Admin
    participant API as Platform API
    participant USER as Invited User
    participant EMAIL as Email Service
    participant DB as Database

    Note over ORG,DB: Organization User Invitation Process
    ORG->>API: POST /api/v1/organizations/{orgUuid}/invitations
    Note right of ORG: {<br/>"email": "newuser@university.edu",<br/>"domainName": "instructor",<br/>"branchUuid": "engineering-dept",<br/>"startDate": "2024-01-15"<br/>}
    
    API->>DB: Create invitation record
    API->>EMAIL: Send invitation email
    
    EMAIL->>USER: Organization invitation
    USER->>API: Accept invitation (with profile completion)
    
    API->>DB: Create user_organisation_domain_mapping
    Note right of DB: active=true, deleted=false<br/>start_date=specified<br/>end_date=null
    
    API->>ORG: Confirmation of user enrollment
    API-->>USER: Organization access granted
```

### Branch Transfer Within Organization

```mermaid
sequenceDiagram
    participant ADMIN as Org Admin
    participant API as Platform API  
    participant USER as User
    participant DB as Database

    Note over ADMIN,DB: Branch Transfer Process
    ADMIN->>API: POST /api/v1/organizations/{orgUuid}/users/{userUuid}/transfer
    Note right of ADMIN: {<br/>"fromBranchUuid": "cs-dept",<br/>"toBranchUuid": "math-dept",<br/>"effectiveDate": "2024-06-01",<br/>"reason": "Departmental reassignment"<br/>}
    
    API->>DB: End current mapping (set end_date)
    API->>DB: Create new mapping with new branch
    API->>USER: Notification of transfer
    API-->>ADMIN: Transfer confirmation
    
    Note over USER,DB: User maintains same role (domain)<br/>but in different department context
```

## Branch-Level Organization Management

### Hierarchical Structure

Organizations can have multiple training branches (departments/divisions) with specific management:

```mermaid
graph TD
    A[ABC University] --> B[College of Engineering]
    A --> C[College of Liberal Arts]
    A --> D[Graduate School]
    
    B --> E[Computer Science Dept]
    B --> F[Electrical Engineering Dept] 
    B --> G[Mechanical Engineering Dept]
    
    C --> H[English Department]
    C --> I[History Department]
    C --> J[Psychology Department]
    
    D --> K[PhD Programs]
    D --> L[Masters Programs]
    
    E --> M[Faculty: 25 Instructors]
    E --> N[Students: 450 Students]
    E --> O[Staff: 5 Admin Users]
    
    style A fill:#1976d2
    style B fill:#388e3c
    style C fill:#388e3c
    style D fill:#388e3c
    style E fill:#ff5722
```

### Branch-Specific User Management

```bash
# Example: Managing users within specific branches

# 1. Get all users in Computer Science department
curl -X GET "/api/v1/organizations/university-abc/branches/cs-dept/users" \
  -H "Authorization: Bearer {admin-token}"

# 2. Get instructors only in CS department  
curl -X GET "/api/v1/organizations/university-abc/branches/cs-dept/users?domain=instructor" \
  -H "Authorization: Bearer {admin-token}"

# 3. Assign user to specific branch with role
curl -X POST "/api/v1/organizations/university-abc/users/prof-smith/assign" \
  -H "Authorization: Bearer {admin-token}" \
  -H "Content-Type: application/json" \
  -d '{
    "domainName": "instructor",
    "branchUuid": "cs-dept-456",
    "startDate": "2024-01-15",
    "academicRank": "Associate Professor",
    "courseLoad": 6
  }'
```

## Organization Domain Repository Capabilities

### Advanced Query Methods

The `UserOrganisationDomainMappingRepository` provides comprehensive querying:

```java
// Active relationship queries with organization context
List<UserOrganisationDomainMapping> findActiveByUser(UUID userUuid);
List<UserOrganisationDomainMapping> findActiveByOrganisation(UUID orgUuid);
Optional<UserOrganisationDomainMapping> findActiveByUserAndOrganisation(UUID userUuid, UUID orgUuid);

// Role-based filtering within organizations
List<UserOrganisationDomainMapping> findActiveByOrganisationAndDomain(UUID orgUuid, UUID domainUuid);

// Branch-specific queries
List<UserOrganisationDomainMapping> findActiveByBranch(UUID branchUuid);
long countDistinctActiveUsersByBranch(UUID branchUuid);

// Temporal and lifecycle queries
List<UserOrganisationDomainMapping> findMappingsEndingBetween(LocalDate start, LocalDate end);
List<UserOrganisationDomainMapping> findCurrentActiveMapping(UUID userUuid, UUID orgUuid);

// Bulk operations for organization management
List<UserOrganisationDomainMapping> findActiveByUserUuidsAndOrganisation(
    List<UUID> userUuids, UUID orgUuid);
```

### Analytics and Reporting Queries

```java
// Organization analytics
long countDistinctActiveUsersByOrganisation(UUID orgUuid);
long countDistinctActiveUsersByOrganisationAndDomain(UUID orgUuid, UUID domainUuid);

// Cross-organization user analysis
List<UUID> findDistinctOrganisationUuidsByUser(UUID userUuid);
List<UUID> findDistinctUserUuidsByOrganisation(UUID orgUuid);
```

## Organization API Reference

### Organization User Management

| Method | Endpoint | Purpose | Organization Context |
|--------|----------|---------|----------------------|
| `GET` | `/api/v1/organizations/{uuid}/users` | List organization users | All affiliated users |
| `GET` | `/api/v1/organizations/{uuid}/users?domain={role}` | Filter by role | Role-specific listing |
| `POST` | `/api/v1/organizations/{uuid}/users/{userUuid}` | Add user to organization | Direct assignment |
| `DELETE` | `/api/v1/organizations/{uuid}/users/{userUuid}` | Remove from organization | Soft delete mapping |

### Branch Management

| Method | Endpoint | Purpose | Branch Context |
|--------|----------|---------|----------------|
| `GET` | `/api/v1/organizations/{uuid}/branches` | List organization branches | Department structure |
| `POST` | `/api/v1/organizations/{uuid}/branches` | Create new branch | Department creation |
| `GET` | `/api/v1/branches/{branchUuid}/users` | List branch users | Department roster |
| `PUT` | `/api/v1/branches/{branchUuid}/users/{userUuid}` | Transfer user to branch | Departmental transfer |

### Analytics and Reporting

| Method | Endpoint | Purpose | Analytical Context |
|--------|----------|---------|-------------------|
| `GET` | `/api/v1/organizations/{uuid}/analytics/users` | User distribution analytics | Role breakdowns, trends |
| `GET` | `/api/v1/organizations/{uuid}/analytics/branches` | Branch performance metrics | Department comparisons |
| `GET` | `/api/v1/organizations/{uuid}/reports/affiliations` | Detailed affiliation report | Audit and compliance |

## Organization Domain Business Rules

### Temporal Management Rules

```mermaid
flowchart TD
    A[User Organization Assignment] --> B{Start Date Validation}
    B -->|Future Date| C[Scheduled Assignment]
    B -->|Current Date| D[Immediate Activation]
    B -->|Past Date| E[Historical Record]
    
    C --> F[active=false, until start_date]
    D --> G[active=true, immediate access]
    E --> H[active=true, backdated record]
    
    G --> I{End Date Set?}
    H --> I
    
    I -->|Yes| J[Temporary Assignment]
    I -->|No| K[Ongoing Assignment]
    
    J --> L[Auto-deactivate on end_date]
    K --> M[Manual deactivation required]
    
    style D fill:#4caf50
    style G fill:#4caf50
    style L fill:#ff9800
```

### Assignment Validation Logic

```java
// Example organization domain assignment validation
public void validateOrganizationDomainAssignment(
    UUID userUuid, UUID orgUuid, String domainName, UUID branchUuid) {
    
    // 1. Validate organization is active
    Organisation org = findOrganisationOrThrow(orgUuid);
    if (!org.isActive()) {
        throw new IllegalStateException("Cannot assign users to inactive organization");
    }
    
    // 2. Validate branch belongs to organization (if specified)
    if (branchUuid != null) {
        TrainingBranch branch = findBranchOrThrow(branchUuid);
        if (!branch.getOrganisationUuid().equals(orgUuid)) {
            throw new IllegalArgumentException("Branch does not belong to specified organization");
        }
    }
    
    // 3. Check for existing active assignment
    Optional<UserOrganisationDomainMapping> existing = 
        repository.findActiveByUserAndOrganisation(userUuid, orgUuid);
    if (existing.isPresent()) {
        // Handle existing assignment (transfer, update, or reject)
        handleExistingAssignment(existing.get(), domainName, branchUuid);
    }
    
    // 4. Validate domain assignment rules
    validateDomainAssignmentRules(userUuid, domainName, orgUuid);
}
```

## Multi-Organization Scenarios

### Users with Multiple Organization Affiliations

```json
{
  "userProfile": {
    "userUuid": "multi-org-user-123",
    "firstName": "Dr. Sarah",
    "lastName": "Johnson",
    "email": "sarah.johnson@email.com",
    
    "organisationAffiliations": [
      {
        "organisationUuid": "state-university",
        "organisationName": "State University",
        "domainInOrganisation": "instructor",
        "branchName": "Computer Science Department",
        "startDate": "2020-08-01",
        "active": true,
        "primaryAffiliation": true
      },
      {
        "organisationUuid": "tech-consulting-firm", 
        "organisationName": "Tech Consulting Solutions",
        "domainInOrganisation": "instructor",
        "branchName": "Training Division",
        "startDate": "2023-01-15",
        "active": true,
        "primaryAffiliation": false
      },
      {
        "organisationUuid": "community-college",
        "organisationName": "Metro Community College", 
        "domainInOrganisation": "instructor",
        "branchName": "Continuing Education",
        "startDate": "2022-06-01",
        "endDate": "2023-12-31",
        "active": false,
        "primaryAffiliation": false
      }
    ]
  }
}
```

### Cross-Organization Collaboration

```mermaid
sequenceDiagram
    participant USER as Multi-Org User
    participant ORG1 as University A
    participant ORG2 as University B
    participant API as Platform API
    participant DB as Database

    Note over USER,DB: Cross-Organization Course Collaboration
    ORG1->>API: Create collaborative course
    API->>DB: Course with multi-org access
    
    ORG1->>API: Invite instructor from University B
    API->>USER: Cross-org teaching invitation
    USER->>API: Accept invitation
    
    API->>DB: Create temporary org assignment
    Note right of DB: Limited scope: specific course only<br/>Duration: course duration<br/>Branch: collaborative projects
    
    API->>ORG2: Notification of instructor participation
    API-->>ORG1: Confirmation of cross-org instructor
```

## Organization Domain Analytics

### Membership Analytics Dashboard

```json
{
  "organizationAnalytics": {
    "organizationUuid": "state-university-123",
    "organizationName": "State University",
    "reportingPeriod": "Fall 2024",
    
    "membershipMetrics": {
      "totalActiveUsers": 12847,
      "usersByDomain": {
        "student": 11234,
        "instructor": 892,
        "organisation_user": 721
      },
      "usersByBranch": {
        "Computer Science": 2341,
        "Engineering": 3567,  
        "Liberal Arts": 2890,
        "Graduate School": 1456
      }
    },
    
    "temporalMetrics": {
      "newAffiliationsThisMonth": 234,
      "expiredAffiliationsThisMonth": 67,
      "transfersBetweenBranches": 12,
      "averageAffiliationDuration": "3.2 years"
    },
    
    "branchPerformance": [
      {
        "branchName": "Computer Science",
        "activeUsers": 2341,
        "courseCompletionRate": 94.2,
        "studentSatisfaction": 4.6,
        "facultyUtilization": 87.3
      }
    ]
  }
}
```

This organization-specific domain system provides sophisticated multi-tenant capabilities with granular control over user roles, responsibilities, and access within institutional contexts while maintaining clear audit trails and temporal management of affiliations.