package apps.sarafrika.elimika.commerce.catalogue.service.impl;

import apps.sarafrika.elimika.commerce.catalogue.dto.CommerceCatalogueItemDTO;
import apps.sarafrika.elimika.commerce.catalogue.dto.UpsertCommerceCatalogueItemRequest;
import apps.sarafrika.elimika.commerce.catalogue.entity.CommerceCatalogueItem;
import apps.sarafrika.elimika.commerce.catalogue.repository.CommerceCatalogueItemRepository;
import apps.sarafrika.elimika.commerce.catalogue.service.CommerceCatalogueAccessService;
import apps.sarafrika.elimika.commerce.catalogue.service.CommerceCatalogueAccessService.VisibilityContext;
import apps.sarafrika.elimika.commerce.internal.repository.CommerceProductVariantRepository;
import apps.sarafrika.elimika.shared.currency.model.PlatformCurrency;
import apps.sarafrika.elimika.shared.currency.service.CurrencyService;
import apps.sarafrika.elimika.shared.spi.ClassScheduleService;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.DisplayName;
import apps.sarafrika.elimika.commerce.internal.entity.CommerceProductVariant;

@ExtendWith(MockitoExtension.class)
class CommerceCatalogueServiceImplTest {

    @Mock
    private CommerceCatalogueItemRepository catalogItemRepository;

    @Mock
    private CurrencyService currencyService;

    @Mock
    private GenericSpecificationBuilder<CommerceCatalogueItem> specificationBuilder;

    @Mock
    private CommerceCatalogueAccessService accessService;

    @Mock
    private CommerceProductVariantRepository variantRepository;

    @Mock
    private ClassScheduleService classScheduleService;

    @Mock
    private apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService catalogueClassDefinitionLookupService;

    private CommerceCatalogueServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CommerceCatalogueServiceImpl(
                catalogItemRepository,
                currencyService,
                specificationBuilder,
                accessService,
                variantRepository,
                classScheduleService,
                catalogueClassDefinitionLookupService);
    }

    @Test
    void createItemShouldAllowProgramAssociationWithoutCourseOrClass() {
        UUID programUuid = UUID.randomUUID();

        PlatformCurrency currency = new PlatformCurrency();
        currency.setCode("KES");

        when(currencyService.resolveCurrencyOrDefault(null)).thenReturn(currency);
        when(variantRepository.findByCode("variant-001")).thenReturn(Optional.empty());
        when(catalogItemRepository.save(any(CommerceCatalogueItem.class))).thenAnswer(invocation -> {
            CommerceCatalogueItem entity = invocation.getArgument(0);
            entity.setUuid(UUID.randomUUID());
            return entity;
        });

        UpsertCommerceCatalogueItemRequest request = new UpsertCommerceCatalogueItemRequest(
                null,
                null,
                programUuid,
                "product-001",
                "variant-001",
                null,
                true,
                true);

        CommerceCatalogueItemDTO dto = service.createItem(request);

        assertThat(dto.programUuid()).isEqualTo(programUuid);
        assertThat(dto.courseUuid()).isNull();
        assertThat(dto.classDefinitionUuid()).isNull();
    }

    @Test
    void getByCourseOrClassOrProgramShouldIncludeProgramMatches() {
        UUID programUuid = UUID.randomUUID();
        CommerceCatalogueItem item = new CommerceCatalogueItem();
        item.setUuid(UUID.randomUUID());
        item.setProgramUuid(programUuid);
        item.setProductCode("product-001");
        item.setVariantCode("variant-001");
        item.setCurrencyCode("KES");

        when(accessService.buildContext()).thenReturn(new VisibilityContext(true, true));
        when(accessService.canView(any(CommerceCatalogueItem.class), any(VisibilityContext.class))).thenReturn(true);
        when(variantRepository.findByCode("variant-001")).thenReturn(Optional.empty());
        when(catalogItemRepository.findByProgramUuid(programUuid)).thenReturn(List.of(item));

        List<CommerceCatalogueItemDTO> results = service.getByCourseOrClassOrProgram(null, null, programUuid);

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().programUuid()).isEqualTo(programUuid);
    }


    // ── One basis, one multiplier: what the learner pays follows the contract's unit ───────────

    private java.math.BigDecimal priceFor(apps.sarafrika.elimika.shared.utils.enums.RateBasis basis,
                                          long minutes, long sessions, long days) {
        UUID classUuid = UUID.randomUUID();
        CommerceCatalogueItem item = new CommerceCatalogueItem();
        item.setUuid(UUID.randomUUID());
        item.setClassDefinitionUuid(classUuid);
        item.setProductCode("product-001");
        item.setVariantCode("variant-001");
        item.setCurrencyCode("KES");

        CommerceProductVariant variant = new CommerceProductVariant();
        variant.setCode("variant-001");
        variant.setUnitAmount(new java.math.BigDecimal("3000.0000"));

        when(accessService.buildContext()).thenReturn(new VisibilityContext(true, true));
        when(accessService.canView(any(CommerceCatalogueItem.class), any(VisibilityContext.class))).thenReturn(true);
        when(variantRepository.findByCode("variant-001")).thenReturn(Optional.of(variant));
        when(catalogItemRepository.findByClassDefinitionUuid(classUuid)).thenReturn(List.of(item));
        when(classScheduleService.getScheduleSummary(classUuid)).thenReturn(
                new ClassScheduleService.ClassScheduleSummary(
                        minutes, sessions, 0, java.math.BigDecimal.ZERO, days));
        when(catalogueClassDefinitionLookupService.findByUuid(classUuid)).thenReturn(
                Optional.of(new apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService.ClassDefinitionSnapshot(
                        classUuid, UUID.randomUUID(), null, "Dairy", null,
                        new java.math.BigDecimal("3000.00"), new java.math.BigDecimal("2000.00"),
                        basis, null, null, 20, true, 30)));

        return service.getByCourseOrClassOrProgram(null, classUuid, null).getFirst().unitAmount();
    }

    @Test
    @DisplayName("a per-hour class bills the rate for every scheduled hour")
    void perHourBillsHours() {
        // 5 sessions across 3 days totalling 7 hours.
        assertThat(priceFor(apps.sarafrika.elimika.shared.utils.enums.RateBasis.PER_HOUR, 420, 5, 3))
                .isEqualByComparingTo("21000");
    }

    @Test
    @DisplayName("a per-session class bills the rate once per session, whatever its length")
    void perSessionBillsSessions() {
        assertThat(priceFor(apps.sarafrika.elimika.shared.utils.enums.RateBasis.PER_SESSION, 420, 5, 3))
                .isEqualByComparingTo("15000");
    }

    @Test
    @DisplayName("a per-day class bills once per calendar day, not once per session")
    void perDayBillsDistinctDays() {
        assertThat(priceFor(apps.sarafrika.elimika.shared.utils.enums.RateBasis.PER_DAY, 420, 5, 3))
                .isEqualByComparingTo("9000");
    }

    @Test
    void createItemShouldRejectWhenNoAssociationProvided() {
        UpsertCommerceCatalogueItemRequest request = new UpsertCommerceCatalogueItemRequest(
                null,
                null,
                null,
                "product-001",
                "variant-001",
                null,
                true,
                true);

        assertThatThrownBy(() -> service.createItem(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("course_uuid")
                .hasMessageContaining("program_uuid");
    }
}
