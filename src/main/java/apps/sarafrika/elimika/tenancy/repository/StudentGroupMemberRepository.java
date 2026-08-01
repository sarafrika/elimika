package apps.sarafrika.elimika.tenancy.repository;

import apps.sarafrika.elimika.tenancy.dto.StudentGroupRosterEntryDTO;
import apps.sarafrika.elimika.tenancy.entity.StudentGroupMember;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StudentGroupMemberRepository extends JpaRepository<StudentGroupMember, Long> {

    List<StudentGroupMember> findByGroupUuid(UUID groupUuid);

    long countByGroupUuid(UUID groupUuid);

    boolean existsByGroupUuidAndStudentUuid(UUID groupUuid, UUID studentUuid);

    void deleteByGroupUuidAndStudentUuid(UUID groupUuid, UUID studentUuid);

    /** Member counts for a set of groups, as [groupUuid, count] rows. */
    @Query("SELECT m.groupUuid, COUNT(m) FROM StudentGroupMember m WHERE m.groupUuid IN :groupUuids GROUP BY m.groupUuid")
    List<Object[]> countByGroupUuids(@Param("groupUuids") List<UUID> groupUuids);

    /**
     * The organisation student roster, one page at a time.
     * <p>
     * Everything the Groups table shows for a student — identity, contact details, the group they
     * sit in and that group's tier and stream label — is assembled here in one query. Fetching it
     * per group instead means one request per group plus batched user lookups on top, with no
     * server-side paging, against a screen that is explicitly a paginated table.
     * <p>
     * {@code studentUuid} holds a {@code users.uuid} despite its name, which is why it joins
     * straight to {@code User}. The join is inner: a membership row pointing at a user that no
     * longer exists has nothing to render and is dropped rather than returned half-empty.
     * <p>
     * The count query is spelled out because Spring Data cannot derive one from a constructor
     * expression.
     */
    @Query(value = """
            SELECT new apps.sarafrika.elimika.tenancy.dto.StudentGroupRosterEntryDTO(
                       m.studentUuid,
                       g.uuid,
                       g.name,
                       t.name,
                       g.groupType,
                       TRIM(CONCAT(COALESCE(u.firstName, ''), ' ', COALESCE(u.lastName, ''))),
                       u.email,
                       u.phoneNumber,
                       u.dob,
                       u.profileImageUrl,
                       m.createdDate)
            FROM StudentGroupMember m
            JOIN StudentGroup g ON g.uuid = m.groupUuid
            JOIN User u ON u.uuid = m.studentUuid
            LEFT JOIN AcademicTier t ON t.uuid = g.tierUuid
            WHERE g.organisationUuid = :organisationUuid
              AND (:branchUuid IS NULL OR g.branchUuid = :branchUuid)
              AND (:tierUuid IS NULL OR g.tierUuid = :tierUuid)
              AND (:groupUuid IS NULL OR m.groupUuid = :groupUuid)
            ORDER BY t.tierOrder ASC NULLS LAST, g.name ASC, u.firstName ASC, u.lastName ASC, m.uuid ASC
            """,
            countQuery = """
            SELECT COUNT(m)
            FROM StudentGroupMember m
            JOIN StudentGroup g ON g.uuid = m.groupUuid
            JOIN User u ON u.uuid = m.studentUuid
            WHERE g.organisationUuid = :organisationUuid
              AND (:branchUuid IS NULL OR g.branchUuid = :branchUuid)
              AND (:tierUuid IS NULL OR g.tierUuid = :tierUuid)
              AND (:groupUuid IS NULL OR m.groupUuid = :groupUuid)
            """)
    Page<StudentGroupRosterEntryDTO> findRoster(@Param("organisationUuid") UUID organisationUuid,
                                                @Param("branchUuid") UUID branchUuid,
                                                @Param("tierUuid") UUID tierUuid,
                                                @Param("groupUuid") UUID groupUuid,
                                                Pageable pageable);
}
