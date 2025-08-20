# User Domains and Roles Documentation

## Overview

The Elimika Learning Management System implements a multi-tenancy user management system with domain-based role segregation. This document outlines the different user domains, their roles, restrictions, and organizational relationships.

## User Domain Architecture

The system defines user domains through a combination of enums and database entities that manage user roles and organizational affiliations.

### Administrative Hierarchy Summary

The Elimika system implements a **three-tier administrative structure**:

1. **System Admins** (`admin`) - Platform-wide control and technical oversight
2. **Organisation Administrators** (`organisation_user` with full org access) - Complete organizational management
3. **Branch-Level Organisation Users** (`organisation_user` with branch-specific access) - Training branch operations

This hierarchy ensures proper separation of concerns while maintaining security and operational efficiency across the multi-tenant platform.

### Core User Domains

Based on the `UserDomain` enum (`apps.sarafrika.elimika.common.enums.UserDomain`), the system recognizes four primary user domains:

#### 1. **Student** (`student`)
- **Primary Role**: Course learners and content consumers
- **Domain Scope**: Access to enrolled courses, lessons, assignments, and assessments
- **Key Responsibilities**:
  - Enroll in courses and training programs
  - Access lesson content and track progress
  - Submit assignments and take quizzes
  - View certificates upon completion
  - Track personal learning progress

#### 2. **Instructor** (`instructor`)
- **Primary Role**: Content creators and course facilitators
- **Domain Scope**: Course creation, student management, and assessment delivery
- **Key Responsibilities**:
  - Create and manage courses and lessons
  - Design and grade assessments and assignments
  - Create rubrics and scoring criteria
  - Monitor student progress and performance
  - Generate and manage course certificates
- **Professional Attributes**:
  - Educational background tracking
  - Professional experience records
  - Skill certifications
  - Professional membership documentation
  - Document management for credentials

#### 3. **Admin** (`admin`)
- **Primary Role**: System administration and platform oversight
- **Domain Scope**: Full system access across the entire platform
- **Administrative Level**: **System-wide Administrator**
- **Key Responsibilities**:
  - Configure global system settings and parameters
  - Manage platform-wide user accounts and system roles
  - Oversee all organizational structures across the platform
  - Monitor system-wide analytics and performance metrics
  - Handle platform maintenance and technical operations
  - Manage system integrations and third-party services
  - Control access to system administration functions
- **Access Level**: Complete administrative control over the entire Elimika platform

#### 4. **Organisation User** (`organisation_user`)
This domain encompasses two distinct administrative levels within organizations:

##### 4a. **Organisation Administrator**
- **Primary Role**: Complete organizational management and oversight
- **Domain Scope**: Full control within their assigned organization(s)
- **Administrative Level**: **Organization-wide Administrator**
- **Key Responsibilities**:
  - Manage all users within their organization
  - Control organizational settings and configurations
  - Oversee all training branches within the organization
  - Manage organization-wide training programs and curricula
  - Handle organization-level reporting and analytics
  - Process and approve organizational invitations
  - Assign and manage organisation users to specific branches
  - Monitor organization-wide performance metrics
  - Manage organization's instructors and students
- **Access Level**: Complete administrative control within their organization

##### 4b. **Branch-Level Organisation User**
- **Primary Role**: Training branch management and operations
- **Domain Scope**: Limited to their specifically assigned training branch(es)
- **Administrative Level**: **Branch-specific Administrator**
- **Key Responsibilities**:
  - Manage users assigned to their specific training branch
  - Oversee training programs within their branch scope
  - Handle branch-level reporting and student progress tracking
  - Process branch-specific invitations and enrollments
  - Coordinate with instructors assigned to their branch
  - Monitor branch-specific performance and completion rates
  - Manage branch resources and scheduling
- **Access Level**: Administrative control limited to assigned training branches

## Role Mapping and Restrictions

### Database Structure

The system uses three main entities to manage user-domain relationships:

1. **UserDomain** - Defines available domains
2. **UserDomainMapping** - Maps users to their primary domains
3. **UserOrganisationDomainMapping** - Maps users to domains within specific organizations

### Multi-Tenancy Support

Users can have different roles across multiple organizations through the `UserOrganisationDomainMapping` entity:

- **User UUID**: Identifies the user
- **Organisation UUID**: Specifies the organization
- **Domain UUID**: Defines the user's role within that organization
- **Branch UUID**: (Optional) Assigns user to specific training branch
- **Active Status**: Controls active/inactive status
- **Temporal Tracking**: Start and end dates for role assignments

### Key Restrictions and Access Control

#### Student Restrictions
- Cannot create or modify course content
- Limited to read-only access for most system configurations
- Can only access courses they are enrolled in
- Cannot view other students' assessment results
- Cannot access administrative functions

#### Instructor Restrictions
- Cannot access system-wide administrative functions
- Limited to organizations and branches they are assigned to
- Cannot modify user roles or organizational structures
- Cannot access other instructors' private content without permission
- Course creation limited to their assigned organizations

#### System Admin Privileges
- **Full Platform Access**: Complete administrative control over the entire Elimika platform
- **Global Configuration**: Can configure system-wide settings and parameters
- **Cross-Organization Management**: Can manage all organizations and their structures
- **User Role Management**: Can assign and modify any user roles across the platform
- **System Analytics**: Access to comprehensive platform-wide analytics and reporting
- **Technical Operations**: Handle system maintenance, integrations, and technical configurations

#### Organisation Administrator Restrictions & Privileges
**Privileges within their Organization:**
- Full control over organizational settings and configurations
- Manage all users within their organization (students, instructors, branch users)
- Oversee all training branches and assign branch administrators
- Access to organization-wide reporting and analytics
- Process organizational invitations and user approvals

**Restrictions:**
- Cannot access or modify other organizations' data or settings
- Cannot access system-wide administrative functions
- Cannot modify global system settings or configurations
- Cannot manage users outside their organizational scope
- Cannot access platform-level analytics or technical operations

#### Branch-Level Organisation User Restrictions & Privileges
**Privileges within their Training Branch:**
- Manage users assigned specifically to their branch
- Oversee training programs and courses within branch scope
- Handle branch-level student enrollments and progress tracking
- Process branch-specific invitations and user assignments
- Access branch-specific reporting and analytics

**Restrictions:**
- Cannot access users or data from other branches within the organization
- Cannot modify organization-wide settings or policies
- Cannot assign users to other branches
- Cannot access organization-level analytics beyond their branch
- Cannot manage instructors assigned to other branches
- Cannot access system or organization administrative functions

## Authentication and Authorization

### Keycloak Integration

The system integrates with Keycloak for authentication and role management:

- **Role Management**: Through `KeycloakRoleService`
- **User Synchronization**: Bidirectional sync with Keycloak
- **JWT Processing**: Custom `KeyCloakJwtAuthenticationConverter` for role extraction
- **Realm-based Isolation**: Multi-realm support for organization separation

### Security Implementation

- **Role-based Access Control (RBAC)**: Domain-based permissions
- **Organization-level Isolation**: Users can only access assigned organizations
- **Temporal Access Control**: Role assignments with start/end dates
- **Active/Inactive Status**: Fine-grained access control
- **Branch-level Segregation**: Additional isolation within organizations

## User Lifecycle Management

### User Registration Process
1. User account created in Keycloak
2. User entity created in local database
3. Domain assignment through UserDomainMapping
4. Organization assignment (if applicable) through UserOrganisationDomainMapping
5. Branch assignment (optional) for additional segregation

### Role Assignment Workflow
1. Admin or Organisation User creates invitation
2. Invited user accepts invitation
3. System creates appropriate domain mappings
4. Keycloak roles synchronized
5. User gains access to organization-specific resources

### Access Revocation
- Set mapping status to inactive
- Update end date for temporal access control
- Mark mapping as deleted for audit trail
- Synchronize role changes with Keycloak

## Invitation System

### Invitation Types
- **Organization Invitations**: Invite users to join an organization
- **Branch Invitations**: Invite users to specific training branches
- **Role-specific Invitations**: Assign specific domain roles during invitation

### Invitation Workflow
1. Admin/Organisation User sends invitation
2. Email notification sent to invitee
3. Invitee accepts/declines invitation
4. System processes acceptance and creates appropriate mappings
5. Notification sent to inviter about status change

## Analytics and Reporting

### User Statistics Available
- Total users per domain
- Active users per organization
- Users per training branch
- Domain distribution analytics
- Role assignment history
- Organizational membership trends

### Compliance and Audit
- Complete audit trail of role changes
- Temporal tracking of user assignments
- Document management for instructor credentials
- Invitation and access logs

## Best Practices

### For Administrators
- Regularly review active user assignments
- Monitor role assignment expiry dates
- Maintain up-to-date organizational structures
- Implement proper onboarding/offboarding procedures

### For Organisation Users
- Limit user access to necessary functions only
- Regularly review branch assignments
- Monitor user activity within organization
- Ensure proper role assignments for new users

### For Security
- Implement time-based role assignments where appropriate
- Regular audit of user permissions
- Monitor cross-organizational access patterns
- Maintain separation between organizations

## Technical Implementation Notes

- **Database Schema**: PostgreSQL with proper indexing for performance
- **Migration Support**: Flyway migrations for schema changes
- **Event-driven Architecture**: Domain events for user lifecycle management
- **Caching Strategy**: Optimized queries for large user bases
- **API Design**: RESTful APIs with proper pagination support

---

*Last Updated: 2025-08-20*  
*Version: 1.0*  
*Maintainer: Elimika Development Team*