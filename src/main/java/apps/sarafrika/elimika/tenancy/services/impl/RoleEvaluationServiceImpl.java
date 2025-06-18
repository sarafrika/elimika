package apps.sarafrika.elimika.tenancy.services.impl;

import apps.sarafrika.elimika.common.exceptions.RecordNotFoundException;
import apps.sarafrika.elimika.tenancy.dto.RoleDTO;
import apps.sarafrika.elimika.tenancy.entity.Role;
import apps.sarafrika.elimika.tenancy.entity.User;
import apps.sarafrika.elimika.tenancy.factory.RoleFactory;
import apps.sarafrika.elimika.tenancy.repository.RoleRepository;
import apps.sarafrika.elimika.tenancy.repository.UserRepository;
import apps.sarafrika.elimika.tenancy.services.RoleEvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;


@Service @RequiredArgsConstructor
public class RoleEvaluationServiceImpl implements RoleEvaluationService {
    private final UserRepository userRepository;
    private final UserGroupRepository userGroupRepository;
    private final RoleRepository roleRepository;

    @Override
    @Transactional(readOnly = true)
    public List<RoleDTO> getEffectiveRolesForUser(UUID userUuid) {
        User user = userRepository.findByUuid(userUuid)
                .orElseThrow(() -> new RecordNotFoundException("User not found for UUID: " + userUuid));

        Set<Role> directRoles = new HashSet<>(roleRepository.findByUsers_Id(user.getId()));

        Set<Role> groupRoles = userGroupRepository.findByUsers_Id(user.getId()).stream()
                .flatMap(group -> group.getRoles().stream())
                .collect(Collectors.toSet());

        groupRoles.removeAll(directRoles);
        directRoles.addAll(groupRoles);

        return directRoles.stream().map(RoleFactory::toDTO).collect(Collectors.toList());
    }
}
