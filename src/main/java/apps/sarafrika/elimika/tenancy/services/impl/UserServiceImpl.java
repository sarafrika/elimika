package apps.sarafrika.elimika.tenancy.services.impl;

import apps.sarafrika.elimika.common.enums.UserDomain;
import apps.sarafrika.elimika.common.event.admin.RegisterAdmin;
import apps.sarafrika.elimika.common.event.instructor.RegisterInstructor;
import apps.sarafrika.elimika.common.event.role.AssignRoleToUserEvent;
import apps.sarafrika.elimika.common.event.student.RegisterStudent;
import apps.sarafrika.elimika.common.event.user.*;
import apps.sarafrika.elimika.common.exceptions.RecordNotFoundException;
import apps.sarafrika.elimika.common.model.BaseEntity;
import apps.sarafrika.elimika.common.util.GenericSpecificationBuilder;
import apps.sarafrika.elimika.common.util.RoleNameConverter;
import apps.sarafrika.elimika.shared.storage.service.StorageService;
import apps.sarafrika.elimika.tenancy.dto.RoleDTO;
import apps.sarafrika.elimika.tenancy.dto.UserDTO;
import apps.sarafrika.elimika.tenancy.entity.Organisation;
import apps.sarafrika.elimika.tenancy.entity.Role;
import apps.sarafrika.elimika.tenancy.entity.User;
import apps.sarafrika.elimika.tenancy.factory.UserFactory;
import apps.sarafrika.elimika.tenancy.repository.OrganisationRepository;
import apps.sarafrika.elimika.tenancy.repository.RoleRepository;
import apps.sarafrika.elimika.tenancy.repository.UserRepository;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    @Value("${app.keycloak.realm}")
    private String realm;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OrganisationRepository organisationRepository;

    private final StorageService storageService;

    private final GenericSpecificationBuilder<User> specificationBuilder;
    private final ApplicationEventPublisher applicationEventPublisher;

    public static final String PROFILE_IMAGE_FOLDER = "profile_images";

    @Override
    @Transactional
    public void createUser(UserRepresentation userRep) {
        log.debug("Creating new user with email: {}", userRep.getEmail());

        userRepository.save(new User(userRep.getFirstName(), null, userRep.getLastName(),
                userRep.getEmail(), userRep.getUsername(), null, null, null,
                userRep.isEnabled(), userRep.getId(), null, null, null, null
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO getUserByUuid(UUID uuid) {
        log.debug("Fetching user by UUID: {}", uuid);
        return userRepository.findByUuid(uuid)
                .map(UserFactory::toDTO)
                .orElseThrow(() -> {
                    log.warn("User not found for UUID: {}", uuid);
                    return new RecordNotFoundException("User not found for UUID: " + uuid);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserDTO> getUsersByOrganisation(UUID organisationId, Pageable pageable) {
        log.debug("Fetching users for organisation ID: {}", organisationId);

        Organisation organisation = findOrganisationOrThrow(organisationId);

        return userRepository.findByOrganisationId(organisation.getId(), pageable)
                .map(UserFactory::toDTO);
    }


    @Override
    @Transactional
    public UserDTO updateUser(UUID uuid, UserDTO userDTO, MultipartFile profileImage) {
        log.debug("Updating user with UUID: {}", uuid);

        Organisation organisation = findOrganisationOrThrow(userDTO.organisationUuid());
        User user = findUserOrThrow(uuid);

        try {
            updateUserFields(user, userDTO, organisation, profileImage);
            User updatedUser = userRepository.save(user);
            userDTO.userDomain().forEach(domain -> publishUserDomainUpdateEvent(updatedUser, domain));
            publishUserUpdateEvent(updatedUser);

            log.info("Successfully updated user with UUID: {}", uuid);
            return UserFactory.toDTO(updatedUser);
        } catch (Exception e) {
            log.error("Failed to update user with UUID: {}", uuid, e);
            throw new RuntimeException("Failed to update user: " + e.getMessage(), e);
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
        } catch (RecordNotFoundException e) {
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
        Page<User> organisations = userRepository.findAll(spec, pageable);
        return organisations.map(UserFactory::toDTO);
    }

    @EventListener
    void onUserCreated(SuccessfulUserCreation event) {
        log.debug("Processing successful user creation event for UUID: {}", event.userId());
        try {
            User user = findUserOrThrow(event.userId());
            user.setKeycloakId(event.keycloakId());
            userRepository.save(user);

            Organisation organisation = findOrganisationOrThrow(user.getOrganisation().getUuid());

            publishAddUserToOrganisationEvent(event.keycloakId(), organisation.getKeycloakId());

            List<Role> persistedRoles = roleRepository.findAllByUuidIn(
                    user.getRoles().stream()
                            .map(BaseEntity::getUuid)
                            .collect(Collectors.toList())
            );

            persistedRoles.stream().flatMap(role -> role.getPermissions().stream()).forEach(permission -> {
                applicationEventPublisher.publishEvent(new AssignRoleToUserEvent(UUID.fromString(user.getKeycloakId()), RoleNameConverter.createRoleName(permission), realm));
            });

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

            List<Role> persistedRoles = roleRepository.findAllByUuidIn(
                    user.getRoles().stream()
                            .map(BaseEntity::getUuid)
                            .collect(Collectors.toList())
            );

            persistedRoles.stream().flatMap(role -> role.getPermissions().stream()).forEach(permission -> {
                applicationEventPublisher.publishEvent(new AssignRoleToUserEvent(UUID.fromString(user.getKeycloakId()), RoleNameConverter.createRoleName(permission), realm));
            });

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
                .orElseThrow(() -> new RecordNotFoundException("User not found for UUID: " + uuid));
    }

    private void updateUserFields(User user, UserDTO userDTO, Organisation organisation, MultipartFile profileImage) {
        user.setProfileImageUrl(profileImage != null ? storeProfileImage(profileImage) : null);
        user.setFirstName(userDTO.firstName());
        user.setMiddleName(userDTO.middleName());
        user.setLastName(userDTO.lastName());
        user.setEmail(userDTO.email());
        user.setPhoneNumber(userDTO.phoneNumber());
        user.setActive(userDTO.active());
        user.setOrganisation(organisation);
        user.setGender(userDTO.gender().toString().toUpperCase());

        if (userDTO.roles() != null) {
            List<Role> persistedRoles = roleRepository.findAllByUuidIn(
                    userDTO.roles().stream()
                            .map(RoleDTO::uuid)
                            .toList()
            );
            user.setRoles(persistedRoles);

            persistedRoles.stream().flatMap(role -> role.getPermissions().stream()).forEach(
                    permission -> {
                applicationEventPublisher.publishEvent(new AssignRoleToUserEvent(UUID.fromString(user.getKeycloakId()),
                        RoleNameConverter.createRoleName(permission), realm));
            });
        }
    }

    @Transactional
    public void publishUserDomainUpdateEvent(User user, UserDomain userDomain) {

        log.debug("Publishing user creation event for user: {} with uuid {}", user.getEmail(), user.getUuid());
        applicationEventPublisher.publishEvent(
                new UserCreationEvent(user.getEmail(), user.getFirstName(), user.getLastName(), user.getEmail(),
                        user.isActive(), userDomain, realm, user.getUuid()
                )
        );

        String fullName = new StringBuilder().append(user.getFirstName()).append(" ")
                .append(user.getMiddleName() != null ? user.getMiddleName() + " " : ""
                ).append(user.getLastName()).toString().toUpperCase();

        switch (userDomain) {
            case instructor -> {
                applicationEventPublisher.publishEvent(new RegisterInstructor(fullName, user.getUuid()));
            }
            case admin -> {
                applicationEventPublisher.publishEvent(new RegisterAdmin(fullName, user.getUuid()));
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