package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.CourseTrainingApplication;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicantType;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

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
}
