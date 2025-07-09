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
                user.getPhoneNumber(),
                user.getProfileImageUrl(),
                user.getDob(),
                user.getUsername(),
                user.isActive(),
                user.getCreatedDate(),
                user.getLastModifiedDate(),
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
        user.setPhoneNumber(dto.phoneNumber());
        user.setActive(dto.active());
        user.setGender(dto.gender());
        user.setDob(dto.dob());
        return user;
    }
}
