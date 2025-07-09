package apps.sarafrika.elimika.tenancy.services.impl;

import apps.sarafrika.elimika.common.event.admin.RegisterAdmin;
import apps.sarafrika.elimika.common.event.instructor.RegisterInstructor;
import apps.sarafrika.elimika.common.event.student.RegisterStudent;
import apps.sarafrika.elimika.common.event.user.AddUserToOrganisationEvent;
import apps.sarafrika.elimika.common.event.user.SuccessfulUserCreation;
import apps.sarafrika.elimika.common.event.user.SuccessfulUserUpdateEvent;
import apps.sarafrika.elimika.common.event.user.UserUpdateEvent;
import apps.sarafrika.elimika.common.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.common.util.GenericSpecificationBuilder;
import apps.sarafrika.elimika.shared.storage.service.StorageService;
import apps.sarafrika.elimika.tenancy.dto.UserDTO;
import apps.sarafrika.elimika.tenancy.entity.*;
import apps.sarafrika.elimika.tenancy.enums.Gender;
import apps.sarafrika.elimika.tenancy.factory.UserFactory;
import apps.sarafrika.elimika.tenancy.repository.*;
import apps.sarafrika.elimika.tenancy.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    @Value("${app.keycloak.realm}")
    private String realm;

    private final UserRepository userRepository;
    private final OrganisationRepository organisationRepository;
    private final UserDomainRepository userDomainRepository;
    private final UserDomainMappingRepository userDomainMappingRepository;
    private final UserOrganisationDomainMappingRepository userOrganisationDomainMappingRepository;
    private final TrainingBranchRepository trainingBranchRepository;

    private final StorageService storageService;
    private final GenericSpecificationBuilder<User> specificationBuilder;
    private final ApplicationEventPublisher applicationEventPublisher;

    public static final String PROFILE_IMAGE_FOLDER = "profile_images";

    @Override
    @Transactional
    public void createUser(UserRepresentation userRep) {
        log.debug("Creating new user with email: {}", userRep.getEmail());
        User user = new User(userRep.getFirstName(), null, userRep.getLastName(),
                userRep.getEmail(), userRep.getUsername(), null, null, null,
                userRep.isEnabled(), userRep.getId(), Gender.PREFER_NOT_TO_SAY
        );
        log.info("User {}", user);
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO getUserByUuid(UUID uuid) {
        User user = findUserOrThrow(uuid);
        List<String> userDomains = getUserDomainsFromMappings(uuid);
        return UserFactory.toDTO(user, userDomains);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserDTO> getUsersByOrganisation(UUID organisationId, Pageable pageable) {
        Organisation organisation = findOrganisationOrThrow(organisationId);

        // Get users through the new organisation domain mapping
        List<UserOrganisationDomainMapping> mappings = userOrganisationDomainMappingRepository
                .findActiveByOrganisation(organisation.getUuid());

        Set<UUID> userUuids = mappings.stream()
                .map(UserOrganisationDomainMapping::getUserUuid)
                .collect(Collectors.toSet());

        Page<User> users = userRepository.findByUuidIn(userUuids, pageable);

        return users.map(user -> {
            List<String> userDomains = getUserDomainsFromMappings(user.getUuid());
            return UserFactory.toDTO(user, userDomains);
        });
    }

    @Override
    @Transactional
    public UserDTO updateUser(UUID uuid, UserDTO userDTO) {
        log.debug("Updating user with UUID: {}", uuid);

        User user = findUserOrThrow(uuid);

        try {
            updateUserFields(user, userDTO);
            User updatedUser = userRepository.save(user);

            // Update user domains if provided
            if (userDTO.userDomain() != null) {
                userDTO.userDomain().forEach(domain -> publishUserDomainUpdateEvent(updatedUser, domain));
            }

            publishUserUpdateEvent(updatedUser);

            return UserFactory.toDTO(updatedUser, getUserDomainsFromMappings(uuid));
        } catch (Exception e) {
            log.error("Failed to update user with UUID: {}", uuid, e);
            throw new RuntimeException("Failed to update user: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public UserDTO inviteUserToOrganisation(String email, UUID organisationUuid, String domainName, UUID branchUuid) {
        log.debug("Inviting user with email {} to organisation {} with domain {}", email, organisationUuid, domainName);

        // Find or create user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        return assignUserToOrganisation(user.getUuid(), organisationUuid, domainName, branchUuid);
    }

    @Override
    @Transactional
    public UserDTO assignUserToOrganisation(UUID userUuid, UUID organisationUuid, String domainName, UUID branchUuid) {
        log.debug("Assigning user {} to organisation {} with domain {}", userUuid, organisationUuid, domainName);

        User user = findUserOrThrow(userUuid);
        Organisation organisation = findOrganisationOrThrow(organisationUuid);
        UserDomain domain = findDomainByNameOrThrow(domainName);

        // Validate branch belongs to organisation if provided
        if (branchUuid != null) {
            TrainingBranch branch = trainingBranchRepository.findByUuid(branchUuid)
                    .orElseThrow(() -> new ResourceNotFoundException("Training branch not found"));

            if (!branch.getOrganisationUuid().equals(organisationUuid)) {
                throw new IllegalArgumentException("Training branch does not belong to the specified organisation");
            }
        }

        // Check if active mapping already exists
        Optional<UserOrganisationDomainMapping> existingMapping =
                userOrganisationDomainMappingRepository.findActiveByUserAndOrganisation(userUuid, organisationUuid);

        if (existingMapping.isPresent()) {
            // Update existing mapping
            UserOrganisationDomainMapping mapping = existingMapping.get();
            mapping.setDomainUuid(domain.getUuid());
            mapping.setBranchUuid(branchUuid);
            userOrganisationDomainMappingRepository.save(mapping);
            log.info("Updated existing mapping for user {} in organisation {}", userUuid, organisationUuid);
        } else {
            // Create new mapping
            UserOrganisationDomainMapping mapping = UserOrganisationDomainMapping.builder()
                    .userUuid(userUuid)
                    .organisationUuid(organisationUuid)
                    .domainUuid(domain.getUuid())
                    .branchUuid(branchUuid)
                    .active(true)
                    .startDate(LocalDate.now())
                    .createdBy("system") // TODO: Replace with actual user context
                    .build();

            userOrganisationDomainMappingRepository.save(mapping);
            log.info("Created new mapping for user {} in organisation {} with domain {}", userUuid, organisationUuid, domainName);
        }

        // Publish domain-specific events
        publishUserDomainEvent(user, domainName);

        // Publish organisation assignment event
        if (user.getKeycloakId() != null && organisation.getKeycloakId() != null) {
            publishAddUserToOrganisationEvent(user.getKeycloakId(), organisation.getKeycloakId());
        }

        List<String> userDomains = getUserDomainsFromMappings(userUuid);
        return UserFactory.toDTO(user, userDomains);
    }

    @Override
    @Transactional
    public void removeUserFromOrganisation(UUID userUuid, UUID organisationUuid) {
        log.debug("Removing user {} from organisation {}", userUuid, organisationUuid);

        UserOrganisationDomainMapping mapping = userOrganisationDomainMappingRepository
                .findActiveByUserAndOrganisation(userUuid, organisationUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No active mapping found for user and organisation"));

        // Soft delete - set end date and deactivate
        mapping.setActive(false);
        mapping.setEndDate(LocalDate.now());

        userOrganisationDomainMappingRepository.save(mapping);
        log.info("Removed user {} from organisation {}", userUuid, organisationUuid);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> getUsersByOrganisationAndDomain(UUID organisationUuid, String domainName) {
        UserDomain domain = findDomainByNameOrThrow(domainName);

        List<UserOrganisationDomainMapping> mappings =
                userOrganisationDomainMappingRepository.findActiveByOrganisationAndDomain(organisationUuid, domain.getUuid());

        return mappings.stream()
                .map(mapping -> {
                    User user = findUserOrThrow(mapping.getUserUuid());
                    List<String> userDomains = getUserDomainsFromMappings(user.getUuid());
                    return UserFactory.toDTO(user, userDomains);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> getUsersByBranch(UUID branchUuid) {
        List<UserOrganisationDomainMapping> mappings =
                userOrganisationDomainMappingRepository.findActiveByBranch(branchUuid);

        return mappings.stream()
                .map(mapping -> {
                    User user = findUserOrThrow(mapping.getUserUuid());
                    List<String> userDomains = getUserDomainsFromMappings(user.getUuid());
                    return UserFactory.toDTO(user, userDomains);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasUserRoleInOrganisation(UUID userUuid, UUID organisationUuid, String domainName) {
        UserDomain domain = findDomainByNameOrThrow(domainName);
        return userOrganisationDomainMappingRepository
                .existsActiveByUserOrganisationAndDomain(userUuid, organisationUuid, domain.getUuid());
    }

    @Override
    public UserDTO uploadProfileImage(UUID userUuid, MultipartFile profileImage) {
        User user = findUserOrThrow(userUuid);

        try {
            user.setProfileImageUrl(profileImage != null ? storeProfileImage(profileImage) : null);
            List<String> userDomains = getUserDomainsFromMappings(userUuid);
            return UserFactory.toDTO(userRepository.save(user), userDomains);
        } catch (Exception ex) {
            log.error("Failed to upload User's Image ", ex);
            throw new RuntimeException("Failed to upload user's profile image: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional
    public void deleteUser(UUID uuid) {
        log.debug("Deleting user with UUID: {}", uuid);
        try {
            User user = findUserOrThrow(uuid);

            // Soft delete all organisation mappings
            List<UserOrganisationDomainMapping> mappings =
                    userOrganisationDomainMappingRepository.findActiveByUser(uuid);
            mappings.forEach(mapping -> {
                mapping.setActive(false);
                mapping.setEndDate(LocalDate.now());
                mapping.setDeleted(true);
            });
            userOrganisationDomainMappingRepository.saveAll(mappings);

            // Delete user
            userRepository.delete(user);
            log.info("Successfully deleted user with UUID: {}", uuid);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to delete user with UUID: {}", uuid, e);
            throw new RuntimeException("Failed to delete user: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserDTO> search(Map<String, String> searchParams, Pageable pageable) {
        Specification<User> spec = specificationBuilder.buildSpecification(User.class, searchParams);
        Page<User> users = userRepository.findAll(spec, pageable);
        return users.map(u -> {
            List<String> userDomains = getUserDomainsFromMappings(u.getUuid());
            return UserFactory.toDTO(u, userDomains);
        });
    }

    @Override
    public List<String> getUserDomains(UUID userUuid) {
        return getUserDomainsFromMappings(userUuid);
    }

    // Enhanced method to get user domains from new mapping table
    private List<String> getUserDomainsFromMappings(UUID userUuid) {
        List<UserOrganisationDomainMapping> mappings =
                userOrganisationDomainMappingRepository.findActiveByUser(userUuid);

        return mappings.stream()
                .map(mapping -> {
                    UserDomain domain = userDomainRepository.findByUuid(mapping.getDomainUuid())
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "User domain not found for UUID: " + mapping.getDomainUuid()));
                    return domain.getDomainName();
                })
                .distinct()
                .collect(Collectors.toList());
    }

    // Event Listeners
    @EventListener
    void onUserCreated(SuccessfulUserCreation event) {
        log.debug("Processing successful user creation event for UUID: {}", event.userId());
        try {
            User user = findUserOrThrow(event.userId());
            user.setKeycloakId(event.keycloakId());
            userRepository.save(user);
            log.info("Successfully processed user creation event for UUID: {}", event.userId());
        } catch (Exception e) {
            log.error("Failed to process user creation event for UUID: {}", event.userId(), e);
            throw new RuntimeException("Failed to process user creation event: " + e.getMessage(), e);
        }
    }

    @EventListener
    void onUserUpdated(SuccessfulUserUpdateEvent event) {
        log.debug("Processing successful user update event for UUID: {}", event.userId());
        try {
            User user = findUserOrThrow(event.userId());
            log.info("Successfully processed user update event for UUID: {}", event.userId());
        } catch (Exception e) {
            log.error("Failed to process user update event for UUID: {}", event.userId(), e);
            throw new RuntimeException("Failed to process user update event: " + e.getMessage(), e);
        }
    }

    // Private Helper Methods
    private Organisation findOrganisationOrThrow(UUID orgUuid) {
        return organisationRepository.findByUuid(orgUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Organisation not found with UUID: " + orgUuid));
    }

    private User findUserOrThrow(UUID uuid) {
        return userRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for UUID: " + uuid));
    }

    private UserDomain findDomainByNameOrThrow(String domainName) {
        return userDomainRepository.findByDomainName(domainName)
                .orElseThrow(() -> new IllegalArgumentException("No known domain with the provided name: " + domainName));
    }

    private void updateUserFields(User user, UserDTO userDTO) {
        user.setFirstName(userDTO.firstName());
        user.setMiddleName(userDTO.middleName());
        user.setLastName(userDTO.lastName());
        user.setEmail(userDTO.email());
        user.setPhoneNumber(userDTO.phoneNumber());
        user.setActive(userDTO.active());
        user.setGender(userDTO.gender());
    }

    @Transactional
    public void publishUserDomainUpdateEvent(User user, String userDomain) {
        UUID domainUuid = userDomainRepository.findByDomainName(userDomain)
                .orElseThrow(() -> new IllegalArgumentException("No known domain with the provided name"))
                .getUuid();

        // Keep backward compatibility with old table for now
        if (!userDomainMappingRepository.existsByUserUuidAndUserDomainUuid(user.getUuid(), domainUuid)) {
            UserDomainMapping userDomainMapping = new UserDomainMapping(null, user.getUuid(), domainUuid, null, null);
            userDomainMappingRepository.save(userDomainMapping);
        }

        publishUserDomainEvent(user, userDomain);
    }

    private void publishUserDomainEvent(User user, String userDomain) {
        String fullName = new StringBuilder().append(user.getFirstName()).append(" ")
                .append(user.getMiddleName() != null ? user.getMiddleName() + " " : "")
                .append(user.getLastName()).toString().toUpperCase();

        switch (userDomain) {
            case "instructor" -> {
                applicationEventPublisher.publishEvent(new RegisterInstructor(fullName, user.getUuid()));
            }
            case "admin" -> {
                applicationEventPublisher.publishEvent(new RegisterAdmin(fullName, user.getUuid()));
            }
            case "organisation_user" -> {
                // Handle organisation user specific logic
            }
            default -> {
                applicationEventPublisher.publishEvent(new RegisterStudent(fullName, user.getUuid()));
            }
        }
    }

    private void publishUserUpdateEvent(User user) {
        applicationEventPublisher.publishEvent(
                new UserUpdateEvent(
                        user.getEmail(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getEmail(),
                        user.isActive(),
                        realm,
                        user.getUuid(),
                        user.getKeycloakId()
                )
        );
    }

    private void publishAddUserToOrganisationEvent(String userKeycloakId, String orgKeycloakId) {
        applicationEventPublisher.publishEvent(
                new AddUserToOrganisationEvent(
                        userKeycloakId,
                        orgKeycloakId,
                        realm
                )
        );
    }

    private String storeProfileImage(MultipartFile file) {
        String fileName = storageService.store(file);
        return ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/api/v1/users/profile-image/")
                .path(fileName)
                .build()
                .toUriString();
    }
}