package apps.sarafrika.elimika.coursecreator.repository;

import apps.sarafrika.elimika.coursecreator.model.CourseCreatorProfessionalMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface CourseCreatorProfessionalMembershipRepository extends JpaRepository<CourseCreatorProfessionalMembership, Long>,
        JpaSpecificationExecutor<CourseCreatorProfessionalMembership> {

    Optional<CourseCreatorProfessionalMembership> findByUuid(UUID uuid);

    boolean existsByUuid(UUID uuid);

    void deleteByUuid(UUID uuid);
}
