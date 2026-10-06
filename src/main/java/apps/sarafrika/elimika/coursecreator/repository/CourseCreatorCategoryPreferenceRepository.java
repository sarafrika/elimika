package apps.sarafrika.elimika.coursecreator.repository;

import apps.sarafrika.elimika.coursecreator.model.CourseCreatorCategoryPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CourseCreatorCategoryPreferenceRepository extends JpaRepository<CourseCreatorCategoryPreference, Long>,
        JpaSpecificationExecutor<CourseCreatorCategoryPreference> {

    List<CourseCreatorCategoryPreference> findByCourseCreatorUuid(UUID courseCreatorUuid);

    long countByCourseCreatorUuid(UUID courseCreatorUuid);

    void deleteByCourseCreatorUuidAndCategoryUuidNotIn(UUID courseCreatorUuid, Collection<UUID> categoryUuids);

    void deleteByCourseCreatorUuid(UUID courseCreatorUuid);

    boolean existsByCourseCreatorUuidAndCategoryUuid(UUID courseCreatorUuid, UUID categoryUuid);
}
