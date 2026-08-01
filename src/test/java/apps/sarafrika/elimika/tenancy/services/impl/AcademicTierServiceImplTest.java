package apps.sarafrika.elimika.tenancy.services.impl;

import apps.sarafrika.elimika.tenancy.dto.AcademicTierDTO;
import apps.sarafrika.elimika.tenancy.entity.AcademicTier;
import apps.sarafrika.elimika.tenancy.repository.AcademicTierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcademicTierServiceImplTest {

    @Mock
    private AcademicTierRepository academicTierRepository;

    private AcademicTierServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AcademicTierServiceImpl(academicTierRepository);
    }

    @Test
    void getPlatformTiersReturnsTheCatalogueInTierOrder() {
        // Ordering is the whole point of the table: a school thinks "PP1 then Grade 1", never
        // alphabetically, so the sequence the repository returns must survive untouched.
        when(catalogue("KE")).thenReturn(List.of(
                tier("Kindergarten", 10, "KE", null, true),
                tier("PP1", 20, "KE", null, true),
                tier("Grade 1", 40, "KE", null, true)));

        List<AcademicTierDTO> tiers = service.getPlatformTiers("KE");

        assertThat(tiers).extracting(AcademicTierDTO::name)
                .containsExactly("Kindergarten", "PP1", "Grade 1");
        assertThat(tiers).extracting(AcademicTierDTO::tierOrder)
                .containsExactly(10, 20, 40);
    }

    @Test
    void getPlatformTiersLeavesActiveOnlyAndPlatformOnlyToTheQuery() {
        // Both restrictions are encoded in the finder name. Filtering in Java instead would mean
        // reading every tenant's private tiers into memory to throw them away, which is exactly
        // the leak the platform-only rule exists to prevent.
        when(catalogue("KE")).thenReturn(List.of(tier("Grade 7", 100, "KE", null, true)));

        assertThat(service.getPlatformTiers("KE")).hasSize(1);

        verify(academicTierRepository)
                .findByEducationSystemIgnoreCaseAndOrganisationUuidIsNullAndActiveTrueOrderByTierOrderAsc("KE");
        verify(academicTierRepository, never()).findAll();
        verify(academicTierRepository, never()).findByUuidIn(any());
    }

    @Test
    void getPlatformTiersFallsBackToTheSeededEducationSystem() {
        when(catalogue("KE")).thenReturn(List.of(tier("Form 4", 160, "KE", null, true)));

        assertThat(service.getPlatformTiers(null)).hasSize(1);
        assertThat(service.getPlatformTiers("   ")).hasSize(1);

        verify(academicTierRepository, org.mockito.Mockito.times(2))
                .findByEducationSystemIgnoreCaseAndOrganisationUuidIsNullAndActiveTrueOrderByTierOrderAsc("KE");
    }

    @Test
    void getPlatformTiersTrimsTheRequestedEducationSystem() {
        when(catalogue("UK")).thenReturn(List.of());

        assertThat(service.getPlatformTiers("  UK  ")).isEmpty();

        verify(academicTierRepository)
                .findByEducationSystemIgnoreCaseAndOrganisationUuidIsNullAndActiveTrueOrderByTierOrderAsc("UK");
    }

    @Test
    void getPlatformTiersCarriesEveryFieldTheGroupsPageNeeds() {
        AcademicTier grade7 = tier("Grade 7", 100, "KE", null, true);
        grade7.setDescription("Junior secondary");
        when(catalogue("KE")).thenReturn(List.of(grade7));

        AcademicTierDTO dto = service.getPlatformTiers("KE").getFirst();

        assertThat(dto.uuid()).isEqualTo(grade7.getUuid());
        assertThat(dto.name()).isEqualTo("Grade 7");
        assertThat(dto.tierOrder()).isEqualTo(100);
        assertThat(dto.educationSystem()).isEqualTo("KE");
        assertThat(dto.organisationUuid()).isNull();
        assertThat(dto.active()).isTrue();
        assertThat(dto.description()).isEqualTo("Junior secondary");
    }

    private List<AcademicTier> catalogue(String educationSystem) {
        return academicTierRepository
                .findByEducationSystemIgnoreCaseAndOrganisationUuidIsNullAndActiveTrueOrderByTierOrderAsc(
                        educationSystem);
    }

    private AcademicTier tier(String name, int order, String system, UUID organisationUuid, boolean active) {
        AcademicTier tier = new AcademicTier();
        tier.setUuid(UUID.randomUUID());
        tier.setName(name);
        tier.setTierOrder(order);
        tier.setEducationSystem(system);
        tier.setOrganisationUuid(organisationUuid);
        tier.setActive(active);
        return tier;
    }
}
