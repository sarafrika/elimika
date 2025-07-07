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
import apps.sarafrika.elimika.tenancy.entity.Organisation;
import apps.sarafrika.elimika.tenancy.entity.User;
import apps.sarafrika.elimika.tenancy.entity.UserDomainMapping;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    @Value("${app.keycloak.realm}")
    private String realm;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OrganisationRepository organisationRepository;
    private final UserDomainRepository userDomainRepository;
    private final UserDomainMappingRepository userDomainMappingRepository;

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
                userRep.isEnabled(), userRep.getId(),null,Gender.PREFER_NOT_TO_SAY
        );
        log.info("User {}", user);
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO getUserByUuid(UUID uuid) {
        List<String> userDomains =  getUserDomains(uuid);
        return userRepository.findByUuid(uuid)
                .map(u -> UserFactory.toDTO(u, userDomains))
                .orElseThrow(() -> {
                    log.warn("User not found for UUID: {}", uuid);
                    return new ResourceNotFoundException("User not found for UUID: " + uuid);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserDTO> getUsersByOrganisation(UUID organisationId, Pageable pageable) {
        Organisation organisation = findOrganisationOrThrow(organisationId);

        List<String> userDomains =  getUserDomains(organisationId);

        return userRepository.findByOrganisationUuid(organisation.getUuid(), pageable)
                .map(u -> UserFactory.toDTO(u, userDomains));
    }


    @Override
    @Transactional
    public UserDTO updateUser(UUID uuid, UserDTO userDTO) {
        log.debug("Updating user with UUID: {}", uuid);

        Organisation organisation = null;

        if (userDTO.organisationUuid() != null) {
            organisation = findOrganisationOrThrow(userDTO.organisationUuid());
        }

        User user = findUserOrThrow(uuid);

        try {
            updateUserFields(user, userDTO, organisation);
            User updatedUser = userRepository.save(user);

            userDTO.userDomain().forEach(domain -> publishUserDomainUpdateEvent(updatedUser, domain));
            publishUserUpdateEvent(updatedUser);

            return UserFactory.toDTO(updatedUser, getUserDomains(uuid));
        } catch (Exception e) {
            log.error("Failed to update user with UUID: {}", uuid, e);
            throw new RuntimeException("Failed to update user: " + e.getMessage(), e);
        }
    }

    @Override
    public UserDTO uploadProfileImage(UUID userUuid, MultipartFile profileImage) {
        User user = findUserOrThrow(userUuid);

        try {
            user.setProfileImageUrl(profileImage != null ? storeProfileImage(profileImage) : null);

            List<String> userDomains =  getUserDomains(userUuid);

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
            List<String> userDomains = getUserDomains(u.getUuid());
            return UserFactory.toDTO(u, userDomains);
        });
    }

    @Override
    public List<String> getUserDomains(UUID userUuid) {
        List<UserDomainMapping> userDomainMappings = userDomainMappingRepository.findByUserUuid(userUuid);
        List<String> userDomains = new ArrayList<>();
        for (UserDomainMapping userDomainMapping : userDomainMappings) {
            apps.sarafrika.elimika.tenancy.entity.UserDomain userDomain = userDomainRepository
                    .findByUuid(userDomainMapping.getUserDomainUuid())
                    .orElseThrow(() -> new ResourceNotFoundException("User domain not found for UUID: " + userDomainMapping.getUserDomainUuid()));
            userDomains.add(userDomain.getDomainName());
        }
        return userDomains;
    }

    @EventListener
    void onUserCreated(SuccessfulUserCreation event) {
        log.debug("Processing successful user creation event for UUID: {}", event.userId());
        try {
            User user = findUserOrThrow(event.userId());
            user.setKeycloakId(event.keycloakId());
            userRepository.save(user);

            Organisation organisation = null;

            if (user.getOrganisationUuid() != null) {
                organisation = findOrganisationOrThrow(user.getOrganisationUuid());
            }

            publishAddUserToOrganisationEvent(event.keycloakId(), organisation.getKeycloakId());

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
            throw new RuntimeException("Failed to process user creation event: " + e.getMessage(), e);
        }
    }

    private Organisation findOrganisationOrThrow(UUID orgUuid) {
        return organisationRepository.findByUuid(orgUuid)
                .orElseThrow(() -> new IllegalArgumentException("Organisation not found with UUID: " + orgUuid));
    }

    private User findUserOrThrow(UUID uuid) {
        return userRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for UUID: " + uuid));
    }

    private void updateUserFields(User user, UserDTO userDTO, Organisation organisation) {
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

        if (!userDomainMappingRepository.existsByUserUuidAndUserDomainUuid(user.getUuid(), domainUuid)) {
            UserDomainMapping userDomainMapping = new UserDomainMapping(null, user.getUuid(), domainUuid, null, null);
            userDomainMappingRepository.save(userDomainMapping);
        }

        String fullName = new StringBuilder().append(user.getFirstName()).append(" ")
                .append(user.getMiddleName() != null ? user.getMiddleName() + " " : ""
                ).append(user.getLastName()).toString().toUpperCase();

        switch (userDomain) {
            case "instructor" -> {
                applicationEventPublisher.publishEvent(new RegisterInstructor(fullName, user.getUuid()));
            }
            case "admin" -> {
                applicationEventPublisher.publishEvent(new RegisterAdmin(fullName, user.getUuid()));
            }
            case "organisation_user" -> {
                //
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
        // Build the URL to access the profile image through your endpoint
        return ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/api/v1/users/profile-image/")
                .path(fileName)
                .build()
                .toUriString();

    }
}