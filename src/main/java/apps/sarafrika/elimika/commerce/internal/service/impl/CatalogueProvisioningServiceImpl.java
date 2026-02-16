package apps.sarafrika.elimika.commerce.internal.service.impl;

import apps.sarafrika.elimika.commerce.catalogue.entity.CommerceCatalogueItem;
import apps.sarafrika.elimika.commerce.catalogue.repository.CommerceCatalogueItemRepository;
import apps.sarafrika.elimika.commerce.internal.config.InternalCommerceProperties;
import apps.sarafrika.elimika.commerce.internal.entity.CommerceProduct;
import apps.sarafrika.elimika.commerce.internal.entity.CommerceProductVariant;
import apps.sarafrika.elimika.commerce.internal.enums.ProductStatus;
import apps.sarafrika.elimika.commerce.internal.enums.VariantStatus;
import apps.sarafrika.elimika.commerce.internal.repository.CommerceProductRepository;
import apps.sarafrika.elimika.commerce.internal.repository.CommerceProductVariantRepository;
import apps.sarafrika.elimika.commerce.internal.service.CatalogueProvisioningService;
import apps.sarafrika.elimika.shared.enums.ClassVisibility;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.math.BigInteger;
import java.util.List;
import java.util.Locale;
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
public class CatalogueProvisioningServiceImpl implements CatalogueProvisioningService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

    private final CommerceProductRepository productRepository;
    private final CommerceProductVariantRepository variantRepository;
    private final CommerceCatalogueItemRepository catalogItemRepository;
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
        UUID programUuid = snapshot.programUuid();
        if (courseUuid == null && programUuid == null) {
            log.warn("Class definition {} is not linked to a course or training program; skipping catalogue provisioning",
                    classDefinitionUuid);
            return;
        }

        List<CommerceCatalogueItem> existingCatalogItems = catalogItemRepository.findByClassDefinitionUuid(classDefinitionUuid);
        CommerceCatalogueItem existingCatalogItem = selectExistingCatalogueItem(existingCatalogItems);
        CommerceProduct product = resolveProduct(snapshot);

        CommerceProductVariant variant = resolveVariant(existingCatalogItem, product, classDefinitionUuid, snapshot);

        upsertCatalogItem(existingCatalogItem, product, variant, snapshot);
    }

    private CommerceProduct resolveProduct(ClassDefinitionLookupService.ClassDefinitionSnapshot snapshot) {
        UUID courseUuid = snapshot.courseUuid();
        if (courseUuid != null) {
            return productRepository.findByCourseUuid(courseUuid)
                    .orElseGet(() -> createCourseProduct(courseUuid, snapshot));
        }
        UUID classDefinitionUuid = snapshot.classDefinitionUuid();
        return productRepository.findByClassDefinitionUuid(classDefinitionUuid)
                .orElseGet(() -> createProgramClassProduct(classDefinitionUuid, snapshot));
    }

    private CommerceProduct createCourseProduct(UUID courseUuid, ClassDefinitionLookupService.ClassDefinitionSnapshot snapshot) {
        String title = snapshot.title();
        CommerceProduct product = new CommerceProduct();
        product.setCourseUuid(courseUuid);
        product.setProgramUuid(snapshot.programUuid());
        product.setTitle(title);
        product.setDescription(snapshot.description());
        product.setCurrencyCode(resolveCurrency());
        product.setStatus(ProductStatus.ACTIVE);
        product.setActive(true);
        CommerceProduct saved = productRepository.save(product);
        log.info("Provisioned commerce product for course {} with uuid {}", courseUuid, saved.getUuid());
        return saved;
    }

    private CommerceProduct createProgramClassProduct(
            UUID classDefinitionUuid,
            ClassDefinitionLookupService.ClassDefinitionSnapshot snapshot) {
        CommerceProduct product = new CommerceProduct();
        product.setClassDefinitionUuid(classDefinitionUuid);
        product.setProgramUuid(snapshot.programUuid());
        product.setTitle(snapshot.title());
        product.setDescription(snapshot.description());
        product.setCurrencyCode(resolveCurrency());
        product.setStatus(ProductStatus.ACTIVE);
        product.setActive(true);
        CommerceProduct saved = productRepository.save(product);
        log.info("Provisioned commerce product for program class {} with uuid {}", classDefinitionUuid, saved.getUuid());
        return saved;
    }

    private CommerceProductVariant resolveVariant(CommerceCatalogueItem existingCatalogItem,
                                                  CommerceProduct product,
                                                  UUID classDefinitionUuid,
                                                  ClassDefinitionLookupService.ClassDefinitionSnapshot snapshot) {
        if (existingCatalogItem != null && StringUtils.hasText(existingCatalogItem.getVariantCode())) {
            Optional<CommerceProductVariant> existingVariant =
                    variantRepository.findByCode(existingCatalogItem.getVariantCode());
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
        variant.setCode(generateVariantCode(product.getCourseUuid(), classDefinitionUuid));
        variant.setTitle(snapshot.title());
        variant.setCurrencyCode(resolveCurrency());
        variant.setUnitAmount(resolvePrice(snapshot));
        variant.setInventoryQuantity(0);
        variant.setStatus(VariantStatus.ACTIVE);
        CommerceProductVariant saved = variantRepository.save(variant);
        log.info("Provisioned commerce variant {} for class definition {}", saved.getUuid(), classDefinitionUuid);
        return saved;
    }

    private void upsertCatalogItem(CommerceCatalogueItem existingItem, CommerceProduct product, CommerceProductVariant variant,
                                   ClassDefinitionLookupService.ClassDefinitionSnapshot snapshot) {
        CommerceCatalogueItem item = existingItem == null ? new CommerceCatalogueItem() : existingItem;
        item.setCourseUuid(snapshot.courseUuid());
        item.setClassDefinitionUuid(snapshot.classDefinitionUuid());
        item.setProgramUuid(snapshot.programUuid());
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

    private CommerceCatalogueItem selectExistingCatalogueItem(List<CommerceCatalogueItem> items) {
        return items.stream()
                .filter(CommerceCatalogueItem::isActive)
                .findFirst()
                .orElse(items.stream().findFirst().orElse(null));
    }

    private String resolveCurrency() {
        String configured = internalCommerceProperties.getDefaultCurrency();
        if (!StringUtils.hasText(configured)) {
            throw new IllegalStateException("commerce.internal.default-currency must be configured");
        }
        return configured.trim().toUpperCase(Locale.ROOT);
    }

    private String generateVariantCode(UUID courseUuid, UUID classDefinitionUuid) {
        String typeSegment = classDefinitionUuid != null ? "CLS" : "CRS";
        UUID source = classDefinitionUuid != null ? classDefinitionUuid : courseUuid;
        String hexSeed = (source == null ? UUID.randomUUID() : source).toString().replace("-", "");

        BigInteger numericSeed = new BigInteger(hexSeed, 16);
        String base36 = numericSeed.toString(36).toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");

        StringBuilder body = new StringBuilder(base36);
        if (!containsDigit(body)) {
            body.append('1');
        }
        if (!containsLetter(body)) {
            body.append('A');
        }
        while (body.length() < 8) {
            body.append('0');
        }
        int maxLength = 16;
        if (body.length() > maxLength) {
            body.setLength(maxLength);
        }

        return "SKU-" + typeSegment + "-" + body;
    }

    private boolean containsDigit(CharSequence value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isDigit(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private boolean containsLetter(CharSequence value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isLetter(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }
}
