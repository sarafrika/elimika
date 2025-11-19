package apps.sarafrika.elimika.tenancy.services.impl;

import apps.sarafrika.elimika.authentication.spi.KeycloakAdminEventService;
import apps.sarafrika.elimika.authentication.spi.KeycloakAdminEventSummary;
import apps.sarafrika.elimika.shared.spi.analytics.CommerceAnalyticsService;
import apps.sarafrika.elimika.shared.spi.analytics.CommerceAnalyticsSnapshot;
import apps.sarafrika.elimika.shared.spi.analytics.CourseAnalyticsService;
import apps.sarafrika.elimika.shared.spi.analytics.CourseAnalyticsSnapshot;
import apps.sarafrika.elimika.shared.spi.analytics.CourseCreatorAnalyticsService;
import apps.sarafrika.elimika.shared.spi.analytics.CourseCreatorAnalyticsSnapshot;
import apps.sarafrika.elimika.shared.spi.analytics.InstructorAnalyticsService;
import apps.sarafrika.elimika.shared.spi.analytics.InstructorAnalyticsSnapshot;
import apps.sarafrika.elimika.shared.spi.analytics.NotificationAnalyticsService;
import apps.sarafrika.elimika.shared.spi.analytics.NotificationAnalyticsSnapshot;
import apps.sarafrika.elimika.shared.spi.analytics.TimetablingAnalyticsService;
import apps.sarafrika.elimika.shared.spi.analytics.TimetablingAnalyticsSnapshot;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.instructor.spi.InstructorManagementService;
import apps.sarafrika.elimika.tenancy.dto.AdminActivityEventDTO;
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
import apps.sarafrika.elimika.shared.tracking.entity.RequestAuditLog;
import apps.sarafrika.elimika.shared.tracking.repository.RequestAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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
    private final InstructorManagementService instructorManagementService;
    private final CourseAnalyticsService courseAnalyticsService;
    private final TimetablingAnalyticsService timetablingAnalyticsService;
    private final CommerceAnalyticsService commerceAnalyticsService;
    private final NotificationAnalyticsService notificationAnalyticsService;
    private final InstructorAnalyticsService instructorAnalyticsService;
    private final CourseCreatorAnalyticsService courseCreatorAnalyticsService;
    private final KeycloakAdminEventService keycloakAdminEventService;
    private final RequestAuditLogRepository requestAuditLogRepository;

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
        UserDomainMapping mapping = new UserDomainMapping();
        mapping.setUserUuid(userUuid);
        mapping.setUserDomainUuid(adminDomain.getUuid());
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

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime twentyFourHoursAgo = now.minusHours(24);
        LocalDateTime sevenDaysAgo = now.minusDays(7);

        // Cross-module snapshots
        CourseAnalyticsSnapshot courseAnalytics = courseAnalyticsService.captureSnapshot();
        TimetablingAnalyticsSnapshot timetablingAnalytics = timetablingAnalyticsService.captureSnapshot();
        CommerceAnalyticsSnapshot commerceAnalytics = commerceAnalyticsService.captureSnapshot();
        NotificationAnalyticsSnapshot notificationAnalytics = notificationAnalyticsService.captureSnapshot();
        InstructorAnalyticsSnapshot instructorAnalytics = instructorAnalyticsService.captureSnapshot();
        CourseCreatorAnalyticsSnapshot courseCreatorAnalytics = courseCreatorAnalyticsService.captureSnapshot();
        KeycloakAdminEventSummary keycloakAdminEventSummary = keycloakAdminEventService.getAdminEventSummary();

        // User metrics
        long totalUsers = userRepository.count();
        long suspendedUsers = userRepository.countByActiveFalse();
        long activeUsers24h = userRepository.countByLastModifiedDateAfter(twentyFourHoursAgo);
        long newRegistrations7d = userRepository.countByCreatedDateAfter(sevenDaysAgo);

        // Organization metrics
        long totalOrganizations = organisationRepository.countByDeletedFalse();
        long pendingApprovals = organisationRepository.countPendingApproval();
        long activeOrganizations = organisationRepository.countByActiveTrueAndDeletedFalse();
        long suspendedOrganizations = organisationRepository.countByActiveFalseAndDeletedFalse();

        // Admin metrics
        UserDomain adminDomain = userDomainRepository.findByDomainName("admin").orElse(null);
        long systemAdmins = adminDomain != null ?
                userDomainMappingRepository.countByUserDomainUuid(adminDomain.getUuid()) : 0;

        long organizationAdmins = userOrganisationDomainMappingRepository
                .countActiveByDomainName("organisation_user");

        long totalAdmins = systemAdmins + organizationAdmins;

        return new AdminDashboardStatsDTO(
                now,
                "HEALTHY",
                new AdminDashboardStatsDTO.UserMetrics(
                        totalUsers,
                        activeUsers24h,
                        newRegistrations7d,
                        suspendedUsers
                ),
                new AdminDashboardStatsDTO.OrganizationMetrics(
                        totalOrganizations,
                        pendingApprovals,
                        activeOrganizations,
                        suspendedOrganizations
                ),
                new AdminDashboardStatsDTO.ContentMetrics(
                        courseAnalytics.totalCourses(),
                        courseAnalytics.inReviewCourses(),
                        0,
                        courseAnalytics.averageCourseProgress()
                ),
                new AdminDashboardStatsDTO.SystemPerformance(
                        "99.9%", // serverUptime - would need monitoring integration
                        "150ms", // averageResponseTime
                        "0.01%", // errorRate
                        "65%" // storageUsage
                ),
                new AdminDashboardStatsDTO.AdminMetrics(
                        totalAdmins,
                        0, // activeAdminSessions - would need session tracking
                        keycloakAdminEventSummary.eventsLast24Hours(),
                        systemAdmins,
                        organizationAdmins
                ),
                new AdminDashboardStatsDTO.KeycloakAdminEventMetrics(
                        keycloakAdminEventSummary.eventsLast24Hours(),
                        keycloakAdminEventSummary.eventsLast7Days(),
                        keycloakAdminEventSummary.operationsLast24Hours(),
                        keycloakAdminEventSummary.resourceTypesLast24Hours()
                ),
                new AdminDashboardStatsDTO.LearningMetrics(
                        courseAnalytics.totalCourses(),
                        courseAnalytics.publishedCourses(),
                        courseAnalytics.inReviewCourses(),
                        courseAnalytics.draftCourses(),
                        courseAnalytics.archivedCourses(),
                        courseAnalytics.totalCourseEnrollments(),
                        courseAnalytics.activeCourseEnrollments(),
                        courseAnalytics.newCourseEnrollments7d(),
                        courseAnalytics.completedCourseEnrollments30d(),
                        courseAnalytics.averageCourseProgress(),
                        courseAnalytics.totalTrainingPrograms(),
                        courseAnalytics.publishedTrainingPrograms(),
                        courseAnalytics.activeTrainingPrograms(),
                        courseAnalytics.programEnrollments(),
                        courseAnalytics.completedProgramEnrollments30d()
                ),
                new AdminDashboardStatsDTO.TimetablingMetrics(
                        timetablingAnalytics.sessionsNext7Days(),
                        timetablingAnalytics.sessionsLast30Days(),
                        timetablingAnalytics.sessionsCompletedLast30Days(),
                        timetablingAnalytics.sessionsCancelledLast30Days(),
                        timetablingAnalytics.attendedEnrollmentsLast30Days(),
                        timetablingAnalytics.absentEnrollmentsLast30Days()
                ),
                new AdminDashboardStatsDTO.CommerceMetrics(
                        commerceAnalytics.totalOrders(),
                        commerceAnalytics.ordersLast30Days(),
                        commerceAnalytics.capturedOrders(),
                        commerceAnalytics.uniqueCustomers(),
                        commerceAnalytics.newCustomersLast30Days(),
                        commerceAnalytics.coursePurchasesLast30Days(),
                        commerceAnalytics.classPurchasesLast30Days()
                ),
                new AdminDashboardStatsDTO.CommunicationMetrics(
                        notificationAnalytics.notificationsCreated7d(),
                        notificationAnalytics.notificationsDelivered7d(),
                        notificationAnalytics.notificationsFailed7d(),
                        notificationAnalytics.pendingNotifications()
                ),
                new AdminDashboardStatsDTO.ComplianceMetrics(
                        instructorAnalytics.verifiedInstructors(),
                        instructorAnalytics.pendingInstructors(),
                        instructorAnalytics.documentsPendingVerification(),
                        instructorAnalytics.documentsExpiring30d(),
                        courseCreatorAnalytics.totalCourseCreators(),
                        courseCreatorAnalytics.verifiedCourseCreators(),
                        courseCreatorAnalytics.pendingCourseCreators()
                )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminActivityEventDTO> getDashboardActivity(Pageable pageable) {
        log.debug("Fetching admin activity feed with pageable: {}", pageable);

        Sort sort = pageable.getSort().isSorted()
                ? pageable.getSort()
                : Sort.by(Sort.Direction.DESC, "createdDate");

        PageRequest pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

        Page<RequestAuditLog> auditLogs = requestAuditLogRepository.findAdminActivity(
                "/api/v1/admin",
                200,
                399,
                pageRequest
        );

        return auditLogs.map(this::toAdminActivityEventDTO);
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

    private AdminActivityEventDTO toAdminActivityEventDTO(RequestAuditLog log) {
        return new AdminActivityEventDTO(
                log.getUuid(),
                log.getCreatedDate(),
                deriveSummary(log),
                log.getHttpMethod(),
                log.getRequestUri(),
                log.getQueryString(),
                log.getResponseStatus(),
                log.getProcessingTimeMs(),
                log.getUserFullName(),
                log.getUserEmail(),
                log.getUserUuid(),
                log.getUserDomains(),
                log.getRequestId()
        );
    }

    private String deriveSummary(RequestAuditLog log) {
        String method = log.getHttpMethod() == null ? "" : log.getHttpMethod().toUpperCase();
        String uri = log.getRequestUri() == null ? "" : log.getRequestUri();

        if (uri.contains("/organizations") && uri.contains("/moderate")) {
            String action = extractQueryParam(log.getQueryString(), "action");
            return switch (action != null ? action.toLowerCase() : "moderate") {
                case "approve" -> "Approved organisation";
                case "reject" -> "Rejected organisation";
                case "revoke" -> "Revoked organisation verification";
                default -> "Moderated organisation";
            };
        }

        if (uri.contains("/users") && uri.endsWith("/domains") && "POST".equals(method)) {
            return "Assigned admin domain";
        }

        if (uri.contains("/users") && uri.contains("/domains/" ) && "DELETE".equals(method)) {
            return "Removed admin domain";
        }

        if (uri.contains("/dashboard/statistics")) {
            return "Viewed dashboard statistics";
        }

        if (uri.contains("/dashboard/activity-feed")) {
            return "Viewed dashboard activity";
        }

        if (uri.contains("/instructors") && uri.contains("/verify")) {
            return "Verified instructor";
        }

        if (uri.contains("/instructors") && uri.contains("/unverify")) {
            return "Unverified instructor";
        }

        return (method + " " + uri).trim();
    }

    private String extractQueryParam(String query, String key) {
        if (query == null || query.isBlank()) {
            return null;
        }
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf('=');
            if (idx < 0) {
                if (pair.equalsIgnoreCase(key)) {
                    return "";
                }
                continue;
            }
            String paramKey = pair.substring(0, idx);
            if (paramKey.equalsIgnoreCase(key)) {
                String value = pair.substring(idx + 1);
                return URLDecoder.decode(value, StandardCharsets.UTF_8);
            }
        }
        return null;
    }

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
