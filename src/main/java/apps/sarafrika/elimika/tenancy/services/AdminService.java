package apps.sarafrika.elimika.tenancy.services;

import apps.sarafrika.elimika.tenancy.dto.AdminActivityEventDTO;
import apps.sarafrika.elimika.tenancy.dto.AdminDashboardStatsDTO;
import apps.sarafrika.elimika.tenancy.dto.AdminDomainAssignmentRequestDTO;
import apps.sarafrika.elimika.tenancy.dto.UserDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service interface for admin-specific operations
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2024-12-01
 */
public interface AdminService {

    /**
     * Assign admin domain to a user
     *
     * @param userUuid UUID of the user to assign admin domain to
     * @param request  Admin domain assignment request details
     * @return Updated user DTO with admin domain assigned
     */
    UserDTO assignAdminDomain(UUID userUuid, AdminDomainAssignmentRequestDTO request);

    /**
     * Remove admin domain from a user
     *
     * @param userUuid   UUID of the user to remove admin domain from
     * @param domainName Domain name to remove
     * @param reason     Reason for removing admin domain
     * @return Updated user DTO with admin domain removed
     */
    UserDTO removeAdminDomain(UUID userUuid, String domainName, String reason);

    /**
     * Get all admin users with filtering and pagination
     *
     * @param filters  Optional filters (adminLevel, status, etc.)
     * @param pageable Pagination information
     * @return Paginated list of admin users
     */
    Page<UserDTO> getAdminUsers(Map<String, String> filters, Pageable pageable);

    /**
     * Get all system admin users (global admin domain)
     *
     * @param pageable Pagination information
     * @return Paginated list of system admin users
     */
    Page<UserDTO> getSystemAdminUsers(Pageable pageable);

    /**
     * Get all organization admin users
     *
     * @param pageable Pagination information
     * @return Paginated list of organization admin users
     */
    Page<UserDTO> getOrganizationAdminUsers(Pageable pageable);

    /**
     * Check if a user has admin privileges
     *
     * @param userUuid UUID of the user to check
     * @return True if user has any admin domain, false otherwise
     */
    boolean isAdmin(UUID userUuid);

    /**
     * Check if a user has system admin privileges
     *
     * @param userUuid UUID of the user to check
     * @return True if user has system admin domain, false otherwise
     */
    boolean isSystemAdmin(UUID userUuid);

    /**
     * Get admin dashboard statistics
     *
     * @return Comprehensive admin dashboard statistics
     */
    AdminDashboardStatsDTO getDashboardStatistics();

    /**
     * Get recent admin activity feed for dashboard timeline.
     *
     * @param pageable pagination information
     * @return paginated list of recent admin activities
     */
    Page<AdminActivityEventDTO> getDashboardActivity(Pageable pageable);

    /**
     * Get list of users eligible for admin promotion
     * (Active users who are not already admins)
     *
     * @param searchTerm Optional search term for filtering
     * @param pageable   Pagination information
     * @return Paginated list of eligible users
     */
    Page<UserDTO> getAdminEligibleUsers(String searchTerm, Pageable pageable);

    /**
     * Validate if current user can perform admin operations
     *
     * @param currentUserUuid UUID of the current user
     * @return True if current user has permission to manage admins
     */
    boolean canManageAdmins(UUID currentUserUuid);

    /**
     * Get admin domain assignment history for a user
     *
     * @param userUuid UUID of the user
     * @return List of domain assignment history (if audit system exists)
     */
    List<String> getAdminDomainHistory(UUID userUuid);
}
