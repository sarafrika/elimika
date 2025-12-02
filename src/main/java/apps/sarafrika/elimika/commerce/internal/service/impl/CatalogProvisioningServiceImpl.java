package apps.sarafrika.elimika.commerce.internal.service.impl;

import apps.sarafrika.elimika.commerce.catalog.entity.CommerceCatalogItem;
import apps.sarafrika.elimika.commerce.catalog.repository.CommerceCatalogItemRepository;
import apps.sarafrika.elimika.commerce.internal.config.InternalCommerceProperties;
import apps.sarafrika.elimika.commerce.internal.entity.CommerceProduct;
import apps.sarafrika.elimika.commerce.internal.entity.CommerceProductVariant;
import apps.sarafrika.elimika.commerce.internal.enums.ProductStatus;
import apps.sarafrika.elimika.commerce.internal.enums.VariantStatus;
import apps.sarafrika.elimika.commerce.internal.repository.CommerceProductRepository;
import apps.sarafrika.elimika.commerce.internal.repository.CommerceProductVariantRepository;
import apps.sarafrika.elimika.commerce.internal.service.CatalogProvisioningService;
import apps.sarafrika.elimika.shared.enums.ClassVisibility;
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
    private final CommerceCatalogItemRepository catalogItemRepository;
    private final ClassDefinitionLookupService classDefinitionLookupService;
    private final InternalCommerceProperties internalCommerceProperties;

    @Override
    public void ensureClassIsPurchasable(UUID classDefinitionUuid) {
        if (classDefinitionUuid == null) {
            return;
        }
        Optional<ClassDefinitionLookupService.ClassDefinitionSnapshot> snapshotOpt =
                classDefinitionLookupService.findByUuid(classDefinitionUuid);
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

        Optional<CommerceCatalogItem> existingCatalogItem = catalogItemRepository.findByClassDefinitionUuid(classDefinitionUuid);
        CommerceProduct product = productRepository.findByCourseUuid(courseUuid)
                .orElseGet(() -> createCourseProduct(courseUuid, snapshot));

        CommerceProductVariant variant = resolveVariant(existingCatalogItem, product, classDefinitionUuid, snapshot);

        upsertCatalogItem(existingCatalogItem.orElse(null), product, variant, snapshot);
    }

    private CommerceProduct createCourseProduct(UUID courseUuid, ClassDefinitionLookupService.ClassDefinitionSnapshot snapshot) {
        String title = snapshot.title();
        CommerceProduct product = new CommerceProduct();
        product.setCourseUuid(courseUuid);
        product.setTitle(title);
        product.setDescription(snapshot.description());
        product.setCurrencyCode(resolveCurrency());
        product.setStatus(ProductStatus.ACTIVE);
        product.setActive(true);
        CommerceProduct saved = productRepository.save(product);
        log.info("Provisioned commerce product for course {} with uuid {}", courseUuid, saved.getUuid());
        return saved;
    }

    private CommerceProductVariant resolveVariant(Optional<CommerceCatalogItem> existingCatalogItem,
                                                  CommerceProduct product,
                                                  UUID classDefinitionUuid,
                                                  ClassDefinitionLookupService.ClassDefinitionSnapshot snapshot) {
        if (existingCatalogItem.isPresent() && StringUtils.hasText(existingCatalogItem.get().getVariantCode())) {
            Optional<CommerceProductVariant> existingVariant =
                    variantRepository.findByCode(existingCatalogItem.get().getVariantCode());
            if (existingVariant.isPresent()) {
                return existingVariant.get();
            }
        }

        Optional<CommerceProductVariant> legacyVariant = variantRepository.findByCode(classDefinitionUuid.toString());
        if (legacyVariant.isPresent()) {
            return legacyVariant.get();
        }

        return createVariant(product, classDefinitionUuid, snapshot);
    }

    private CommerceProductVariant createVariant(CommerceProduct product, UUID classDefinitionUuid,
                                                 ClassDefinitionLookupService.ClassDefinitionSnapshot snapshot) {
        CommerceProductVariant variant = new CommerceProductVariant();
        variant.setProduct(product);
        variant.setCode(generateVariantCode(snapshot.title(), classDefinitionUuid));
        variant.setTitle(snapshot.title());
        variant.setCurrencyCode(resolveCurrency());
        variant.setUnitAmount(resolvePrice(snapshot));
        variant.setInventoryQuantity(0);
        variant.setStatus(VariantStatus.ACTIVE);
        CommerceProductVariant saved = variantRepository.save(variant);
        log.info("Provisioned commerce variant {} for class definition {}", saved.getUuid(), classDefinitionUuid);
        return saved;
    }

    private void upsertCatalogItem(CommerceCatalogItem existingItem, CommerceProduct product, CommerceProductVariant variant,
                                   ClassDefinitionLookupService.ClassDefinitionSnapshot snapshot) {
        CommerceCatalogItem item = existingItem == null ? new CommerceCatalogItem() : existingItem;
        item.setCourseUuid(snapshot.courseUuid());
        item.setClassDefinitionUuid(snapshot.classDefinitionUuid());
        item.setProductCode(product.getUuid() == null ? null : product.getUuid().toString());
        item.setVariantCode(variant.getCode());
        item.setCurrencyCode(variant.getCurrencyCode());
        item.setActive(true);
        item.setPubliclyVisible(resolveVisibility(snapshot.classVisibility()));
        catalogItemRepository.save(item);
    }

    private BigDecimal resolvePrice(ClassDefinitionLookupService.ClassDefinitionSnapshot snapshot) {
        if (snapshot.trainingFee() != null) {
            return snapshot.trainingFee().setScale(4, RoundingMode.HALF_UP);
        }
        return ZERO;
    }

    private boolean resolveVisibility(ClassVisibility visibility) {
        return visibility == null || visibility == ClassVisibility.PUBLIC;
    }

    private String resolveCurrency() {
        String configured = internalCommerceProperties.getDefaultCurrency();
        return StringUtils.hasText(configured) ? configured.trim().toUpperCase() : "USD";
    }

    private String generateVariantCode(String title, UUID classDefinitionUuid) {
        String base = toSlug(title);
        String shortId = classDefinitionUuid == null ? "unknown" : classDefinitionUuid.toString().substring(0, 8);
        int maxBaseLength = Math.max(8, 48 - shortId.length());
        if (base.length() > maxBaseLength) {
            base = base.substring(0, maxBaseLength);
        }
        return base + "-" + shortId;
    }

    private String toSlug(String value) {
        if (!StringUtils.hasText(value)) {
            return "class";
        }
        String slug = value.trim().toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        if (!StringUtils.hasText(slug)) {
            return "class";
        }
        return slug;
    }
}
