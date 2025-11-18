package apps.sarafrika.elimika.commerce.internal.repository;

import apps.sarafrika.elimika.commerce.internal.entity.CommerceProductVariant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommerceProductVariantRepository extends JpaRepository<CommerceProductVariant, Long> {

    Optional<CommerceProductVariant> findByCode(String code);
}
