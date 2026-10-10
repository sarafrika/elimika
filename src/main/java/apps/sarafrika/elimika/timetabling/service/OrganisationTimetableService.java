package apps.sarafrika.elimika.timetabling.service;

import apps.sarafrika.elimika.timetabling.dto.OrganisationTimetableEntryDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface OrganisationTimetableService {

    /** Widest range one request may ask for, so a calendar cannot pull an organisation's whole history. */
    int MAX_RANGE_DAYS = 366;

    /** Non-cancelled sessions of the organisation's classes overlapping the inclusive date range, by start time. */
    List<OrganisationTimetableEntryDTO> getOrganisationTimetable(UUID organisationUuid, LocalDate start, LocalDate end);
}
