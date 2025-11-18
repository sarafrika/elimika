package apps.sarafrika.elimika.commerce.internal.repository;

import apps.sarafrika.elimika.commerce.internal.entity.CommerceProduct;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommerceProductRepository extends JpaRepository<CommerceProduct, Long> {

    Optional<CommerceProduct> findByCourseUuid(UUID courseUuid);

    Optional<CommerceProduct> findByClassDefinitionUuid(UUID classDefinitionUuid);
}
