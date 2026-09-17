package apps.sarafrika.elimika.resourcing.repository.projection;

import apps.sarafrika.elimika.resourcing.spi.ResourceBookingStatus;

import java.util.UUID;

/**
 * One distinct booking state a marketplace job holds on one resource.
 */
public record JobResourceBookingStatus(UUID jobUuid, UUID resourceUuid, ResourceBookingStatus status) {
}
