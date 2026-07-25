package apps.sarafrika.elimika.instructor.repository;

import apps.sarafrika.elimika.instructor.model.Instructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface InstructorRepository extends JpaRepository<Instructor, Long>, JpaSpecificationExecutor<Instructor> {

    /**
     * Aggregated instructor directory rows for a single organisation.
     * <p>
     * Joins the tenancy org-domain mapping (active, non-deleted, domain = 'instructor')
     * to the instructor profile, then LEFT JOINs the most recent qualification, a
     * representative skill, aggregate review metrics, and the class-definition count.
     * Column order matches {@code apps.sarafrika.elimika.instructor.dto.OrgInstructorSummaryDTO}.
     */
    @Query(value = """
            SELECT
                i.user_uuid                        AS user_uuid,
                i.uuid                             AS instructor_uuid,
                i.full_name                        AS full_name,
                u.email                            AS email,
                edu.qualification                  AS highest_qualification,
                edu.field_of_study                 AS field_of_study,
                sk.skill_name                      AS top_skill,
                rv.avg_rating                      AS average_rating,
                COALESCE(rv.review_count, 0)       AS review_count,
                COALESCE(cd.class_count, 0)        AS class_count
            FROM user_organisation_domain_mapping m
            JOIN user_domain d
                ON d.uuid = m.domain_uuid AND d.domain_name = 'instructor'
            JOIN instructors i
                ON i.user_uuid = m.user_uuid
            LEFT JOIN users u
                ON u.uuid = i.user_uuid
            LEFT JOIN LATERAL (
                SELECT e.qualification, e.field_of_study
                FROM instructor_education e
                WHERE e.instructor_uuid = i.uuid
                ORDER BY e.year_completed DESC NULLS LAST
                LIMIT 1
            ) edu ON true
            LEFT JOIN LATERAL (
                SELECT s.skill_name
                FROM instructor_skills s
                WHERE s.instructor_uuid = i.uuid
                LIMIT 1
            ) sk ON true
            LEFT JOIN (
                SELECT r.instructor_uuid, AVG(r.rating) AS avg_rating, COUNT(*) AS review_count
                FROM instructor_reviews r
                GROUP BY r.instructor_uuid
            ) rv ON rv.instructor_uuid = i.uuid
            LEFT JOIN (
                SELECT c.default_instructor_uuid, COUNT(*) AS class_count
                FROM class_definitions c
                GROUP BY c.default_instructor_uuid
            ) cd ON cd.default_instructor_uuid = i.uuid
            WHERE m.organisation_uuid = :organisationUuid
                AND m.active = true
                AND m.deleted = false
            ORDER BY i.full_name ASC
            """, nativeQuery = true)
    List<Object[]> findInstructorSummariesForOrganisation(@Param("organisationUuid") UUID organisationUuid);

    Set<Instructor> findByIdIn(Set<Long> ids);

    Optional<Instructor> findByUuid(UUID uuid);

    void deleteByUuid(UUID uuid);

    boolean existsByUuid(UUID uuid);

    boolean existsByUserUuid(UUID userUuid);

    Optional<Instructor> findByUserUuid(UUID userUuid);

    /**
     * Find instructors by their verification status with pagination.
     *
     * @param adminVerified the verification status to filter by
     * @param pageable pagination information
     * @return paginated list of instructors with the specified verification status
     */
    Page<Instructor> findByAdminVerified(Boolean adminVerified, Pageable pageable);

    /**
     * Count instructors by their verification status.
     *
     * @param adminVerified the verification status to count
     * @return number of instructors with the specified verification status
     */
    long countByAdminVerified(Boolean adminVerified);
}
