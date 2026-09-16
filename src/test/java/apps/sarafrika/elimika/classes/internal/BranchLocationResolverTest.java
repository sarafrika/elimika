package apps.sarafrika.elimika.classes.internal;

import apps.sarafrika.elimika.classes.internal.BranchLocationResolver.ResolvedLocation;
import apps.sarafrika.elimika.shared.enums.LocationType;
import apps.sarafrika.elimika.tenancy.spi.BranchLocation;
import apps.sarafrika.elimika.tenancy.spi.TrainingBranchLookupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BranchLocationResolverTest {

    private static final UUID ORGANISATION_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID BRANCH_UUID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final BigDecimal CLIENT_LATITUDE = new BigDecimal("-4.043477");
    private static final BigDecimal CLIENT_LONGITUDE = new BigDecimal("39.668206");

    @Mock
    private TrainingBranchLookupService trainingBranchLookupService;

    private BranchLocationResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new BranchLocationResolver(trainingBranchLookupService);
    }

    @Test
    void copiesRoundedBranchPinAndBuildsLabel() {
        stubBranch(branch("Main Campus", "Kasarani, Nairobi", "-1.221812345678", "36.897000000001", true));

        ResolvedLocation location = resolve(LocationType.IN_PERSON, true);

        assertThat(location.locationName()).isEqualTo("Main Campus · Kasarani, Nairobi");
        assertThat(location.latitude()).isEqualTo(new BigDecimal("-1.221812"));
        assertThat(location.longitude()).isEqualTo(new BigDecimal("36.897000"));
    }

    @Test
    void labelFallsBackToBranchNameWithoutAddress() {
        assertThat(BranchLocationResolver.label(branch("Main Campus", "  ", "-1.2", "36.8", true)))
                .isEqualTo("Main Campus");
        assertThat(BranchLocationResolver.label(branch("Main Campus", null, "-1.2", "36.8", true)))
                .isEqualTo("Main Campus");
    }

    @Test
    void labelIsTruncatedTo255Characters() {
        String label = BranchLocationResolver.label(branch("Main Campus", "x".repeat(400), "-1.2", "36.8", true));

        assertThat(label).hasSize(255).startsWith("Main Campus · x");
    }

    @Test
    void strictModeRefusesBranchWithoutPinForInPerson() {
        stubBranch(branch("Main Campus", "Kasarani, Nairobi", null, null, true));

        assertThatThrownBy(() -> resolve(LocationType.IN_PERSON, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Training branch 'Main Campus' has no location pin; set it on the branch first");
    }

    @Test
    void onlineLocationPassesThroughWithoutPin() {
        stubBranch(branch("Main Campus", null, null, null, true));

        ResolvedLocation location = resolve(LocationType.ONLINE, true);

        assertThat(location).isEqualTo(new ResolvedLocation("Client name", CLIENT_LATITUDE, CLIENT_LONGITUDE));
    }

    @Test
    void refusesBranchOfAnotherOrganisation() {
        when(trainingBranchLookupService.findBranch(ORGANISATION_UUID, BRANCH_UUID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolve(LocationType.HYBRID, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Training branch %s does not belong to organisation %s", BRANCH_UUID, ORGANISATION_UUID);
    }

    @Test
    void strictModeRefusesInactiveBranch() {
        stubBranch(branch("Old Annex", null, "-1.2", "36.8", false));

        assertThatThrownBy(() -> resolve(LocationType.HYBRID, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Training branch 'Old Annex' is inactive");
    }

    @Test
    void withoutBranchPassesClientLocationThrough() {
        ResolvedLocation location = resolver.resolve(ORGANISATION_UUID, null, LocationType.IN_PERSON,
                "Client name", CLIENT_LATITUDE, CLIENT_LONGITUDE, true);

        assertThat(location).isEqualTo(new ResolvedLocation("Client name", CLIENT_LATITUDE, CLIENT_LONGITUDE));
        verifyNoInteractions(trainingBranchLookupService);
    }

    @Test
    void refusesBranchWithoutOrganisation() {
        assertThatThrownBy(() -> resolver.resolve(null, BRANCH_UUID, LocationType.IN_PERSON,
                "Client name", CLIENT_LATITUDE, CLIENT_LONGITUDE, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("branch_uuid requires organisation_uuid");
    }

    @Test
    void lenientModeKeepsExistingCoordinatesWhenPinMissing() {
        stubBranch(branch("Old Annex", "Kasarani, Nairobi", null, null, false));

        ResolvedLocation location = resolve(LocationType.IN_PERSON, false);

        assertThat(location).isEqualTo(new ResolvedLocation("Client name", CLIENT_LATITUDE, CLIENT_LONGITUDE));
    }

    private ResolvedLocation resolve(LocationType locationType, boolean strict) {
        return resolver.resolve(ORGANISATION_UUID, BRANCH_UUID, locationType,
                "Client name", CLIENT_LATITUDE, CLIENT_LONGITUDE, strict);
    }

    private void stubBranch(BranchLocation branch) {
        when(trainingBranchLookupService.findBranch(ORGANISATION_UUID, BRANCH_UUID)).thenReturn(Optional.of(branch));
    }

    private BranchLocation branch(String name, String address, String latitude, String longitude, boolean active) {
        return new BranchLocation(BRANCH_UUID, ORGANISATION_UUID, name, address,
                latitude == null ? null : new BigDecimal(latitude),
                longitude == null ? null : new BigDecimal(longitude),
                active);
    }
}
