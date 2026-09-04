package apps.sarafrika.elimika.availability.service.impl;

import apps.sarafrika.elimika.availability.dto.AvailabilitySlotDTO;
import apps.sarafrika.elimika.availability.model.InstructorAvailability;
import apps.sarafrika.elimika.availability.repository.AvailabilityRepository;
import apps.sarafrika.elimika.shared.enums.AvailabilityType;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A slot is addressed by its own UUID, which says nothing about whose calendar it sits on. The route
 * above these methods is authorised on the instructor in the path, so without the owner being part
 * of the operation an instructor could name themselves in the path and edit or delete a colleague's
 * slot. These cover that: the owner gets through, anyone else is told the slot does not exist, and
 * nothing is written on the way out.
 */
@ExtendWith(MockitoExtension.class)
class AvailabilitySlotOwnershipTest {

    private static final UUID OWNER_UUID = UUID.randomUUID();
    private static final UUID OTHER_INSTRUCTOR_UUID = UUID.randomUUID();
    private static final UUID SLOT_UUID = UUID.randomUUID();

    @Mock private AvailabilityRepository availabilityRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private GenericSpecificationBuilder<InstructorAvailability> specificationBuilder;

    private AvailabilityServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AvailabilityServiceImpl(availabilityRepository, eventPublisher, specificationBuilder);
    }

    @Test
    void theOwningInstructorMayUpdateTheirSlot() {
        InstructorAvailability existing = slotOwnedBy(OWNER_UUID);
        when(availabilityRepository.findByUuid(SLOT_UUID)).thenReturn(Optional.of(existing));
        when(availabilityRepository.save(any(InstructorAvailability.class))).thenAnswer(call -> call.getArgument(0));

        AvailabilitySlotDTO updated = service.updateAvailabilitySlot(OWNER_UUID, SLOT_UUID, request());

        assertThat(updated.instructorUuid()).isEqualTo(OWNER_UUID);
        assertThat(existing.getEndTime()).isEqualTo(LocalTime.of(17, 0));
    }

    @Test
    void anotherInstructorCannotUpdateSomebodyElsesSlot() {
        when(availabilityRepository.findByUuid(SLOT_UUID)).thenReturn(Optional.of(slotOwnedBy(OWNER_UUID)));

        assertThatThrownBy(() -> service.updateAvailabilitySlot(OTHER_INSTRUCTOR_UUID, SLOT_UUID, request()))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(availabilityRepository, never()).save(any());
    }

    @Test
    void theOwningInstructorMayDeleteTheirSlot() {
        InstructorAvailability existing = slotOwnedBy(OWNER_UUID);
        when(availabilityRepository.findByUuid(SLOT_UUID)).thenReturn(Optional.of(existing));

        service.deleteAvailabilitySlot(OWNER_UUID, SLOT_UUID);

        verify(availabilityRepository).delete(existing);
    }

    @Test
    void anotherInstructorCannotDeleteSomebodyElsesSlot() {
        when(availabilityRepository.findByUuid(SLOT_UUID)).thenReturn(Optional.of(slotOwnedBy(OWNER_UUID)));

        assertThatThrownBy(() -> service.deleteAvailabilitySlot(OTHER_INSTRUCTOR_UUID, SLOT_UUID))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(availabilityRepository, never()).delete(any(InstructorAvailability.class));
    }

    @Test
    void aSlotThatDoesNotExistIsIndistinguishableFromOneYouDoNotOwn() {
        when(availabilityRepository.findByUuid(SLOT_UUID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteAvailabilitySlot(OWNER_UUID, SLOT_UUID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(SLOT_UUID.toString());
    }

    @Test
    void theOwningInstructorIsRequired() {
        assertThatThrownBy(() -> service.deleteAvailabilitySlot(null, SLOT_UUID))
                .isInstanceOf(IllegalArgumentException.class);

        verify(availabilityRepository, never()).findByUuid(any());
    }

    private InstructorAvailability slotOwnedBy(UUID instructorUuid) {
        InstructorAvailability entity = new InstructorAvailability();
        entity.setUuid(SLOT_UUID);
        entity.setInstructorUuid(instructorUuid);
        entity.setAvailabilityType(AvailabilityType.WEEKLY);
        entity.setDayOfWeek(1);
        entity.setStartTime(LocalTime.of(9, 0));
        entity.setEndTime(LocalTime.of(12, 0));
        entity.setIsAvailable(Boolean.TRUE);
        entity.setRecurrenceInterval(1);
        return entity;
    }

    private AvailabilitySlotDTO request() {
        return new AvailabilitySlotDTO(
                SLOT_UUID,
                OWNER_UUID,
                AvailabilityType.WEEKLY,
                1,
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
                null
        );
    }
}
