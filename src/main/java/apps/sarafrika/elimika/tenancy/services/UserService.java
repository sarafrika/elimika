package apps.sarafrika.elimika.tenancy.services;

import apps.sarafrika.elimika.common.enums.UserDomain;
import apps.sarafrika.elimika.tenancy.dto.UserDTO;
import apps.sarafrika.elimika.tenancy.entity.User;
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

    UserDTO updateUser(UUID uuid, UserDTO userDTO, MultipartFile file);

    void deleteUser(UUID uuid);

    Page<UserDTO> search(Map<String, String> searchParams, Pageable pageable);
}
