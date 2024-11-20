package apps.sarafrika.elimika.course.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseLearningObjectiveRepository extends JpaRepository<CourseLearningObjective, Long> {
    List<CourseLearningObjective> findAllByCourseId(Long courseId);

    List<CourseLearningObjective> findByIdIn(List<Long> ids);
}
