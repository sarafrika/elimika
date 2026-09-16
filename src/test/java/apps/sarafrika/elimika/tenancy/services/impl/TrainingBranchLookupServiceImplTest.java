package apps.sarafrika.elimika.tenancy.services.impl;

import apps.sarafrika.elimika.tenancy.entity.TrainingBranch;
import apps.sarafrika.elimika.tenancy.repository.TrainingBranchRepository;
import apps.sarafrika.elimika.tenancy.spi.BranchLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainingBranchLookupServiceImplTest {

    private static final UUID ORGANISATION_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_ORGANISATION_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID BRANCH_UUID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID DELETED_BRANCH_UUID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Mock
    private TrainingBranchRepository trainingBranchRepository;

    private TrainingBranchLookupServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TrainingBranchLookupServiceImpl(trainingBranchRepository);
    }

    @Test
    void findBranchMapsNameAddressPinAndActive() {
        TrainingBranch branch = branch(BRANCH_UUID, "Main Campus", false);
        branch.setAddress("Kasarani, Nairobi");
        branch.setLatitude(new BigDecimal("-1.221800"));
        branch.setLongitude(new BigDecimal("36.897000"));
        branch.setActive(false);
        when(trainingBranchRepository.findByUuidAndOrganisationUuidAndDeletedFalse(BRANCH_UUID, ORGANISATION_UUID))
                .thenReturn(Optional.of(branch));

        BranchLocation location = service.findBranch(ORGANISATION_UUID, BRANCH_UUID).orElseThrow();

        assertThat(location.uuid()).isEqualTo(BRANCH_UUID);
        assertThat(location.organisationUuid()).isEqualTo(ORGANISATION_UUID);
        assertThat(location.name()).isEqualTo("Main Campus");
        assertThat(location.address()).isEqualTo("Kasarani, Nairobi");
        assertThat(location.latitude()).isEqualByComparingTo("-1.2218");
        assertThat(location.longitude()).isEqualByComparingTo("36.897");
        assertThat(location.active()).isFalse();
        assertThat(location.hasPin()).isTrue();
    }

    @Test
    void findBranchIsEmptyForAnotherOrganisation() {
        when(trainingBranchRepository.findByUuidAndOrganisationUuidAndDeletedFalse(BRANCH_UUID, OTHER_ORGANISATION_UUID))
                .thenReturn(Optional.empty());

        assertThat(service.findBranch(OTHER_ORGANISATION_UUID, BRANCH_UUID)).isEmpty();
    }

    @Test
    void findBranchIsEmptyForNullArguments() {
        assertThat(service.findBranch(null, BRANCH_UUID)).isEmpty();
        assertThat(service.findBranch(ORGANISATION_UUID, null)).isEmpty();
        verifyNoInteractions(trainingBranchRepository);
    }

    @Test
    @SuppressWarnings("unchecked")
    void findBranchNamesSkipsNullsAndKeepsDeletedBranchLabels() {
        when(trainingBranchRepository.findByUuidIn(any()))
                .thenReturn(List.of(branch(BRANCH_UUID, "Main Campus", false),
                        branch(DELETED_BRANCH_UUID, "Old Annex", true)));

        Map<UUID, String> names = service.findBranchNames(Arrays.asList(BRANCH_UUID, null, DELETED_BRANCH_UUID, BRANCH_UUID));

        assertThat(names).containsExactly(
                Map.entry(BRANCH_UUID, "Main Campus"),
                Map.entry(DELETED_BRANCH_UUID, "Old Annex"));
        ArgumentCaptor<Collection<UUID>> requested = ArgumentCaptor.forClass(Collection.class);
        verify(trainingBranchRepository).findByUuidIn(requested.capture());
        assertThat(requested.getValue()).containsExactly(BRANCH_UUID, DELETED_BRANCH_UUID);
    }

    private TrainingBranch branch(UUID uuid, String name, boolean deleted) {
        TrainingBranch branch = new TrainingBranch();
        branch.setUuid(uuid);
        branch.setOrganisationUuid(ORGANISATION_UUID);
        branch.setBranchName(name);
        branch.setDeleted(deleted);
        return branch;
    }
}
