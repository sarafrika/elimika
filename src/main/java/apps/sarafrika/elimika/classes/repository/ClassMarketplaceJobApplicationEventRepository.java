package apps.sarafrika.elimika.classes.repository;

import apps.sarafrika.elimika.classes.model.ClassMarketplaceJobApplicationEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClassMarketplaceJobApplicationEventRepository extends JpaRepository<ClassMarketplaceJobApplicationEvent, Long> {

    List<ClassMarketplaceJobApplicationEvent> findByApplicationUuidOrderByCreatedDateDescIdDesc(UUID applicationUuid);
}
