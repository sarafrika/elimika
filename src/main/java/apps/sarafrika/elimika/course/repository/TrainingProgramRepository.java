// TrainingProgramRepository.java
package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.TrainingProgram;
import apps.sarafrika.elimika.course.util.enums.ContentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TrainingProgramRepository extends JpaRepository<TrainingProgram, Long>, JpaSpecificationExecutor<TrainingProgram> {
    Optional<TrainingProgram> findByUuid(UUID uuid);

    void deleteByUuid(UUID uuid);

    boolean existsByUuid(UUID uuid);

    List<TrainingProgram> findByActiveTrue();

    List<TrainingProgram> findByCategoryUuid(UUID categoryUuid);

    List<TrainingProgram> findByCourseCreatorUuid(UUID courseCreatorUuid);

    @Query("select p.uuid from TrainingProgram p where p.courseCreatorUuid = :courseCreatorUuid")
    List<UUID> findUuidsByCourseCreatorUuid(@Param("courseCreatorUuid") UUID courseCreatorUuid);

    List<TrainingProgram> findByStatus(ContentStatus status);

    List<TrainingProgram> findByPriceIsNullOrPrice(BigDecimal price);

    List<TrainingProgram> findByTotalDurationHoursGreaterThanEqual(long totalDurationHours);

    List<TrainingProgram> findByTotalDurationHoursBetween(long startHours, long endHours);

    long countByActiveTrue();

    long countByIsPublishedTrue();

    long countByStatus(ContentStatus status);
}
