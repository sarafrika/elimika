package apps.sarafrika.elimika.coursecreator.repository;

import apps.sarafrika.elimika.coursecreator.model.CourseCreatorCompetency;
import apps.sarafrika.elimika.profile.spi.WalletVerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseCreatorCompetencyRepository extends JpaRepository<CourseCreatorCompetency, Long> {

    List<CourseCreatorCompetency> findByCourseCreatorUuidOrderByCreatedDateAsc(UUID courseCreatorUuid);

    Optional<CourseCreatorCompetency> findByUuidAndCourseCreatorUuid(UUID uuid, UUID courseCreatorUuid);

    long countByCourseCreatorUuid(UUID courseCreatorUuid);

    boolean existsByCourseCreatorUuidAndVerificationStatus(UUID courseCreatorUuid, WalletVerificationStatus status);
}
