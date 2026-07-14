package apps.sarafrika.elimika.resourcing.service;

import apps.sarafrika.elimika.resourcing.dto.OrganisationResourceDTO;
import apps.sarafrika.elimika.resourcing.dto.ResourceAvailabilityRuleDTO;
import apps.sarafrika.elimika.resourcing.dto.ResourceBookingDTO;
import apps.sarafrika.elimika.resourcing.dto.ResourceCalendarEntryDTO;
import apps.sarafrika.elimika.resourcing.spi.ResourceBookingStatus;
import apps.sarafrika.elimika.resourcing.spi.ResourceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Internal service contract for organisation resource management.
 */
public interface OrganisationResourceServiceInterface {

    OrganisationResourceDTO createResource(UUID organisationUuid, OrganisationResourceDTO dto);

    Page<OrganisationResourceDTO> listResources(UUID organisationUuid,
                                                ResourceType resourceType,
                                                UUID branchUuid,
                                                Boolean active,
                                                Pageable pageable);

    OrganisationResourceDTO getResource(UUID organisationUuid, UUID resourceUuid);

    OrganisationResourceDTO updateResource(UUID organisationUuid, UUID resourceUuid, OrganisationResourceDTO dto);

    void deactivateResource(UUID organisationUuid, UUID resourceUuid);

    RuleChangeResult addAvailabilityRule(UUID organisationUuid, UUID resourceUuid, ResourceAvailabilityRuleDTO dto);

    List<ResourceAvailabilityRuleDTO> listAvailabilityRules(UUID organisationUuid, UUID resourceUuid);

    RuleChangeResult updateAvailabilityRule(UUID organisationUuid, UUID resourceUuid, UUID ruleUuid,
                                            ResourceAvailabilityRuleDTO dto);

    void deleteAvailabilityRule(UUID organisationUuid, UUID resourceUuid, UUID ruleUuid);

    List<ResourceCalendarEntryDTO> getCalendar(UUID organisationUuid, UUID resourceUuid,
                                               LocalDate startDate, LocalDate endDate);

    Page<ResourceBookingDTO> listBookings(UUID organisationUuid, UUID resourceUuid,
                                          ResourceBookingStatus status,
                                          LocalDate startDate, LocalDate endDate,
                                          Pageable pageable);

    /**
     * A saved rule plus how many existing active bookings intersect it, so the
     * organisation can review bookings a new blackout affects (they are not
     * auto-invalidated).
     */
    record RuleChangeResult(ResourceAvailabilityRuleDTO rule, long affectedActiveBookings) {
    }
}
