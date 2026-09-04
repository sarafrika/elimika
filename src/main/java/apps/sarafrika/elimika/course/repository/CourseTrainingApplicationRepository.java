package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.CourseTrainingApplication;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicantType;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseTrainingApplicationRepository extends JpaRepository<CourseTrainingApplication, Long>,
        JpaSpecificationExecutor<CourseTrainingApplication> {

    Optional<CourseTrainingApplication> findByUuid(UUID uuid);

    Optional<CourseTrainingApplication> findByCourseUuidAndApplicantTypeAndApplicantUuid(UUID courseUuid,
                                                                                        CourseTrainingApplicantType applicantType,
                                                                                        UUID applicantUuid);

    Optional<CourseTrainingApplication> findByCourseUuidAndApplicantTypeAndApplicantUuidAndStatus(UUID courseUuid,
                                                                                                  CourseTrainingApplicantType applicantType,
                                                                                                  UUID applicantUuid,
                                                                                                  CourseTrainingApplicationStatus status);

    boolean existsByCourseUuidAndApplicantTypeAndApplicantUuidAndStatus(UUID courseUuid,
                                                                        CourseTrainingApplicantType applicantType,
                                                                        UUID applicantUuid,
                                                                        CourseTrainingApplicationStatus status);

    Page<CourseTrainingApplication> findByCourseUuid(UUID courseUuid, Pageable pageable);

    Page<CourseTrainingApplication> findByCourseUuidAndStatus(UUID courseUuid,
                                                              CourseTrainingApplicationStatus status,
                                                              Pageable pageable);

    List<CourseTrainingApplication> findByApplicantUuidAndStatus(UUID applicantUuid,
                                                                 CourseTrainingApplicationStatus status);

    /**
     * Whether the applicant has applied to any course the given creator owns, at any status.
     * <p>
     * Used to decide whether a creator is entitled to look at the applicant's file, which is true
     * from the moment the application is lodged and stays true after a decision so the record of
     * why it was taken remains readable.
     */
    @Query("""
            SELECT COUNT(application) > 0 FROM CourseTrainingApplication application
            JOIN Course course ON course.uuid = application.courseUuid
            WHERE application.applicantType = :applicantType
              AND application.applicantUuid = :applicantUuid
              AND course.courseCreatorUuid = :courseCreatorUuid
            """)
    boolean existsForCourseCreator(@Param("applicantType") CourseTrainingApplicantType applicantType,
                                   @Param("applicantUuid") UUID applicantUuid,
                                   @Param("courseCreatorUuid") UUID courseCreatorUuid);
}
