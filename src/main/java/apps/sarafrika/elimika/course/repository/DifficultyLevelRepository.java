package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.DifficultyLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DifficultyLevelRepository extends JpaRepository<DifficultyLevel, Long>, JpaSpecificationExecutor<DifficultyLevel> {
    Optional<DifficultyLevel> findByUuid(UUID uuid);

    Optional<DifficultyLevel> findByName(String name);

    void deleteByUuid(UUID uuid);

    boolean existsByUuid(UUID uuid);

    List<DifficultyLevel> findAllByOrderByLevelOrderAsc();

    Optional<DifficultyLevel> findByLevelOrder(long levelOrder);
}