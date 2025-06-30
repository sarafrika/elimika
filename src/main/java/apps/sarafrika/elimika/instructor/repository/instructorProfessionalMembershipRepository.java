package apps.sarafrika.elimika.instructor.repository;

import apps.sarafrika.elimika.instructor.model.InstructorProfessionalMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface instructorProfessionalMembershipRepository extends JpaRepository<InstructorProfessionalMembership, Long>,
        JpaSpecificationExecutor<InstructorProfessionalMembership> {
}
