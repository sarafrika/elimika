package apps.sarafrika.elimika.assessment.persistence;

import apps.sarafrika.elimika.assessment.dto.response.AssessmentResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface AssessmentRepository extends JpaRepository<Assessment, Long> {

    Optional<Assessment> findByLessonId(Long lessonId);

    Page<Assessment> findAllByLessonId(Long lessonId, Pageable pageable);

    Page<Assessment> findAllByCourseId(Long courseId, Pageable pageable);
}
