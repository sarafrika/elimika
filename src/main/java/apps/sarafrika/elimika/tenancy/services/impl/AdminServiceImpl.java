package apps.sarafrika.elimika.tenancy.services.impl;

import apps.sarafrika.elimika.common.enums.UserDomain;
import apps.sarafrika.elimika.common.event.admin.RegisterAdmin;
import apps.sarafrika.elimika.common.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.tenancy.dto.UserDTO;
import apps.sarafrika.elimika.tenancy.entity.User;
import apps.sarafrika.elimika.tenancy.entity.UserDomainMapping;
import apps.sarafrika.elimika.tenancy.factory.UserFactory;
import apps.sarafrika.elimika.tenancy.repository.UserDomainMappingRepository;
import apps.sarafrika.elimika.tenancy.repository.UserDomainRepository;
import apps.sarafrika.elimika.tenancy.repository.UserOrganisationDomainMappingRepository;
import apps.sarafrika.elimika.tenancy.repository.UserRepository;
import apps.sarafrika.elimika.tenancy.services.AdminService;
import apps.sarafrika.elimika.tenancy.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of AdminService for managing system administrators.
 * 
 * This service handles the registration, management, and privilege control
 * for system administrators who have platform-wide access.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements AdminService {
    
    private final UserRepository userRepository;
    private final UserDomainRepository userDomainRepository;
    private final UserDomainMappingRepository userDomainMappingRepository;
    private final UserOrganisationDomainMappingRepository userOrganisationDomainMappingRepository;
    private final UserService userService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional
    public UserDTO registerAdmin(UUID userUuid, String fullName) {
        log.debug("Registering user {} as system administrator", userUuid);
        
        // Verify user exists
        User user = userRepository.findByUuid(userUuid)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with UUID: " + userUuid));
        
        // Check if user is already an admin
        if (isSystemAdmin(userUuid)) {
            throw new IllegalStateException("User is already a system administrator");
        }
        
        try {
            // Publish admin registration event
            applicationEventPublisher.publishEvent(
                new RegisterAdmin(fullName, userUuid)
            );
            
            log.info("Successfully registered user {} ({}) as system administrator", 
                    fullName, userUuid);
            
            // Return updated user DTO
            return userService.getUserByUuid(userUuid);
            
        } catch (Exception e) {
            log.error("Failed to register admin for user: {} ({})", fullName, userUuid, e);
            throw new RuntimeException("Failed to register admin: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserDTO> getAllAdmins(Pageable pageable) {
        log.debug("Retrieving all system administrators with pagination: {}", pageable);
        
        // Get admin domain UUID
        UUID adminDomainUuid = userDomainRepository.findByDomainName(UserDomain.admin.name())
                .orElseThrow(() -> new IllegalStateException("Admin domain not found"))
                .getUuid();
        
        // Get all admin mappings
        List<UserDomainMapping> adminMappings = userDomainMappingRepository.findByUserDomainUuid(adminDomainUuid);
        
        List<UUID> adminUserUuids = adminMappings.stream()
                .map(UserDomainMapping::getUserUuid)
                .collect(Collectors.toList());
        
        if (adminUserUuids.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }
        
        // Get admin users with pagination
        Page<User> adminUsers = userRepository.findByUuidIn(adminUserUuids, pageable);
        
        return adminUsers.map(user -> {
            List<String> userDomains = getUserDomainsForUser(user.getUuid());
            return UserFactory.toDTO(user, userDomains);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSystemAdmin(UUID userUuid) {
        UUID adminDomainUuid = userDomainRepository.findByDomainName(UserDomain.admin.name())
                .map(domain -> domain.getUuid())
                .orElse(null);
        
        if (adminDomainUuid == null) {
            return false;
        }
        
        return userDomainMappingRepository.existsByUserUuidAndUserDomainUuid(userUuid, adminDomainUuid);
    }

    @Override
    @Transactional
    public UserDTO revokeAdminPrivileges(UUID userUuid) {
        log.debug("Revoking admin privileges for user {}", userUuid);
        
        if (!isSystemAdmin(userUuid)) {
            throw new IllegalArgumentException("User is not a system administrator");
        }
        
        try {
            UUID adminDomainUuid = userDomainRepository.findByDomainName(UserDomain.admin.name())
                    .orElseThrow(() -> new IllegalStateException("Admin domain not found"))
                    .getUuid();
            
            // Remove admin domain mapping
            userDomainMappingRepository.deleteByUserUuidAndUserDomainUuid(userUuid, adminDomainUuid);
            
            log.info("Successfully revoked admin privileges for user {}", userUuid);
            
            return userService.getUserByUuid(userUuid);
            
        } catch (Exception e) {
            log.error("Failed to revoke admin privileges for user {}", userUuid, e);
            throw new RuntimeException("Failed to revoke admin privileges: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getSystemStatistics() {
        log.debug("Generating system-wide statistics");
        
        Map<String, Object> stats = new HashMap<>();
        
        // Total users
        long totalUsers = userRepository.count();
        stats.put("totalUsers", totalUsers);
        
        // Users by domain
        for (UserDomain domain : UserDomain.values()) {
            UUID domainUuid = userDomainRepository.findByDomainName(domain.name())
                    .map(d -> d.getUuid())
                    .orElse(null);
            
            if (domainUuid != null) {
                long domainUserCount = userDomainMappingRepository.countByUserDomainUuid(domainUuid);
                stats.put(domain.name() + "Count", domainUserCount);
            }
        }
        
        // Active organisation mappings
        long activeOrgMappings = userOrganisationDomainMappingRepository.countByActiveTrueAndDeletedFalse();
        stats.put("activeOrganisationMappings", activeOrgMappings);
        
        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserDTO> getAllUsersSystemWide(Pageable pageable) {
        log.debug("Retrieving all users system-wide with pagination: {}", pageable);
        
        Page<User> users = userRepository.findAll(pageable);
        
        return users.map(user -> {
            List<String> userDomains = getUserDomainsForUser(user.getUuid());
            return UserFactory.toDTO(user, userDomains);
        });
    }
    
    /**
     * Helper method to get user domains for a specific user.
     */
    private List<String> getUserDomainsForUser(UUID userUuid) {
        List<UserDomainMapping> mappings = userDomainMappingRepository.findByUserUuid(userUuid);
        
        return mappings.stream()
                .map(mapping -> userDomainRepository.findByUuid(mapping.getUserDomainUuid()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(domain -> domain.getDomainName())
                .collect(Collectors.toList());
    }
}