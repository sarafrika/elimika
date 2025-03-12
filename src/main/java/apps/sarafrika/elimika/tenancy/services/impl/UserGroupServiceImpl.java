package apps.sarafrika.elimika.tenancy.services.impl;

import apps.sarafrika.elimika.common.exceptions.RecordNotFoundException;
import apps.sarafrika.elimika.common.util.GenericSpecificationBuilder;
import apps.sarafrika.elimika.tenancy.dto.RoleDTO;
import apps.sarafrika.elimika.tenancy.dto.UserDTO;
import apps.sarafrika.elimika.tenancy.dto.UserGroupDTO;
import apps.sarafrika.elimika.tenancy.entity.Organisation;
import apps.sarafrika.elimika.tenancy.entity.Role;
import apps.sarafrika.elimika.tenancy.entity.User;
import apps.sarafrika.elimika.tenancy.entity.UserGroup;
import apps.sarafrika.elimika.tenancy.factory.RoleFactory;
import apps.sarafrika.elimika.tenancy.factory.UserFactory;
import apps.sarafrika.elimika.tenancy.factory.UserGroupFactory;
import apps.sarafrika.elimika.tenancy.repository.OrganisationRepository;
import apps.sarafrika.elimika.tenancy.repository.RoleRepository;
import apps.sarafrika.elimika.tenancy.repository.UserGroupRepository;
import apps.sarafrika.elimika.tenancy.repository.UserRepository;
import apps.sarafrika.elimika.tenancy.services.UserGroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service @RequiredArgsConstructor @Slf4j
public class UserGroupServiceImpl implements UserGroupService {
    private final UserGroupRepository userGroupRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OrganisationRepository organisationRepository;

    private final GenericSpecificationBuilder<UserGroup> genericSpecificationBuilder;

    @Override
    @Transactional
    public UserGroupDTO createUserGroup(UserGroupDTO userGroupDTO) {
        UserGroup userGroup = UserGroupFactory.toEntity(userGroupDTO);
        Organisation organisation = organisationRepository.findByUuid(userGroupDTO.organisationId())
                .orElseThrow(() -> new RecordNotFoundException("Organisation not found for UUID: " + userGroupDTO.organisationId()));
        userGroup.setOrganisation(organisation);
        userGroup = userGroupRepository.save(userGroup);
        return UserGroupFactory.toDTO(userGroup);
    }

    @Override
    @Transactional(readOnly = true)
    public UserGroupDTO getUserGroupByUuid(UUID uuid) {
        UserGroup userGroup = userGroupRepository.findByUuid(uuid)
                .orElseThrow(() -> new RecordNotFoundException("UserGroup not found for UUID: " + uuid));
        return UserGroupFactory.toDTO(userGroup);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserGroupDTO> getUserGroupsByOrganisation(UUID organisationUuid, Pageable pageable) {
        return userGroupRepository.findByOrganisationUuid(organisationUuid, pageable)
                .map(UserGroupFactory::toDTO);
    }

    @Override
    @Transactional
    public UserGroupDTO updateUserGroup(UUID uuid, UserGroupDTO userGroupDTO) {
        UserGroup userGroup = userGroupRepository.findByUuid(uuid)
                .orElseThrow(() -> new RecordNotFoundException("UserGroup not found for UUID: " + uuid));

        userGroup.setName(userGroupDTO.name());
        userGroup.setActive(userGroupDTO.active());
        userGroup = userGroupRepository.save(userGroup);

        return UserGroupFactory.toDTO(userGroup);
    }

    @Override
    @Transactional
    public void deleteUserGroup(UUID uuid) {
        UserGroup userGroup = userGroupRepository.findByUuid(uuid)
                .orElseThrow(() -> new RecordNotFoundException("UserGroup not found for UUID: " + uuid));

        userGroup.getUsers().clear();
        userGroup.getRoles().clear();
        userGroupRepository.delete(userGroup);
    }

    @Override
    @Transactional
    public void addUsersToGroup(UUID groupUuid, List<UUID> userUuids) {
        UserGroup userGroup = userGroupRepository.findByUuid(groupUuid)
                .orElseThrow(() -> new RecordNotFoundException("UserGroup not found for UUID: " + groupUuid));

        List<User> users = userRepository.findAllByUuidIn(userUuids);
        userGroup.getUsers().addAll(users);
        userGroupRepository.save(userGroup);
    }

    @Override
    @Transactional
    public void removeUsersFromGroup(UUID groupUuid, List<UUID> userUuids) {
        UserGroup userGroup = userGroupRepository.findByUuid(groupUuid)
                .orElseThrow(() -> new RecordNotFoundException("UserGroup not found for UUID: " + groupUuid));

        List<User> users = userRepository.findAllByUuidIn(userUuids);
        userGroup.getUsers().removeAll(users);
        userGroupRepository.save(userGroup);
    }

    @Override
    @Transactional
    public void assignRolesToGroup(UUID groupUuid, List<UUID> roleUuids) {
        UserGroup userGroup = userGroupRepository.findByUuid(groupUuid)
                .orElseThrow(() -> new RecordNotFoundException("UserGroup not found for UUID: " + groupUuid));

        List<Role> roles = roleRepository.findAllByUuidIn(roleUuids);
        userGroup.getRoles().addAll(roles);
        userGroupRepository.save(userGroup);
    }

    @Override
    @Transactional
    public void removeRolesFromGroup(UUID groupUuid, List<UUID> roleUuids) {
        UserGroup userGroup = userGroupRepository.findByUuid(groupUuid)
                .orElseThrow(() -> new RecordNotFoundException("UserGroup not found for UUID: " + groupUuid));

        List<Role> roles = roleRepository.findAllByUuidIn(roleUuids);
        userGroup.getRoles().removeAll(roles);
        userGroupRepository.save(userGroup);
    }

    @Override
    public Page<UserGroupDTO> search(Map<String, String> searchParams, Pageable pageable) {
        Specification<UserGroup> spec = genericSpecificationBuilder.buildSpecification(UserGroup.class, searchParams);
        return userGroupRepository.findAll(spec, pageable).map(UserGroupFactory::toDTO);
    }

    @Override
    public Page<UserDTO> getUsersForUserGroup(UUID uuid, Pageable pageable) {
        return userRepository.getUsersInUserGroup(uuid, pageable).map(UserFactory::toDTO);
    }

    @Override
    public Page<RoleDTO> getRolesForUserGroup(UUID uuid, Pageable pageable) {
        return roleRepository.getRolesAssignedToUserGroup(uuid, pageable).map(RoleFactory::toDTO);
    }
}
