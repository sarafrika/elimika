package apps.sarafrika.elimika.timetabling.repository;

import apps.sarafrika.elimika.timetabling.model.InstructorTimeHold;
import apps.sarafrika.elimika.timetabling.spi.InstructorTimeHoldStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface InstructorTimeHoldRepository extends JpaRepository<InstructorTimeHold, Long>,
        JpaSpecificationExecutor<InstructorTimeHold> {

    List<InstructorTimeHold> findByApplicationUuidAndStatusIn(UUID applicationUuid,
                                                              Collection<InstructorTimeHoldStatus> statuses);

    List<InstructorTimeHold> findByJobUuidAndStatusIn(UUID jobUuid,
                                                      Collection<InstructorTimeHoldStatus> statuses);

    /**
     * Holds in the given states overlapping the window. Strict inequalities so back-to-back
     * sessions sharing a boundary do not collide.
     */
    @Query("""
            SELECT h FROM InstructorTimeHold h
            WHERE h.instructorUuid = :instructorUuid
              AND h.status IN :statuses
              AND h.startTime < :endTime
              AND h.endTime > :startTime
            ORDER BY h.startTime
            """)
    List<InstructorTimeHold> findOverlappingHolds(@Param("instructorUuid") UUID instructorUuid,
                                                  @Param("statuses") Collection<InstructorTimeHoldStatus> statuses,
                                                  @Param("startTime") LocalDateTime startTime,
                                                  @Param("endTime") LocalDateTime endTime);
}
