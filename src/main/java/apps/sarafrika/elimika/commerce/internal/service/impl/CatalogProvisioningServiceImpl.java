package apps.sarafrika.elimika.commerce.internal.service.impl;

import apps.sarafrika.elimika.commerce.internal.config.InternalCommerceProperties;
import apps.sarafrika.elimika.commerce.internal.entity.CommerceProduct;
import apps.sarafrika.elimika.commerce.internal.entity.CommerceProductVariant;
import apps.sarafrika.elimika.commerce.internal.enums.ProductStatus;
import apps.sarafrika.elimika.commerce.internal.enums.VariantStatus;
import apps.sarafrika.elimika.commerce.internal.repository.CommerceProductRepository;
import apps.sarafrika.elimika.commerce.internal.repository.CommerceProductVariantRepository;
import apps.sarafrika.elimika.commerce.internal.service.CatalogProvisioningService;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CatalogProvisioningServiceImpl implements CatalogProvisioningService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

    private final CommerceProductRepository productRepository;
    private final CommerceProductVariantRepository variantRepository;
    private final ClassDefinitionLookupService classDefinitionLookupService;
    private final InternalCommerceProperties internalCommerceProperties;

    @Override
    public void ensureClassIsPurchasable(UUID classDefinitionUuid) {
        if (classDefinitionUuid == null) {
            return;
        }
        Optional<ClassDefinitionLookupService.ClassDefinitionSnapshot> snapshotOpt =
                classDefinitionLookupService.findByUuidWithoutCourse(classDefinitionUuid);
        if (snapshotOpt.isEmpty()) {
            log.warn("No class definition snapshot found for {}, skipping catalog provisioning", classDefinitionUuid);
            return;
        }
        ClassDefinitionLookupService.ClassDefinitionSnapshot snapshot = snapshotOpt.get();
        UUID courseUuid = snapshot.courseUuid();
        if (courseUuid == null) {
            log.warn("Class definition {} is not linked to a course; skipping catalog provisioning", classDefinitionUuid);
            return;
        }

        CommerceProduct product = productRepository.findByCourseUuid(courseUuid)
                .orElseGet(() -> createCourseProduct(courseUuid));

        variantRepository.findByCode(classDefinitionUuid.toString())
                .orElseGet(() -> createVariant(product, classDefinitionUuid, snapshot));
    }

    private CommerceProduct createCourseProduct(UUID courseUuid) {
        String title = "Course " + courseUuid;
        CommerceProduct product = new CommerceProduct();
        product.setCourseUuid(courseUuid);
        product.setTitle(title);
        product.setCurrencyCode(resolveCurrency());
        product.setStatus(ProductStatus.ACTIVE);
        product.setActive(true);
        CommerceProduct saved = productRepository.save(product);
        log.info("Provisioned commerce product for course {} with uuid {}", courseUuid, saved.getUuid());
        return saved;
    }

    private CommerceProductVariant createVariant(CommerceProduct product, UUID classDefinitionUuid,
                                                 ClassDefinitionLookupService.ClassDefinitionSnapshot snapshot) {
        CommerceProductVariant variant = new CommerceProductVariant();
        variant.setProduct(product);
        variant.setCode(classDefinitionUuid.toString());
        variant.setTitle(snapshot.title());
        variant.setCurrencyCode(resolveCurrency());
        variant.setUnitAmount(resolvePrice(snapshot));
        variant.setInventoryQuantity(0);
        variant.setStatus(VariantStatus.ACTIVE);
        CommerceProductVariant saved = variantRepository.save(variant);
        log.info("Provisioned commerce variant {} for class definition {}", saved.getUuid(), classDefinitionUuid);
        return saved;
    }

    private BigDecimal resolvePrice(ClassDefinitionLookupService.ClassDefinitionSnapshot snapshot) {
        if (snapshot.trainingFee() != null) {
            return snapshot.trainingFee().setScale(4, RoundingMode.HALF_UP);
        }
        return ZERO;
    }

    private String resolveCurrency() {
        String configured = internalCommerceProperties.getDefaultCurrency();
        return StringUtils.hasText(configured) ? configured.trim().toUpperCase() : "USD";
    }
}
