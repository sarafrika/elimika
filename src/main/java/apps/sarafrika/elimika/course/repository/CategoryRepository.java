package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    List<Category> findByParentUuidIsNull();

    List<Category> findByParentUuid(UUID uuid);

    List<Category> findByIsActiveTrue();

    @Query(value = """
            WITH RECURSIVE category_hierarchy AS (
                -- Base case: start with the given category
                SELECT uuid, name, description, parent_uuid, is_active, 
                       created_date, updated_date, created_by, updated_by, id, 0 as level
                FROM course_categories 
                WHERE uuid = :categoryUuid
                
                UNION ALL
                
                -- Recursive case: find all children
                SELECT c.uuid, c.name, c.description, c.parent_uuid, c.is_active,
                       c.created_date, c.updated_date, c.created_by, c.updated_by, c.id, ch.level + 1
                FROM course_categories c
                INNER JOIN category_hierarchy ch ON c.parent_uuid = ch.uuid
            )
            SELECT * FROM category_hierarchy
            ORDER BY level, name
            """, nativeQuery = true)
    List<Category> findCategoryHierarchy(@Param("categoryUuid") UUID categoryUuid);

    long countByParentUuid(UUID categoryUuid);
}