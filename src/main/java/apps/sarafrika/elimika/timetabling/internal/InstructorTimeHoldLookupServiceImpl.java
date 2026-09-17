package apps.sarafrika.elimika.timetabling.internal;

import apps.sarafrika.elimika.shared.spi.timetabling.InstructorTimeHoldEntry;
import apps.sarafrika.elimika.shared.spi.timetabling.InstructorTimeHoldLookupService;
import apps.sarafrika.elimika.timetabling.spi.InstructorTimeHoldDTO;
import apps.sarafrika.elimika.timetabling.spi.InstructorTimeHoldService;
import apps.sarafrika.elimika.timetabling.spi.InstructorTimeHoldStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class InstructorTimeHoldLookupServiceImpl implements InstructorTimeHoldLookupService {

    private final InstructorTimeHoldService instructorTimeHoldService;

    @Override
    public List<InstructorTimeHoldEntry> findActiveHolds(UUID instructorUuid, LocalDate startDate, LocalDate endDate) {
        return instructorTimeHoldService.findActiveHolds(instructorUuid, startDate, endDate).stream()
                .map(InstructorTimeHoldLookupServiceImpl::toEntry)
                .toList();
    }

    @Override
    public boolean hasFirmHold(UUID instructorUuid, LocalDateTime start, LocalDateTime end) {
        return !instructorTimeHoldService.findBlockingHolds(instructorUuid, start, end, null).isEmpty();
    }

    private static InstructorTimeHoldEntry toEntry(InstructorTimeHoldDTO hold) {
        return new InstructorTimeHoldEntry(
                hold.uuid(),
                hold.jobUuid(),
                hold.title(),
                hold.organisationUuid(),
                hold.organisationName(),
                hold.startTime(),
                hold.endTime(),
                hold.status() == InstructorTimeHoldStatus.FIRM
        );
    }
}
