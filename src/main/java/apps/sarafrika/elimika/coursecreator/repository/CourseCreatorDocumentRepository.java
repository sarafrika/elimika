package apps.sarafrika.elimika.coursecreator.repository;

import apps.sarafrika.elimika.coursecreator.model.CourseCreatorDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseCreatorDocumentRepository extends JpaRepository<CourseCreatorDocument, Long>,
        JpaSpecificationExecutor<CourseCreatorDocument> {

    Optional<CourseCreatorDocument> findByUuid(UUID uuid);

    List<CourseCreatorDocument> findByCourseCreatorUuid(UUID courseCreatorUuid);

    boolean existsByUuid(UUID uuid);

    boolean existsByEducationUuidAndCourseCreatorUuidAndDocumentTypeUuid(UUID educationUuid,
                                                                         UUID courseCreatorUuid,
                                                                         UUID documentTypeUuid);

    void deleteByUuid(UUID uuid);
}
