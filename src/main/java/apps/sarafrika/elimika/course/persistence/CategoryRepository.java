package apps.sarafrika.elimika.course.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long>, JpaSpecificationExecutor<Category> {

    List<Category> findByIdIn(List<Long> ids);

    Optional<Category> findByName(String name);
}