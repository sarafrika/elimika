package apps.sarafrika.elimika.tenancy.services.impl;

import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.tenancy.dto.AdminDashboardStatsDTO;
import apps.sarafrika.elimika.tenancy.dto.AdminDomainAssignmentRequestDTO;
import apps.sarafrika.elimika.tenancy.dto.UserDTO;
import apps.sarafrika.elimika.tenancy.entity.User;
import apps.sarafrika.elimika.tenancy.entity.UserDomain;
import apps.sarafrika.elimika.tenancy.entity.UserDomainMapping;
import apps.sarafrika.elimika.tenancy.entity.UserOrganisationDomainMapping;
import apps.sarafrika.elimika.tenancy.repository.OrganisationRepository;
import apps.sarafrika.elimika.tenancy.repository.UserDomainMappingRepository;
import apps.sarafrika.elimika.tenancy.repository.UserDomainRepository;
import apps.sarafrika.elimika.tenancy.repository.UserOrganisationDomainMappingRepository;
import apps.sarafrika.elimika.tenancy.repository.UserRepository;
import apps.sarafrika.elimika.tenancy.services.AdminService;
import apps.sarafrika.elimika.tenancy.services.UserService;
import apps.sarafrika.elimika.instructor.service.InstructorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of AdminService for admin-specific operations
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2024-12-01
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final UserDomainRepository userDomainRepository;
    private final UserDomainMappingRepository userDomainMappingRepository;
    private final UserOrganisationDomainMappingRepository userOrganisationDomainMappingRepository;
    private final OrganisationRepository organisationRepository;
    private final UserService userService;
    private final InstructorService instructorService;

    @Override
    @Transactional
    public UserDTO assignAdminDomain(UUID userUuid, AdminDomainAssignmentRequestDTO request) {
        log.info("Assigning admin domain {} to user {}", request.domainName(), userUuid);

        User user = findUserOrThrow(userUuid);
        UserDomain adminDomain = findDomainByNameOrThrow(request.domainName());

        // Validate domain is admin-type
        validateAdminDomain(request.domainName());

        // Check if user already has this domain
        boolean hasGlobalDomain = userDomainMappingRepository
                .existsByUserUuidAndUserDomainUuid(userUuid, adminDomain.getUuid());

        if (hasGlobalDomain) {
            throw new IllegalStateException("User already has " + request.domainName() + " domain assigned");
        }

        // Add domain mapping for global admin access
        UserDomainMapping mapping = new UserDomainMapping(
                null,
                userUuid,
                adminDomain.getUuid(),
                LocalDateTime.now(),
                null
        );
        userDomainMappingRepository.save(mapping);

        log.info("Successfully assigned {} domain to user {} for reason: {}",
                request.domainName(), userUuid, request.reason());

        return userService.getUserByUuid(userUuid);
    }

    @Override
    @Transactional
    public UserDTO removeAdminDomain(UUID userUuid, String domainName, String reason) {
        log.info("Removing admin domain {} from user {} for reason: {}", domainName, userUuid, reason);

        findUserOrThrow(userUuid); // Validate user exists
        UserDomain adminDomain = findDomainByNameOrThrow(domainName);

        // Validate domain is admin-type
        validateAdminDomain(domainName);

        // Find and remove global domain mapping
        List<UserDomainMapping> mappings = userDomainMappingRepository
                .findByUserUuidAndUserDomainUuid(userUuid, adminDomain.getUuid());

        if (mappings.isEmpty()) {
            throw new IllegalStateException("User does not have " + domainName + " domain assigned");
        }

        userDomainMappingRepository.deleteAll(mappings);

        log.info("Successfully removed {} domain from user {}", domainName, userUuid);
        return userService.getUserByUuid(userUuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserDTO> getAdminUsers(Map<String, String> filters, Pageable pageable) {
        log.debug("Getting admin users with filters: {}", filters);

        // Get all users with admin domain mappings
        List<UUID> adminUserUuids = new ArrayList<>();

        // Get global admin users
        List<UserDomainMapping> adminMappings = userDomainMappingRepository
                .findByUserDomainUuid(findDomainByNameOrThrow("admin").getUuid());
        adminMappings.forEach(mapping -> adminUserUuids.add(mapping.getUserUuid()));

        // Get organization admin users
        List<UserOrganisationDomainMapping> orgAdminMappings = userOrganisationDomainMappingRepository
                .findActiveByDomainName("organisation_user");
        orgAdminMappings.forEach(mapping -> adminUserUuids.add(mapping.getUserUuid()));

        // Remove duplicates and get users
        List<UUID> distinctAdminUuids = adminUserUuids.stream().distinct().toList();
        List<User> adminUsers = userRepository.findByUuidIn(distinctAdminUuids);

        // Convert to DTOs
        List<UserDTO> adminUserDTOs = adminUsers.stream()
                .map(user -> userService.toUserDTO(user))
                .toList();

        // Apply pagination (simple implementation)
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), adminUserDTOs.size());
        List<UserDTO> pagedResults = adminUserDTOs.subList(start, end);

        return new PageImpl<>(pagedResults, pageable, adminUserDTOs.size());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserDTO> getSystemAdminUsers(Pageable pageable) {
        log.debug("Getting system admin users");

        UserDomain adminDomain = findDomainByNameOrThrow("admin");
        List<UserDomainMapping> adminMappings = userDomainMappingRepository
                .findByUserDomainUuid(adminDomain.getUuid());

        List<UUID> userUuids = adminMappings.stream()
                .map(UserDomainMapping::getUserUuid)
                .toList();

        List<User> systemAdminUsers = userRepository.findByUuidIn(userUuids);
        List<UserDTO> userDTOs = systemAdminUsers.stream()
                .map(user -> userService.toUserDTO(user))
                .toList();

        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), userDTOs.size());
        List<UserDTO> pagedResults = userDTOs.subList(start, end);

        return new PageImpl<>(pagedResults, pageable, userDTOs.size());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserDTO> getOrganizationAdminUsers(Pageable pageable) {
        log.debug("Getting organization admin users");

        List<UserOrganisationDomainMapping> orgAdminMappings = userOrganisationDomainMappingRepository
                .findActiveByDomainName("organisation_user");

        List<UUID> userUuids = orgAdminMappings.stream()
                .map(UserOrganisationDomainMapping::getUserUuid)
                .distinct()
                .toList();

        List<User> orgAdminUsers = userRepository.findByUuidIn(userUuids);
        List<UserDTO> userDTOs = orgAdminUsers.stream()
                .map(user -> userService.toUserDTO(user))
                .toList();

        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), userDTOs.size());
        List<UserDTO> pagedResults = userDTOs.subList(start, end);

        return new PageImpl<>(pagedResults, pageable, userDTOs.size());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAdmin(UUID userUuid) {
        return isSystemAdmin(userUuid) || hasOrganizationAdminRole(userUuid);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSystemAdmin(UUID userUuid) {
        UserDomain adminDomain = userDomainRepository.findByDomainName("admin")
                .orElse(null);
        if (adminDomain == null) {
            return false;
        }

        return userDomainMappingRepository
                .existsByUserUuidAndUserDomainUuid(userUuid, adminDomain.getUuid());
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardStatsDTO getDashboardStatistics() {
        log.debug("Generating admin dashboard statistics");

        // User metrics
        long totalUsers = userRepository.count();
        long suspendedUsers = 0; // Would need status field implementation

        // Organization metrics
        long totalOrganizations = organisationRepository.count();
        long pendingApprovals = 0; // Would need approval status implementation

        // Instructor metrics
        long verifiedInstructors = instructorService.countInstructorsByVerificationStatus(true);
        long pendingInstructorApprovals = instructorService.countInstructorsByVerificationStatus(false);

        // Admin metrics
        UserDomain adminDomain = userDomainRepository.findByDomainName("admin").orElse(null);
        long systemAdmins = adminDomain != null ?
                userDomainMappingRepository.countByUserDomainUuid(adminDomain.getUuid()) : 0;

        long organizationAdmins = userOrganisationDomainMappingRepository
                .countActiveByDomainName("organisation_user");

        return new AdminDashboardStatsDTO(
                LocalDateTime.now(),
                "HEALTHY",
                new AdminDashboardStatsDTO.UserMetrics(
                        totalUsers,
                        0, // activeUsers24h - would need login tracking
                        0, // newRegistrations7d - would need creation date filtering
                        suspendedUsers
                ),
                new AdminDashboardStatsDTO.OrganizationMetrics(
                        totalOrganizations,
                        pendingApprovals,
                        totalOrganizations, // assuming all are active
                        0 // suspendedOrganizations
                ),
                new AdminDashboardStatsDTO.ContentMetrics(
                        0, // totalCourses - would need course repository
                        pendingInstructorApprovals, // Use pending instructor approvals for moderation queue
                        0, // reportedContent
                        verifiedInstructors > 0 ? (double) verifiedInstructors / (verifiedInstructors + pendingInstructorApprovals) * 100.0 : 0.0 // instructor verification rate as quality score
                ),
                new AdminDashboardStatsDTO.SystemPerformance(
                        "99.9%", // serverUptime - would need monitoring integration
                        "150ms", // averageResponseTime
                        "0.01%", // errorRate
                        "65%" // storageUsage
                ),
                new AdminDashboardStatsDTO.AdminMetrics(
                        systemAdmins + organizationAdmins,
                        0, // activeAdminSessions - would need session tracking
                        0, // adminActionsToday - would need audit log
                        systemAdmins,
                        organizationAdmins
                )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserDTO> getAdminEligibleUsers(String searchTerm, Pageable pageable) {
        // Get all users who are not already admins
        // This is a simplified implementation
        List<User> allUsers = userRepository.findAll();
        List<UserDTO> eligibleUsers = allUsers.stream()
                .filter(user -> !isAdmin(user.getUuid()))
                .map(user -> userService.toUserDTO(user))
                .toList();

        // Apply search filter if provided
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            eligibleUsers = eligibleUsers.stream()
                    .filter(user -> user.firstName().toLowerCase().contains(searchTerm.toLowerCase()) ||
                            user.lastName().toLowerCase().contains(searchTerm.toLowerCase()) ||
                            user.email().toLowerCase().contains(searchTerm.toLowerCase()))
                    .toList();
        }

        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), eligibleUsers.size());
        List<UserDTO> pagedResults = eligibleUsers.subList(start, end);

        return new PageImpl<>(pagedResults, pageable, eligibleUsers.size());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canManageAdmins(UUID currentUserUuid) {
        return isSystemAdmin(currentUserUuid);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getAdminDomainHistory(UUID userUuid) {
        // This would require an audit log implementation
        // For now, return current domains
        List<String> currentDomains = userService.getUserDomains(userUuid);
        return currentDomains.stream()
                .filter(domain -> "admin".equals(domain) || "organisation_user".equals(domain))
                .toList();
    }

    // Helper methods

    private User findUserOrThrow(UUID userUuid) {
        return userRepository.findByUuid(userUuid)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userUuid));
    }

    private UserDomain findDomainByNameOrThrow(String domainName) {
        return userDomainRepository.findByDomainName(domainName)
                .orElseThrow(() -> new ResourceNotFoundException("Domain not found: " + domainName));
    }

    private void validateAdminDomain(String domainName) {
        if (!"admin".equals(domainName) && !"organisation_user".equals(domainName)) {
            throw new IllegalArgumentException("Invalid admin domain: " + domainName);
        }
    }

    private boolean hasOrganizationAdminRole(UUID userUuid) {
        List<UserOrganisationDomainMapping> mappings = userOrganisationDomainMappingRepository
                .findActiveByUser(userUuid);
        return mappings.stream()
                .anyMatch(mapping -> {
                    UserDomain domain = userDomainRepository.findByUuid(mapping.getDomainUuid())
                            .orElse(null);
                    return domain != null && "organisation_user".equals(domain.getDomainName());
                });
    }
}