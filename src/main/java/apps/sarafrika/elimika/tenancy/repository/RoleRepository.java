package apps.sarafrika.elimika.tenancy.repository;

import apps.sarafrika.elimika.tenancy.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, Long>, JpaSpecificationExecutor<Role> {
    Optional<Role> findByUuid(UUID uuid);

    Optional<Role> findByName(String name);

    Page<Role> findByOrganisationUuid(UUID organisationUid, Pageable pageable);

    List<Role> findAllByUuidIn(List<UUID> uuids);

    List<Role> findByUsers_Id(Long userId);

    @Query(value = """
            SELECT r.*
                FROM role r
                JOIN user_group_role ugr ON ugr.role_id = r.id
                JOIN user_group ug ON ug.id = ugr.group_id
            WHERE ug.uuid = :uuid
            """, nativeQuery = true)
    Page<Role> getRolesAssignedToUserGroup(@Param("uuid") UUID uuid, Pageable pageable);
}
