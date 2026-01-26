package apps.sarafrika.elimika.commerce.purchase.spi;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface CommerceRevenueQueryService {

    List<CommerceRevenueLineItem> findCapturedRevenueLines(OffsetDateTime startDate, OffsetDateTime endDate);

    List<CommerceRevenueLineItem> findCapturedRevenueLinesByCourseUuids(
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            List<UUID> courseUuids
    );

    List<CommerceRevenueLineItem> findCapturedRevenueLinesByClassDefinitionUuids(
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            List<UUID> classDefinitionUuids
    );

    List<CommerceRevenueLineItem> findCapturedRevenueLinesByStudentUuids(
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            List<UUID> studentUuids
    );
}
