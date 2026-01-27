package apps.sarafrika.elimika.commerce.internal.spi;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CommercePaymentQueryService {

    Page<CommercePaymentView> findPayments(
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            String status,
            Pageable pageable
    );

    Page<CommercePaymentView> findPaymentsByOrderUuids(
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            String status,
            List<UUID> orderUuids,
            Pageable pageable
    );
}
