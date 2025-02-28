package apps.sarafrika.elimika.tenancy.services.impl;

import apps.sarafrika.elimika.common.event.role.CreateRoleOnKeyCloakEvent;
import apps.sarafrika.elimika.common.event.role.SuccessfulRoleCreationOnKeycloakEvent;
import apps.sarafrika.elimika.common.exceptions.RecordNotFoundException;
import apps.sarafrika.elimika.common.util.GenericSpecificationBuilder;
import apps.sarafrika.elimika.common.util.RoleNameConverter;
import apps.sarafrika.elimika.tenancy.dto.PermissionDTO;
import apps.sarafrika.elimika.tenancy.dto.RoleDTO;
import apps.sarafrika.elimika.tenancy.entity.Permission;
import apps.sarafrika.elimika.tenancy.entity.Role;
import apps.sarafrika.elimika.tenancy.factory.RoleFactory;
import apps.sarafrika.elimika.tenancy.repository.PermissionRepository;
import apps.sarafrika.elimika.tenancy.repository.RoleRepository;
import apps.sarafrika.elimika.tenancy.services.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.modulith.events.ApplicationModuleListener;
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
public class RoleServiceImpl implements RoleService {
    @Value("${app.keycloak.realm}")
    private String realm;

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    private final ApplicationEventPublisher applicationEventPublisher;
    private final GenericSpecificationBuilder<Role> specificationBuilder;

    @Override
    @Transactional
    public RoleDTO createRole(RoleDTO roleDTO) {
        log.debug("Creating new role with name: {}", roleDTO.name());
        Role role = RoleFactory.toEntity(roleDTO);

        // Add permissions if provided
        if (roleDTO.permissions() != null && !roleDTO.permissions().isEmpty()) {
            List<Permission> permissions = permissionRepository.findAllByUuidIn(
                    roleDTO.permissions().stream().map(PermissionDTO::uuid).collect(Collectors.toList())
            );
            role.setPermissions(new ArrayList<>(permissions));
        }

        role = roleRepository.save(role);

        if(role.getPermissions() != null && !role.getPermissions().isEmpty()) {
            role.getPermissions().stream().filter(permission -> permission.getKeycloakId() == null).forEach(permission -> {
                applicationEventPublisher.publishEvent(
                        new CreateRoleOnKeyCloakEvent(RoleNameConverter.createRoleName(permission),
                                permission.getDescription(), realm, permission.getUuid()));
            });
        }

        return RoleFactory.toDTO(role);
    }

    @Override
    @Transactional(readOnly = true)
    public RoleDTO getRoleByUuid(UUID uuid) {
        log.info("Fetching role with UUID: {}", uuid);
        Role role = roleRepository.findByUuid(uuid).orElseThrow(
                () -> new RecordNotFoundException("Role not found for UUID: " + uuid)
        );
        return RoleFactory.toDTO(role);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RoleDTO> getRolesByOrganisation(UUID organisationUid, Pageable pageable) {
        log.debug("Fetching roles for organisation UID: {}", organisationUid);
        return roleRepository.findByOrganisationUuid(organisationUid, pageable).map(RoleFactory::toDTO);
    }

    @Override
    @Transactional
    public RoleDTO updateRole(UUID uuid, RoleDTO roleDTO) {
        log.info("Updating role with UUID: {}", uuid);
        Role role = roleRepository.findByUuid(uuid).orElseThrow(
                () -> new RecordNotFoundException("Role not found for UUID: " + uuid)
        );

        role.setName(roleDTO.name());
        role.setActive(roleDTO.active());

        // Update permissions if provided
        if (roleDTO.permissions() != null) {
            List<Permission> permissions = permissionRepository.findAllByUuidIn(
                    roleDTO.permissions().stream().map(PermissionDTO::uuid).collect(Collectors.toList())
            );
            role.setPermissions(new ArrayList<>(permissions));
        }

        role = roleRepository.save(role);

        if(role.getPermissions() != null && !role.getPermissions().isEmpty()) {
            role.getPermissions().stream().filter(permission -> permission.getKeycloakId() == null).forEach(permission -> {
                applicationEventPublisher.publishEvent(
                        new CreateRoleOnKeyCloakEvent(RoleNameConverter.createRoleName(permission),
                                permission.getDescription(), realm, permission.getUuid()));
            });
        }

        return RoleFactory.toDTO(role);
    }

    @Override
    @Transactional
    public void deleteRole(UUID uuid) {
        log.info("Deleting role with UUID: {}", uuid);
        Role role = roleRepository.findByUuid(uuid).orElseThrow(
                () -> new RecordNotFoundException("Role not found for UUID: " + uuid)
        );

        // Clear permissions before deletion
        role.getPermissions().clear();
        roleRepository.delete(role);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RoleDTO> search(Map<String, String> searchParams, Pageable pageable) {
        log.debug("Searching roles with params: {}", searchParams);
        Specification<Role> spec = specificationBuilder.buildSpecification(Role.class, searchParams);
        return roleRepository.findAll(spec, pageable).map(RoleFactory::toDTO);
    }

    @Override
    @Transactional
    public RoleDTO addPermissionsToRole(UUID roleUuid, List<UUID> permissionUuids) {
        log.info("Adding permissions to role with UUID: {}", roleUuid);
        Role role = roleRepository.findByUuid(roleUuid).orElseThrow(
                () -> new RecordNotFoundException("Role with UUID " + roleUuid + " not found")
        );

        List<Permission> permissions = permissionRepository.findAllByUuidIn(permissionUuids);

        if (permissions.isEmpty()) {
            throw new RecordNotFoundException("No permissions found for the provided UUIDs");
        }

        role.getPermissions().addAll(permissions);
        Role updatedRole = roleRepository.save(role);
        return RoleFactory.toDTO(updatedRole);
    }

    @Override
    @Transactional
    public RoleDTO removePermissionsFromRole(UUID roleUuid, List<UUID> permissionUuids) {
        log.info("Removing permissions from role with UUID: {}", roleUuid);
        Role role = roleRepository.findByUuid(roleUuid).orElseThrow(
                () -> new RecordNotFoundException("Role with UUID " + roleUuid + " not found")
        );

        List<Permission> permissions = permissionRepository.findAllByUuidIn(permissionUuids);

        if (permissions.isEmpty()) {
            throw new RecordNotFoundException("No permissions found for the provided UUIDs");
        }

        role.getPermissions().removeAll(permissions);
        Role updatedRole = roleRepository.save(role);
        return RoleFactory.toDTO(updatedRole);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionDTO> getPermissionsByRole(UUID roleUuid) {
        log.info("Fetching permissions for role with UUID: {}", roleUuid);
        Role role = roleRepository.findByUuid(roleUuid).orElseThrow(
                () -> new RecordNotFoundException("Role with UUID " + roleUuid + " not found")
        );

        return role.getPermissions().stream()
                .map(permission -> new PermissionDTO(
                        permission.getUuid(),
                        permission.getModuleName(),
                        permission.getPermissionName(),
                        permission.getDescription()
                ))
                .collect(Collectors.toList());
    }

    @ApplicationModuleListener
    void onSuccessfulRoleCreation(SuccessfulRoleCreationOnKeycloakEvent event) {
        Permission permission = permissionRepository.findByUuid(event.keycloakId()).orElseThrow(
                () -> new RecordNotFoundException("Role not found for UUID: " + event.keycloakId())
        );
        permission.setKeycloakId(event.keycloakId());
        permissionRepository.save(permission);
    }
}
