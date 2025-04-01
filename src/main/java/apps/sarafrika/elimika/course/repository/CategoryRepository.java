package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, Long>, JpaSpecificationExecutor<Category> {

    List<Category> findByIdIn(List<Long> ids);

    Optional<Category> findByName(String name);

    boolean existsByNameIgnoreCase(String name);

    Optional<Category> findByUuid(UUID uuid);
}