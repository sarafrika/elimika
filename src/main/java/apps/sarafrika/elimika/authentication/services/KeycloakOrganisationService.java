package apps.sarafrika.elimika.authentication.services;

import org.keycloak.representations.idm.MemberRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;

import java.util.List;
import java.util.Map;

public interface KeycloakOrganisationService {
    String createOrganization(String realm, String name, String displayName, String description);

    void deleteOrganization(String realm, String orgId);

    void addUserToOrganization(String realm, String orgId, String userId);

    void removeUserFromOrganization(String realm, String orgId, String userId);

    Map<String, String> getOrganizationMetadata(String realm, String orgId);

    List<OrganizationRepresentation> getAllOrganizations(String realm);

    List<MemberRepresentation> getOrganizationMembers(String orgId, String realm);
}