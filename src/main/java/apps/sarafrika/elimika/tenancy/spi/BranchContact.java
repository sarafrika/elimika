package apps.sarafrika.elimika.tenancy.spi;

import java.util.UUID;

/** A training branch's point of contact; any of the three may be missing. */
public record BranchContact(UUID branchUuid, String name, String phone, String email) {
}
