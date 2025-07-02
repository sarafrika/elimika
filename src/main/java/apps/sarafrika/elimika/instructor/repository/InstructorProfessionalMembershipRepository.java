package apps.sarafrika.elimika.instructor.repository;

import apps.sarafrika.elimika.instructor.model.InstructorProfessionalMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface InstructorProfessionalMembershipRepository extends JpaRepository<InstructorProfessionalMembership, Long>,
        JpaSpecificationExecutor<InstructorProfessionalMembership> {

    Optional<InstructorProfessionalMembership> findByUuid(UUID uuid);

    boolean existsByUuid(UUID uuid);

    void deleteByUuid(UUID uuid);

}
