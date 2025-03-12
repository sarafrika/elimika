package apps.sarafrika.elimika.tenancy.repository;

import apps.sarafrika.elimika.tenancy.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByUuid(UUID uuid);

    Optional<User> findByEmail(String email);

    Optional<User> findByPhoneNumber(String phoneNumber);

    boolean existsByEmail(String email);

    Page<User> findByOrganisationId(Long organisationId, Pageable pageable);

    List<User> findAllByUuidIn(List<UUID> uuids);

    @Query(value = """
            SELECT u.*
            FROM users u
            JOIN user_group_membership ugm ON ugm.user_id = u.id
            JOIN user_group ug ON ug.id = ugm.group_id
            WHERE ug.uuid = :uuid
            """, nativeQuery = true)
    Page<User> getUsersInUserGroup(@Param("uuid") UUID uuid, Pageable pageable);
}
