package apps.sarafrika.elimika.coursecreator.repository;

import apps.sarafrika.elimika.coursecreator.model.CourseCreatorPortfolioItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseCreatorPortfolioItemRepository extends JpaRepository<CourseCreatorPortfolioItem, Long> {

    List<CourseCreatorPortfolioItem> findByCourseCreatorUuidOrderByCreatedDateAsc(UUID courseCreatorUuid);

    Optional<CourseCreatorPortfolioItem> findByUuidAndCourseCreatorUuid(UUID uuid, UUID courseCreatorUuid);

    long countByCourseCreatorUuid(UUID courseCreatorUuid);
}
