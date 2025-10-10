package apps.sarafrika.elimika.commerce.purchase.service.impl;

import apps.sarafrika.elimika.commerce.cart.dto.CartItemResponse;
import apps.sarafrika.elimika.commerce.order.dto.CheckoutRequest;
import apps.sarafrika.elimika.commerce.order.dto.OrderResponse;
import apps.sarafrika.elimika.commerce.purchase.entity.CommercePurchase;
import apps.sarafrika.elimika.commerce.purchase.entity.CommercePurchaseItem;
import apps.sarafrika.elimika.commerce.purchase.enums.PurchaseScope;
import apps.sarafrika.elimika.commerce.purchase.repository.CommercePurchaseRepository;
import apps.sarafrika.elimika.commerce.purchase.service.CommercePurchaseService;
import apps.sarafrika.elimika.tenancy.entity.User;
import apps.sarafrika.elimika.tenancy.repository.UserRepository;
import apps.sarafrika.elimika.student.model.Student;
import apps.sarafrika.elimika.student.repository.StudentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommercePurchaseServiceImpl implements CommercePurchaseService {

    private final CommercePurchaseRepository purchaseRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void recordOrder(OrderResponse order, CheckoutRequest checkoutRequest) {
        if (order == null) {
            return;
        }

        CommercePurchase purchase = purchaseRepository.findByMedusaOrderId(order.getId())
                .orElseGet(CommercePurchase::new);

        purchase.setMedusaOrderId(order.getId());
        purchase.setMedusaDisplayId(order.getDisplayId());
        purchase.setPaymentStatus(order.getPaymentStatus());
        purchase.setMedusaCreatedAt(order.getCreatedAt());

        if (checkoutRequest != null) {
            purchase.setCustomerEmail(checkoutRequest.getCustomerEmail());
            resolveUser(checkoutRequest.getCustomerEmail()).ifPresent(user -> purchase.setUserUuid(user.getUuid()));
        }

        List<CartItemResponse> items = order.getItems();
        if (purchase.getItems() == null) {
            purchase.setItems(new ArrayList<>());
        }
        purchase.getItems().clear();

        if (!CollectionUtils.isEmpty(items)) {
            for (CartItemResponse item : items) {
                CommercePurchaseItem entity = buildPurchaseItem(purchase, item, checkoutRequest);
                purchase.getItems().add(entity);
            }
        }

        purchaseRepository.save(purchase);
    }

    private CommercePurchaseItem buildPurchaseItem(
            CommercePurchase purchase,
            CartItemResponse item,
            CheckoutRequest checkoutRequest
    ) {
        CommercePurchaseItem entity = new CommercePurchaseItem();
        entity.setPurchase(purchase);
        entity.setMedusaLineItemId(item.getId());
        entity.setVariantId(item.getVariantId());
        entity.setTitle(item.getTitle());
        entity.setQuantity(item.getQuantity());

        Map<String, Object> metadata = item.getMetadata();
        if (!CollectionUtils.isEmpty(metadata)) {
            entity.setMetadataJson(writeMetadata(metadata));
            entity.setCourseUuid(parseUuid(metadata.get("course_uuid")));
            entity.setClassDefinitionUuid(parseUuid(metadata.get("class_definition_uuid")));
            entity.setStudentUuid(resolveStudentUuid(metadata, checkoutRequest));
            entity.setScope(determineScope(metadata));
        } else {
            entity.setStudentUuid(resolveStudentUuid(Map.of(), checkoutRequest));
        }
        return entity;
    }

    private Optional<User> resolveUser(String email) {
        if (!StringUtils.hasText(email)) {
            return Optional.empty();
        }
        return userRepository.findByEmail(email);
    }

    private UUID resolveStudentUuid(Map<String, Object> metadata, CheckoutRequest checkoutRequest) {
        UUID metadataStudent = parseUuid(metadata.get("student_uuid"));
        if (metadataStudent != null) {
            return metadataStudent;
        }

        String email = checkoutRequest != null ? checkoutRequest.getCustomerEmail() : null;
        if (!StringUtils.hasText(email)) {
            return null;
        }
        return resolveUser(email)
                .flatMap(user -> studentRepository.findByUserUuid(user.getUuid()))
                .map(Student::getUuid)
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
