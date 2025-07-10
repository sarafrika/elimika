package apps.sarafrika.elimika.tenancy.factory;

import apps.sarafrika.elimika.tenancy.dto.UserDTO;
import apps.sarafrika.elimika.tenancy.entity.User;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UserFactory {

    public static UserDTO toDTO(User user, List<String> userDomains) {
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
                userDomains
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