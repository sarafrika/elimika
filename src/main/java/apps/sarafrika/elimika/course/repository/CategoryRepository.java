package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long>, JpaSpecificationExecutor<Category> {
    Optional<Category> findByUuid(UUID uuid);

    Optional<Category> findByName(String name);

    void deleteByUuid(UUID uuid);

    boolean existsByUuid(UUID uuid);

    Optional<Category> findByParentUuidIsNull();

    Optional<Category> findByParentUuid(UUID uuid);

    Optional<Category> findByIsActiveTrue();

    List<Category> findCategoryHierarchy(UUID categoryUuid);

    long countByParentUuid(UUID categoryUuid);
}