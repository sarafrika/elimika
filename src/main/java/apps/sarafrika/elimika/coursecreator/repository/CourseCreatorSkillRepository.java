package apps.sarafrika.elimika.coursecreator.repository;

import apps.sarafrika.elimika.coursecreator.model.CourseCreatorSkill;
import apps.sarafrika.elimika.profile.spi.WalletVerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface CourseCreatorSkillRepository extends JpaRepository<CourseCreatorSkill, Long>,
        JpaSpecificationExecutor<CourseCreatorSkill> {

    Optional<CourseCreatorSkill> findByUuid(UUID uuid);

    long countByCourseCreatorUuid(UUID courseCreatorUuid);

    boolean existsByUuid(UUID uuid);

    void deleteByUuid(UUID uuid);

    boolean existsByCourseCreatorUuidAndVerificationStatus(UUID courseCreatorUuid, WalletVerificationStatus status);
}
