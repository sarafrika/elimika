package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.Prerequisite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PrerequisiteRepository extends JpaRepository<Prerequisite, Long>, JpaSpecificationExecutor<Prerequisite> {
}
