package apps.sarafrika.elimika.commerce.purchase.repository;

import apps.sarafrika.elimika.commerce.purchase.entity.CommercePurchaseItem;
import apps.sarafrika.elimika.commerce.purchase.enums.PurchaseScope;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommercePurchaseItemRepository extends JpaRepository<CommercePurchaseItem, Long> {

    List<CommercePurchaseItem> findByStudentUuidAndScopeAndCourseUuid(UUID studentUuid, PurchaseScope scope, UUID courseUuid);

    List<CommercePurchaseItem> findByStudentUuidAndScopeAndClassDefinitionUuid(UUID studentUuid, PurchaseScope scope, UUID classDefinitionUuid);

    long countByScope(PurchaseScope scope);

    long countByScopeAndCreatedDateAfter(PurchaseScope scope, LocalDateTime createdDate);
}
