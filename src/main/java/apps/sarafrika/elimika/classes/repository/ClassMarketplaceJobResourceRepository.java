package apps.sarafrika.elimika.classes.repository;

import apps.sarafrika.elimika.classes.model.ClassMarketplaceJobResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;
import java.util.UUID;

public interface ClassMarketplaceJobResourceRepository extends JpaRepository<ClassMarketplaceJobResource, Long> {

    List<ClassMarketplaceJobResource> findByJobUuidOrderByCreatedDateAsc(UUID jobUuid);

    @Modifying
    void deleteByJobUuid(UUID jobUuid);
}
