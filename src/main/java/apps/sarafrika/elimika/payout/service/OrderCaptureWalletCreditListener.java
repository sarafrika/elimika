package apps.sarafrika.elimika.payout.service;

import apps.sarafrika.elimika.course.spi.CourseInfoService;
import apps.sarafrika.elimika.course.spi.CourseInfoService.RevenueShare;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.shared.dto.commerce.CartItemResponse;
import apps.sarafrika.elimika.shared.dto.commerce.OrderResponse;
import apps.sarafrika.elimika.shared.event.commerce.OrderCompletedEvent;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService.ClassDefinitionSnapshot;
import apps.sarafrika.elimika.shared.spi.revenue.PurchaseScope;
import apps.sarafrika.elimika.wallet.service.WalletService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Credits earner wallets when a commerce order is captured.
 * <p>
 * For every purchased line item the earning party and revenue share are resolved by scope:
 * <ul>
 *     <li>{@link PurchaseScope#COURSE} &rarr; the course creator earns the creator share.</li>
 *     <li>{@link PurchaseScope#CLASS} &rarr; the class' default instructor earns the instructor share.</li>
 * </ul>
 * Crediting is idempotent per line item so replayed capture events never double-credit, and any
 * failure is logged without breaking the checkout flow.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCaptureWalletCreditListener {

    private static final String STATUS_CAPTURED = "CAPTURED";
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final WalletService walletService;
    private final CourseInfoService courseInfoService;
    private final InstructorLookupService instructorLookupService;
    private final ClassDefinitionLookupService classDefinitionLookupService;

    @EventListener
    public void handleOrderCompleted(OrderCompletedEvent event) {
        if (event == null || event.order() == null) {
            return;
        }
        OrderResponse order = event.order();
        if (!STATUS_CAPTURED.equalsIgnoreCase(order.getPaymentStatus())) {
            return;
        }
        if (CollectionUtils.isEmpty(order.getItems())) {
            return;
        }

        for (CartItemResponse item : order.getItems()) {
            try {
                creditItem(order, item);
            } catch (Exception ex) {
                log.error("Failed to credit wallet for order {} item {}: {}",
                        order.getId(), item.getId(), ex.getMessage(), ex);
            }
        }
    }

    private void creditItem(OrderResponse order, CartItemResponse item) {
        BigDecimal total = item.getTotal();
        if (total == null || total.signum() <= 0) {
            return;
        }

        Map<String, Object> metadata = item.getMetadata();
        UUID courseUuid = parseUuid(metadata, "course_uuid");
        UUID classDefinitionUuid = parseUuid(metadata, "class_definition_uuid");
        PurchaseScope scope = determineScope(courseUuid, classDefinitionUuid);
        if (scope == null) {
            log.debug("Skipping wallet credit for order {} item {}: no course/class scope",
                    order.getId(), item.getId());
            return;
        }

        Earning earning = switch (scope) {
            case CLASS -> resolveClassEarning(classDefinitionUuid, total);
            case COURSE -> resolveCourseEarning(courseUuid, total);
        };
        if (earning == null) {
            return;
        }

        String reference = buildReference(order, item);
        boolean credited = walletService.creditSaleIdempotent(
                earning.userUuid(),
                earning.amount(),
                order.getCurrencyCode(),
                reference,
                earning.description());
        if (credited) {
            log.info("Credited {} {} to user {} for {} sale on order {} (item {})",
                    earning.amount(), order.getCurrencyCode(), earning.userUuid(),
                    scope, order.getId(), item.getId());
        } else {
            log.debug("Wallet credit already applied for reference {}", reference);
        }
    }

    private Earning resolveCourseEarning(UUID courseUuid, BigDecimal total) {
        Optional<UUID> creatorUserUuid = courseInfoService.getCourseCreatorUserUuid(courseUuid);
        if (creatorUserUuid.isEmpty()) {
            log.warn("No course creator user resolved for course {}", courseUuid);
            return null;
        }
        BigDecimal share = courseInfoService.getRevenueShare(courseUuid)
                .map(RevenueShare::creatorSharePercentage)
                .orElse(null);
        BigDecimal amount = applyShare(total, share);
        if (amount == null) {
            return null;
        }
        return new Earning(creatorUserUuid.get(), amount,
                "Course sale earnings (course " + courseUuid + ")");
    }

    private Earning resolveClassEarning(UUID classDefinitionUuid, BigDecimal total) {
        Optional<ClassDefinitionSnapshot> snapshot =
                classDefinitionLookupService.findByUuid(classDefinitionUuid);
        if (snapshot.isEmpty() || snapshot.get().courseUuid() == null) {
            log.warn("No course resolved for class definition {}", classDefinitionUuid);
            return null;
        }
        UUID courseUuid = snapshot.get().courseUuid();

        UUID instructorUuid = classDefinitionLookupService
                .findDefaultInstructorUuid(classDefinitionUuid)
                .orElse(null);
        if (instructorUuid == null) {
            log.warn("No default instructor for class definition {}", classDefinitionUuid);
            return null;
        }
        Optional<UUID> instructorUserUuid = instructorLookupService.getInstructorUserUuid(instructorUuid);
        if (instructorUserUuid.isEmpty()) {
            log.warn("No user resolved for instructor {}", instructorUuid);
            return null;
        }

        BigDecimal share = courseInfoService.getRevenueShare(courseUuid)
                .map(RevenueShare::instructorSharePercentage)
                .orElse(null);
        BigDecimal amount = applyShare(total, share);
        if (amount == null) {
            return null;
        }
        return new Earning(instructorUserUuid.get(), amount,
                "Class sale earnings (class " + classDefinitionUuid + ")");
    }

    private BigDecimal applyShare(BigDecimal total, BigDecimal sharePercentage) {
        if (sharePercentage == null || sharePercentage.signum() <= 0) {
            return null;
        }
        BigDecimal amount = total.multiply(sharePercentage)
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);
        return amount.signum() > 0 ? amount : null;
    }

    private PurchaseScope determineScope(UUID courseUuid, UUID classDefinitionUuid) {
        if (classDefinitionUuid != null) {
            return PurchaseScope.CLASS;
        }
        if (courseUuid != null) {
            return PurchaseScope.COURSE;
        }
        return null;
    }

    private String buildReference(OrderResponse order, CartItemResponse item) {
        String lineItemId = StringUtils.hasText(item.getId()) ? item.getId() : item.getVariantId();
        return StringUtils.hasText(lineItemId)
                ? order.getId() + ":" + lineItemId
                : order.getId();
    }

    private UUID parseUuid(Map<String, Object> metadata, String key) {
        if (CollectionUtils.isEmpty(metadata)) {
            return null;
        }
        Object value = metadata.get(key);
        if (value instanceof UUID uuid) {
            return uuid;
        }
        if (value instanceof String str && StringUtils.hasText(str)) {
            try {
                return UUID.fromString(str);
            } catch (IllegalArgumentException ex) {
                log.warn("Unable to parse UUID from metadata {}={}", key, str);
            }
        }
        return null;
    }

    private record Earning(UUID userUuid, BigDecimal amount, String description) { }
}
