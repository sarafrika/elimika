package apps.sarafrika.elimika.booking.repository;

import apps.sarafrika.elimika.booking.model.Booking;
import apps.sarafrika.elimika.shared.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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
}
