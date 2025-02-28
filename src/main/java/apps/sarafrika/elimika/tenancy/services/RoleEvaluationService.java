package apps.sarafrika.elimika.tenancy.services;

import apps.sarafrika.elimika.tenancy.dto.RoleDTO;

import java.util.List;
import java.util.UUID;

public interface RoleEvaluationService {
    List<RoleDTO> getEffectiveRolesForUser(UUID userUuid);
}
