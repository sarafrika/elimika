package apps.sarafrika.elimika.resourcing.spi;

import java.util.UUID;

/**
 * Lightweight cross-module view of an organisation resource.
 */
public record ResourceSummary(UUID uuid,
                              UUID organisationUuid,
                              UUID branchUuid,
                              ResourceType resourceType,
                              String name,
                              Integer seatCapacity,
                              Integer totalQuantity,
                              boolean active) {
}
