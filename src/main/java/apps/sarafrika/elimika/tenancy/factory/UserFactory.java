package apps.sarafrika.elimika.tenancy.factory;

import apps.sarafrika.elimika.tenancy.dto.UserDTO;
import apps.sarafrika.elimika.tenancy.entity.User;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.UUID;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UserFactory {

    public static UserDTO toDTO(User user) {
        UUID organisationUuid = null;
        if (user.getOrganisation() != null) {
            organisationUuid = user.getOrganisation().getUuid();
        }

        return new UserDTO(
                user.getUuid(),
                user.getFirstName(),
                user.getMiddleName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getProfileImageUrl(),
                user.getDob(),
                user.getUsername(),
                organisationUuid,
                user.isActive(),
                user.getCreatedDate(),
                user.getLastModifiedDate(),
                user.getRoles().stream()
                        .map(RoleFactory::toDTO)
                        .collect(Collectors.toSet()),
                user.getGender()
        );
    }

    public static User toEntity(UserDTO dto) {
        User user = new User();
        user.setUuid(dto.uuid());
        user.setFirstName(dto.firstName());
        user.setMiddleName(dto.middleName());
        user.setLastName(dto.lastName());
        user.setEmail(dto.email());
        user.setPhoneNumber(dto.phoneNumber());
        user.setActive(dto.active());
        user.setRoles(new ArrayList<>());
        user.setGender(dto.gender());
        return user;
    }
}
