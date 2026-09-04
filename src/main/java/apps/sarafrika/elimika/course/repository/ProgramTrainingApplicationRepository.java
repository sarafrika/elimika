package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.ProgramTrainingApplication;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicantType;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProgramTrainingApplicationRepository extends JpaRepository<ProgramTrainingApplication, Long>,
        JpaSpecificationExecutor<ProgramTrainingApplication> {

    /**
     * Whether this instructor has applied to train any programme owned by this course creator. The
     * programme-shaped twin of
     * {@code CourseTrainingApplicationRepository#existsForInstructorApplicantAndCourseCreator}.
     */
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN TRUE ELSE FALSE END " +
           "FROM ProgramTrainingApplication a JOIN TrainingProgram tp ON a.programUuid = tp.uuid " +
           "WHERE a.applicantUuid = :instructorUuid " +
           "AND a.applicantType = apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicantType.INSTRUCTOR " +
           "AND tp.courseCreatorUuid = :courseCreatorUuid")
    boolean existsForInstructorApplicantAndCourseCreator(@Param("instructorUuid") UUID instructorUuid,
                                                         @Param("courseCreatorUuid") UUID courseCreatorUuid);

    Optional<ProgramTrainingApplication> findByUuid(UUID uuid);

    Optional<ProgramTrainingApplication> findByProgramUuidAndApplicantTypeAndApplicantUuid(UUID programUuid,
                                                                                          CourseTrainingApplicantType applicantType,
                                                                                          UUID applicantUuid);

    Optional<ProgramTrainingApplication> findByProgramUuidAndApplicantTypeAndApplicantUuidAndStatus(UUID programUuid,
                                                                                                    CourseTrainingApplicantType applicantType,
                                                                                                    UUID applicantUuid,
                                                                                                    CourseTrainingApplicationStatus status);

    boolean existsByProgramUuidAndApplicantTypeAndApplicantUuidAndStatus(UUID programUuid,
                                                                         CourseTrainingApplicantType applicantType,
                                                                         UUID applicantUuid,
                                                                         CourseTrainingApplicationStatus status);

    Page<ProgramTrainingApplication> findByProgramUuid(UUID programUuid, Pageable pageable);

    Page<ProgramTrainingApplication> findByProgramUuidAndStatus(UUID programUuid,
                                                                CourseTrainingApplicationStatus status,
                                                                Pageable pageable);

    /**
     * The programme counterpart of
     * {@link CourseTrainingApplicationRepository#existsForCourseCreator}: whether the applicant has
     * applied to any training programme the given creator owns, at any status.
     */
    @Query("""
            SELECT COUNT(application) > 0 FROM ProgramTrainingApplication application
            JOIN TrainingProgram program ON program.uuid = application.programUuid
            WHERE application.applicantType = :applicantType
              AND application.applicantUuid = :applicantUuid
              AND program.courseCreatorUuid = :courseCreatorUuid
            """)
    boolean existsForCourseCreator(@Param("applicantType") CourseTrainingApplicantType applicantType,
                                   @Param("applicantUuid") UUID applicantUuid,
                                   @Param("courseCreatorUuid") UUID courseCreatorUuid);

    /**
     * Programs one set of applicants is approved to deliver, as bare UUIDs.
     * <p>
     * A class may be scheduled against a program rather than a course, and an instructor approved
     * that way holds no {@code course_training_applications} row for the courses inside it. This is
     * the first hop of resolving such an approval to the courses it actually covers.
     */
    @Query("""
            SELECT a.programUuid FROM ProgramTrainingApplication a
            WHERE a.applicantType = :applicantType
              AND a.applicantUuid IN :applicantUuids
              AND a.status = :status
            """)
    List<UUID> findApprovedProgramUuids(@Param("applicantType") CourseTrainingApplicantType applicantType,
                                        @Param("applicantUuids") Collection<UUID> applicantUuids,
                                        @Param("status") CourseTrainingApplicationStatus status);
}
