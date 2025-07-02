package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.ContentProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContentProgressRepository extends JpaRepository<ContentProgress, Long>, JpaSpecificationExecutor<ContentProgress> {
    Optional<ContentProgress> findByUuid(UUID uuid);

    void deleteByUuid(UUID uuid);
}
