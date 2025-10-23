/**
 * The Tenancy Module provides multi-tenant organization and user management for the Elimika platform.
 * 
 * This module follows Spring Modulith principles and serves as the foundation for multi-tenancy
 * across the entire platform. It provides:
 * - Organization management and hierarchical structures
 * - Multi-tenant user management with domain isolation
 * - Training branch management for organizational subdivisions
 * - User invitation and onboarding workflows
 * - Role-based access control integration
 * - Cross-tenant data isolation and security
 * 
 * Key Features:
 * - Organization Management: Create and manage educational institutions and organizations
 * - Training Branches: Support for multiple campuses, departments, or training centers
 * - User Management: Comprehensive user lifecycle management with tenant isolation
 * - Invitation System: Secure user invitation and registration workflows
 * - Domain Mapping: User-to-organization domain associations for proper data isolation
 * - Role Integration: Seamless integration with authentication and authorization systems
 * 
 * Module Boundaries:
 * - Owns: Organizations, training branches, users, invitations, domain mappings
 * - Does Not Own: Authentication tokens, specific user roles (student/instructor), course data
 * 
 * The module exposes its services through well-defined interfaces while keeping implementation
 * details internal. Other modules depend on this for tenant context and user information.
 * 
 * Key Components:
 * - OrganisationService: Main service for organization management
 * - UserService: Comprehensive user management operations
 * - TrainingBranchService: Branch and subdivision management
 * - InvitationService: User invitation and onboarding workflows
 * - UserOrganisationDomainMapping: Tenant isolation enforcement
 * 
 * Integration Points:
 * - Provides tenant context for all other modules
 * - Integrates with notifications module for user preferences
 * - Publishes domain events for user lifecycle management
 * - Foundation for role-based access control across the platform
 * 
 * Security Considerations:
 * - Enforces strict tenant data isolation
 * - Manages user access across organizational boundaries
 * - Provides audit trails for user management operations
 * 
 * @since 1.0.0
 */
@ApplicationModule(
        allowedDependencies = {
                "notifications :: preferences-spi",
                "notifications :: events-api",
                "instructor :: instructor-spi",
                "shared",
                "authentication :: keycloak-integration"
        }
)
package apps.sarafrika.elimika.tenancy;

import org.springframework.modulith.ApplicationModule;