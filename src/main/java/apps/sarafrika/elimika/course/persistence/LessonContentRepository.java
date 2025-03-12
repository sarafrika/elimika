package apps.sarafrika.elimika.course.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface LessonContentRepository extends JpaRepository<LessonContent, Long>, JpaSpecificationExecutor<LessonContent> {
}
