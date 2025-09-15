package apps.sarafrika.elimika.tenancy.repository;

import apps.sarafrika.elimika.tenancy.entity.UserOrganisationDomainMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing UserOrganisationDomainMapping entities.
 * Provides data access methods for user-organisation-domain relationships including
 * branch assignments, role management, and temporal tracking.
 *
 * @author Elimika Team
 * @since 1.0
 */
@Repository
public interface UserOrganisationDomainMappingRepository extends JpaRepository<UserOrganisationDomainMapping, Long> {

    // ================================
    // BASIC LOOKUP METHODS
    // ================================

    /**
     * Find mapping by UUID.
     *
     * @param uuid the mapping UUID
     * @return Optional containing the mapping if found
     */
    Optional<UserOrganisationDomainMapping> findByUuid(UUID uuid);

    /**
     * Find all mappings for a user (including inactive, excluding deleted).
     * Ordered by start date descending to show most recent first.
     *
     * @param userUuid the user UUID
     * @return list of mappings for the user
     */
    List<UserOrganisationDomainMapping> findByUserUuidAndDeletedFalseOrderByStartDateDesc(UUID userUuid);

    /**
     * Find all mappings for an organisation (including inactive, excluding deleted).
     * Ordered by start date descending to show most recent first.
     *
     * @param organisationUuid the organisation UUID
     * @return list of mappings for the organisation
     */
    List<UserOrganisationDomainMapping> findByOrganisationUuidAndDeletedFalseOrderByStartDateDesc(UUID organisationUuid);

    // ================================
    // ACTIVE RELATIONSHIP QUERIES
    // ================================

    /**
     * Find all active mappings for a specific user.
     * Active means: active = true, deleted = false
     * Called as: findActiveByUser() in services
     *
     * @param userUuid the user UUID
     * @return list of active mappings for the user
     */
    List<UserOrganisationDomainMapping> findByUserUuidAndActiveTrueAndDeletedFalse(UUID userUuid);

    /**
     * Find all active mappings for a specific organisation.
     * Active means: active = true, deleted = false
     * Called as: findActiveByOrganisation() in services
     *
     * @param organisationUuid the organisation UUID
     * @return list of active mappings for the organisation
     */
    List<UserOrganisationDomainMapping> findByOrganisationUuidAndActiveTrueAndDeletedFalse(UUID organisationUuid);

    /**
     * Find active mapping between specific user and organisation.
     * Used to check if user is currently associated with organisation.
     * Called as: findActiveByUserAndOrganisation() in services
     *
     * @param userUuid the user UUID
     * @param organisationUuid the organisation UUID
     * @return Optional containing the active mapping if exists
     */
    Optional<UserOrganisationDomainMapping> findByUserUuidAndOrganisationUuidAndActiveTrueAndDeletedFalse(
            UUID userUuid, UUID organisationUuid);

    /**
     * Find active mappings for organisation filtered by domain (role).
     * Used to get all users with specific role in organisation.
     * Called as: findActiveByOrganisationAndDomain() in services
     *
     * @param organisationUuid the organisation UUID
     * @param domainUuid the domain UUID
     * @return list of active mappings for organisation and domain
     */
    List<UserOrganisationDomainMapping> findByOrganisationUuidAndDomainUuidAndActiveTrueAndDeletedFalse(
            UUID organisationUuid, UUID domainUuid);

    /**
     * Find active mappings for a specific training branch.
     * Used to get all users assigned to a branch.
     * Called as: findActiveByBranch() in services
     *
     * @param branchUuid the branch UUID
     * @return list of active mappings for the branch
     */
    List<UserOrganisationDomainMapping> findByBranchUuidAndActiveTrueAndDeletedFalse(UUID branchUuid);

    // ================================
    // SERVICE METHOD ALIASES
    // These methods provide the exact names used in your services
    // ================================

    /**
     * Alias for findByUserUuidAndActiveTrueAndDeletedFalse()
     * Used directly in services as: findActiveByUser()
     */
    default List<UserOrganisationDomainMapping> findActiveByUser(UUID userUuid) {
        return findByUserUuidAndActiveTrueAndDeletedFalse(userUuid);
    }

    /**
     * Alias for findByOrganisationUuidAndActiveTrueAndDeletedFalse()
     * Used directly in services as: findActiveByOrganisation()
     */
    default List<UserOrganisationDomainMapping> findActiveByOrganisation(UUID organisationUuid) {
        return findByOrganisationUuidAndActiveTrueAndDeletedFalse(organisationUuid);
    }

    /**
     * Alias for findByUserUuidAndOrganisationUuidAndActiveTrueAndDeletedFalse()
     * Used directly in services as: findActiveByUserAndOrganisation()
     */
    default Optional<UserOrganisationDomainMapping> findActiveByUserAndOrganisation(UUID userUuid, UUID organisationUuid) {
        return findByUserUuidAndOrganisationUuidAndActiveTrueAndDeletedFalse(userUuid, organisationUuid);
    }

    /**
     * Alias for findByOrganisationUuidAndDomainUuidAndActiveTrueAndDeletedFalse()
     * Used directly in services as: findActiveByOrganisationAndDomain()
     */
    default List<UserOrganisationDomainMapping> findActiveByOrganisationAndDomain(UUID organisationUuid, UUID domainUuid) {
        return findByOrganisationUuidAndDomainUuidAndActiveTrueAndDeletedFalse(organisationUuid, domainUuid);
    }

    /**
     * Alias for findByBranchUuidAndActiveTrueAndDeletedFalse()
     * Used directly in services as: findActiveByBranch()
     */
    default List<UserOrganisationDomainMapping> findActiveByBranch(UUID branchUuid) {
        return findByBranchUuidAndActiveTrueAndDeletedFalse(branchUuid);
    }

    /**
     * Find active mappings for user filtered by domain across all organisations.
     * Used to get all organisations where user has specific role.
     *
     * @param userUuid the user UUID
     * @param domainUuid the domain UUID
     * @return list of active mappings for user and domain
     */
    List<UserOrganisationDomainMapping> findByUserUuidAndDomainUuidAndActiveTrueAndDeletedFalse(
            UUID userUuid, UUID domainUuid);

    // ================================
    // EXISTENCE CHECKS
    // ================================

    /**
     * Check if active mapping exists between user and organisation.
     * Used for quick membership validation.
     *
     * @param userUuid the user UUID
     * @param organisationUuid the organisation UUID
     * @return true if active mapping exists
     */
    boolean existsByUserUuidAndOrganisationUuidAndActiveTrueAndDeletedFalse(UUID userUuid, UUID organisationUuid);

    /**
     * Check if active mapping exists for user, organisation and specific domain.
     * Used for role-based authorization checks.
     * Called as: existsActiveByUserOrganisationAndDomain() in services
     *
     * @param userUuid the user UUID
     * @param organisationUuid the organisation UUID
     * @param domainUuid the domain UUID
     * @return true if active mapping exists with specified role
     */
    boolean existsByUserUuidAndOrganisationUuidAndDomainUuidAndActiveTrueAndDeletedFalse(
            UUID userUuid, UUID organisationUuid, UUID domainUuid);

    // ================================
    // SERVICE METHOD ALIASES FOR EXISTENCE CHECKS
    // ================================

    /**
     * Alias for existsByUserUuidAndOrganisationUuidAndDomainUuidAndActiveTrueAndDeletedFalse()
     * Used directly in services as: existsActiveByUserOrganisationAndDomain()
     */
    default boolean existsActiveByUserOrganisationAndDomain(UUID userUuid, UUID organisationUuid, UUID domainUuid) {
        return existsByUserUuidAndOrganisationUuidAndDomainUuidAndActiveTrueAndDeletedFalse(userUuid, organisationUuid, domainUuid);
    }

    // ================================
    // ADVANCED QUERIES WITH CUSTOM JPQL
    // ================================

    /**
     * Find current active mapping for user in organisation with date validation.
     * Ensures the mapping is currently valid (not expired).
     *
     * @param userUuid the user UUID
     * @param organisationUuid the organisation UUID
     * @return Optional containing current valid mapping
     */
    @Query("SELECT uodm FROM UserOrganisationDomainMapping uodm " +
            "WHERE uodm.userUuid = :userUuid " +
            "AND uodm.organisationUuid = :organisationUuid " +
            "AND uodm.active = true " +
            "AND uodm.deleted = false " +
            "AND (uodm.endDate IS NULL OR uodm.endDate >= CURRENT_DATE)")
    Optional<UserOrganisationDomainMapping> findCurrentActiveMapping(
            @Param("userUuid") UUID userUuid,
            @Param("organisationUuid") UUID organisationUuid);

    /**
     * Find distinct user UUIDs for organisation (for pagination support).
     * Returns only UUIDs to optimize memory usage when dealing with large datasets.
     *
     * @param organisationUuid the organisation UUID
     * @return list of distinct user UUIDs in the organisation
     */
    @Query("SELECT DISTINCT uodm.userUuid FROM UserOrganisationDomainMapping uodm " +
            "WHERE uodm.organisationUuid = :organisationUuid " +
            "AND uodm.active = true " +
            "AND uodm.deleted = false")
    List<UUID> findDistinctUserUuidsByOrganisation(@Param("organisationUuid") UUID organisationUuid);

    /**
     * Find distinct organisation UUIDs for user.
     * Returns only UUIDs to optimize memory usage.
     *
     * @param userUuid the user UUID
     * @return list of distinct organisation UUIDs for the user
     */
    @Query("SELECT DISTINCT uodm.organisationUuid FROM UserOrganisationDomainMapping uodm " +
            "WHERE uodm.userUuid = :userUuid " +
            "AND uodm.active = true " +
            "AND uodm.deleted = false")
    List<UUID> findDistinctOrganisationUuidsByUser(@Param("userUuid") UUID userUuid);

    /**
     * Find mappings for multiple users in specific organisation.
     * Used for bulk operations and batch processing.
     *
     * @param userUuids list of user UUIDs
     * @param organisationUuid the organisation UUID
     * @return list of active mappings for the users in organisation
     */
    @Query("SELECT uodm FROM UserOrganisationDomainMapping uodm " +
            "WHERE uodm.userUuid IN :userUuids " +
            "AND uodm.organisationUuid = :organisationUuid " +
            "AND uodm.active = true " +
            "AND uodm.deleted = false")
    List<UserOrganisationDomainMapping> findActiveByUserUuidsAndOrganisation(
            @Param("userUuids") List<UUID> userUuids,
            @Param("organisationUuid") UUID organisationUuid);

    // ================================
    // ANALYTICS AND REPORTING QUERIES
    // ================================

    /**
     * Count active users in organisation.
     * Used for organisation metrics and dashboard statistics.
     *
     * @param organisationUuid the organisation UUID
     * @return count of distinct active users
     */
    @Query("SELECT COUNT(DISTINCT uodm.userUuid) FROM UserOrganisationDomainMapping uodm " +
            "WHERE uodm.organisationUuid = :organisationUuid " +
            "AND uodm.active = true " +
            "AND uodm.deleted = false")
    long countDistinctActiveUsersByOrganisation(@Param("organisationUuid") UUID organisationUuid);

    /**
     * Count active users by domain in organisation.
     * Used for role-based statistics (e.g., number of instructors, students).
     *
     * @param organisationUuid the organisation UUID
     * @param domainUuid the domain UUID
     * @return count of distinct active users with specified role
     */
    @Query("SELECT COUNT(DISTINCT uodm.userUuid) FROM UserOrganisationDomainMapping uodm " +
            "WHERE uodm.organisationUuid = :organisationUuid " +
            "AND uodm.domainUuid = :domainUuid " +
            "AND uodm.active = true " +
            "AND uodm.deleted = false")
    long countDistinctActiveUsersByOrganisationAndDomain(
            @Param("organisationUuid") UUID organisationUuid,
            @Param("domainUuid") UUID domainUuid);

    /**
     * Count active users in training branch.
     * Used for branch capacity and utilization metrics.
     *
     * @param branchUuid the branch UUID
     * @return count of distinct active users in branch
     */
    @Query("SELECT COUNT(DISTINCT uodm.userUuid) FROM UserOrganisationDomainMapping uodm " +
            "WHERE uodm.branchUuid = :branchUuid " +
            "AND uodm.active = true " +
            "AND uodm.deleted = false")
    long countDistinctActiveUsersByBranch(@Param("branchUuid") UUID branchUuid);

    // ================================
    // OPERATIONAL QUERIES
    // ================================

    /**
     * Find mappings ending within specified date range.
     * Used for notification systems to alert about expiring memberships.
     *
     * @param startDate the start date for the range
     * @param endDate the end date for the range
     * @return list of mappings ending within the date range
     */
    @Query("SELECT uodm FROM UserOrganisationDomainMapping uodm " +
            "WHERE uodm.endDate IS NOT NULL " +
            "AND uodm.endDate BETWEEN :startDate AND :endDate " +
            "AND uodm.active = true " +
            "AND uodm.deleted = false " +
            "ORDER BY uodm.endDate ASC")
    List<UserOrganisationDomainMapping> findMappingsEndingBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find mappings that started after specified date.
     * Used for reporting new memberships and onboarding tracking.
     *
     * @param startDate the date to search from
     * @return list of mappings created after the specified date
     */
    List<UserOrganisationDomainMapping> findByStartDateAfterAndActiveTrueAndDeletedFalseOrderByStartDateDesc(
            LocalDate startDate);

    /**
     * Find mappings by created date range for audit purposes.
     * Used for compliance reporting and audit trails.
     *
     * @param startDate the start of date range
     * @param endDate the end of date range
     * @return list of mappings created within the date range
     */
    @Query("SELECT uodm FROM UserOrganisationDomainMapping uodm " +
            "WHERE DATE(uodm.createdDate) BETWEEN :startDate AND :endDate " +
            "AND uodm.deleted = false " +
            "ORDER BY uodm.createdDate DESC")
    List<UserOrganisationDomainMapping> findByCreatedDateBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // ================================
    // DOMAIN-BASED QUERIES FOR ADMIN SERVICE
    // ================================

    /**
     * Find active mappings by domain name across all organizations.
     * Used to get all users with a specific role globally.
     *
     * @param domainName the domain name (e.g., "organisation_user", "admin")
     * @return list of active mappings with the specified domain
     */
    @Query("SELECT uodm FROM UserOrganisationDomainMapping uodm " +
            "JOIN UserDomain ud ON ud.uuid = uodm.domainUuid " +
            "WHERE ud.domainName = :domainName " +
            "AND uodm.active = true " +
            "AND uodm.deleted = false")
    List<UserOrganisationDomainMapping> findActiveByDomainName(@Param("domainName") String domainName);

    /**
     * Count active mappings by domain name across all organizations.
     * Used for admin statistics and reporting.
     *
     * @param domainName the domain name
     * @return count of active mappings with the specified domain
     */
    @Query("SELECT COUNT(uodm) FROM UserOrganisationDomainMapping uodm " +
            "JOIN UserDomain ud ON ud.uuid = uodm.domainUuid " +
            "WHERE ud.domainName = :domainName " +
            "AND uodm.active = true " +
            "AND uodm.deleted = false")
    long countActiveByDomainName(@Param("domainName") String domainName);
}