package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.CourseTrainingApplication;
import apps.sarafrika.elimika.course.repository.projection.CourseTrainerRateView;
import apps.sarafrika.elimika.course.repository.projection.CourseTrainerView;
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

public interface CourseTrainingApplicationRepository extends JpaRepository<CourseTrainingApplication, Long>,
        JpaSpecificationExecutor<CourseTrainingApplication> {

    /**
     * Whether this instructor has applied to train any course owned by this course creator.
     * <p>
     * Backs the contact-visibility check on the applicant review screens: a creator deciding on an
     * application has to be able to contact the applicant. Status is not filtered — the point of
     * reaching out is usually that the application is still undecided — and the applicant type is,
     * because an organisation applicant's identifier is an organisation, not a person.
     */
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN TRUE ELSE FALSE END " +
           "FROM CourseTrainingApplication a JOIN Course c ON a.courseUuid = c.uuid " +
           "WHERE a.applicantUuid = :instructorUuid " +
           "AND a.applicantType = apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicantType.INSTRUCTOR " +
           "AND c.courseCreatorUuid = :courseCreatorUuid")
    boolean existsForInstructorApplicantAndCourseCreator(@Param("instructorUuid") UUID instructorUuid,
                                                         @Param("courseCreatorUuid") UUID courseCreatorUuid);

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

    /**
     * Whether any of these applicants holds an application on this course at the given status.
     * <p>
     * A caller can be staff of several organisations, so "is my side approved to train this?" is a
     * question about a set rather than a single applicant. Asked as one existence query so a viewer
     * belonging to a dozen schools costs the same as one belonging to a single school.
     * <p>
     * Callers must not pass an empty collection — an empty {@code IN} list is a query the database
     * need never be asked.
     */
    boolean existsByCourseUuidAndApplicantTypeAndApplicantUuidInAndStatus(UUID courseUuid,
                                                                          CourseTrainingApplicantType applicantType,
                                                                          Collection<UUID> applicantUuids,
                                                                          CourseTrainingApplicationStatus status);

    /**
     * Whether any of these applicants has an application on this course at all, whatever its status.
     * <p>
     * Separates "applied and waiting" from "never applied", which are two different pages for the
     * viewer: one is told where their application stands, the other is invited to make one.
     * <p>
     * Callers must not pass an empty collection.
     */
    boolean existsByCourseUuidAndApplicantTypeAndApplicantUuidIn(UUID courseUuid,
                                                                 CourseTrainingApplicantType applicantType,
                                                                 Collection<UUID> applicantUuids);

    /**
     * How many applications a course holds at one status.
     * <p>
     * Counted rather than listed: the public statistics block advertises how many trainers are
     * approved to deliver a course, and it must not become a way to enumerate who they are or what
     * they charge.
     */
    long countByCourseUuidAndStatus(UUID courseUuid, CourseTrainingApplicationStatus status);

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

    /**
     * Courses one set of applicants is approved to train, as bare UUIDs.
     * <p>
     * Authorization asks "which courses may this caller mark?" once per request and then answers by
     * set membership. Loading the identifiers alone keeps that a single projection query instead of
     * an existence check per course, and instantiates no application rows the decision never reads.
     */
    @Query("""
            SELECT a.courseUuid FROM CourseTrainingApplication a
            WHERE a.applicantType = :applicantType
              AND a.applicantUuid IN :applicantUuids
              AND a.status = :status
            """)
    List<UUID> findApprovedCourseUuids(@Param("applicantType") CourseTrainingApplicantType applicantType,
                                       @Param("applicantUuids") Collection<UUID> applicantUuids,
                                       @Param("status") CourseTrainingApplicationStatus status);

    /**
     * The course's trainer directory for a caller who may not see what anyone charges.
     * <p>
     * A projection rather than a filtered entity read: the select list names three columns and no
     * rate column, so the figures are never fetched, never held in a managed entity, and cannot be
     * blanked-but-present in anything downstream. Ordered by approval date so the result is stable
     * before the service applies whatever ordering the caller asked for.
     */
    @Query("""
            SELECT new apps.sarafrika.elimika.course.repository.projection.CourseTrainerView(
                       application.applicantType, application.applicantUuid, application.reviewedAt)
            FROM CourseTrainingApplication application
            WHERE application.courseUuid = :courseUuid
              AND application.status = :status
            ORDER BY application.reviewedAt DESC NULLS LAST, application.applicantUuid ASC
            """)
    List<CourseTrainerView> findTrainerDirectory(@Param("courseUuid") UUID courseUuid,
                                                 @Param("status") CourseTrainingApplicationStatus status);

    /**
     * The same directory with the rate cards attached, for the course owner and platform admins.
     * <p>
     * Deliberately a second query rather than a flag on the first: the caller's entitlement decides
     * which method is invoked, so an unprivileged request has no path that reaches these columns.
     */
    @Query("""
            SELECT new apps.sarafrika.elimika.course.repository.projection.CourseTrainerRateView(
                       application.applicantType, application.applicantUuid, application.reviewedAt,
                       application.rateCurrency,
                       application.privateOnlineHourlyRate, application.privateInpersonHourlyRate,
                       application.groupOnlineHourlyRate, application.groupInpersonHourlyRate,
                       application.privateOnlineSessionRate, application.privateInpersonSessionRate,
                       application.groupOnlineSessionRate, application.groupInpersonSessionRate,
                       application.privateOnlineDailyRate, application.privateInpersonDailyRate,
                       application.groupOnlineDailyRate, application.groupInpersonDailyRate)
            FROM CourseTrainingApplication application
            WHERE application.courseUuid = :courseUuid
              AND application.status = :status
            ORDER BY application.reviewedAt DESC NULLS LAST, application.applicantUuid ASC
            """)
    List<CourseTrainerRateView> findTrainerDirectoryWithRates(@Param("courseUuid") UUID courseUuid,
                                                              @Param("status") CourseTrainingApplicationStatus status);

    /**
     * How many applications on this course are still awaiting a decision. Answered by a count so the
     * pending rows — which carry the rate cards their applicants proposed — are never loaded.
     */
    long countByCourseUuidAndStatus(UUID courseUuid, CourseTrainingApplicationStatus status);
}
