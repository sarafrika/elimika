package apps.sarafrika.elimika.shared.repository;

import apps.sarafrika.elimika.shared.model.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DocumentTypeRepository extends JpaRepository<DocumentType, Long> {

    Optional<DocumentType> findByUuid(UUID uuid);

    Optional<DocumentType> findByNameIgnoreCase(String name);
}
