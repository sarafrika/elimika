package apps.sarafrika.elimika.classes.internal;

import apps.sarafrika.elimika.shared.enums.LocationType;
import apps.sarafrika.elimika.shared.search.SearchGeoPoint;
import apps.sarafrika.elimika.tenancy.spi.BranchContact;
import apps.sarafrika.elimika.tenancy.spi.BranchLocation;
import apps.sarafrika.elimika.tenancy.spi.TrainingBranchLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
     * The location a class or job is delivered at. Its own coordinates come first: an in-person class
     * pinned to a hall across town keeps that pin, and only one without coordinates of its own takes
     * its branch's pin (and, when it has no name of its own either, the branch's label).
     * <p>
     * Strict mode refuses an inactive branch, and an unpinned branch when the class has no coordinates
     * of its own to fall back on; lenient mode keeps the supplied values when the pin is missing.
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
        if (latitude != null && longitude != null) {
            boolean named = locationName != null && !locationName.isBlank();
            return named ? supplied : new ResolvedLocation(label(branch), latitude, longitude);
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

    /**
     * The near-me point a class or job is indexed with: only for IN_PERSON or HYBRID delivery, from
     * its own coordinates or, when those are missing, its branch's pin; always rounded to about 1 km.
     * ONLINE delivery and anything without a location stays out of every geo query.
     *
     * @param branchPins memo of branch lookups for one indexing batch, so a batch reads each branch once
     */
    public SearchGeoPoint searchPoint(UUID organisationUuid,
                                      UUID branchUuid,
                                      LocationType locationType,
                                      BigDecimal latitude,
                                      BigDecimal longitude,
                                      Map<UUID, Optional<BranchLocation>> branchPins) {
        if (locationType != LocationType.IN_PERSON && locationType != LocationType.HYBRID) {
            return null;
        }
        if (latitude != null && longitude != null) {
            return SearchGeoPoint.rounded(latitude, longitude);
        }
        if (organisationUuid == null || branchUuid == null) {
            return null;
        }
        Optional<BranchLocation> branch = branchPins.computeIfAbsent(branchUuid,
                ignored -> trainingBranchLookupService.findBranch(organisationUuid, branchUuid));
        return branch.filter(location -> organisationUuid.equals(location.organisationUuid()))
                .filter(BranchLocation::hasPin)
                .map(pin -> SearchGeoPoint.rounded(pin.latitude(), pin.longitude()))
                .orElse(null);
    }

    /** A fresh memo for {@link #searchPoint}. */
    public static Map<UUID, Optional<BranchLocation>> branchPinMemo() {
        return new HashMap<>();
    }

    /**
     * A memo for {@link #searchPoint} pre-filled from one batch lookup: every requested branch is
     * answered (empty when unknown or deleted), so the batch never falls back to per-branch reads.
     */
    public static Map<UUID, Optional<BranchLocation>> branchPinMemo(Map<UUID, BranchLocation> branches,
                                                                    Collection<UUID> branchUuids) {
        Map<UUID, Optional<BranchLocation>> memo = new HashMap<>();
        for (UUID branchUuid : branchUuids) {
            if (branchUuid != null) {
                memo.put(branchUuid, Optional.ofNullable(branches.get(branchUuid)));
            }
        }
        return memo;
    }

    /** {@link #branchPinMemo(Map, Collection)} loaded with one lookup for the given branches. */
    public Map<UUID, Optional<BranchLocation>> loadBranchPins(Collection<UUID> branchUuids) {
        return branchPinMemo(branches(branchUuids), branchUuids);
    }

    /** Non-deleted branches for an indexing batch, in one lookup. */
    public Map<UUID, BranchLocation> branches(Collection<UUID> branchUuids) {
        return branchUuids == null || branchUuids.isEmpty() ? Map.of()
                : trainingBranchLookupService.findBranches(branchUuids);
    }

    /**
     * Branch names from an already loaded batch; only branches missing from it (deleted ones, which
     * keep their historical label) cost a second lookup.
     */
    public Map<UUID, String> branchNames(Map<UUID, BranchLocation> branches, Collection<UUID> branchUuids) {
        Map<UUID, String> names = new HashMap<>();
        List<UUID> missing = new ArrayList<>();
        for (UUID branchUuid : branchUuids) {
            if (branchUuid == null) {
                continue;
            }
            BranchLocation branch = branches.get(branchUuid);
            if (branch == null) {
                missing.add(branchUuid);
            } else if (branch.name() != null) {
                names.put(branchUuid, branch.name());
            }
        }
        if (!missing.isEmpty()) {
            names.putAll(trainingBranchLookupService.findBranchNames(missing));
        }
        return names;
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
