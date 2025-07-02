// TrainingProgramRepository.java
package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.TrainingProgram;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TrainingProgramRepository extends JpaRepository<TrainingProgram, Long>, JpaSpecificationExecutor<TrainingProgram> {
    Optional<TrainingProgram> findByUuid(UUID uuid);
    void deleteByUuid(UUID uuid);
}