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
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import apps.sarafrika.elimika.instructor.spi.InstructorManagementService;
import apps.sarafrika.elimika.tenancy.dto.AdminActivityEventDTO;
import apps.sarafrika.elimika.tenancy.dto.AdminDashboardStatsDTO;
import apps.sarafrika.elimika.tenancy.dto.AdminDomainAssignmentRequestDTO;
import apps.sarafrika.elimika.tenancy.dto.AdminCreateUserRequestDTO;
import apps.sarafrika.elimika.tenancy.dto.OrganisationUserCreateRequestDTO;
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
import apps.sarafrika.elimika.authentication.spi.KeycloakUserService;
import apps.sarafrika.elimika.shared.event.user.UserCreationEvent;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import apps.sarafrika.elimika.shared.tracking.entity.RequestAuditLog;
import apps.sarafrika.elimika.shared.tracking.repository.RequestAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.Map;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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
    private final MeterRegistry meterRegistry;
    private final KeycloakUserService keycloakUserService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Value("${app.keycloak.realm}")
    private String keycloakRealm;

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
    @Transactional
    public UserDTO createAdminUser(AdminCreateUserRequestDTO request) {
        String normalizedEmail = normalizeEmail(request.email());
        log.info("Creating admin user for email {}", normalizedEmail);

        ensureEmailAvailable(normalizedEmail, "Admin");

        User user = createLocalUser(
                request.firstName(),
                request.middleName(),
                request.lastName(),
                normalizedEmail,
                request.phoneNumber()
        );

        publishKeycloakCreation(user, request.firstName(), request.lastName(), normalizedEmail, UserDomain.admin);
        assignAdminDomain(user.getUuid(), new AdminDomainAssignmentRequestDTO("admin", "Created via admin onboarding"));

        log.info("Successfully created admin user {} with UUID {}", normalizedEmail, user.getUuid());
        return userService.getUserByUuid(user.getUuid());
    }

    @Override
    @Transactional
    public UserDTO createOrganisationUser(UUID organisationUuid, OrganisationUserCreateRequestDTO request) {
        String normalizedEmail = normalizeEmail(request.email());
        log.info("Creating organisation user for email {} in organisation {}", normalizedEmail, organisationUuid);

        // Validate organisation exists
        organisationRepository.findByUuid(organisationUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Organisation not found with UUID: " + organisationUuid));

        // Validate domain exists
        UserDomain domain = findDomainByNameOrThrow(request.domainName());

        ensureEmailAvailable(normalizedEmail, "Organisation user");
        User user = createLocalUser(
                request.firstName(),
                request.middleName(),
                request.lastName(),
                normalizedEmail,
                request.phoneNumber()
        );

        publishKeycloakCreation(user, request.firstName(), request.lastName(), normalizedEmail, UserDomain.valueOf(domain.getDomainName()));

        // Assign organisation role (and optional branch)
        userService.assignUserToOrganisation(user.getUuid(), organisationUuid, domain.getDomainName(), request.branchUuid());

        log.info("Successfully created organisation user {} with UUID {} in organisation {}", normalizedEmail, user.getUuid(), organisationUuid);
        return userService.getUserByUuid(user.getUuid());
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            throw new IllegalArgumentException("Email is required");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void ensureEmailAvailable(String normalizedEmail, String context) {
        userRepository.findByEmail(normalizedEmail).ifPresent(user -> {
            throw new IllegalStateException("User already exists locally. Provide the existing email to " + context.toLowerCase() + ".");
        });

        if (keycloakUserService.getUserByUsername(normalizedEmail, keycloakRealm).isPresent()) {
            throw new IllegalStateException("User already exists in Keycloak. Provide the existing email to " + context.toLowerCase() + ".");
        }
    }

    private User createLocalUser(String firstName, String middleName, String lastName, String email, String phoneNumber) {
        User user = new User();
        user.setFirstName(firstName);
        user.setMiddleName(middleName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setUsername(email);
        user.setPhoneNumber(phoneNumber);
        user.setActive(true);
        return userRepository.save(user);
    }

    private void publishKeycloakCreation(User user, String firstName, String lastName, String email, UserDomain domain) {
        UserCreationEvent creationEvent = new UserCreationEvent(
                email,
                firstName,
                lastName,
                email,
                true,
                domain,
                keycloakRealm,
                user.getUuid()
        );
        applicationEventPublisher.publishEvent(creationEvent);
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

        AdminDashboardStatsDTO.SystemPerformance systemPerformance = buildSystemPerformance();

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
                systemPerformance,
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

    private AdminDashboardStatsDTO.SystemPerformance buildSystemPerformance() {
        String uptime = resolveUptime();
        String averageResponseTime = resolveAverageResponseTime();
        String errorRate = resolveErrorRate();
        String storageUsage = resolveStorageUsage();
        return new AdminDashboardStatsDTO.SystemPerformance(
                uptime,
                averageResponseTime,
                errorRate,
                storageUsage
        );
    }

    private String resolveUptime() {
        Double uptimeSeconds = getFirstGaugeValue("process.uptime");
        if (uptimeSeconds == null || uptimeSeconds < 0) {
            return "unknown";
        }
        Duration duration = Duration.ofMillis(Math.round(uptimeSeconds * 1000));
        long days = duration.toDays();
        long hours = duration.minusDays(days).toHours();
        long minutes = duration.minusDays(days).minusHours(hours).toMinutes();

        if (days > 0) {
            return String.format(Locale.ROOT, "%dd %dh %dm", days, hours, minutes);
        }
        if (hours > 0) {
            return String.format(Locale.ROOT, "%dh %dm", hours, minutes);
        }
        return String.format(Locale.ROOT, "%dm", minutes);
    }

    private String resolveStorageUsage() {
        Double total = getFirstGaugeValue("disk.total");
        Double free = getFirstGaugeValue("disk.free");

        if (total == null || free == null || total <= 0) {
            return "unknown";
        }
        double used = total - free;
        double percent = used <= 0 ? 0 : (used / total) * 100;
        return String.format(Locale.ROOT, "%.1f%%", percent);
    }

    private String resolveAverageResponseTime() {
        Collection<Timer> timers = meterRegistry.find("http.server.requests").timers();
        if (timers.isEmpty()) {
            return "unknown";
        }

        double totalTimeMs = 0;
        long count = 0;

        for (Timer timer : timers) {
            totalTimeMs += timer.totalTime(TimeUnit.MILLISECONDS);
            count += timer.count();
        }

        if (count == 0) {
            return "unknown";
        }

        double averageMs = totalTimeMs / count;
        if (averageMs >= 1000) {
            return String.format(Locale.ROOT, "%.2fs", averageMs / 1000);
        }
        return String.format(Locale.ROOT, "%.0fms", averageMs);
    }

    private String resolveErrorRate() {
        Collection<Timer> timers = meterRegistry.find("http.server.requests").timers();
        if (timers.isEmpty()) {
            return "unknown";
        }

        long totalCount = 0;
        long errorCount = 0;

        for (Timer timer : timers) {
            long count = timer.count();
            totalCount += count;
            String status = timer.getId().getTag("status");
            if (status != null && status.startsWith("5")) {
                errorCount += count;
            }
        }

        if (totalCount == 0) {
            return "unknown";
        }

        double rate = ((double) errorCount / totalCount) * 100;
        return String.format(Locale.ROOT, "%.2f%%", rate);
    }

    private Double getFirstGaugeValue(String metricName) {
        return meterRegistry.find(metricName)
                .gauges()
                .stream()
                .findFirst()
                .map(Gauge::value)
                .filter(value -> !Double.isNaN(value))
                .orElse(null);
    }
}
