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

    private CommerceCatalogueServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CommerceCatalogueServiceImpl(
                catalogItemRepository,
                currencyService,
                specificationBuilder,
                accessService,
                variantRepository,
                classScheduleService);
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
