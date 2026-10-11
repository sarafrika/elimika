package apps.sarafrika.elimika.commerce.internal.repository;

import apps.sarafrika.elimika.commerce.internal.entity.CommerceProductVariant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommerceProductVariantRepository extends JpaRepository<CommerceProductVariant, Long> {

    Optional<CommerceProductVariant> findByCode(String code);

    List<CommerceProductVariant> findByCodeIn(Collection<String> codes);
}
