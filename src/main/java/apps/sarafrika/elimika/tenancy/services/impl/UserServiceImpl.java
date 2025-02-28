package apps.sarafrika.elimika.tenancy.services.impl;

import apps.sarafrika.elimika.common.entity.BaseEntity;
import apps.sarafrika.elimika.common.event.role.AssignRoleToUserEvent;
import apps.sarafrika.elimika.common.event.user.*;
import apps.sarafrika.elimika.common.exceptions.RecordNotFoundException;
import apps.sarafrika.elimika.common.util.GenericSpecificationBuilder;
import apps.sarafrika.elimika.common.util.RoleNameConverter;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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

    private final GenericSpecificationBuilder<User> specificationBuilder;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional
    public UserDTO createUser(UserDTO userDTO) {
        log.debug("Creating new user with email: {}", userDTO.email());

        try {
            Organisation organisation = findOrganisationOrThrow(userDTO.organisationUuid());
            User user = createAndSaveUser(userDTO, organisation);
            publishUserCreationEvent(user);

            log.info("Successfully created user with UUID: {}", user.getUuid());
            return UserFactory.toDTO(user);
        } catch (Exception e) {
            log.error("Failed to create user with email: {}", userDTO.email(), e);
            throw new RuntimeException("Failed to create user: " + e.getMessage(), e);
        }
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
    public UserDTO updateUser(UUID uuid, UserDTO userDTO) {
        log.debug("Updating user with UUID: {}", uuid);

        Organisation organisation = findOrganisationOrThrow(userDTO.organisationUuid());
        User user = findUserOrThrow(uuid);

        try {
            updateUserFields(user, userDTO, organisation);
            user = userRepository.save(user);
            publishUserUpdateEvent(user);

            log.info("Successfully updated user with UUID: {}", uuid);
            return UserFactory.toDTO(user);
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

    private User createAndSaveUser(UserDTO userDTO, Organisation organisation) {
        User user = UserFactory.toEntity(userDTO);
        user.setOrganisation(organisation);

        user.setRoles(new ArrayList<>());

        user = userRepository.save(user);

        if (userDTO.roles() != null && !userDTO.roles().isEmpty()) {
            List<Role> persistedRoles = roleRepository.findAllByUuidIn(
                    userDTO.roles().stream()
                            .map(RoleDTO::uuid)
                            .collect(Collectors.toList())
            );

            user.setRoles(persistedRoles);
            user = userRepository.save(user);
        }

        return user;
    }

    private void updateUserFields(User user, UserDTO userDTO, Organisation organisation) {
        user.setFirstName(userDTO.firstName());
        user.setMiddleName(userDTO.middleName());
        user.setLastName(userDTO.lastName());
        user.setEmail(userDTO.email());
        user.setPhoneNumber(userDTO.phoneNumber());
        user.setActive(userDTO.active());
        user.setOrganisation(organisation);

        if (userDTO.roles() != null) {
            List<Role> persistedRoles = roleRepository.findAllByUuidIn(
                    userDTO.roles().stream()
                            .map(RoleDTO::uuid)
                            .collect(Collectors.toList())
            );
            user.setRoles(persistedRoles);

            persistedRoles.stream().flatMap(role -> role.getPermissions().stream()).forEach(permission -> {
                applicationEventPublisher.publishEvent(new AssignRoleToUserEvent(UUID.fromString(user.getKeycloakId()), RoleNameConverter.createRoleName(permission), realm));
            });
        }
    }

    private void publishUserCreationEvent(User user) {

        log.debug("Publishing user creation event for user: {} with uuid {}", user.getEmail(), user.getUuid());
        applicationEventPublisher.publishEvent(
                new UserCreationEvent(
                        user.getEmail(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getEmail(),
                        user.isActive(),
                        realm,
                        user.getUuid()
                )
        );
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
}