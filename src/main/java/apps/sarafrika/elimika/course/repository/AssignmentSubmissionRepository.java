package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.AssignmentSubmission;
import apps.sarafrika.elimika.course.util.enums.SubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, Long>,
        JpaSpecificationExecutor<AssignmentSubmission> {

    // ===== BASIC CRUD OPERATIONS =====

    Optional<AssignmentSubmission> findByUuid(UUID uuid);

    boolean existsByUuid(UUID uuid);

    void deleteByUuid(UUID uuid);

    // ===== SUBMISSION WORKFLOW QUERIES =====

    /**
     * Find submission by enrollment and assignment
     * Used to check if student already submitted for this assignment
     */
    Optional<AssignmentSubmission> findByEnrollmentUuidAndAssignmentUuid(UUID enrollmentUuid, UUID assignmentUuid);

    /**
     * Find all submissions for a specific assignment
     */
    List<AssignmentSubmission> findByAssignmentUuid(UUID assignmentUuid);

    /**
     * Find submissions by assignment and status
     */
    List<AssignmentSubmission> findByAssignmentUuidAndStatus(UUID assignmentUuid, SubmissionStatus status);

    /**
     * Find submissions by status (for pending grading)
     */
    List<AssignmentSubmission> findByStatus(SubmissionStatus status);

    /**
     * Find submissions by enrollment UUID (student's submissions)
     */
    List<AssignmentSubmission> findByEnrollmentUuid(UUID enrollmentUuid);

    // ===== ANALYTICS QUERIES =====

    /**
     * Count submissions by assignment and status
     */
    @Query("SELECT COUNT(s) FROM AssignmentSubmission s WHERE s.assignmentUuid = :assignmentUuid AND s.status = :status")
    long countByAssignmentUuidAndStatus(@Param("assignmentUuid") UUID assignmentUuid, @Param("status") SubmissionStatus status);

    /**
     * Get average percentage for graded submissions of an assignment
     */
    @Query("SELECT AVG(s.percentage) FROM AssignmentSubmission s WHERE s.assignmentUuid = :assignmentUuid AND s.status = 'GRADED' AND s.percentage IS NOT NULL")
    Double getAveragePercentageByAssignmentUuid(@Param("assignmentUuid") UUID assignmentUuid);

    /**
     * Find high performance submissions (above threshold percentage)
     */
    @Query("SELECT s FROM AssignmentSubmission s WHERE s.assignmentUuid = :assignmentUuid AND s.status = 'GRADED' AND s.percentage >= :threshold")
    List<AssignmentSubmission> findHighPerformanceSubmissions(@Param("assignmentUuid") UUID assignmentUuid, @Param("threshold") Double threshold);

    // ===== INSTRUCTOR WORKFLOW QUERIES =====

    /**
     * Find submissions pending grading for assignments created by specific instructor
     * This would typically require a join with Assignment table
     */
    @Query("""
        SELECT s FROM AssignmentSubmission s 
        WHERE s.status = 'SUBMITTED' 
        AND s.assignmentUuid IN (
            SELECT a.uuid FROM Assignment a WHERE a.createdBy = :instructorUuid
        )
        ORDER BY s.submittedAt ASC
        """)
    List<AssignmentSubmission> findPendingGradingByInstructor(@Param("instructorUuid") UUID instructorUuid);

    /**
     * Find all submissions graded by specific instructor
     */
    List<AssignmentSubmission> findByGradedByUuid(UUID gradedByUuid);

    // ===== REPORTING QUERIES =====

    /**
     * Get submission count by status for an assignment
     */
    @Query("""
        SELECT s.status, COUNT(s) 
        FROM AssignmentSubmission s 
        WHERE s.assignmentUuid = :assignmentUuid 
        GROUP BY s.status
        """)
    List<Object[]> getSubmissionCountByStatus(@Param("assignmentUuid") UUID assignmentUuid);

    /**
     * Find overdue submissions (submitted after due date)
     */
    @Query("""
        SELECT s FROM AssignmentSubmission s 
        JOIN Assignment a ON s.assignmentUuid = a.uuid 
        WHERE s.submittedAt > a.dueDate 
        AND a.uuid = :assignmentUuid
        """)
    List<AssignmentSubmission> findOverdueSubmissions(@Param("assignmentUuid") UUID assignmentUuid);

    /**
     * Get submission statistics for an assignment
     */
    @Query("""
        SELECT 
            COUNT(s) as totalSubmissions,
            COUNT(CASE WHEN s.status = 'GRADED' THEN 1 END) as gradedSubmissions,
            COUNT(CASE WHEN s.status = 'SUBMITTED' THEN 1 END) as pendingSubmissions,
            AVG(CASE WHEN s.percentage IS NOT NULL THEN s.percentage END) as averagePercentage,
            MAX(s.percentage) as maxPercentage,
            MIN(s.percentage) as minPercentage
        FROM AssignmentSubmission s 
        WHERE s.assignmentUuid = :assignmentUuid
        """)
    Object[] getSubmissionStatistics(@Param("assignmentUuid") UUID assignmentUuid);
}