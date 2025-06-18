package apps.sarafrika.elimika.tenancy.services;

import apps.sarafrika.elimika.tenancy.dto.UserDTO;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

public interface UserService {
    void createUser(UserRepresentation userRep);

    UserDTO getUserByUuid(UUID uuid);

    Page<UserDTO> getUsersByOrganisation(UUID organisationId, Pageable pageable);

    UserDTO updateUser(UUID uuid, UserDTO userDTO);

    UserDTO uploadProfileImage(UUID userUuid, MultipartFile profileImage);

    void deleteUser(UUID uuid);

    Page<UserDTO> search(Map<String, String> searchParams, Pageable pageable);
}
