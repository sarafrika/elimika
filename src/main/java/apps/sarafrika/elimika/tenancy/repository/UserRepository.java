package apps.sarafrika.elimika.tenancy.repository;

import apps.sarafrika.elimika.tenancy.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
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

    /** Uuids among {@code uuids} whose date of birth is after {@code cutoff}; a NULL dob never matches. */
    @Query("SELECT u.uuid FROM User u WHERE u.uuid IN :uuids AND u.dob > :cutoff")
    List<UUID> findUuidsBornAfter(@Param("uuids") Collection<UUID> uuids, @Param("cutoff") java.time.LocalDate cutoff);

    Page<User> findByUuidIn(Set<UUID> uuids, Pageable pageable);

    /**
     * Users who hold neither the global {@code admin} domain nor an active, non-deleted
     * {@code organisation_user} organisation mapping - i.e. those {@code AdminService#isAdmin}
     * reports as non-admins. A search term goes to the {@code people} index instead (see
     * {@code PeopleSearchService#searchAdminEligible}); this query is the unfiltered, paged listing.
     */
    @Query("""
            SELECT u FROM User u
            WHERE NOT EXISTS (
                SELECT 1 FROM UserDomainMapping udm, UserDomain ud
                WHERE ud.uuid = udm.userDomainUuid
                  AND udm.userUuid = u.uuid
                  AND ud.domainName = 'admin')
              AND NOT EXISTS (
                SELECT 1 FROM UserOrganisationDomainMapping uodm, UserDomain od
                WHERE od.uuid = uodm.domainUuid
                  AND uodm.userUuid = u.uuid
                  AND uodm.active = true
                  AND uodm.deleted = false
                  AND od.domainName = 'organisation_user')
            """)
    Page<User> findAdminEligibleUsers(Pageable pageable);

    /**
     * The users among {@code uuids} who are currently active, non-deleted members of the
     * organisation - the authorization predicate of an organisation roster, re-applied to search hits
     * so a membership revoked after the index was written is never returned.
     */
    @Query("""
            SELECT u FROM User u
            WHERE u.uuid IN :uuids
              AND EXISTS (
                SELECT 1 FROM UserOrganisationDomainMapping uodm
                WHERE uodm.userUuid = u.uuid
                  AND uodm.organisationUuid = :organisationUuid
                  AND uodm.active = true
                  AND uodm.deleted = false)
            """)
    List<User> findOrganisationMembersByUuidIn(@Param("uuids") Collection<UUID> uuids,
                                               @Param("organisationUuid") UUID organisationUuid);

    /**
     * {@link #findOrganisationMembersByUuidIn} over several organisations at once: the users among
     * {@code uuids} who are currently active, non-deleted members of any of them. Re-checks global
     * search hits for a manager of several organisations in one query.
     */
    @Query("""
            SELECT u FROM User u
            WHERE u.uuid IN :uuids
              AND EXISTS (
                SELECT 1 FROM UserOrganisationDomainMapping uodm
                WHERE uodm.userUuid = u.uuid
                  AND uodm.organisationUuid IN :organisationUuids
                  AND uodm.active = true
                  AND uodm.deleted = false)
            """)
    List<User> findMembersOfAnyOrganisationByUuidIn(@Param("uuids") Collection<UUID> uuids,
                                                    @Param("organisationUuids") Collection<UUID> organisationUuids);

    /**
     * The users among {@code uuids} that {@link #findAdminEligibleUsers} would return, re-applied to
     * search hits so a user promoted after the index was written is never offered again.
     */
    @Query("""
            SELECT u FROM User u
            WHERE u.uuid IN :uuids
              AND NOT EXISTS (
                SELECT 1 FROM UserDomainMapping udm, UserDomain ud
                WHERE ud.uuid = udm.userDomainUuid
                  AND udm.userUuid = u.uuid
                  AND ud.domainName = 'admin')
              AND NOT EXISTS (
                SELECT 1 FROM UserOrganisationDomainMapping uodm, UserDomain od
                WHERE od.uuid = uodm.domainUuid
                  AND uodm.userUuid = u.uuid
                  AND uodm.active = true
                  AND uodm.deleted = false
                  AND od.domainName = 'organisation_user')
            """)
    List<User> findAdminEligibleByUuidIn(@Param("uuids") Collection<UUID> uuids);

    long countByActiveFalse();

    long countByCreatedDateAfter(LocalDateTime createdDate);

    long countByLastModifiedDateAfter(LocalDateTime lastModifiedDate);

    List<User> findByCreatedDateBetween(LocalDateTime start, LocalDateTime end);
}
