package apps.sarafrika.elimika.coursecreator.repository;

import apps.sarafrika.elimika.coursecreator.model.CourseCreator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseCreatorRepository extends JpaRepository<CourseCreator, Long>, JpaSpecificationExecutor<CourseCreator> {

    Optional<CourseCreator> findByUuid(UUID uuid);

    Optional<CourseCreator> findByUserUuid(UUID userUuid);

    boolean existsByUserUuid(UUID userUuid);

    boolean existsByUuid(UUID uuid);
}