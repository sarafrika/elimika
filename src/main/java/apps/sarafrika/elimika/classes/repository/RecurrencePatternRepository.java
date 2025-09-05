package apps.sarafrika.elimika.classes.repository;

import apps.sarafrika.elimika.classes.model.RecurrencePattern;
import apps.sarafrika.elimika.classes.util.enums.RecurrenceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecurrencePatternRepository extends JpaRepository<RecurrencePattern, Long> {

    Optional<RecurrencePattern> findByUuid(UUID uuid);

    List<RecurrencePattern> findByRecurrenceType(RecurrenceType recurrenceType);

    @Query("SELECT rp FROM RecurrencePattern rp WHERE rp.endDate IS NULL OR rp.endDate >= :date")
    List<RecurrencePattern> findActivePatterns(@Param("date") LocalDate date);

    @Query("SELECT rp FROM RecurrencePattern rp WHERE rp.recurrenceType = :type AND (rp.endDate IS NULL OR rp.endDate >= :date)")
    List<RecurrencePattern> findActivePatternsOfType(@Param("type") RecurrenceType type, @Param("date") LocalDate date);
}