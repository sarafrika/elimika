package apps.sarafrika.elimika.tenancy.services;

import apps.sarafrika.elimika.tenancy.dto.UserDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface UserService {
    UserDTO createUser(UserDTO userDTO);

    UserDTO getUserByUuid(UUID uuid);

    Page<UserDTO> getUsersByOrganisation(UUID organisationId, Pageable pageable);

    UserDTO updateUser(UUID uuid, UserDTO userDTO);

    void deleteUser(UUID uuid);

    Page<UserDTO> search(Map<String, String> searchParams, Pageable pageable);
}
