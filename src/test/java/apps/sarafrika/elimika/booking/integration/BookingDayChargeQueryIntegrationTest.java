package apps.sarafrika.elimika.booking.integration;

import apps.sarafrika.elimika.booking.model.Booking;
import apps.sarafrika.elimika.booking.repository.BookingRepository;
import apps.sarafrika.elimika.shared.enums.BookingStatus;
import apps.sarafrika.elimika.shared.enums.LocationType;
import apps.sarafrika.elimika.shared.enums.SessionFormat;
import apps.sarafrika.elimika.shared.utils.enums.RateBasis;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// The container dies with this class; a cached context must not outlive it.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(BookingDayChargeQueryIntegrationTest.TestConfig.class)
@DisplayName("A per-day booking finds the charged booking already holding its class day")
class BookingDayChargeQueryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @EnableJpaAuditing
    static class TestConfig {
        @Bean
        @Primary
        AuditorAware<String> auditorAware() {
            return () -> Optional.of("integration-test");
        }
    }

    private static final Set<BookingStatus> ACTIVE = EnumSet.of(BookingStatus.PAYMENT_REQUIRED,
            BookingStatus.CONFIRMED, BookingStatus.ACCEPTED, BookingStatus.ACCEPTED_CONFIRMED);
    private static final UUID STUDENT = UUID.randomUUID();
    private static final UUID INSTRUCTOR = UUID.randomUUID();
    private static final UUID COURSE = UUID.randomUUID();

    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("only an active, non-zero booking in the same basis inside the UTC bounds of the local day counts")
    void onlyAnActiveChargedBookingInTheDayCounts() {
        LocalDate utcDay = LocalDate.now(ZoneOffset.UTC).plusDays(7);
        LocalDateTime dayStart = utcDay.atTime(21, 0);
        LocalDateTime dayEnd = utcDay.plusDays(1).atTime(21, 0);

        booking(utcDay.plusDays(1).atTime(6, 0), RateBasis.PER_DAY, BookingStatus.CONFIRMED, "0.00");
        booking(utcDay.atTime(22, 0), RateBasis.PER_DAY, BookingStatus.CANCELLED, "9000.00");
        booking(utcDay.atTime(23, 0), RateBasis.PER_SESSION, BookingStatus.PAYMENT_REQUIRED, "1800.00");
        booking(utcDay.atTime(20, 0), RateBasis.PER_DAY, BookingStatus.PAYMENT_REQUIRED, "9000.00");
        flushAndClear();

        assertThat(exists(dayStart, dayEnd)).as("zero, cancelled, other-basis and previous-day rows").isFalse();

        UUID charged = booking(utcDay.atTime(22, 0), RateBasis.PER_DAY, BookingStatus.ACCEPTED, "9000.00");
        flushAndClear();

        assertThat(exists(dayStart, dayEnd)).isTrue();
        Booking stored = bookingRepository.findByUuid(charged).orElseThrow();
        assertThat(stored.getRateBasis()).isEqualTo(RateBasis.PER_DAY);
        assertThat(stored.getTrainingFormat()).isEqualTo(SessionFormat.INDIVIDUAL);
        assertThat(stored.getDeliveryMode()).isEqualTo(LocationType.HYBRID);
        assertThat(stored.getUnitRate()).isEqualByComparingTo("9000");
    }

    private boolean exists(LocalDateTime from, LocalDateTime to) {
        return bookingRepository.existsChargedBookingStartingBetween(STUDENT, INSTRUCTOR, COURSE, RateBasis.PER_DAY,
                ACTIVE, from, to);
    }

    private UUID booking(LocalDateTime start, RateBasis basis, BookingStatus status, String price) {
        Booking booking = new Booking();
        booking.setStudentUuid(STUDENT);
        booking.setInstructorUuid(INSTRUCTOR);
        booking.setCourseUuid(COURSE);
        booking.setStartTime(start);
        booking.setEndTime(start.plusHours(1));
        booking.setStatus(status);
        booking.setRateBasis(basis);
        booking.setTrainingFormat(SessionFormat.INDIVIDUAL);
        booking.setDeliveryMode(LocationType.HYBRID);
        booking.setUnitRate(new BigDecimal("9000"));
        booking.setPriceAmount(new BigDecimal(price));
        booking.setCurrency("KES");
        return bookingRepository.save(booking).getUuid();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
