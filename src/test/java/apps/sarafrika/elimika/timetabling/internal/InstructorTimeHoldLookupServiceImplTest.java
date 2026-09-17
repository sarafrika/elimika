package apps.sarafrika.elimika.timetabling.internal;

import apps.sarafrika.elimika.shared.spi.timetabling.InstructorTimeHoldEntry;
import apps.sarafrika.elimika.tenancy.spi.OrganisationLookupService;
import apps.sarafrika.elimika.timetabling.model.InstructorTimeHold;
import apps.sarafrika.elimika.timetabling.repository.InstructorTimeHoldRepository;
import apps.sarafrika.elimika.timetabling.service.impl.InstructorTimeHoldServiceImpl;
import apps.sarafrika.elimika.timetabling.spi.InstructorTimeHoldStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class InstructorTimeHoldLookupServiceImplTest {

    private static final LocalDateTime START = LocalDateTime.of(2031, 5, 6, 9, 0);
    private static final LocalDateTime END = START.plusHours(2);

    @Mock
    private InstructorTimeHoldRepository holdRepository;
    @Mock
    private OrganisationLookupService organisationLookupService;

    private InstructorTimeHoldLookupServiceImpl lookup;
    private final UUID instructorUuid = UUID.randomUUID();
    private List<InstructorTimeHold> stored = List.of();

    @BeforeEach
    void setUp() {
        lookup = new InstructorTimeHoldLookupServiceImpl(
                new InstructorTimeHoldServiceImpl(holdRepository, organisationLookupService));
        // The repository filters by the statuses it is asked for, exactly as its JPQL does.
        lenient().when(holdRepository.findOverlappingHolds(eq(instructorUuid), anyCollection(), any(), any()))
                .thenAnswer(invocation -> {
                    Collection<InstructorTimeHoldStatus> statuses = invocation.getArgument(1);
                    return stored.stream().filter(hold -> statuses.contains(hold.getStatus())).toList();
                });
    }

    @Test
    void aFirmHoldMakesTheWindowBusy() {
        stored = List.of(hold(InstructorTimeHoldStatus.FIRM));

        assertThat(lookup.hasFirmHold(instructorUuid, START, END)).isTrue();
    }

    @Test
    void aTentativeHoldNeverMakesTheWindowBusy() {
        stored = List.of(hold(InstructorTimeHoldStatus.TENTATIVE));

        assertThat(lookup.hasFirmHold(instructorUuid, START, END)).isFalse();
    }

    @Test
    void activeHoldsCarryTheirJobAndWhetherTheyAreFirm() {
        UUID organisationUuid = UUID.randomUUID();
        InstructorTimeHold firm = hold(InstructorTimeHoldStatus.FIRM);
        firm.setOrganisationUuid(organisationUuid);
        InstructorTimeHold tentative = hold(InstructorTimeHoldStatus.TENTATIVE);
        stored = List.of(firm, tentative);
        lenient().when(organisationLookupService.findOrganisationNames(anyCollection()))
                .thenReturn(Map.of(organisationUuid, "Sarafrika Technical College"));

        List<InstructorTimeHoldEntry> entries =
                lookup.findActiveHolds(instructorUuid, LocalDate.of(2031, 5, 6), LocalDate.of(2031, 5, 6));

        assertThat(entries).extracting(InstructorTimeHoldEntry::firm).containsExactly(true, false);
        assertThat(entries.getFirst().jobUuid()).isEqualTo(firm.getJobUuid());
        assertThat(entries.getFirst().title()).isEqualTo("Grade 5 Piano");
        assertThat(entries.getFirst().organisationName()).isEqualTo("Sarafrika Technical College");
    }

    private InstructorTimeHold hold(InstructorTimeHoldStatus status) {
        InstructorTimeHold hold = new InstructorTimeHold();
        hold.setUuid(UUID.randomUUID());
        hold.setInstructorUuid(instructorUuid);
        hold.setJobUuid(UUID.randomUUID());
        hold.setApplicationUuid(UUID.randomUUID());
        hold.setTitle("Grade 5 Piano");
        hold.setStartTime(START);
        hold.setEndTime(END);
        hold.setTimezone("UTC");
        hold.setStatus(status);
        return hold;
    }
}
