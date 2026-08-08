package apps.sarafrika.elimika.commerce.purchase.service.impl;

import apps.sarafrika.elimika.commerce.purchase.entity.CommercePurchase;
import apps.sarafrika.elimika.commerce.purchase.entity.CommercePurchaseItem;
import apps.sarafrika.elimika.commerce.purchase.repository.CommercePurchaseRepository;
import apps.sarafrika.elimika.commerce.purchase.service.CommerceAccessService;
import apps.sarafrika.elimika.commerce.purchase.spi.CommercePurchaseService;
import apps.sarafrika.elimika.shared.spi.revenue.PurchaseScope;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import apps.sarafrika.elimika.student.spi.StudentLookupService;
import apps.sarafrika.elimika.shared.dto.commerce.CartItemResponse;
import apps.sarafrika.elimika.shared.event.commerce.ClassPurchaseRecordedEvent;
import apps.sarafrika.elimika.shared.dto.commerce.CheckoutRequest;
import apps.sarafrika.elimika.shared.dto.commerce.OrderResponse;
import apps.sarafrika.elimika.shared.dto.commerce.PlatformFeeBreakdown;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommercePurchaseServiceImpl implements CommercePurchaseService {

    private final CommercePurchaseRepository purchaseRepository;
    private final UserLookupService userLookupService;
    private final StudentLookupService studentLookupService;
    private final ObjectMapper objectMapper;
    private final CommerceAccessService accessService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void recordOrder(OrderResponse order, CheckoutRequest checkoutRequest) {
        if (order == null) {
            return;
        }

        CommercePurchase purchase = purchaseRepository.findByOrderId(order.getId())
                .orElseGet(CommercePurchase::new);

        purchase.setOrderId(order.getId());
        purchase.setOrderNumber(order.getDisplayId());
        purchase.setPaymentStatus(order.getPaymentStatus());
        purchase.setOrderCurrencyCode(order.getCurrencyCode());
        purchase.setOrderSubtotalAmount(order.getSubtotal());
        purchase.setOrderTotalAmount(order.getTotal());
        purchase.setOrderCreatedAt(order.getCreatedAt());
        applyPlatformFee(purchase, order.getPlatformFee());

        // The order carries the buyer even when payment is captured asynchronously and there is no
        // checkout request to read an email from, which is the case for every M-Pesa capture.
        UUID buyerUserUuid = order.getUserUuid();
        if (checkoutRequest != null) {
            purchase.setCustomerEmail(checkoutRequest.getCustomerEmail());
            if (buyerUserUuid == null) {
                buyerUserUuid = userLookupService.findUserUuidByEmail(checkoutRequest.getCustomerEmail())
                        .orElse(null);
            }
        }
        if (buyerUserUuid != null) {
            purchase.setUserUuid(buyerUserUuid);
        }

        List<CartItemResponse> items = order.getItems();
        if (purchase.getItems() == null) {
            purchase.setItems(new ArrayList<>());
        }
        purchase.getItems().clear();

        if (!CollectionUtils.isEmpty(items)) {
            for (CartItemResponse item : items) {
                CommercePurchaseItem entity = buildPurchaseItem(purchase, item, checkoutRequest, buyerUserUuid);
                purchase.getItems().add(entity);
            }
        }

        CommercePurchase saved = purchaseRepository.save(purchase);
        publishClassPurchases(saved);
    }

    /**
     * Announces every class seat this order actually paid for. Access is re-checked through the same
     * service the enrolment paywall uses, so a seat is never announced that the paywall would refuse.
     */
    private void publishClassPurchases(CommercePurchase purchase) {
        if (CollectionUtils.isEmpty(purchase.getItems())) {
            return;
        }
        for (CommercePurchaseItem item : purchase.getItems()) {
            if (item.getScope() != PurchaseScope.CLASS
                    || item.getStudentUuid() == null
                    || item.getClassDefinitionUuid() == null) {
                continue;
            }
            if (!accessService.hasClassAccess(item.getStudentUuid(), item.getClassDefinitionUuid())) {
                continue;
            }
            eventPublisher.publishEvent(new ClassPurchaseRecordedEvent(
                    item.getStudentUuid(),
                    item.getClassDefinitionUuid(),
                    purchase.getOrderId()));
        }
    }

    private CommercePurchaseItem buildPurchaseItem(
            CommercePurchase purchase,
            CartItemResponse item,
            CheckoutRequest checkoutRequest,
            UUID buyerUserUuid
    ) {
        CommercePurchaseItem entity = new CommercePurchaseItem();
        entity.setPurchase(purchase);
        entity.setLineItemId(item.getId());
        entity.setVariantId(item.getVariantId());
        entity.setTitle(item.getTitle());
        entity.setQuantity(item.getQuantity());
        entity.setUnitPrice(item.getUnitPrice());
        entity.setSubtotal(item.getSubtotal());
        entity.setTotal(item.getTotal());

        Map<String, Object> metadata = item.getMetadata();
        if (!CollectionUtils.isEmpty(metadata)) {
            entity.setMetadataJson(writeMetadata(metadata));
            entity.setCourseUuid(parseUuid(metadata.get("course_uuid")));
            entity.setClassDefinitionUuid(parseUuid(metadata.get("class_definition_uuid")));
            entity.setStudentUuid(resolveStudentUuid(metadata, checkoutRequest, buyerUserUuid));
            entity.setScope(determineScope(metadata));
        } else {
            entity.setStudentUuid(resolveStudentUuid(Map.of(), checkoutRequest, buyerUserUuid));
        }
        return entity;
    }

    private void applyPlatformFee(CommercePurchase purchase, PlatformFeeBreakdown breakdown) {
        if (breakdown == null) {
            purchase.setPlatformFeeAmount(null);
            purchase.setPlatformFeeCurrency(null);
            purchase.setPlatformFeeRuleUuid(null);
            return;
        }
        purchase.setPlatformFeeAmount(breakdown.amount());
        purchase.setPlatformFeeCurrency(breakdown.currency());
        purchase.setPlatformFeeRuleUuid(breakdown.ruleUuid());
    }


    private UUID resolveStudentUuid(Map<String, Object> metadata, CheckoutRequest checkoutRequest, UUID buyerUserUuid) {
        // An explicit student on the line item wins: that is how an admin buys on someone's behalf.
        UUID metadataStudent = parseUuid(metadata.get("student_uuid"));
        if (metadataStudent != null) {
            return metadataStudent;
        }

        if (buyerUserUuid != null) {
            UUID student = studentLookupService.findStudentUuidByUserUuid(buyerUserUuid).orElse(null);
            if (student != null) {
                return student;
            }
        }

        String email = checkoutRequest != null ? checkoutRequest.getCustomerEmail() : null;
        if (!StringUtils.hasText(email)) {
            return null;
        }
        return userLookupService.findUserUuidByEmail(email)
                .flatMap(studentLookupService::findStudentUuidByUserUuid)
                .orElse(null);
    }

    private PurchaseScope determineScope(Map<String, Object> metadata) {
        if (metadata.containsKey("class_definition_uuid")) {
            return PurchaseScope.CLASS;
        }
        if (metadata.containsKey("course_uuid")) {
            return PurchaseScope.COURSE;
        }
        return null;
    }

    private UUID parseUuid(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof UUID uuid) {
            return uuid;
        }
        if (value instanceof String str && StringUtils.hasText(str)) {
            try {
                return UUID.fromString(str);
            } catch (IllegalArgumentException ex) {
                log.warn("Unable to parse UUID from value: {}", str, ex);
            }
        }
        return null;
    }

    private String writeMetadata(Map<String, Object> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialise line item metadata", ex);
            return null;
        }
    }
}
