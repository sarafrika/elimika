package apps.sarafrika.elimika.tenancy.services.impl;

import apps.sarafrika.elimika.common.event.user.*;
import apps.sarafrika.elimika.common.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.common.util.GenericSpecificationBuilder;
import apps.sarafrika.elimika.shared.storage.config.StorageProperties;
import apps.sarafrika.elimika.shared.storage.service.StorageService;
import apps.sarafrika.elimika.tenancy.dto.UserDTO;
import apps.sarafrika.elimika.tenancy.entity.*;
import apps.sarafrika.elimika.tenancy.enums.Gender;
import apps.sarafrika.elimika.tenancy.factory.UserFactory;
import apps.sarafrika.elimika.tenancy.internal.UserMediaValidationService;
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
    private final StorageProperties storageProperties;
    private final GenericSpecificationBuilder<User> specificationBuilder;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final UserMediaValidationService validationService;

    @Override
    @Transactional
    public void createUser(UserRepresentation userRep) {
        log.debug("Creating new user with email: {}", userRep.getEmail());
        
        Map<String, List<String>> attributes = userRep.getAttributes();
        if (attributes == null) {
            attributes = new HashMap<>();
        }
        
        String middleName = getAttributeValue(attributes, "middleName");
        String dobString = getAttributeValue(attributes, "dob");
        String phoneNumber = getAttributeValue(attributes, "primaryPhoneNumber");
        String gender = getAttributeValue(attributes, "gender");
        
        LocalDate dob = null;
        if (dobString != null && !dobString.isEmpty()) {
            try {
                dob = LocalDate.parse(dobString);
            } catch (Exception e) {
                log.warn("Failed to parse date of birth: {}", dobString, e);
            }
        }
        
        Gender genderEnum = Gender.fromString(gender);
        
        User user = new User();
        user.setFirstName(userRep.getFirstName());
        user.setMiddleName(middleName);
        user.setLastName(userRep.getLastName());
        user.setEmail(userRep.getEmail());
        user.setUsername(userRep.getUsername());
        user.setDob(dob);
        user.setPhoneNumber(phoneNumber);
        user.setActive(userRep.isEnabled());
        user.setKeycloakId(userRep.getId());
        user.setGender(genderEnum);
        
        log.info("User created: {}", user);
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserDTO> getAllUsers(Pageable pageable) {
        log.debug("Retrieving all users with pagination: {}", pageable);
        Page<User> users = userRepository.findAll(pageable);
        return users.map(user -> {
            List<String> userDomains = getUserDomainsFromMappings(user.getUuid());
            return UserFactory.toDTO(user, userDomains);
        });
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

        // Get users through the organisation domain mapping
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
        log.debug("Processing organization invitation for {} to organisation {} with domain {}",
                email, organisationUuid, domainName);

        // Check if user exists
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isEmpty()) {
            // User doesn't exist - they need to register on Keycloak first
            throw new IllegalStateException("User must register on the platform before accepting invitations. " +
                    "Please ask them to create an account first.");
        }

        User user = userOptional.get();
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

        // Check if user is already in this organization
        Optional<UserOrganisationDomainMapping> existingMapping =
                userOrganisationDomainMappingRepository.findActiveByUserAndOrganisation(userUuid, organisationUuid);

        if (existingMapping.isPresent()) {
            // Update existing mapping (change role or branch)
            UserOrganisationDomainMapping mapping = existingMapping.get();
            mapping.setDomainUuid(domain.getUuid());
            mapping.setBranchUuid(branchUuid);
            userOrganisationDomainMappingRepository.save(mapping);
            log.info("Updated existing organisation mapping for user {} in organisation {}", userUuid, organisationUuid);
        } else {
            // Create new organisation relationship
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
            log.info("Created new organisation mapping for user {} in organisation {} with domain {}",
                    userUuid, organisationUuid, domainName);
        }

        // For invitation-only roles (admin, organisation_user), add the domain to user's standalone domains
        if ("admin".equals(domainName) || "organisation_user".equals(domainName)) {
            addStandaloneDomainToUser(user, domain);
        }

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
        mapping.setDeleted(true);

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
        return !userOrganisationDomainMappingRepository
                .existsActiveByUserOrganisationAndDomain(userUuid, organisationUuid, domain.getUuid());
    }

    @Override
    @Transactional
    public UserDTO uploadProfileImage(UUID userUuid, MultipartFile profileImage) {
        log.debug("Uploading profile image for user: {}", userUuid);

        // Validate file
        validationService.validateProfileImage(profileImage);

        User user = findUserOrThrow(userUuid);

        try {
            user.setProfileImageUrl(profileImage != null ? storeProfileImage(profileImage) : null);
            User savedUser = userRepository.save(user);
            
            // Sync profile image URL to Keycloak
            publishUserUpdateEvent(savedUser);
            
            List<String> userDomains = getUserDomainsFromMappings(userUuid);
            return UserFactory.toDTO(savedUser, userDomains);
        } catch (Exception ex) {
            log.error("Failed to upload User's profile image for UUID: {}", userUuid, ex);
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

            // Delete standalone domain mappings
            List<UserDomainMapping> domainMappings = userDomainMappingRepository.findByUserUuid(uuid);
            userDomainMappingRepository.deleteAll(domainMappings);

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

    // ================================
    // PRIVATE HELPER METHODS
    // ================================

    /**
     * Gets all domains for a user from both standalone and organisation mappings
     */
    private List<String> getUserDomainsFromMappings(UUID userUuid) {
        Set<String> allDomains = new HashSet<>();

        // Get standalone domains (from user_domain_mapping)
        List<UserDomainMapping> standaloneMappings = userDomainMappingRepository.findByUserUuid(userUuid);
        for (UserDomainMapping mapping : standaloneMappings) {
            UserDomain domain = userDomainRepository.findByUuid(mapping.getUserDomainUuid())
                    .orElse(null);
            if (domain != null) {
                allDomains.add(domain.getDomainName());
            }
        }

        // Get domains from organisation mappings (for context)
        List<UserOrganisationDomainMapping> orgMappings =
                userOrganisationDomainMappingRepository.findActiveByUser(userUuid);
        for (UserOrganisationDomainMapping mapping : orgMappings) {
            UserDomain domain = userDomainRepository.findByUuid(mapping.getDomainUuid())
                    .orElse(null);
            if (domain != null) {
                allDomains.add(domain.getDomainName());
            }
        }

        return new ArrayList<>(allDomains);
    }

    /**
     * Updates user domains with validation for self-registration rules
     */
    private void updateUserDomains(User user, List<String> requestedDomains) {
        log.debug("Updating domains for user {} with domains: {}", user.getUuid(), requestedDomains);

        // Validate that user is not trying to self-assign restricted domains
        for (String domainName : requestedDomains) {
            if ("admin".equals(domainName) || "organisation_user".equals(domainName)) {
                throw new IllegalArgumentException("Domain '" + domainName +
                        "' can only be assigned through invitation. Students and Instructors can self-register.");
            }
        }

        // Get current standalone domains
        List<UserDomainMapping> currentMappings = userDomainMappingRepository.findByUserUuid(user.getUuid());
        Set<String> currentDomains = currentMappings.stream()
                .map(mapping -> {
                    UserDomain domain = userDomainRepository.findByUuid(mapping.getUserDomainUuid()).orElse(null);
                    return domain != null ? domain.getDomainName() : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (String domainName : requestedDomains) {
            if (!currentDomains.contains(domainName)) {
                UserDomain domain = findDomainByNameOrThrow(domainName);
                addStandaloneDomainToUser(user, domain);
            }
        }
    }

    /**
     * Adds a standalone domain to a user (not organisation-specific)
     */
    private void addStandaloneDomainToUser(User user, UserDomain domain) {
        // Check if mapping already exists
        if (!userDomainMappingRepository.existsByUserUuidAndUserDomainUuid(user.getUuid(), domain.getUuid())) {
            UserDomainMapping mapping = new UserDomainMapping(null, user.getUuid(), domain.getUuid(), null, null);
            userDomainMappingRepository.save(mapping);
            log.info("Added standalone domain {} to user {}", domain.getDomainName(), user.getUuid());
        }
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
        user.setDob(userDTO.dob());
        user.setUsername(userDTO.username());
    }

    private void publishUserUpdateEvent(User user) {
        applicationEventPublisher.publishEvent(
                UserUpdateEvent.createFull(
                        user.getUsername(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getEmail(),
                        user.getMiddleName(),
                        user.getPhoneNumber(),
                        user.getDob(),
                        user.getGender(),
                        user.getProfileImageUrl(),
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

    /**
     * Stores a profile image file and returns the full URL
     * Uses simple UUID-based filenames for cleaner URLs
     */
    private String storeProfileImage(MultipartFile file) {
        try {
            String profileImageFolder = storageProperties.getFolders().getProfileImages();

            String storedPath = storageService.store(file, profileImageFolder);

            String fileName = storedPath.substring(storedPath.lastIndexOf('/') + 1);

            String imageUrl;

            if (storageProperties.getBaseUrl() != null && !storageProperties.getBaseUrl().isEmpty()) {
                imageUrl = storageProperties.getBaseUrl() + "/api/v1/users/profile-image/" + fileName;
            } else {
                imageUrl = ServletUriComponentsBuilder
                        .fromCurrentContextPath()
                        .scheme("https")
                        .path("/api/v1/users/profile-image/")
                        .path(fileName)
                        .build()
                        .toUriString();
            }

            log.debug("Generated profile image URL: {}", imageUrl);
            return imageUrl;

        } catch (Exception e) {
            log.error("Failed to store profile image", e);
            throw new RuntimeException("Failed to store profile image: " + e.getMessage(), e);
        }
    }


    @EventListener
    @Transactional
    void assignUserTheirDomain(UserDomainMappingEvent event) {

        UUID domainUuid = userDomainRepository.findByDomainName(event.userDomain())
                .orElseThrow(() -> new IllegalArgumentException("No known domain with the provided name"))
                .getUuid();

        if (!userDomainMappingRepository.existsByUserUuidAndUserDomainUuid(event.userUuid(), domainUuid)) {
            UserDomainMapping userDomainMapping = new UserDomainMapping(null, event.userUuid(), domainUuid, null, null);
            userDomainMappingRepository.save(userDomainMapping);
        }
    }

    private String getAttributeValue(Map<String, List<String>> attributes, String key) {
        List<String> values = attributes.get(key);
        return (values != null && !values.isEmpty()) ? values.get(0) : null;
    }
}