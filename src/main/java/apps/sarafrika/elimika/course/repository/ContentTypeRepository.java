package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.ContentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContentTypeRepository extends JpaRepository<ContentType, Long>, JpaSpecificationExecutor<ContentType> {
    Optional<ContentType> findByUuid(UUID uuid);

    Optional<ContentType> findByName(String name);

    void deleteByUuid(UUID uuid);
}