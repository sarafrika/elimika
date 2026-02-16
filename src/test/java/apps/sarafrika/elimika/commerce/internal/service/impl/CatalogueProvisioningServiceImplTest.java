package apps.sarafrika.elimika.commerce.internal.service.impl;

import apps.sarafrika.elimika.commerce.catalogue.entity.CommerceCatalogueItem;
import apps.sarafrika.elimika.commerce.catalogue.repository.CommerceCatalogueItemRepository;
import apps.sarafrika.elimika.commerce.internal.config.InternalCommerceProperties;
import apps.sarafrika.elimika.commerce.internal.entity.CommerceProduct;
import apps.sarafrika.elimika.commerce.internal.entity.CommerceProductVariant;
import apps.sarafrika.elimika.commerce.internal.repository.CommerceProductRepository;
import apps.sarafrika.elimika.commerce.internal.repository.CommerceProductVariantRepository;
import apps.sarafrika.elimika.shared.enums.ClassVisibility;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogueProvisioningServiceImplTest {

    @Mock
    private CommerceProductRepository productRepository;

    @Mock
    private CommerceProductVariantRepository variantRepository;

    @Mock
    private CommerceCatalogueItemRepository catalogItemRepository;

    @Mock
    private ClassDefinitionLookupService classDefinitionLookupService;

    private CatalogueProvisioningServiceImpl service;

    @BeforeEach
    void setUp() {
        InternalCommerceProperties properties = new InternalCommerceProperties();
        properties.setDefaultCurrency("KES");
        service = new CatalogueProvisioningServiceImpl(
                productRepository,
                variantRepository,
                catalogItemRepository,
                classDefinitionLookupService,
                properties);
    }

    @Test
    void shouldProvisionProgramScopedClassWhenCourseUuidIsMissing() {
        UUID classDefinitionUuid = UUID.randomUUID();
        UUID programUuid = UUID.randomUUID();

        ClassDefinitionLookupService.ClassDefinitionSnapshot snapshot =
                new ClassDefinitionLookupService.ClassDefinitionSnapshot(
                        classDefinitionUuid,
                        null,
                        programUuid,
                        "Data Analytics Cohort",
                        "Program class",
                        new BigDecimal("2400.00"),
                        ClassVisibility.PUBLIC,
                        30,
                        true
                );

        when(classDefinitionLookupService.findByUuid(classDefinitionUuid)).thenReturn(Optional.of(snapshot));
        when(catalogItemRepository.findByClassDefinitionUuid(classDefinitionUuid)).thenReturn(List.of());
        when(productRepository.findByClassDefinitionUuid(classDefinitionUuid)).thenReturn(Optional.empty());
        when(variantRepository.findByCode(classDefinitionUuid.toString())).thenReturn(Optional.empty());

        when(productRepository.save(any(CommerceProduct.class))).thenAnswer(invocation -> {
            CommerceProduct saved = invocation.getArgument(0);
            saved.setUuid(UUID.randomUUID());
            return saved;
        });
        when(variantRepository.save(any(CommerceProductVariant.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(catalogItemRepository.save(any(CommerceCatalogueItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.ensureClassIsPurchasable(classDefinitionUuid);

        ArgumentCaptor<CommerceProduct> productCaptor = ArgumentCaptor.forClass(CommerceProduct.class);
        verify(productRepository).save(productCaptor.capture());
        CommerceProduct savedProduct = productCaptor.getValue();
        assertThat(savedProduct.getCourseUuid()).isNull();
        assertThat(savedProduct.getClassDefinitionUuid()).isEqualTo(classDefinitionUuid);
        assertThat(savedProduct.getProgramUuid()).isEqualTo(programUuid);

        ArgumentCaptor<CommerceCatalogueItem> itemCaptor = ArgumentCaptor.forClass(CommerceCatalogueItem.class);
        verify(catalogItemRepository).save(itemCaptor.capture());
        CommerceCatalogueItem savedItem = itemCaptor.getValue();
        assertThat(savedItem.getClassDefinitionUuid()).isEqualTo(classDefinitionUuid);
        assertThat(savedItem.getProgramUuid()).isEqualTo(programUuid);

        verify(productRepository, never()).findByCourseUuid(any());
    }
}
