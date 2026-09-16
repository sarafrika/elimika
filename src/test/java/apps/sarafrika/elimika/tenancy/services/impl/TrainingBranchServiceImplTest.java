package apps.sarafrika.elimika.tenancy.services.impl;

import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import apps.sarafrika.elimika.tenancy.dto.TrainingBranchDTO;
import apps.sarafrika.elimika.tenancy.entity.TrainingBranch;
import apps.sarafrika.elimika.tenancy.repository.OrganisationRepository;
import apps.sarafrika.elimika.tenancy.repository.TrainingBranchRepository;
import apps.sarafrika.elimika.tenancy.repository.UserDomainRepository;
import apps.sarafrika.elimika.tenancy.repository.UserOrganisationDomainMappingRepository;
import apps.sarafrika.elimika.tenancy.repository.UserRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainingBranchServiceImplTest {

    private static final UUID ORGANISATION_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_ORGANISATION_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID BRANCH_UUID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Mock
    private TrainingBranchRepository trainingBranchRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserDomainRepository userDomainRepository;
    @Mock
    private UserOrganisationDomainMappingRepository userOrganisationDomainMappingRepository;
    @Mock
    private OrganisationRepository organisationRepository;
    @Mock
    private GenericSpecificationBuilder<TrainingBranch> specificationBuilder;

    private TrainingBranchServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TrainingBranchServiceImpl(
                trainingBranchRepository,
                userRepository,
                userDomainRepository,
                userOrganisationDomainMappingRepository,
                organisationRepository,
                specificationBuilder
        );
        lenient().when(trainingBranchRepository.save(any(TrainingBranch.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void updateKeepsCoordinatesWhenTheRequestOmitsThem() {
        TrainingBranch existing = storedBranch();
        when(trainingBranchRepository.findByUuidAndDeletedFalse(BRANCH_UUID)).thenReturn(Optional.of(existing));

        TrainingBranchDTO updated = service.updateTrainingBranch(BRANCH_UUID,
                request(ORGANISATION_UUID, "Kasarani, Nairobi", null, null));

        assertThat(updated.latitude()).isEqualByComparingTo("-1.221800");
        assertThat(updated.longitude()).isEqualByComparingTo("36.897000");
    }

    @Test
    void updateClearsCoordinatesWhenTheAddressIsCleared() {
        TrainingBranch existing = storedBranch();
        when(trainingBranchRepository.findByUuidAndDeletedFalse(BRANCH_UUID)).thenReturn(Optional.of(existing));

        TrainingBranchDTO updated = service.updateTrainingBranch(BRANCH_UUID,
                request(ORGANISATION_UUID, "  ", null, null));

        assertThat(updated.latitude()).isNull();
        assertThat(updated.longitude()).isNull();
    }

    @Test
    void updateReplacesBothCoordinatesWhenSupplied() {
        TrainingBranch existing = storedBranch();
        when(trainingBranchRepository.findByUuidAndDeletedFalse(BRANCH_UUID)).thenReturn(Optional.of(existing));

        TrainingBranchDTO updated = service.updateTrainingBranch(BRANCH_UUID,
                request(ORGANISATION_UUID, "Westlands, Nairobi", new BigDecimal("-1.2676"), new BigDecimal("36.8108")));

        assertThat(updated.address()).isEqualTo("Westlands, Nairobi");
        assertThat(updated.latitude()).isEqualByComparingTo("-1.2676");
        assertThat(updated.longitude()).isEqualByComparingTo("36.8108");
    }

    @Test
    void updateRefusesHalfACoordinatePair() {
        assertThatThrownBy(() -> service.updateTrainingBranch(BRANCH_UUID,
                request(ORGANISATION_UUID, "Kasarani, Nairobi", new BigDecimal("-1.2218"), null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("latitude and longitude must be provided together");

        verify(trainingBranchRepository, never()).save(any());
    }

    @Test
    void createRefusesLatitudeOutsideRange() {
        assertThatThrownBy(() -> service.createTrainingBranch(
                request(ORGANISATION_UUID, "Kasarani, Nairobi", new BigDecimal("91"), new BigDecimal("36.8970"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("latitude must be between -90 and 90");

        verify(trainingBranchRepository, never()).save(any());
    }

    @Test
    void updateRefusesMovingTheBranchToAnotherOrganisation() {
        TrainingBranch existing = storedBranch();
        when(trainingBranchRepository.findByUuidAndDeletedFalse(BRANCH_UUID)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.updateTrainingBranch(BRANCH_UUID,
                request(OTHER_ORGANISATION_UUID, "Kasarani, Nairobi", null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("organisation_uuid cannot be changed after a branch has been created");

        assertThat(existing.getOrganisationUuid()).isEqualTo(ORGANISATION_UUID);
        verify(trainingBranchRepository, never()).save(any());
    }

    @Test
    void createSurfacesDuplicateNameAsIllegalArgument() {
        when(trainingBranchRepository.existsByOrganisationUuidAndBranchNameAndDeletedFalse(ORGANISATION_UUID, "Main Campus"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.createTrainingBranch(
                request(ORGANISATION_UUID, "Kasarani, Nairobi", null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Training branch with this name already exists in the organisation");
    }

    @Test
    void requireBranchInOrganisationThrowsNotFoundForAnotherOrganisationsBranch() {
        when(trainingBranchRepository.findByUuidAndOrganisationUuidAndDeletedFalse(BRANCH_UUID, OTHER_ORGANISATION_UUID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.requireBranchInOrganisation(OTHER_ORGANISATION_UUID, BRANCH_UUID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void requireBranchInOrganisationThrowsNotFoundForDeletedBranch() {
        // The derived query filters deleted rows, so a soft-deleted branch simply does not resolve.
        when(trainingBranchRepository.findByUuidAndOrganisationUuidAndDeletedFalse(BRANCH_UUID, ORGANISATION_UUID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.requireBranchInOrganisation(ORGANISATION_UUID, BRANCH_UUID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void requireBranchInOrganisationAcceptsTheOrganisationsOwnBranch() {
        when(trainingBranchRepository.findByUuidAndOrganisationUuidAndDeletedFalse(BRANCH_UUID, ORGANISATION_UUID))
                .thenReturn(Optional.of(storedBranch()));

        service.requireBranchInOrganisation(ORGANISATION_UUID, BRANCH_UUID);
    }

    private TrainingBranch storedBranch() {
        TrainingBranch branch = new TrainingBranch();
        branch.setUuid(BRANCH_UUID);
        branch.setOrganisationUuid(ORGANISATION_UUID);
        branch.setBranchName("Main Campus");
        branch.setAddress("Kasarani, Nairobi");
        branch.setLatitude(new BigDecimal("-1.221800"));
        branch.setLongitude(new BigDecimal("36.897000"));
        branch.setPocName("Jane Doe");
        branch.setPocEmail("jane@example.com");
        branch.setPocTelephone("+254700000000");
        branch.setActive(true);
        return branch;
    }

    private TrainingBranchDTO request(UUID organisationUuid, String address, BigDecimal latitude, BigDecimal longitude) {
        return new TrainingBranchDTO(null, organisationUuid, "Main Campus", address, latitude, longitude,
                "Jane Doe", "jane@example.com", "+254700000000", true, null, null);
    }
}
