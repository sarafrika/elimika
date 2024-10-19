package apps.sarafrika.elimika.course.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface LearningMaterialRepository extends JpaRepository<LearningMaterial, Long>, JpaSpecificationExecutor<LearningMaterial> {
}
