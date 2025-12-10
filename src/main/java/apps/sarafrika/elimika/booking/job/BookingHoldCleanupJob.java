package apps.sarafrika.elimika.booking.job;

import apps.sarafrika.elimika.booking.spi.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingHoldCleanupJob {

    private final BookingService bookingService;

    @Scheduled(cron = "0 */5 * * * *")
    public void expirePaymentHolds() {
        log.debug("Running booking hold expiration job");
        bookingService.expireHolds();
    }
}
