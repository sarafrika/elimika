package apps.sarafrika.elimika.tenancy.services.impl;

import apps.sarafrika.elimika.tenancy.dto.PermissionDTO;
import apps.sarafrika.elimika.tenancy.repository.PermissionRepository;
import apps.sarafrika.elimika.tenancy.services.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {
    private final PermissionRepository permissionRepository;

    @Override
    public List<PermissionDTO> getAllPermissions() {
        return permissionRepository.findAll().stream()
                .map(permission -> new PermissionDTO(
                        permission.getUuid(),
                        permission.getModuleName(),
                        permission.getPermissionName(),
                        permission.getDescription()
                ))
                .collect(Collectors.toList());
    }
}
