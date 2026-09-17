package apps.sarafrika.elimika.resourcing.spi;

import java.util.UUID;

/** A resource as another module lists it, including where it is: its location label and branch. */
public record ResourceListing(UUID uuid,
                              UUID organisationUuid,
                              UUID branchUuid,
                              ResourceType resourceType,
                              String name,
                              Integer seatCapacity,
                              String locationName,
                              boolean active) {
}
