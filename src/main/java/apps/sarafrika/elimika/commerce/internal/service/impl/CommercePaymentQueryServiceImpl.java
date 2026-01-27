package apps.sarafrika.elimika.commerce.internal.service.impl;

import apps.sarafrika.elimika.commerce.internal.entity.CommerceOrder;
import apps.sarafrika.elimika.commerce.internal.entity.CommercePayment;
import apps.sarafrika.elimika.commerce.internal.enums.PaymentStatus;
import apps.sarafrika.elimika.commerce.internal.repository.CommercePaymentRepository;
import apps.sarafrika.elimika.commerce.internal.spi.CommercePaymentQueryService;
import apps.sarafrika.elimika.commerce.internal.spi.CommercePaymentView;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CommercePaymentQueryServiceImpl implements CommercePaymentQueryService {

    private final CommercePaymentRepository paymentRepository;

    @Override
    public Page<CommercePaymentView> findPayments(
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            String status,
            Pageable pageable
    ) {
        LocalDateTime start = toUtcLocalDateTime(startDate);
        LocalDateTime end = toUtcLocalDateTime(endDate);
        PaymentStatus resolvedStatus = resolveStatus(status);

        Page<CommercePayment> payments = resolvedStatus == null
                ? paymentRepository.findByProcessedAtBetween(start, end, pageable)
                : paymentRepository.findByProcessedAtBetweenAndStatus(start, end, resolvedStatus, pageable);

        return payments.map(this::toView);
    }

    @Override
    public Page<CommercePaymentView> findPaymentsByOrderUuids(
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            String status,
            List<UUID> orderUuids,
            Pageable pageable
    ) {
        if (orderUuids == null || orderUuids.isEmpty()) {
            return Page.empty(pageable);
        }
        LocalDateTime start = toUtcLocalDateTime(startDate);
        LocalDateTime end = toUtcLocalDateTime(endDate);
        PaymentStatus resolvedStatus = resolveStatus(status);

        Page<CommercePayment> payments = resolvedStatus == null
                ? paymentRepository.findByOrderUuids(orderUuids, start, end, pageable)
                : paymentRepository.findByOrderUuidsAndStatus(orderUuids, start, end, resolvedStatus, pageable);

        return payments.map(this::toView);
    }

    private CommercePaymentView toView(CommercePayment payment) {
        if (payment == null) {
            return null;
        }
        CommerceOrder order = payment.getOrder();
        return new CommercePaymentView(
                payment.getUuid(),
                order != null ? order.getUuid() : null,
                order != null ? order.getTotalAmount() : null,
                order != null ? order.getCurrencyCode() : null,
                payment.getProvider(),
                payment.getStatus() != null ? payment.getStatus().name() : null,
                payment.getAmount(),
                payment.getCurrencyCode(),
                payment.getExternalReference(),
                payment.getProcessedAt()
        );
    }

    private LocalDateTime toUtcLocalDateTime(OffsetDateTime timestamp) {
        if (timestamp == null) {
            return null;
        }
        return timestamp.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    private PaymentStatus resolveStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        try {
            return PaymentStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
