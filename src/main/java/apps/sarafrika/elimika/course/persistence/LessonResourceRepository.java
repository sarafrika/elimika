package apps.sarafrika.elimika.course.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface LessonResourceRepository extends JpaRepository<LessonResource, Long>, JpaSpecificationExecutor<LessonResource> {

    Optional<LessonResource> findByIdAndLessonId(Long lessonResourceId, Long lessonId);
}
