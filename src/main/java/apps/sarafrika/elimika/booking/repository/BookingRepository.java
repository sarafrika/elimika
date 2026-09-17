package apps.sarafrika.elimika.booking.repository;

import apps.sarafrika.elimika.booking.model.Booking;
import apps.sarafrika.elimika.shared.enums.BookingStatus;
import apps.sarafrika.elimika.shared.utils.enums.RateBasis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByUuid(UUID uuid);

    List<Booking> findByStatusAndHoldExpiresAtBefore(BookingStatus status, LocalDateTime cutoff);

    Page<Booking> findByStudentUuid(UUID studentUuid, Pageable pageable);

    Page<Booking> findByStudentUuidAndStatus(UUID studentUuid, BookingStatus status, Pageable pageable);

    Page<Booking> findByInstructorUuid(UUID instructorUuid, Pageable pageable);

    Page<Booking> findByInstructorUuidAndStatus(UUID instructorUuid, BookingStatus status, Pageable pageable);

    /** Whether the student holds a booking with a non-zero charge in this basis, instructor and course starting in [from, to). */
    @Query("""
            SELECT COUNT(b) > 0 FROM Booking b
            WHERE b.studentUuid = :studentUuid
              AND b.instructorUuid = :instructorUuid
              AND b.courseUuid = :courseUuid
              AND b.rateBasis = :rateBasis
              AND b.status IN :statuses
              AND b.priceAmount > 0
              AND b.startTime >= :from
              AND b.startTime < :to
            """)
    boolean existsChargedBookingStartingBetween(@Param("studentUuid") UUID studentUuid,
                                                @Param("instructorUuid") UUID instructorUuid,
                                                @Param("courseUuid") UUID courseUuid,
                                                @Param("rateBasis") RateBasis rateBasis,
                                                @Param("statuses") Collection<BookingStatus> statuses,
                                                @Param("from") LocalDateTime from,
                                                @Param("to") LocalDateTime to);
}
