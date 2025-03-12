package apps.sarafrika.elimika.common.internal;

import apps.sarafrika.elimika.authentication.services.KeycloakOrganisationService;
import apps.sarafrika.elimika.authentication.services.KeycloakRoleService;
import apps.sarafrika.elimika.authentication.services.KeycloakUserService;
import apps.sarafrika.elimika.tenancy.entity.Organisation;
import apps.sarafrika.elimika.tenancy.entity.Permission;
import apps.sarafrika.elimika.tenancy.entity.User;
import apps.sarafrika.elimika.tenancy.repository.OrganisationRepository;
import apps.sarafrika.elimika.tenancy.repository.PermissionRepository;
import apps.sarafrika.elimika.tenancy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.keycloak.representations.idm.OrganizationDomainRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static apps.sarafrika.elimika.common.util.RoleNameConverter.createRoleName;

@Component
@RequiredArgsConstructor
public class TenancyInitializer implements CommandLineRunner {

    @Value("${app.keycloak.realm}")
    private String realm;

    private final KeycloakRoleService keycloakRoleService;
    private final KeycloakUserService keycloakUserService;
    private final KeycloakOrganisationService keycloakOrganisationService;
    private final OrganisationRepository organisationRepository;
    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;

    @Override
    public void run(String... args) {
        loadOrganizations();
        loadUsers();
        loadRoles();
    }

    private void loadOrganizations() {
        List<OrganizationRepresentation> organizations = keycloakOrganisationService.getAllOrganizations(realm);
        organizations.forEach(this::processOrganization);
    }

    private void processOrganization(OrganizationRepresentation orgRep) {
        String domain = getFirstDomainName(orgRep);
        if (organisationRepository.existsByDomain(domain)) {
            return;
        }

        Organisation organisation = new Organisation(
                orgRep.getName(),
                orgRep.getDescription(),
                true,
                null,
                domain,
                orgRep.getAlias(),
                realm,
                orgRep.getId(),
                null,
                null
        );

        Organisation savedOrg = organisationRepository.save(organisation);

        List<User> users = Optional.ofNullable(orgRep.getMembers())
                .orElse(Collections.emptyList())
                .stream()
                .filter(member -> !userRepository.existsByEmail(member.getEmail()))
                .map(member -> new User(
                        member.getFirstName(),
                        null,
                        member.getLastName(),
                        member.getEmail(),
                        null,
                        true,
                        member.getId(),
                        savedOrg,
                        null,
                        null
                ))
                .toList();

        userRepository.saveAll(users);
    }

    private String getFirstDomainName(OrganizationRepresentation orgRep) {
        return orgRep.getDomains().stream()
                .findFirst()
                .map(OrganizationDomainRepresentation::getName)
                .orElse("");
    }

    private void loadUsers() {
        List<UserRepresentation> userRepresentations = keycloakUserService.getAllUsers(realm);
        userRepresentations.stream()
                .filter(userRep -> !userRepository.existsByEmail(userRep.getEmail()))
                .forEach(this::saveUser);
    }

    private void saveUser(UserRepresentation userRep) {
        User user = new User(
                userRep.getFirstName(),
                null,
                userRep.getLastName(),
                userRep.getEmail(),
                null,
                true,
                userRep.getId(),
                null,
                null,
                null
        );
        userRepository.save(user);
    }

    private void loadRoles() {
        List<Permission> permissions = permissionRepository.findAll();
        permissions.forEach(this::processPermission);
    }

    private void processPermission(Permission permission) {
        String roleName = createRoleName(permission);
        Optional<RoleRepresentation> roleRepOpt = keycloakRoleService.getRoleByName(roleName, realm);

        if (roleRepOpt.isEmpty()) {
            RoleRepresentation createdRole = keycloakRoleService.createRole(roleName, permission.getDescription(), realm);
            permission.setKeycloakId(UUID.fromString(createdRole.getId()));
            permissionRepository.save(permission);
        } else {
            permission.setKeycloakId(UUID.fromString(roleRepOpt.get().getId()));
            permissionRepository.save(permission);
        }
    }
}

