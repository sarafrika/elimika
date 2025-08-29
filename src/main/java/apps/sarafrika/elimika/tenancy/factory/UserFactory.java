package apps.sarafrika.elimika.tenancy.factory;

import apps.sarafrika.elimika.tenancy.dto.UserDTO;
import apps.sarafrika.elimika.tenancy.dto.UserOrganisationAffiliationDTO;
import apps.sarafrika.elimika.tenancy.entity.User;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UserFactory {

    /**
     * Converts User entity to UserDTO with domains only (backward compatibility).
     * Organization affiliations will be empty.
     */
    public static UserDTO toDTO(User user, List<String> userDomains) {
        return toDTO(user, userDomains, Collections.emptyList());
    }

    /**
     * Converts User entity to UserDTO with domains and organization affiliations.
     * This is the enhanced method that includes organization-specific information.
     */
    public static UserDTO toDTO(User user, List<String> userDomains, List<UserOrganisationAffiliationDTO> organisationAffiliations) {
        return new UserDTO(
                user.getUuid(),
                user.getFirstName(),
                user.getMiddleName(),
                user.getLastName(),
                user.getEmail(),
                user.getUsername(),
                user.getProfileImageUrl(),
                user.getDob(),
                user.getPhoneNumber(),
                user.isActive(),
                user.getKeycloakId(),
                user.getCreatedDate(),
                user.getLastModifiedDate(),
                user.getCreatedBy(),
                user.getLastModifiedBy(),
                user.getGender(),
                userDomains,
                organisationAffiliations
        );
    }

    public static User toEntity(UserDTO dto) {
        User user = new User();
        user.setUuid(dto.uuid());
        user.setFirstName(dto.firstName());
        user.setMiddleName(dto.middleName());
        user.setLastName(dto.lastName());
        user.setEmail(dto.email());
        user.setUsername(dto.username());
        user.setPhoneNumber(dto.phoneNumber());
        user.setDob(dto.dob());
        user.setActive(dto.active());
        user.setKeycloakId(dto.keycloakId());
        user.setGender(dto.gender());
        return user;
    }
}