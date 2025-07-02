package apps.sarafrika.elimika.instructor.repository;

import apps.sarafrika.elimika.instructor.model.InstructorEducation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InstructorEducationRepository extends JpaRepository<InstructorEducation,Long>,
        JpaSpecificationExecutor<InstructorEducation> {

    Optional<InstructorEducation> findByUuid(UUID uuid);

    boolean existsByUuid(UUID uuid);

    void deleteByUuid(UUID uuid);

    List<InstructorEducation> findByInstructorUuid(UUID instructorUuid);

    List<InstructorEducation> findByQualificationContainingIgnoreCase(String qualification);
}
