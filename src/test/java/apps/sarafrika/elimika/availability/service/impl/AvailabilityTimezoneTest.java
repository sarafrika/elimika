package apps.sarafrika.elimika.availability.service.impl;

import apps.sarafrika.elimika.availability.dto.AvailabilitySlotDTO;
import apps.sarafrika.elimika.availability.model.InstructorAvailability;
import apps.sarafrika.elimika.availability.repository.AvailabilityRepository;
import apps.sarafrika.elimika.shared.enums.AvailabilityType;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A slot stores the wall clock the instructor typed, which is only a moment in time once the zone it
 * was typed in is applied. Read as UTC, a 09:00 Nairobi block was enforced from noon to eight in the
 * evening: classes refused in windows she never blocked, and accepted in the ones she did.
 * <p>
 * Every bound here is built in UTC on a fixed date, because a check that leans on the clock the test
 * happens to run under is the same mistake in a different place.
 */
@ExtendWith(MockitoExtension.class)
class AvailabilityTimezoneTest {

    private static final UUID INSTRUCTOR_UUID = UUID.randomUUID();
    private static final LocalDate MONDAY = LocalDate.of(2026, 3, 2);
    private static final String NAIROBI = "Africa/Nairobi";

    @Mock private AvailabilityRepository availabilityRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private GenericSpecificationBuilder<InstructorAvailability> specificationBuilder;

    private AvailabilityServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AvailabilityServiceImpl(availabilityRepository, eventPublisher, specificationBuilder);
    }

    @Test
    void theWallClockIsStoredUnshiftedAlongsideTheZoneItWasWrittenIn() {
        when(availabilityRepository.save(any(InstructorAvailability.class))).thenAnswer(call -> call.getArgument(0));

        service.createAvailabilitySlot(nairobiRequest());

        ArgumentCaptor<InstructorAvailability> captor = ArgumentCaptor.forClass(InstructorAvailability.class);
        verify(availabilityRepository).save(captor.capture());
        assertThat(captor.getValue().getStartTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(captor.getValue().getEndTime()).isEqualTo(LocalTime.of(17, 0));
        assertThat(captor.getValue().getTimezone()).isEqualTo(NAIROBI);
    }

    @Test
    void aSlotSavedWithoutAZoneIsStoredAsUtc() {
        when(availabilityRepository.save(any(InstructorAvailability.class))).thenAnswer(call -> call.getArgument(0));

        AvailabilitySlotDTO created = service.createAvailabilitySlot(request(null));

        assertThat(created.timezone()).isEqualTo("UTC");
    }

    @Test
    void aZoneThatIsNotARealPlaceIsRefused() {
        assertThatThrownBy(() -> service.createAvailabilitySlot(request("Africa/Nowhere")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid IANA timezone");

        verify(availabilityRepository, never()).save(any(InstructorAvailability.class));
    }

    @Test
    void nineInTheMorningUtcFallsInsideANairobiBlockBecauseItIsNoonThere() {
        givenBlockedSlot(NAIROBI);

        assertThat(isAvailable(MONDAY.atTime(9, 0), MONDAY.atTime(10, 0))).isFalse();
    }

    @Test
    void sixInTheMorningUtcIsNineInNairobiAndTheBlockStartsThere() {
        // Read as UTC this window looked free, so a class was booked over the block she had set.
        givenBlockedSlot(NAIROBI);

        assertThat(isAvailable(MONDAY.atTime(6, 0), MONDAY.atTime(7, 0))).isFalse();
    }

    @Test
    void anEveningInNairobiIsFreeEvenThoughItReadsAsInsideTheBlockInUtc() {
        // 15:00Z is 18:00 in Nairobi, an hour after the block ends; it used to be refused.
        givenBlockedSlot(NAIROBI);

        assertThat(isAvailable(MONDAY.atTime(15, 0), MONDAY.atTime(16, 0))).isTrue();
    }

    @Test
    void aWindowOnTheUtcDayBeforeIsStillTheBlockedWeekdayWhereTheInstructorIs() {
        // 21:00 Sunday UTC is 10:00 Monday in Auckland, so the Monday block she set covers it.
        givenBlockedSlot("Pacific/Auckland");

        assertThat(isAvailable(MONDAY.minusDays(1).atTime(21, 0), MONDAY.minusDays(1).atTime(22, 0))).isFalse();
    }

    @Test
    void anAfternoonBehindUtcIsBlockedThoughTheClockReadsPastTheBlockInUtc() {
        // 20:00Z is 15:00 in New York, mid-block; read as UTC it looked like evening and was booked.
        givenBlockedSlot("America/New_York");

        assertThat(isAvailable(MONDAY.atTime(20, 0), MONDAY.atTime(21, 0))).isFalse();
    }

    @Test
    void aUtcSlotDecidesExactlyWhatItDecidedBefore() {
        givenBlockedSlot("UTC");

        assertThat(isAvailable(MONDAY.atTime(9, 0), MONDAY.atTime(10, 0))).isFalse();
        assertThat(isAvailable(MONDAY.atTime(6, 0), MONDAY.atTime(7, 0))).isTrue();
    }

    @Test
    void aSlotRecordedBeforeZonesWereCapturedIsReadAsUtc() {
        givenBlockedSlot(null);

        assertThat(isAvailable(MONDAY.atTime(9, 0), MONDAY.atTime(10, 0))).isFalse();
        assertThat(isAvailable(MONDAY.atTime(6, 0), MONDAY.atTime(7, 0))).isTrue();
    }

    @Test
    void aWindowNoSlotTouchesIsStillOpen() {
        givenBlockedSlot(NAIROBI);

        assertThat(isAvailable(MONDAY.atTime(20, 0), MONDAY.atTime(21, 0))).isTrue();
    }

    private boolean isAvailable(LocalDateTime startUtc, LocalDateTime endUtc) {
        return service.isInstructorAvailable(INSTRUCTOR_UUID, startUtc, endUtc);
    }

    private void givenBlockedSlot(String timezone) {
        when(availabilityRepository.findEffectiveAvailabilityBetween(eq(INSTRUCTOR_UUID), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(blockedSlot(timezone)));
    }

    private InstructorAvailability blockedSlot(String timezone) {
        InstructorAvailability entity = new InstructorAvailability();
        entity.setUuid(UUID.randomUUID());
        entity.setInstructorUuid(INSTRUCTOR_UUID);
        entity.setAvailabilityType(AvailabilityType.WEEKLY);
        entity.setDayOfWeek(MONDAY.getDayOfWeek().getValue());
        entity.setStartTime(LocalTime.of(9, 0));
        entity.setEndTime(LocalTime.of(17, 0));
        entity.setTimezone(timezone);
        entity.setIsAvailable(Boolean.FALSE);
        entity.setRecurrenceInterval(1);
        return entity;
    }

    private AvailabilitySlotDTO nairobiRequest() {
        return request(NAIROBI);
    }

    private AvailabilitySlotDTO request(String timezone) {
        return new AvailabilitySlotDTO(
                null,
                INSTRUCTOR_UUID,
                AvailabilityType.WEEKLY,
                MONDAY.getDayOfWeek().getValue(),
                null,
                null,
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                null,
                Boolean.TRUE,
                1,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                timezone
        );
    }
}
