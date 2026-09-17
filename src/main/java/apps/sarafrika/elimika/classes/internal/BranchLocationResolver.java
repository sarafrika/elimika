package apps.sarafrika.elimika.classes.internal;

import apps.sarafrika.elimika.shared.enums.LocationType;
import apps.sarafrika.elimika.tenancy.spi.BranchContact;
import apps.sarafrika.elimika.tenancy.spi.BranchLocation;
import apps.sarafrika.elimika.tenancy.spi.TrainingBranchLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Turns a class or job's training branch into the location it is delivered at.
 */
@Component
@RequiredArgsConstructor
public class BranchLocationResolver {

    private static final int MAX_LOCATION_NAME_LENGTH = 255;
    private static final int COORDINATE_SCALE = 6;

    private final TrainingBranchLookupService trainingBranchLookupService;

    public record ResolvedLocation(String locationName, BigDecimal latitude, BigDecimal longitude) {
    }

    /**
     * Strict mode refuses inactive or unpinned branches; lenient mode keeps the supplied values when the pin is missing.
     */
    public ResolvedLocation resolve(UUID organisationUuid,
                                    UUID branchUuid,
                                    LocationType locationType,
                                    String locationName,
                                    BigDecimal latitude,
                                    BigDecimal longitude,
                                    boolean strict) {
        ResolvedLocation supplied = new ResolvedLocation(locationName, latitude, longitude);
        if (branchUuid == null) {
            return supplied;
        }
        if (organisationUuid == null) {
            throw new IllegalArgumentException("branch_uuid requires organisation_uuid");
        }

        BranchLocation branch = trainingBranchLookupService.findBranch(organisationUuid, branchUuid)
                .orElseThrow(() -> new IllegalArgumentException(String.format(
                        "Training branch %s does not belong to organisation %s", branchUuid, organisationUuid)));
        if (strict && !branch.active()) {
            throw new IllegalArgumentException(String.format("Training branch '%s' is inactive", branch.name()));
        }
        if (locationType == null || locationType == LocationType.ONLINE) {
            return supplied;
        }
        if (!branch.hasPin()) {
            if (strict) {
                throw new IllegalArgumentException(String.format(
                        "Training branch '%s' has no location pin; set it on the branch first", branch.name()));
            }
            return supplied;
        }
        return new ResolvedLocation(
                label(branch),
                branch.latitude().setScale(COORDINATE_SCALE, RoundingMode.HALF_UP),
                branch.longitude().setScale(COORDINATE_SCALE, RoundingMode.HALF_UP));
    }

    public Optional<String> branchName(UUID branchUuid) {
        if (branchUuid == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(trainingBranchLookupService.findBranchNames(List.of(branchUuid)).get(branchUuid));
    }

    /** Branch names for a page of jobs in one lookup. */
    public Map<UUID, String> branchNames(Collection<UUID> branchUuids) {
        return trainingBranchLookupService.findBranchNames(branchUuids);
    }

    /** Points of contact for a page of branches in one lookup; only for callers entitled to see them. */
    public Map<UUID, BranchContact> branchContacts(Collection<UUID> branchUuids) {
        return branchUuids.isEmpty() ? Map.of() : trainingBranchLookupService.findBranchContacts(branchUuids);
    }

    public static String label(BranchLocation branch) {
        String name = branch.name() == null ? "" : branch.name().trim();
        String address = branch.address() == null ? "" : branch.address().trim();
        String label = address.isEmpty() ? name : name + " · " + address;
        return label.length() > MAX_LOCATION_NAME_LENGTH ? label.substring(0, MAX_LOCATION_NAME_LENGTH) : label;
    }
}
