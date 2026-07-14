package apps.sarafrika.elimika.shared.storage.repository;

import apps.sarafrika.elimika.shared.storage.model.MediaFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MediaFileRepository extends JpaRepository<MediaFile, Long> {

    Optional<MediaFile> findByFileKey(String fileKey);

    List<MediaFile> findByOwnerTypeAndOwnerUuid(String ownerType, UUID ownerUuid);

    boolean existsByFileKey(String fileKey);

    void deleteByFileKey(String fileKey);
}
