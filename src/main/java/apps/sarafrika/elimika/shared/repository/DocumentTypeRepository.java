package apps.sarafrika.elimika.shared.repository;

import apps.sarafrika.elimika.shared.model.DocumentType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentTypeRepository extends JpaRepository<DocumentType, Long> {

    Optional<DocumentType> findByUuid(UUID uuid);

    List<DocumentType> findByAppliesToIgnoreCase(String appliesTo, Sort sort);

    Optional<DocumentType> findByNameIgnoreCase(String name);
}
