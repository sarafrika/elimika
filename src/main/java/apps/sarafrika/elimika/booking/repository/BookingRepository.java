package apps.sarafrika.elimika.booking.repository;

import apps.sarafrika.elimika.booking.model.Booking;
import apps.sarafrika.elimika.shared.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByUuid(UUID uuid);

    List<Booking> findByStatusAndHoldExpiresAtBefore(BookingStatus status, LocalDateTime cutoff);
}
