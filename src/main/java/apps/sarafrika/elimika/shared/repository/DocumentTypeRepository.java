package apps.sarafrika.elimika.shared.repository;

import apps.sarafrika.elimika.shared.model.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DocumentTypeRepository extends JpaRepository<DocumentType, Long> {

    Optional<DocumentType> findByNameIgnoreCase(String name);
}
