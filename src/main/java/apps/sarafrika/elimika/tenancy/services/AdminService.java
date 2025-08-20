package apps.sarafrika.elimika.tenancy.services;

import apps.sarafrika.elimika.common.dto.PagedDTO;
import apps.sarafrika.elimika.tenancy.dto.UserDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service interface for managing system administrators.
 * 
 * System administrators have platform-wide privileges including:
 * - Global system configuration
 * - Cross-organization management
 * - User role management across all organizations
 * - System-wide analytics and reporting
 * - Technical operations and maintenance
 */
public interface AdminService {
    
    /**
     * Register a user as a system administrator.
     * 
     * This method assigns admin privileges to an existing user, giving them
     * platform-wide administrative access.
     * 
     * @param userUuid the UUID of the user to promote to admin
     * @param fullName the full name of the admin (for logging purposes)
     * @return UserDTO with updated admin domain assignment
     * @throws IllegalArgumentException if user doesn't exist
     * @throws IllegalStateException if user is already an admin
     */
    UserDTO registerAdmin(UUID userUuid, String fullName);
    
    /**
     * Get all system administrators with pagination.
     * 
     * @param pageable pagination parameters
     * @return paged list of admin users
     */
    Page<UserDTO> getAllAdmins(Pageable pageable);
    
    /**
     * Check if a user has system admin privileges.
     * 
     * @param userUuid the user UUID to check
     * @return true if user is a system admin, false otherwise
     */
    boolean isSystemAdmin(UUID userUuid);
    
    /**
     * Revoke admin privileges from a user.
     * 
     * This removes the admin domain from the user, but preserves any other
     * domain assignments they may have.
     * 
     * @param userUuid the UUID of the admin to demote
     * @return UserDTO with updated domain assignments
     * @throws IllegalArgumentException if user doesn't exist or is not an admin
     */
    UserDTO revokeAdminPrivileges(UUID userUuid);
    
    /**
     * Get system-wide statistics accessible only to system administrators.
     * 
     * @return map containing system statistics like total users, organizations, etc.
     */
    Map<String, Object> getSystemStatistics();
    
    /**
     * Get all users across all organizations (admin-only function).
     * 
     * @param pageable pagination parameters
     * @return paged list of all users in the system
     */
    Page<UserDTO> getAllUsersSystemWide(Pageable pageable);
}