package apps.sarafrika.elimika.tenancy.repository;

import apps.sarafrika.elimika.tenancy.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByUuid(UUID uuid);

    /**
     * Every user whose email equals the given one, ignoring case. Uses {@code lower(email)} so the
     * {@code idx_users_email_lower} expression index applies.
     */
    @Query("SELECT u FROM User u WHERE lower(u.email) = lower(:email) ORDER BY u.id")
    List<User> findAllByEmailIgnoreCase(@Param("email") String email);

    /**
     * Case-insensitive email lookup. The unique constraint on {@code users.email} is case-sensitive,
     * so two rows could differ only by case; an exact match wins, otherwise the oldest row does.
     */
    default Optional<User> findByEmailIgnoreCase(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        String trimmed = email.trim();
        List<User> matches = findAllByEmailIgnoreCase(trimmed);
        return matches.stream()
                .filter(user -> trimmed.equals(user.getEmail()))
                .findFirst()
                .or(() -> matches.stream().findFirst());
    }

    Optional<User> findByPhoneNumber(String phoneNumber);

    Optional<User> findByKeycloakId(String keycloakId);

    boolean existsByKeycloakId(String keycloakId);

    boolean existsByUuid(UUID uuid);

    List<User> findAllByUuidIn(List<UUID> uuids);

    List<User> findByUuidIn(List<UUID> uuids);

    @Query(value = """
            SELECT u.*
            FROM users u
            JOIN user_group_membership ugm ON ugm.user_id = u.id
            JOIN user_group ug ON ug.id = ugm.group_id
            WHERE ug.uuid = :uuid
            """, nativeQuery = true)
    Page<User> getUsersInUserGroup(@Param("uuid") UUID uuid, Pageable pageable);

    Page<User> findByUuidIn(Set<UUID> uuids, Pageable pageable);

    long countByActiveFalse();

    long countByCreatedDateAfter(LocalDateTime createdDate);

    long countByLastModifiedDateAfter(LocalDateTime lastModifiedDate);

    List<User> findByCreatedDateBetween(LocalDateTime start, LocalDateTime end);
}
