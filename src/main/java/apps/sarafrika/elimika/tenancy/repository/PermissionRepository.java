package apps.sarafrika.elimika.tenancy.repository;

import apps.sarafrika.elimika.tenancy.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    List<Permission> findAllByUuidIn(List<UUID> uuids);

    Optional<Permission> findByUuid(UUID uuid);

    @Query(value = """
        WITH user_permissions AS (
            SELECT 
                p.module_name, 
                p.permission_name
            FROM 
                permissions p
            INNER JOIN 
                role_permissions rp ON rp.permission_id = p.id
            INNER JOIN 
                user_role ur ON ur.role_id = rp.role_id
            INNER JOIN 
                users u ON u.id = ur.user_id
            WHERE 
                u.email = :email
            
            UNION ALL
            
            SELECT 
                p.module_name, 
                p.permission_name
            FROM 
                permissions p
            INNER JOIN 
                role_permissions rp ON rp.permission_id = p.id
            INNER JOIN 
                user_group_role ugr ON ugr.role_id = rp.role_id
            INNER JOIN 
                user_group_membership ugm ON ugm.group_id = ugr.group_id
            INNER JOIN 
                users u ON u.id = ugm.user_id
            WHERE 
                u.email = :email
        )
        SELECT DISTINCT 
            module_name, 
            permission_name
        FROM 
            user_permissions
        """, nativeQuery = true)
    List<Object[]> findPermissionsByEmail(@Param("email") String email);
}
