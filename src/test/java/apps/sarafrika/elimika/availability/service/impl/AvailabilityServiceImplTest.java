package apps.sarafrika.elimika.availability.service.impl;

import apps.sarafrika.elimika.availability.dto.BlockedTimeSlotRequestDTO;
import apps.sarafrika.elimika.availability.model.InstructorAvailability;
import apps.sarafrika.elimika.availability.repository.AvailabilityRepository;
import apps.sarafrika.elimika.shared.event.availability.InstructorAvailabilityChangedEventDTO;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceImplTest {

    @Mock
    private AvailabilityRepository availabilityRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private GenericSpecificationBuilder<InstructorAvailability> specificationBuilder;

    @InjectMocks
    private AvailabilityServiceImpl availabilityService;

    @Test
    void blockTimeSlots_savesAllSlotsAndPublishesEvent() {
        UUID instructorUuid = UUID.randomUUID();
        List<BlockedTimeSlotRequestDTO> slots = List.of(
                new BlockedTimeSlotRequestDTO(
                        LocalDateTime.of(2024, 10, 15, 9, 0),
                        LocalDateTime.of(2024, 10, 15, 10, 0),
                        "#FF6B6B"
                ),
                new BlockedTimeSlotRequestDTO(
                        LocalDateTime.of(2024, 10, 16, 11, 0),
                        LocalDateTime.of(2024, 10, 16, 12, 0),
                        "#95E1D3"
                )
        );

        when(availabilityRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        availabilityService.blockTimeSlots(instructorUuid, slots);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<InstructorAvailability>> captor = ArgumentCaptor.forClass(List.class);
        verify(availabilityRepository).saveAll(captor.capture());
        List<InstructorAvailability> savedSlots = captor.getValue();

        assertThat(savedSlots).hasSize(2);
        assertThat(savedSlots.get(0).getIsAvailable()).isFalse();
        assertThat(savedSlots.get(0).getSpecificDate()).isEqualTo(LocalDate.of(2024, 10, 15));
        assertThat(savedSlots.get(1).getSpecificDate()).isEqualTo(LocalDate.of(2024, 10, 16));

        verify(eventPublisher).publishEvent(any(InstructorAvailabilityChangedEventDTO.class));
    }

    @Test
    void blockTimeSlots_rejectsInvalidTimeRange() {
        UUID instructorUuid = UUID.randomUUID();
        BlockedTimeSlotRequestDTO invalidSlot = new BlockedTimeSlotRequestDTO(
                LocalDateTime.of(2024, 10, 15, 11, 0),
                LocalDateTime.of(2024, 10, 15, 10, 0),
                "#FF6B6B"
        );

        assertThrows(IllegalArgumentException.class,
                () -> availabilityService.blockTimeSlots(instructorUuid, List.of(invalidSlot)));

        verifyNoInteractions(availabilityRepository);
        verifyNoInteractions(eventPublisher);
    }
}
